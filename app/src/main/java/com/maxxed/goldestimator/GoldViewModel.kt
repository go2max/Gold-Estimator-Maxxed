package com.maxxed.goldestimator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { HOME, CAMERA, REFERENCE, CORRECT, RESULTS, HISTORY, COMPARE, SAFETY }

data class GoldUiState(
    val screen: Screen = Screen.HOME,
    val captures: List<File> = emptyList(),
    val qualities: List<CaptureQuality> = emptyList(),
    val poses: List<CapturePose> = emptyList(),
    val error: String? = null,
    val referenceMm: Double? = null,
    val referencePixels: Double? = null,
    val knownWeightG: Double? = null,
    val maskSensitivity: Double = 0.5,
    val fieldContext: FieldContext = FieldContext(),
    val lastFieldContext: FieldContext = FieldContext(),
    val samples: List<PixelSample> = emptyList(),
    val originalPixelArea: Double? = null,
    val subjectPixelArea: Double? = null,
    val clusters: List<VisualCluster> = emptyList(),
    val assignments: Map<Int, Material> = emptyMap(),
    val current: BatchResult? = null,
    val saved: List<BatchResult> = emptyList(),
    val compareIds: Set<String> = emptySet(),
    val busy: Boolean = false
)

class GoldViewModel(application: Application) : AndroidViewModel(application) {
    private val store = BatchStore(application)
    var state = androidx.compose.runtime.mutableStateOf(GoldUiState(saved = store.load(), lastFieldContext = store.loadLastFieldContext()))
        private set

    fun navigate(screen: Screen) { state.value = state.value.copy(screen = screen, error = null) }
    fun clearError() { state.value = state.value.copy(error = null) }

    fun newBatch() {
        state.value.captures.forEach { it.delete() }
        state.value = GoldUiState(screen = Screen.CAMERA, saved = state.value.saved, lastFieldContext = state.value.lastFieldContext)
    }

    fun acceptCapture(file: File, quality: CaptureQuality, pose: CapturePose) {
        val current = state.value
        if (!quality.accepted) {
            file.delete(); state.value = current.copy(error = quality.issues.joinToString(". ")); return
        }
        if (current.qualities.any { VisualAnalysis.fingerprintDistance(it, quality) < 0.10 }) {
            file.delete(); state.value = current.copy(error = "This view is too similar to a previous angle. Move around the sample."); return
        }
        if (current.poses.any { poseDistance(it, pose) < 12.0 }) {
            file.delete(); state.value = current.copy(error = "The phone orientation is too close to an accepted view. Move to the guided angle."); return
        }
        val captures = current.captures + file
        val qualities = current.qualities + quality
        state.value = current.copy(captures = captures, qualities = qualities, poses = current.poses + pose, error = null, screen = if (captures.size >= ANGLES.size) Screen.REFERENCE else Screen.CAMERA)
    }

    fun removeLastCapture() {
        val current = state.value
        current.captures.lastOrNull()?.delete()
        state.value = current.copy(captures = current.captures.dropLast(1), qualities = current.qualities.dropLast(1), poses = current.poses.dropLast(1), error = null)
    }

    fun setReference(referenceMm: Double?, referencePixels: Double?, weightG: Double?, fieldContext: FieldContext = FieldContext()) {
        if (referenceMm == null || referencePixels == null || referenceMm <= 0 || referencePixels <= 0) {
            state.value = state.value.copy(error = "Measure a visible known-size reference before analysis."); return
        }
        val normalizedContext = fieldContext.normalized()
        store.saveLastFieldContext(normalizedContext)
        state.value = state.value.copy(referenceMm = referenceMm, referencePixels = referencePixels, knownWeightG = weightG?.takeIf { it > 0 }, fieldContext = normalizedContext, lastFieldContext = normalizedContext, error = null)
        analyze()
    }

    fun updateMask(sensitivity: Double) {
        state.value = state.value.copy(maskSensitivity = sensitivity)
        recomputeClusters()
    }

    fun assign(clusterId: Int, material: Material) {
        state.value = state.value.copy(assignments = state.value.assignments + (clusterId to material))
    }

    fun calculate(name: String) {
        val current = state.value
        val pixelsPerMm = current.referencePixels?.div(current.referenceMm ?: 1.0)
        val quality = confidence(current.qualities)
        val estimates = VisualAnalysis.estimates(
            current.clusters,
            current.assignments,
            quality,
            current.knownWeightG,
            pixelsPerMm,
            current.subjectPixelArea,
            current.fieldContext.goldPricePerGram,
            current.fieldContext.recoverableFraction
        )
        val warnings = buildList {
            add("Visual estimate only; this is not a chemical assay.")
            if (current.knownWeightG == null) add("Weight uses scale-derived volume and a broad depth range.")
            if (current.fieldContext.goldPricePerGram == null) add("Enter a current gold price per gram to estimate field value.")
            if (quality < 0.65) add("Capture conditions reduce confidence.")
            if (current.clusters.any { (current.assignments[it.id] ?: it.suggested) == Material.GOLD && it.confidence < 0.65 }) add("Gold-colored material may be pyrite, mica, staining, or another look-alike.")
        }
        val result = BatchResult(name = name.ifBlank { suggestedBatchName(current.fieldContext, current.saved.size + 1) }, angleCount = current.captures.size,
            referenceMm = current.referenceMm, pixelsPerMm = pixelsPerMm, knownTotalWeightG = current.knownWeightG,
            maskSensitivity = current.maskSensitivity, fieldContext = current.fieldContext, clusters = current.clusters, assignments = current.assignments,
            estimates = estimates, overallConfidence = quality * current.clusters.map { it.confidence }.average().coerceAtLeast(0.2), warnings = warnings)
        state.value = current.copy(current = result, screen = Screen.RESULTS)
    }

    fun saveCurrent() {
        val result = state.value.current ?: return
        val saved = listOf(result) + state.value.saved.filterNot { it.id == result.id }
        store.save(saved); state.value = state.value.copy(saved = saved)
    }

    fun delete(id: String) { val saved = state.value.saved.filterNot { it.id == id }; store.save(saved); state.value = state.value.copy(saved = saved, compareIds = state.value.compareIds - id) }
    fun toggleCompare(id: String) { val ids = state.value.compareIds; state.value = state.value.copy(compareIds = if (id in ids) ids - id else if (ids.size < 2) ids + id else ids) }
    fun openBatch(batch: BatchResult) { state.value = state.value.copy(current = batch, screen = Screen.RESULTS) }

    fun csv(items: List<BatchResult>): String = buildString {
        appendLine("batch,created,sample_type,site_or_batch,gold_price_per_g,recovery_pct,material,visible_share_low_pct,visible_share_high_pct,density_low_g_cm3,density_high_g_cm3,volume_low_cm3,volume_high_cm3,weight_low_g,weight_high_g,recoverable_gold_low_g,recoverable_gold_high_g,recoverable_gold_low_ozt,recoverable_gold_high_ozt,recoverable_gold_low_dwt,recoverable_gold_high_dwt,value_low,value_high,confidence,note")
        items.forEach { batch -> batch.estimates.forEach { estimate ->
            append(csvCell(batch.name)); append(','); append(batch.createdAt); append(',')
            append(csvCell(batch.fieldContext.sampleType.label)); append(','); append(csvCell(batch.fieldContext.siteLabel)); append(',')
            append(format(batch.fieldContext.goldPricePerGram)); append(','); append(format(batch.fieldContext.recoveryPercent)); append(',')
            append(csvCell(estimate.material.label)); append(',')
            append(format(estimate.visibleShareLow * 100)); append(','); append(format(estimate.visibleShareHigh * 100)); append(',')
            append(format(estimate.densityLow)); append(','); append(format(estimate.densityHigh)); append(',')
            append(format(estimate.volumeLowCm3)); append(','); append(format(estimate.volumeHighCm3)); append(',')
            append(format(estimate.weightLowG)); append(','); append(format(estimate.weightHighG)); append(',')
            append(format(estimate.recoverableWeightLowG)); append(','); append(format(estimate.recoverableWeightHighG)); append(',')
            append(format(estimate.recoverableWeightLowG?.div(GRAMS_PER_TROY_OUNCE))); append(','); append(format(estimate.recoverableWeightHighG?.div(GRAMS_PER_TROY_OUNCE))); append(',')
            append(format(estimate.recoverableWeightLowG?.div(GRAMS_PER_PENNYWEIGHT))); append(','); append(format(estimate.recoverableWeightHighG?.div(GRAMS_PER_PENNYWEIGHT))); append(',')
            append(format(estimate.valueLow)); append(','); append(format(estimate.valueHigh)); append(',')
            append(format(estimate.confidence)); append(',')
            appendLine(csvCell("Visual estimate only; not a chemical assay"))
        } }
    }

    fun summary(batch: BatchResult): String = buildString {
        appendLine("${batch.name} - visual estimate only (not a chemical assay)")
        appendLine("${batch.fieldContext.sampleType.label}: ${batch.fieldContext.displayLabel}; recovery ${batch.fieldContext.recoveryPercent.oneDecimal()}%")
        batch.estimates.firstOrNull { it.material == Material.GOLD }?.let { gold ->
            appendLine("Gold: ${range(gold.weightLowG, gold.weightHighG, "g")} visual; recoverable ${range(gold.recoverableWeightLowG, gold.recoverableWeightHighG, "g")}")
            if (gold.valueLow != null && gold.valueHigh != null) appendLine("Estimated recoverable gold value: ${moneyRange(gold.valueLow, gold.valueHigh)}")
        }
        batch.estimates.filterNot { it.material == Material.GOLD }.forEach { appendLine("${it.material.label}: ${(it.visibleShareLow * 100).oneDecimal()}-${(it.visibleShareHigh * 100).oneDecimal()}% visible; ${range(it.weightLowG, it.weightHighG, "g")}") }
        append("Confidence: ${(batch.overallConfidence * 100).toInt()}%")
    }

    private fun analyze() {
        state.value = state.value.copy(busy = true)
        runCatching {
            state.value.captures.map { BitmapPipeline.decodeSamples(it) }
        }.onSuccess { decoded ->
            val originalArea = decoded.map { it.second[0].toDouble() * it.second[1] }.average()
            state.value = state.value.copy(samples = decoded.flatMap { it.first }, originalPixelArea = originalArea, busy = false)
            recomputeClusters()
            state.value = state.value.copy(screen = Screen.CORRECT)
        }.onFailure { state.value = state.value.copy(busy = false, error = "Could not analyze captures: ${it.message}") }
    }

    private fun recomputeClusters() {
        val current = state.value
        val subject = VisualAnalysis.subjectSamples(current.samples, current.maskSensitivity)
        val clusters = VisualAnalysis.cluster(subject)
        val foregroundFraction = subject.size.toDouble() / current.samples.size.coerceAtLeast(1)
        state.value = current.copy(subjectPixelArea = current.originalPixelArea?.times(foregroundFraction), clusters = clusters,
            assignments = clusters.associate { it.id to (current.assignments[it.id] ?: it.suggested) })
    }

    companion object {
        val ANGLES = listOf("Top", "Front", "Right side", "Back", "Left side", "Low oblique")
        private const val GRAMS_PER_TROY_OUNCE = 31.1034768
        private const val GRAMS_PER_PENNYWEIGHT = 1.55517384
        fun confidence(qualities: List<CaptureQuality>): Double {
            if (qualities.isEmpty()) return 0.0
            return qualities.map { q ->
                val exposure = (1.0 - kotlin.math.abs(q.meanLight - 128) / 128).coerceIn(0.0, 1.0)
                (exposure * 0.30 + (q.contrast / 60).coerceIn(0.0, 1.0) * 0.20 + (q.sharpness / 35).coerceIn(0.0, 1.0) * 0.25 + (1 - q.glareFraction * 3).coerceIn(0.0, 1.0) * 0.10 + (q.subjectPresence / 70).coerceIn(0.0, 1.0) * 0.15)
            }.average()
        }
        fun poseDistance(a: CapturePose, b: CapturePose): Double {
            val az = kotlin.math.abs(a.azimuthDegrees - b.azimuthDegrees).let { kotlin.math.min(it, 360.0 - it) }
            val pitch = kotlin.math.abs(a.pitchDegrees - b.pitchDegrees)
            val roll = kotlin.math.abs(a.rollDegrees - b.rollDegrees)
            return kotlin.math.sqrt(az * az + pitch * pitch + roll * roll * 0.25)
        }
        fun range(low: Double?, high: Double?, unit: String) = if (low == null || high == null) "Not available" else "${low.oneDecimal()}-${high.oneDecimal()} $unit"
        fun troyOunceRange(lowG: Double?, highG: Double?) = convertedRange(lowG, highG, GRAMS_PER_TROY_OUNCE, "oz t")
        fun pennyweightRange(lowG: Double?, highG: Double?) = convertedRange(lowG, highG, GRAMS_PER_PENNYWEIGHT, "dwt")
        fun moneyRange(low: Double?, high: Double?) = if (low == null || high == null) "Not available" else "\$${low.oneDecimal()}-\$${high.oneDecimal()}"
        fun suggestedBatchName(fieldContext: FieldContext, fallbackNumber: Int): String {
            val date = SimpleDateFormat("MMM d", Locale.US).format(Date())
            val label = fieldContext.siteLabel.ifBlank { fieldContext.sampleType.label }
            return "$label - $date".take(60).ifBlank { "Batch $fallbackNumber" }
        }
        private fun convertedRange(lowG: Double?, highG: Double?, gramsPerUnit: Double, unit: String): String =
            if (lowG == null || highG == null) "Not available" else "${(lowG / gramsPerUnit).oneDecimal()}-${(highG / gramsPerUnit).oneDecimal()} $unit"
        private fun format(value: Double?) = value?.let { String.format(Locale.US, "%.4f", it) } ?: ""
        private fun csvCell(value: String) = "\"${value.replace("\"", "\"\"")}\""
    }
}
