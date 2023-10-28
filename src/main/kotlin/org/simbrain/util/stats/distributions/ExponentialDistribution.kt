package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.NegatableDistribution
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

/**
 * See https://en.wikipedia.org/wiki/Exponential_distribution
 */
class ExponentialDistribution(lambda: Double = 1.0, negate: Boolean = false)
    : NegatableDistribution, ProbabilityDistribution() {

    @UserParameter(
        label = "Rate (\u03BB)",
        description = "The rate of exponential decay. The mean is 1/λ. For higher λ the mean is closer to 0 " +
                "For lower λ the mean is farther from 0 and the tail is longer.",
        minimumValue = 0.00001,
        increment = .1,
        order = 1
    )
    var lambda = lambda
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ExponentialDistribution(randomGenerator, 1 / value)
        }

    @Transient
    var dist: AbstractRealDistribution = org.apache.commons.math3.distribution.ExponentialDistribution(
        randomGenerator, 1 / lambda
    )

    override var negate: Boolean = negate

    override fun sampleDouble(): Double = dist.sample().conditionalNegate()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n).conditionalNegate()

    override fun sampleInt(): Int = dist.sample().toInt().conditionalNegate()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray().conditionalNegate()

    val mean get() = 1/lambda

    val variance get() = 1/(lambda*lambda)

    override fun deepCopy(): ProbabilityDistribution {
        val copy = ExponentialDistribution()
        copy.randomSeed = randomSeed
        copy.lambda = lambda
        copy.negate = negate
        return copy
    }

    override val name = "Exponential"

}