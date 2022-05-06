package org.simbrain.util

import org.apache.commons.math3.distribution.ChiSquaredDistribution
import org.apache.commons.math3.distribution.TDistribution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.*
import kotlin.math.sqrt

/**
 * Tests of probability distributions.
 *
 * @author Scott Hotton
 * @author Jeff Yoshimi
 */
class ProbabilityDistributionTest {

    // TODO: Finish for all probability distributions

    val alpha = .001 // For 1 - alpha confidence level
    val N = 1000

    @Test
    fun `test uniform`() {
        val sample = UniformRealDistribution(0.0, 1.0)
        var nums = sample.sampleDouble(N)
        // TODO:  Properly implememnt this
        // assertTrue(nums.average() in ((.5 - alpha)..(.5 + alpha)))
        assertTrue(nums.maxOrNull()!! <= 1.0)
        assertTrue(nums.minOrNull()!! >= 0.0)
        sample.floor = -2.0
        sample.ceil = -1.0
        nums = sample.sampleDouble(N)
        println(nums.average())
        // assertTrue(nums.average() in ((-1.5 - alpha)..(-1.5 + alpha)))
        assertTrue(nums.maxOrNull()!! <= -1.0)
        assertTrue(nums.minOrNull()!! >= -2.0)
    }

    @Test
    fun `test uniform bounds`() {
        val dist = UniformRealDistribution(1.0, 3.0)
        var nums = dist.sampleDouble(10)
        assertTrue(nums.maxOrNull()!! <= 3.0)
        assertTrue(nums.minOrNull()!! >= 1.0)
    }


    @Test
    fun `test normal distribution mean 1 and stdev point-5`() {
        val mean = 1.0
        val stdev = .5
        val stats = NormalDistribution(mean,stdev).sampleStats()
        assertTrue(mean in stats.confidenceIntervalForMeanOfNormalDist())
        assertTrue(stdev*stdev in stats.confidenceIntervalForVarianceOfNormalDist())
    }

    @Test
    fun `test normal distribution mean -1 and stdev point-25`() {
        val mean = -1.0
        val stdev = .25
        val stats = NormalDistribution(mean,stdev).sampleStats()
        assertTrue(mean in stats.confidenceIntervalForMeanOfNormalDist())
        assertTrue(stdev*stdev in stats.confidenceIntervalForVarianceOfNormalDist())
    }

    @Test
    fun `test poisson`() {
        val poisson = PoissonDistribution()
        poisson.p = 1.0
        val poissonSample = poisson.sampleStats()
        val poissonApproximatedByNormal = NormalDistribution(poisson.p * N,poisson.p * N)
        val testStats = poissonApproximatedByNormal.sampleStats()
        // println(poissonSample)
        // println(testStats)
        assertTrue(poissonSample.sum in testStats.confidenceIntervalForMeanOfNormalDist())
        // TOOD: Think about variance case
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
    fun `test exponential negation`() {
        val dist = ExponentialDistribution()
        dist.negate = true
        assertTrue(dist.sampleDouble() <= 0)
        assertTrue(dist.sampleInt() <= 0)
        var sample = dist.sampleDouble(5)
        assertTrue(sample.minOrNull()!! <= 0)
        var intNums = dist.sampleInt(5)
        assertTrue(intNums.minOrNull()!! <= 0)
    }

    @Test
    fun `test same results from same seed`() {
        var dist1: ProbabilityDistribution = NormalDistribution(1.0, .5)
        dist1.randomSeed = 1
        var dist2: ProbabilityDistribution = NormalDistribution(1.0, .5)
        dist2.randomSeed = 1
        assertEquals(dist1.sampleDouble(), dist2.sampleDouble())

        dist1 = TwoValued()
        dist1.randomSeed = 1
        dist2 = TwoValued()
        dist2.randomSeed = 1
        assertEquals(dist1.sampleDouble(), dist2.sampleDouble())

        dist1 = UniformRealDistribution()
        dist1.randomSeed = 1
        dist2 = UniformRealDistribution()
        dist2.randomSeed = 1
        assertEquals(dist1.sampleDouble(), dist2.sampleDouble())

    }

    @Test
    fun `test different results if no seed set`() {
        var dist1: ProbabilityDistribution = NormalDistribution(1.0, .5)
        var dist2: ProbabilityDistribution = NormalDistribution(1.0, .5)
        assertNotEquals(dist1.sampleDouble(), dist2.sampleDouble())

        dist1 = UniformRealDistribution()
        dist2 = UniformRealDistribution()
        assertNotEquals(dist1.sampleDouble(), dist2.sampleDouble())
    }

    @Test
    fun `test different results if no seed set and deep copy`() {
        var dist: ProbabilityDistribution = NormalDistribution(1.0, .5)
        var dist2 = dist.copy()
        assertNotEquals(dist.sampleDouble(), dist2.sampleDouble());
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
    fun `test two valued`() {
        val dist = TwoValued(-3.0, 3.0)
        var nums = dist.sampleDouble(N)
        assertTrue(nums.all { it == -3.0 || it == 3.0 })
        assertTrue { nums.average() in (-.5.. .5) } // TODO
        dist.p = 1.0
        nums = dist.sampleDouble(N)
        assertTrue(nums.all { it == 3.0 })
        dist.p = 0.0
        nums = dist.sampleDouble(N)
        assertTrue(nums.all { it == -3.0 })
    }

    @Test
    fun `test deep copy`() {
        run {
            val dist1 = NormalDistribution(1.0, .5)
            val dist2 = dist1.deepCopy()
            assertTrue((dist1.randomSeed == dist2.randomSeed))
            assertTrue((dist1.mean == dist2.mean))
            assertTrue((dist1.standardDeviation == dist2.standardDeviation))
        }
        run {
            val dist1 = TwoValued()
            val dist2 = dist1.deepCopy()
            assertTrue((dist1.randomSeed == dist2.randomSeed))
            assertTrue((dist1.upperValue == dist2.upperValue))
            assertTrue((dist1.lowerValue == dist2.lowerValue))
        }
    }

    @Test
    fun `test serialization using xstream`() {
        var dist: ProbabilityDistribution = UniformRealDistribution(-2.2, 1.2)
        with(dist) {
            val xml = ProbabilityDistribution.getXStream().toXML(this)
            val deserialized = ProbabilityDistribution.getXStream().fromXML(xml) as UniformRealDistribution
            assertNotNull(deserialized.dist)
            assertNotNull(deserialized.randomGenerator)
            assertTrue(deserialized.ceil == 1.2)
            assertTrue(deserialized.floor == -2.2)
        }

        dist = TwoValued(-2.0, 2.0, .7)
        dist.randomSeed = 1
        with(dist) {
            val xml = ProbabilityDistribution.getXStream().toXML(this)
            val deserialized = ProbabilityDistribution.getXStream().fromXML(xml) as TwoValued
            assertTrue(deserialized.lowerValue == -2.0)
            assertTrue(deserialized.upperValue == 2.0)
            assertTrue(deserialized.p == .7)
            assertTrue(deserialized.randomSeed == 1)
        }

        // Test randomizer serializes properly
        val randomizer = ProbabilityDistribution.Randomizer(NormalDistribution(1.0, 0.25))
        with(randomizer) {
            val xml = ProbabilityDistribution.getXStream().toXML(this)
            val deserialized = ProbabilityDistribution.getXStream().fromXML(xml) as ProbabilityDistribution.Randomizer
            with(deserialized.probabilityDistribution as NormalDistribution) {
                assertNotNull(randomGenerator)
                assertTrue(mean == 1.0)
                assertTrue(standardDeviation == 0.25)
            }
        }

    }

    @Test
    fun `test random seed after deserializing xstream`() {
        // Use random seed, should get same samples
        run {
            val dist1: ProbabilityDistribution = UniformRealDistribution()
            dist1.randomSeed = 1
            val dist2 = dist1.deepCopy()
            val xml1 = ProbabilityDistribution.getXStream().toXML(dist1)
            val xml2 = ProbabilityDistribution.getXStream().toXML(dist2)
            val deserialized1 = ProbabilityDistribution.getXStream().fromXML(xml1) as UniformRealDistribution
            val deserialized2 = ProbabilityDistribution.getXStream().fromXML(xml2) as UniformRealDistribution
            assertEquals(deserialized1.sampleInt(), deserialized2.sampleInt())
        }
        // No seed set, should get different results
        run {
            val dist1: ProbabilityDistribution = UniformRealDistribution()
            val dist2 = dist1.deepCopy()
            val xml1 = ProbabilityDistribution.getXStream().toXML(dist1)
            val xml2 = ProbabilityDistribution.getXStream().toXML(dist2)
            val deserialized1 = ProbabilityDistribution.getXStream().fromXML(xml1) as UniformRealDistribution
            val deserialized2 = ProbabilityDistribution.getXStream().fromXML(xml2) as UniformRealDistribution
            assertNotEquals(deserialized1.sampleDouble(), deserialized2.sampleDouble())
        }
    }

    ///////// Utilities /////////////

    data class Stats(val mean:Double, val stdev: Double, val variance: Double, val sum: Double)

    fun ProbabilityDistribution.sampleStats(): Stats {
        var sample = sampleDouble(N)
        val sampleMean = sample.average()
        val sampleStdev = sample.stdev()
        val sampleVar = sampleStdev * sampleStdev
        val sum = sample.sum()
        return Stats(sampleMean, sampleStdev, sampleVar, sum)
    }

    fun stderr(sampleStdev: Double, sampleSize: Int): Double {
        return sampleStdev / sqrt(sampleSize.toDouble())
    }

    fun tscore(alpha: Double, df: Int): Double {
        val tdist = TDistribution(df.toDouble())
        // Absolute because t-values from t-tables are the absolute value of inverse cumulative probabilities
        return Math.abs(tdist.inverseCumulativeProbability(alpha))
    }

    fun chiSquareScore(alpha: Double, df: Int): Double {
        val dist = ChiSquaredDistribution(df.toDouble())
        return dist.inverseCumulativeProbability(alpha)
    }

    fun Stats.confidenceIntervalForMeanOfNormalDist(): ClosedFloatingPointRange<Double> {
        val halfInterval = tscore(alpha / 2, N - 1) * stderr(stdev, N)
        return (mean - halfInterval)..(mean + halfInterval)
    }
    fun Stats.confidenceIntervalForVarianceOfNormalDist() = (
                    (N - 1) / chiSquareScore(1 - alpha / 2, N - 1) * variance ..
                    (N - 1) / chiSquareScore(alpha / 2, N - 1) * variance)

}