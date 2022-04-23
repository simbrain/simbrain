package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

/**
 * See https://en.wikipedia.org/wiki/Exponential_distribution
 */
class ExponentialDistribution(lambda: Double = 1.0): ProbabilityDistribution() {

    @UserParameter(
        label = "Rate (\u03BB)",
        useSetter = true,
        description = "The rate of exponential decay. The mean is 1/λ. For higher λ the mean is closer to 0 " +
                "For lower λ the mean is farther from 0 and the tail is longer.",
        minimumValue = 0.00001,
        order = 1
    )
    var lambda = lambda
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ExponentialDistribution(randomGenerator, 1/value)
        }

    @Transient
    var dist: AbstractRealDistribution = org.apache.commons.math3.distribution.ExponentialDistribution(
        randomGenerator, 1/lambda)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    override fun deepCopy(): ProbabilityDistribution {
        val copy = ExponentialDistribution()
        copy.randomSeed = randomSeed
        copy.lambda = lambda
        return copy
    }

    override fun getName(): String {
        return "Exponential"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}