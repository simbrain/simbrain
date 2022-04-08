package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

class ExponentialDistribution(lambda: Double = 1.0): ProbabilityDistribution() {

    @UserParameter(
        label = "Rate (\u03BB)",
        description = "The rate of exponential decay; higher rate parameters will produce more small values.",
        order = 1
    )
    var lambda = lambda
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ExponentialDistribution(randomGenerator, 1/value)
        }

    var dist: AbstractRealDistribution = org.apache.commons.math3.distribution.ExponentialDistribution(
        randomGenerator, 1/lambda)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    override fun deepCopy(): ProbabilityDistribution {
        val cpy = ExponentialDistribution()
        cpy.lambda = lambda
        return cpy
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