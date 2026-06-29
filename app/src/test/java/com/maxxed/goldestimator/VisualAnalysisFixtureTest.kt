package com.maxxed.goldestimator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAnalysisFixtureTest {
    @Test
    fun poorLightFixtureIsRejected() {
        val samples = grid { x, y -> PixelSample(12 + x % 3, 13 + y % 3, 11, x, y) }
        val quality = VisualAnalysis.quality(samples)
        assertFalse(quality.accepted)
        assertTrue("Too dark" in quality.issues)
    }

    @Test
    fun missingScaleAndWeightDoesNotInventMass() {
        val cluster = VisualCluster(0, 1.0, 140, 102, 35, Material.GOLD, 0.6)
        val result = VisualAnalysis.estimates(listOf(cluster), mapOf(0 to Material.GOLD), 0.8, null, null, null)
        assertNull(result.first().volumeLowCm3)
        assertNull(result.first().weightLowG)
    }

    @Test
    fun brightGoldColoredFixtureDefaultsToPyriteOrMica() {
        val result = VisualAnalysis.classify(224.0, 198.0, 105.0, 18.0)
        assertEquals(Material.PYRITE_MICA, result.first)
    }

    @Test
    fun darkerSaturatedGoldFixtureRemainsLowConfidenceVisualGold() {
        val result = VisualAnalysis.classify(165.0, 125.0, 44.0, 20.0)
        assertEquals(Material.GOLD, result.first)
        assertTrue(result.second < 0.65)
    }

    @Test
    fun knownWeightAllocationsStayFiniteAndOrdered() {
        val clusters = listOf(
            VisualCluster(0, 0.2, 160, 120, 35, Material.GOLD, 0.58),
            VisualCluster(1, 0.8, 210, 210, 204, Material.QUARTZ, 0.74)
        )
        val estimates = VisualAnalysis.estimates(clusters, clusters.associate { it.id to it.suggested }, 0.8, 100.0, null, null)
        estimates.forEach {
            assertTrue((it.weightLowG ?: 0.0).isFinite())
            assertTrue((it.weightHighG ?: 0.0) >= (it.weightLowG ?: 0.0))
            assertTrue(it.visibleShareHigh >= it.visibleShareLow)
        }
    }

    @Test
    fun fieldValueUsesRecoverableGoldOnly() {
        val clusters = listOf(
            VisualCluster(0, 0.25, 160, 120, 35, Material.GOLD, 0.58),
            VisualCluster(1, 0.75, 210, 210, 204, Material.QUARTZ, 0.74)
        )
        val estimates = VisualAnalysis.estimates(
            clusters,
            clusters.associate { it.id to it.suggested },
            0.85,
            knownWeightG = 80.0,
            pixelsPerMm = null,
            subjectPixelArea = null,
            goldPricePerGram = 70.0,
            recoverableFraction = 0.80
        )
        val gold = estimates.first { it.material == Material.GOLD }
        val quartz = estimates.first { it.material == Material.QUARTZ }
        assertTrue((gold.recoverableWeightLowG ?: 0.0) > 0.0)
        assertTrue((gold.valueHigh ?: 0.0) > (gold.valueLow ?: 0.0))
        assertNull(quartz.valueLow)
        assertNull(quartz.recoverableWeightLowG)
    }

    @Test
    fun fieldHelpersProduceUsefulLabels() {
        val context = FieldContext(SampleType.SLUICE_CONCENTRATE, "Creek cleanup", 68.0, 85.0)
        assertTrue(GoldViewModel.suggestedBatchName(context, 1).startsWith("Creek cleanup - "))
        assertEquals("1.0-2.0 oz t", GoldViewModel.troyOunceRange(31.1034768, 62.2069536))
        assertEquals("10.0-20.0 dwt", GoldViewModel.pennyweightRange(15.5517384, 31.1034768))
    }

    @Test
    fun fieldContextNormalizesFieldInputs() {
        val context = FieldContext(SampleType.PAN_CONCENTRATE, "  Test pan  ", -1.0, 0.0).normalized()
        assertEquals("Test pan", context.siteLabel)
        assertNull(context.goldPricePerGram)
        assertEquals(1.0, context.recoveryPercent, 0.0)
    }

    @Test
    fun subjectMaskSeparatesCenterObjectFromPlainBackground() {
        val samples = grid { x, y ->
            if (x in 3..6 && y in 3..6) PixelSample(130, 85, 25, x, y) else PixelSample(235, 235, 235, x, y)
        }
        val subject = VisualAnalysis.subjectSamples(samples, 0.4)
        assertTrue(subject.size in 12..20)
        assertTrue(subject.all { it.r < 200 })
    }

    @Test
    fun duplicateOrientationDoesNotCountAsCoverage() {
        val first = CapturePose(358.0, -42.0, 2.0)
        val nearAcrossNorth = CapturePose(2.0, -40.0, 3.0)
        val distinct = CapturePose(55.0, -38.0, 2.0)
        assertTrue(GoldViewModel.poseDistance(first, nearAcrossNorth) < 12.0)
        assertTrue(GoldViewModel.poseDistance(first, distinct) > 12.0)
    }

    private fun grid(block: (Int, Int) -> PixelSample): List<PixelSample> =
        (0 until 10).flatMap { y -> (0 until 10).map { x -> block(x, y) } }
}
