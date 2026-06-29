package com.maxxed.goldestimator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BatchStore(context: Context) {
    private val prefs = context.getSharedPreferences("gold_batches", Context.MODE_PRIVATE)

    fun load(): List<BatchResult> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]"))
        List(array.length()) { decode(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun save(items: List<BatchResult>) {
        val array = JSONArray().apply { items.forEach { put(encode(it)) } }
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun loadLastFieldContext(): FieldContext = runCatching {
        prefs.getString("lastFieldContext", null)?.let { decodeFieldContext(JSONObject(it)) } ?: FieldContext()
    }.getOrDefault(FieldContext())

    fun saveLastFieldContext(fieldContext: FieldContext) {
        prefs.edit().putString("lastFieldContext", encodeFieldContext(fieldContext.normalized()).toString()).apply()
    }

    private fun encode(item: BatchResult) = JSONObject().apply {
        put("id", item.id); put("name", item.name); put("createdAt", item.createdAt)
        put("angleCount", item.angleCount); put("referenceMm", item.referenceMm)
        put("pixelsPerMm", item.pixelsPerMm); put("knownTotalWeightG", item.knownTotalWeightG)
        put("maskSensitivity", item.maskSensitivity); put("overallConfidence", item.overallConfidence)
        put("fieldContext", encodeFieldContext(item.fieldContext))
        put("warnings", JSONArray(item.warnings))
        put("clusters", JSONArray().apply { item.clusters.forEach { cluster -> put(JSONObject().apply {
            put("id", cluster.id); put("share", cluster.share); put("r", cluster.meanR); put("g", cluster.meanG); put("b", cluster.meanB)
            put("suggested", cluster.suggested.name); put("confidence", cluster.confidence)
        }) } })
        put("assignments", JSONObject().apply { item.assignments.forEach { (id, material) -> put(id.toString(), material.name) } })
        put("estimates", JSONArray().apply { item.estimates.forEach { estimate -> put(JSONObject().apply {
            put("material", estimate.material.name); put("shareLow", estimate.visibleShareLow); put("shareHigh", estimate.visibleShareHigh)
            put("densityLow", estimate.densityLow); put("densityHigh", estimate.densityHigh)
            put("volumeLow", estimate.volumeLowCm3); put("volumeHigh", estimate.volumeHighCm3)
            put("weightLow", estimate.weightLowG); put("weightHigh", estimate.weightHighG); put("confidence", estimate.confidence)
            put("recoverableWeightLow", estimate.recoverableWeightLowG); put("recoverableWeightHigh", estimate.recoverableWeightHighG)
            put("valueLow", estimate.valueLow); put("valueHigh", estimate.valueHigh)
        }) } })
    }

    private fun decode(json: JSONObject): BatchResult {
        val clustersJson = json.getJSONArray("clusters")
        val clusters = List(clustersJson.length()) { i -> clustersJson.getJSONObject(i).run {
            VisualCluster(getInt("id"), getDouble("share"), getInt("r"), getInt("g"), getInt("b"), Material.valueOf(getString("suggested")), getDouble("confidence"))
        } }
        val assignmentsJson = json.getJSONObject("assignments")
        val assignments = assignmentsJson.keys().asSequence().associate { it.toInt() to Material.valueOf(assignmentsJson.getString(it)) }
        val estimatesJson = json.getJSONArray("estimates")
        val estimates = List(estimatesJson.length()) { i -> estimatesJson.getJSONObject(i).run {
            MaterialEstimate(
                Material.valueOf(getString("material")),
                getDouble("shareLow"),
                getDouble("shareHigh"),
                getDouble("densityLow"),
                getDouble("densityHigh"),
                optNullable("volumeLow"),
                optNullable("volumeHigh"),
                optNullable("weightLow"),
                optNullable("weightHigh"),
                getDouble("confidence"),
                optNullable("recoverableWeightLow"),
                optNullable("recoverableWeightHigh"),
                optNullable("valueLow"),
                optNullable("valueHigh")
            )
        } }
        val warningJson = json.optJSONArray("warnings") ?: JSONArray()
        val fieldContext = json.optJSONObject("fieldContext")?.let(::decodeFieldContext) ?: FieldContext()
        return BatchResult(
            id = json.getString("id"), name = json.getString("name"), createdAt = json.getLong("createdAt"),
            angleCount = json.getInt("angleCount"), referenceMm = json.optNullable("referenceMm"), pixelsPerMm = json.optNullable("pixelsPerMm"),
            knownTotalWeightG = json.optNullable("knownTotalWeightG"), maskSensitivity = json.getDouble("maskSensitivity"),
            fieldContext = fieldContext, clusters = clusters, assignments = assignments, estimates = estimates, overallConfidence = json.getDouble("overallConfidence"),
            warnings = List(warningJson.length()) { warningJson.getString(it) }
        )
    }

    private fun encodeFieldContext(fieldContext: FieldContext) = JSONObject().apply {
        val normalized = fieldContext.normalized()
        put("sampleType", normalized.sampleType.name)
        put("siteLabel", normalized.siteLabel)
        put("goldPricePerGram", normalized.goldPricePerGram)
        put("recoveryPercent", normalized.recoveryPercent)
    }

    private fun decodeFieldContext(json: JSONObject) = FieldContext(
        sampleType = runCatching { SampleType.valueOf(json.optString("sampleType")) }.getOrDefault(SampleType.PAN_CONCENTRATE),
        siteLabel = json.optString("siteLabel"),
        goldPricePerGram = json.optNullable("goldPricePerGram"),
        recoveryPercent = json.optDouble("recoveryPercent", 85.0)
    ).normalized()

    private fun JSONObject.optNullable(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)
}
