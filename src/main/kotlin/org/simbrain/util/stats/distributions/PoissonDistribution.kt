package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractIntegerDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toDoubleArray

/**
 * https://en.wikipedia.org/wiki/Poisson_distribution
 */
class PoissonDistribution(p: Double = 1.0) : ProbabilityDistribution() {

    @UserParameter(
        label = "Mean value",
        useSetter = true,
        description = "The discrete value you should expect to see most often.",
        minimumValue = 0.00001,
        increment = 1.0,
        order = 2)
    var p = p
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.PoissonDistribution(randomGenerator, value, 1e-12, 10000000 )
        }

    @Transient
    var dist: AbstractIntegerDistribution =
        org.apache.commons.math3.distribution.PoissonDistribution(randomGenerator, p,  1e-12, 10000000)

    override fun sampleDouble(): Double = dist.sample().toDouble()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n).toDoubleArray()

    override fun sampleInt(): Int = dist.sample()

    override fun sampleInt(n: Int) = dist.sample(n)

    override fun getName(): String {
        return "Poisson"
    }

    override fun deepCopy(): PoissonDistribution {
        val copy = PoissonDistribution()
        copy.randomSeed = randomSeed
        copy.p = p
        return copy
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}