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
