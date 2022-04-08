package org.simbrain.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.ExponentialDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

internal class ProbabilityDistributionTest {

    // TODO: Specify a proper confidence interval based on N
    val CL = .01 // Confidence level
    val N = 10_000

    @Test
    fun `test uniform`() {
        val dist = UniformRealDistribution(0.0, 1.0)
        var nums = dist.sampleDouble(N)
        assertTrue(nums.average() in ((.5 - CL)..(.5 + CL)))
        assertTrue(nums.maxOrNull()!! <= 1.0)
        assertTrue(nums.minOrNull()!! >= 0.0)
        dist.floor = -2.0
        dist.ceil = -1.0
        nums = dist.sampleDouble(N)
        println(nums.average())
        assertTrue(nums.average() in ((-1.5 - CL)..(-1.5 + CL)))
        assertTrue(nums.maxOrNull()!! <= -1.0)
        assertTrue(nums.minOrNull()!! >= -2.0)
    }

    @Test
    fun `test normal`() {
        val dist = NormalDistribution(1.0, .5)
        var nums = dist.sampleDouble(N)
        assertTrue(nums.average() in ((1.0 - CL)..(1.0 + CL)))
        assertTrue(nums.stdev() in ((0.5 - CL)..(0.5 + CL)))
        dist.mean = -.5
        dist.standardDeviation = .25
        nums = dist.sampleDouble(N)
        assertTrue(nums.average() in ((-.5 - CL)..(-.5 + CL)))
        assertTrue(nums.stdev() in ((0.25 - CL)..(.25 + CL)))
    }

    @Test
    fun `test exponential`() {
        val dist = ExponentialDistribution(1.0)
        var nums = dist.sampleDouble(N)
        assertTrue(nums.minOrNull()!! > 0)
        assertTrue(nums.average() in ((1.0 - .25)..(1.0 + .25)))
        dist.lambda = .5
        nums = dist.sampleDouble(N)
        assertTrue(nums.average() in ((2.0 - .25)..(2.0 + .25)))
    }

    @Test
    fun `test same results from same seed`() {
        val dist1 = NormalDistribution(1.0, .5)
        dist1.setSeed(1)
        val dist2 = NormalDistribution(1.0, .5)
        dist2.setSeed(1)
        assertEquals(dist1.sampleDouble(), dist2.sampleDouble())
    }

    @Test
    fun `test different results if no seed set`() {
        val dist1 = NormalDistribution(1.0, .5)
        val dist2 = NormalDistribution(1.0, .5)
        assertNotEquals(dist1.sampleDouble(), dist2.sampleDouble())
    }

    @Test
    fun `test uniform int`() {
        val dist = UniformIntegerDistribution(1, 10)
        var nums = dist.sampleInt(N)
        print(nums.contentToString())
        assertTrue(nums.minOrNull()!! >= 1)
        assertTrue(nums.maxOrNull()!! <= 10)
    }

    @Test
    fun `test serialization using xstream`() {
        val dist = UniformRealDistribution(-2.2, 1.2)
        val xml = ProbabilityDistribution.getXStream().toXML(dist)
        val deserialized = ProbabilityDistribution.getXStream().fromXML(xml) as UniformRealDistribution
        assertNotNull(deserialized.dist)
        assertNotNull(deserialized.randomGenerator)
        assertTrue(deserialized.ceil == 1.2)
        assertTrue(deserialized.floor == -2.2)
    }
}