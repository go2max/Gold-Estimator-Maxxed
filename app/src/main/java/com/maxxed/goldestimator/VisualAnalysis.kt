package com.maxxed.goldestimator

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object VisualAnalysis {
    fun quality(samples: List<PixelSample>): CaptureQuality {
        require(samples.isNotEmpty())
        val luminance = samples.map { 0.2126 * it.r + 0.7152 * it.g + 0.0722 * it.b }
        val mean = luminance.average()
        val contrast = sqrt(luminance.sumOf { (it - mean).pow(2) } / luminance.size)
        val sorted = samples.sortedWith(compareBy<PixelSample> { it.y }.thenBy { it.x })
        val sharpness = sorted.zipWithNext().map { (a, b) ->
            (abs(a.r - b.r) + abs(a.g - b.g) + abs(a.b - b.b)) / 3.0
        }.average().let { if (it.isNaN()) 0.0 else it }
        val glare = samples.count { max(it.r, max(it.g, it.b)) > 248 && min(it.r, min(it.g, it.b)) > 225 }.toDouble() / samples.size
        val maxX = samples.maxOfOrNull { it.x } ?: 0
        val maxY = samples.maxOfOrNull { it.y } ?: 0
        val border = samples.filter { it.x < maxX / 8 || it.x > maxX * 7 / 8 || it.y < maxY / 8 || it.y > maxY * 7 / 8 }
        val center = samples.filter { it.x in maxX / 3..maxX * 2 / 3 && it.y in maxY / 3..maxY * 2 / 3 }
        val borderMean = meanRgb(border.ifEmpty { samples })
        val centerMean = meanRgb(center.ifEmpty { samples })
        val subjectPresence = colorDistance(centerMean[0], centerMean[1], centerMean[2], borderMean[0], borderMean[1], borderMean[2])
        val histogram = DoubleArray(12)
        luminance.forEach { histogram[(it.toInt().coerceIn(0, 255) * histogram.size / 256).coerceAtMost(11)]++ }
        return CaptureQuality(mean, contrast, sharpness, glare, subjectPresence, histogram.map { it / samples.size })
    }

    fun fingerprintDistance(a: CaptureQuality, b: CaptureQuality): Double =
        a.fingerprint.zip(b.fingerprint).sumOf { (x, y) -> abs(x - y) } +
            abs(a.meanLight - b.meanLight) / 255.0

    fun subjectSamples(samples: List<PixelSample>, sensitivity: Double): List<PixelSample> {
        if (samples.size < 25 || samples.none { it.x != 0 || it.y != 0 }) return samples
        val maxX = samples.maxOf { it.x }
        val maxY = samples.maxOf { it.y }
        val border = samples.filter { it.x <= maxX / 12 || it.x >= maxX * 11 / 12 || it.y <= maxY / 12 || it.y >= maxY * 11 / 12 }
        if (border.isEmpty()) return samples
        val bg = intArrayOf(border.map { it.r }.average().toInt(), border.map { it.g }.average().toInt(), border.map { it.b }.average().toInt())
        val threshold = 24.0 + sensitivity.coerceIn(0.0, 1.0) * 72.0
        val foreground = samples.filter {
            colorDistance(it.r, it.g, it.b, bg[0], bg[1], bg[2]) > threshold
        }
        return if (foreground.size >= samples.size / 20) foreground else samples
    }

    fun cluster(samples: List<PixelSample>, count: Int = 6): List<VisualCluster> {
        if (samples.isEmpty()) return emptyList()
        val k = min(count, samples.size)
        var centers = (0 until k).map { samples[(it * samples.size / k).coerceAtMost(samples.lastIndex)] }.map { doubleArrayOf(it.r.toDouble(), it.g.toDouble(), it.b.toDouble()) }
        var assignment = IntArray(samples.size)
        repeat(10) {
            assignment = IntArray(samples.size) { i ->
                centers.indices.minBy { c -> colorDistance(samples[i], centers[c]) }
            }
            centers = centers.indices.map { c ->
                val members = samples.indices.filter { assignment[it] == c }
                if (members.isEmpty()) centers[c] else doubleArrayOf(
                    members.map { samples[it].r }.average(),
                    members.map { samples[it].g }.average(),
                    members.map { samples[it].b }.average()
                )
            }
        }
        return centers.indices.mapNotNull { c ->
            val members = samples.indices.filter { assignment[it] == c }
            if (members.isEmpty()) null else {
                val center = centers[c]
                val spread = members.map { colorDistance(samples[it], center) }.average()
                val material = classify(center[0], center[1], center[2], spread)
                VisualCluster(c, members.size.toDouble() / samples.size, center[0].toInt(), center[1].toInt(), center[2].toInt(), material.first, material.second)
            }
        }.sortedByDescending { it.share }.mapIndexed { index, cluster -> cluster.copy(id = index) }
    }

    fun classify(r: Double, g: Double, b: Double, spread: Double = 0.0): Pair<Material, Double> {
        val maxC = max(r, max(g, b)); val minC = min(r, min(g, b))
        val saturation = if (maxC == 0.0) 0.0 else (maxC - minC) / maxC
        val brightness = maxC / 255.0
        val yellow = r > g * 1.04 && g > b * 1.18
        return when {
            brightness < 0.22 && saturation < 0.28 -> Material.BLACK_SAND to 0.75
            brightness < 0.30 -> Material.DIRT_CLAY to 0.58
            saturation < 0.12 && brightness > 0.68 -> Material.QUARTZ to 0.74
            yellow && brightness > 0.72 -> Material.PYRITE_MICA to 0.63
            yellow && brightness in 0.32..0.72 && saturation > 0.38 && spread < 62 -> Material.GOLD to 0.58
            r > g * 1.10 && g > b * 1.05 -> Material.DIRT_CLAY to 0.55
            else -> Material.UNKNOWN to 0.35
        }
    }

    fun estimates(
        clusters: List<VisualCluster>,
        assignments: Map<Int, Material>,
        qualityConfidence: Double,
        knownWeightG: Double?,
        pixelsPerMm: Double?,
        subjectPixelArea: Double?,
        goldPricePerGram: Double? = null,
        recoverableFraction: Double = 0.85,
        depthMmRange: ClosedFloatingPointRange<Double> = 3.0..12.0
    ): List<MaterialEstimate> {
        val grouped = Material.entries.associateWith { material ->
            clusters.filter { (assignments[it.id] ?: it.suggested) == material }.sumOf { it.share }
        }
        val weightedDensity = grouped.entries.sumOf { (material, share) -> share * (material.densityLow + material.densityHigh) / 2.0 }.coerceAtLeast(0.01)
        val totalVolumeRange = if (pixelsPerMm != null && subjectPixelArea != null && pixelsPerMm > 0) {
            val areaCm2 = subjectPixelArea / (pixelsPerMm * pixelsPerMm) / 100.0
            (areaCm2 * depthMmRange.start / 10.0)..(areaCm2 * depthMmRange.endInclusive / 10.0)
        } else null
        return Material.entries.map { material ->
            val share = grouped.getValue(material)
            val uncertainty = 0.04 + (1.0 - qualityConfidence) * 0.12
            val lowShare = (share - uncertainty).coerceAtLeast(0.0)
            val highShare = (share + uncertainty).coerceAtMost(1.0)
            val confidence = clusters.filter { (assignments[it.id] ?: it.suggested) == material }
                .map { it.confidence }.average().let { if (it.isNaN()) 0.15 else it * qualityConfidence }
            val volumeLow = totalVolumeRange?.start?.times(lowShare)
            val volumeHigh = totalVolumeRange?.endInclusive?.times(highShare)
            val weightLow: Double?
            val weightHigh: Double?
            if (knownWeightG != null) {
                weightLow = knownWeightG * lowShare * material.densityLow / weightedDensity
                weightHigh = knownWeightG * highShare * material.densityHigh / weightedDensity
            } else {
                weightLow = volumeLow?.times(material.densityLow)
                weightHigh = volumeHigh?.times(material.densityHigh)
            }
            val recovery = recoverableFraction.coerceIn(0.0, 1.0)
            val recoverableLow = if (material == Material.GOLD) weightLow?.times(recovery) else null
            val recoverableHigh = if (material == Material.GOLD) weightHigh?.times(recovery) else null
            val valueLow = goldPricePerGram?.takeIf { it > 0 }?.let { price -> recoverableLow?.times(price) }
            val valueHigh = goldPricePerGram?.takeIf { it > 0 }?.let { price -> recoverableHigh?.times(price) }
            MaterialEstimate(
                material,
                lowShare,
                highShare,
                material.densityLow,
                material.densityHigh,
                volumeLow,
                volumeHigh,
                weightLow,
                weightHigh,
                confidence,
                recoverableLow,
                recoverableHigh,
                valueLow,
                valueHigh
            )
        }
    }

    private fun colorDistance(sample: PixelSample, center: DoubleArray) = colorDistance(sample.r.toDouble(), sample.g.toDouble(), sample.b.toDouble(), center[0], center[1], center[2])
    private fun meanRgb(samples: List<PixelSample>) = doubleArrayOf(samples.map { it.r }.average(), samples.map { it.g }.average(), samples.map { it.b }.average())
    private fun colorDistance(r1: Number, g1: Number, b1: Number, r2: Number, g2: Number, b2: Number): Double =
        sqrt((r1.toDouble() - r2.toDouble()).pow(2) * 0.30 + (g1.toDouble() - g2.toDouble()).pow(2) * 0.59 + (b1.toDouble() - b2.toDouble()).pow(2) * 0.11)
}
