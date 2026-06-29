package com.maxxed.goldestimator

import java.util.UUID
import kotlin.math.roundToInt

enum class Material(val label: String, val densityLow: Double, val densityHigh: Double) {
    GOLD("Gold", 15.0, 19.3),
    QUARTZ("Quartz", 2.60, 2.70),
    PYRITE_MICA("Pyrite / mica", 2.80, 5.10),
    BLACK_SAND("Black sand", 4.00, 5.20),
    DIRT_CLAY("Dirt / clay", 1.20, 2.20),
    UNKNOWN("Unknown", 1.50, 6.00)
}

enum class SampleType(val label: String) {
    PAN_CONCENTRATE("Pan concentrate"),
    SLUICE_CONCENTRATE("Sluice concentrate"),
    PAYDIRT("Paydirt sample"),
    HARD_ROCK("Hard-rock specimen"),
    TAILINGS("Tailings / cleanup"),
    UNKNOWN("Field sample")
}

data class FieldContext(
    val sampleType: SampleType = SampleType.PAN_CONCENTRATE,
    val siteLabel: String = "",
    val goldPricePerGram: Double? = null,
    val recoveryPercent: Double = 85.0
) {
    val recoverableFraction: Double get() = (recoveryPercent / 100.0).coerceIn(0.0, 1.0)
    val displayLabel: String get() = siteLabel.ifBlank { sampleType.label }
    fun normalized(): FieldContext = copy(
        siteLabel = siteLabel.trim(),
        goldPricePerGram = goldPricePerGram?.takeIf { it > 0 },
        recoveryPercent = recoveryPercent.coerceIn(1.0, 100.0)
    )
}

data class CaptureQuality(
    val meanLight: Double,
    val contrast: Double,
    val sharpness: Double,
    val glareFraction: Double,
    val subjectPresence: Double,
    val fingerprint: List<Double>
) {
    val issues: List<String>
        get() = buildList {
            if (meanLight < 42) add("Too dark")
            if (meanLight > 224) add("Too bright")
            if (contrast < 20) add("Low contrast")
            if (sharpness < 10) add("Hold steady and refocus")
            if (glareFraction > 0.16) add("Excess glare")
            if (subjectPresence < 22) add("No clear sample detected in the guide")
        }
    val accepted: Boolean get() = issues.isEmpty()
}

data class CapturePose(val azimuthDegrees: Double, val pitchDegrees: Double, val rollDegrees: Double, val capturedAt: Long = System.currentTimeMillis())

data class PixelSample(val r: Int, val g: Int, val b: Int, val x: Int = 0, val y: Int = 0)

data class VisualCluster(
    val id: Int,
    val share: Double,
    val meanR: Int,
    val meanG: Int,
    val meanB: Int,
    val suggested: Material,
    val confidence: Double
)

data class MaterialEstimate(
    val material: Material,
    val visibleShareLow: Double,
    val visibleShareHigh: Double,
    val densityLow: Double,
    val densityHigh: Double,
    val volumeLowCm3: Double?,
    val volumeHighCm3: Double?,
    val weightLowG: Double?,
    val weightHighG: Double?,
    val confidence: Double,
    val recoverableWeightLowG: Double? = null,
    val recoverableWeightHighG: Double? = null,
    val valueLow: Double? = null,
    val valueHigh: Double? = null
)

data class BatchResult(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val angleCount: Int,
    val referenceMm: Double?,
    val pixelsPerMm: Double?,
    val knownTotalWeightG: Double?,
    val maskSensitivity: Double,
    val clusters: List<VisualCluster>,
    val assignments: Map<Int, Material>,
    val estimates: List<MaterialEstimate>,
    val overallConfidence: Double,
    val warnings: List<String>,
    val fieldContext: FieldContext = FieldContext()
)

fun Double.percentText(): String = "${(this * 100).roundToInt()}%"
fun Double.oneDecimal(): String = String.format(java.util.Locale.US, "%.1f", this)
