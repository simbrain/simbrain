package org.simbrain.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.math.ProbDistributions.NormalDistribution
import org.simbrain.util.math.ProbDistributions.UniformDistribution
import org.simbrain.util.math.ProbabilityDistribution

internal class ProbabilityDistributionTest {

    // TODO: Specify a proper confidence interval based on N
    val CI = .01
    val N = 10_000
    fun sample(n: Int, dist: ProbabilityDistribution): DoubleArray {
        return DoubleArray(n) { dist.nextDouble() }
    }

    @Test
    fun `test uniform`() {
        val dist = UniformDistribution(0.0, 1.0)
        val nums = sample(N, dist)
        assertTrue(nums.average() in ((.5 - CI)..(.5 + CI)))
        assertTrue(nums.maxOrNull()!! <= 1.0)
        assertTrue(nums.minOrNull()!! >= 0.0)
    }

    @Test
    fun `test normal`() {
        val dist = NormalDistribution(1.0, .5)
        var nums = sample(N, dist)
        assertTrue(nums.average() in ((1.0 - CI)..(1.0 + CI)))
        assertTrue(nums.stdev() in ((0.5 - CI)..(0.5 + CI)))
        dist.mean = -.5
        dist.standardDeviation = .25
        nums = sample(N, dist)
        assertTrue(nums.average() in ((-.5 - CI)..(-.5 + CI)))
        assertTrue(nums.stdev() in ((0.25 - CI)..(.25 + CI)))
    }

    @Test
    fun `test same results from same seed` () {
        val dist1 = NormalDistribution(1.0, .5)
        dist1.setSeed(1)
        val dist2 = NormalDistribution(1.0, .5)
        dist2.setSeed(1)
        assertEquals(dist1.nextDouble(), dist2.nextDouble())
    }

    @Test
    fun `test different results if no seed set` () {
        val dist1 = NormalDistribution(1.0, .5)
        val dist2 = NormalDistribution(1.0, .5)
        assertNotEquals(dist1.nextDouble(), dist2.nextDouble())
    }
}