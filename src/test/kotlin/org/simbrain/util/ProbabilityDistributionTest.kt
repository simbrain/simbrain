package org.simbrain.util

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.distribution.TDistribution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.ExponentialDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformIntegerDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import kotlin.math.pow

/**
 * Tests of probability distributions.
 *
 * @author Scott Hotton
 * @author Jeff Yoshimi
 */
class ProbabilityDistributionTest {

    // TODO: Finish for all probability distributions

    val alpha = .05 // For 1 - alpha confidence level
    val N = 10_000

    @Test
    fun `test uniform`() {
        val dist = UniformRealDistribution(0.0, 1.0)
        var nums = dist.sampleDouble(N)
        assertTrue(nums.average() in ((.5 - alpha)..(.5 + alpha)))
        assertTrue(nums.maxOrNull()!! <= 1.0)
        assertTrue(nums.minOrNull()!! >= 0.0)
        dist.floor = -2.0
        dist.ceil = -1.0
        nums = dist.sampleDouble(N)
        println(nums.average())
        assertTrue(nums.average() in ((-1.5 - alpha)..(-1.5 + alpha)))
        assertTrue(nums.maxOrNull()!! <= -1.0)
        assertTrue(nums.minOrNull()!! >= -2.0)
    }

    fun tscoreNormalMean(alpha: Double, sampleSize: Int, sampleStdev: Double): Double {
        val tdist = TDistribution((sampleSize-1).toDouble())
        val stderr = sampleStdev/Math.sqrt(sampleSize.toDouble())
        // This works but there is un unexplained negative
        return -tdist.inverseCumulativeProbability(alpha/2) * stderr
    }

    // Rename lower/upper after reversal issue figured out
    fun tscoreNormalVarianceLeft(alpha: Double, sampleSize: Int, sampleStdev: Double): Double {
        val tdist = ChiSquaredDistribution((sampleSize-1).toDouble())
        return ((sampleSize-1)/tdist.inverseCumulativeProbability(alpha/2)) * sampleStdev.pow(2)
    }

    fun tscoreNormalVarianceRight(alpha: Double, sampleSize: Int, sampleStdev: Double): Double {
        val tdist = ChiSquaredDistribution((sampleSize-1).toDouble())
        return ((sampleSize-1)/tdist.inverseCumulativeProbability(1-alpha/2)) * sampleStdev.pow(2)
    }


    @Test
    fun `test normal`() {
        val dist = NormalDistribution(1.0, .5)
        var nums = dist.sampleDouble(N)
        val sampleMean = nums.average()
        val sampleStdev = nums.stdev()
        val tscoreMean = tscoreNormalMean(alpha, N, sampleStdev)
        // println("" + sampleMean + " in " + (1.0 - tscore) + ".." + (1.0 + tscore))
        assertTrue(sampleMean in ((1.0 - tscoreMean)..(1.0 + tscoreMean)))
        val tLeft = tscoreNormalVarianceLeft(alpha, N, sampleStdev)
        val tRight = tscoreNormalVarianceRight(alpha, N, sampleStdev)
        println("" + sampleStdev.pow(2)  + " in " + tLeft + ".." + tRight)
        assertTrue(sampleStdev.pow(2) in tRight..tLeft)

        // assertTrue(nums.stdev() in ((0.5 - alpha)..(0.5 + alpha)))
        // dist.mean = -.5
        // dist.standardDeviation = .25
        // nums = dist.sampleDouble(N)
        // assertTrue(nums.average() in ((-.5 - alpha)..(-.5 + alpha)))
        // assertTrue(nums.stdev() in ((0.25 - alpha)..(.25 + alpha)))
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