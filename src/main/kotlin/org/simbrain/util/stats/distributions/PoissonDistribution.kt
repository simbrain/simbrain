package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractIntegerDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toDoubleArray

class PoissonDistribution(p: Double = 1.0, epsilon:Double = 1e-12, maxIterations: Int = 10000000) : ProbabilityDistribution
() {

    @UserParameter(label = "Pouisson mean", useSetter = true, description = "Todo.", order = 2)
    var p = p
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.PoissonDistribution(randomGenerator, value, epsilon,
                maxIterations)
        }

    @UserParameter(label = "Epsilon", useSetter = true, description = "Todo.", order = 2)
    var epsilon = epsilon
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.PoissonDistribution(randomGenerator, p, value, maxIterations)
        }

    @UserParameter(label = "Max iterations", description = "Todo.", order = 3)
    var maxIterations = maxIterations
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.PoissonDistribution(randomGenerator, p, epsilon, value)
        }

    @Transient
    var dist: AbstractIntegerDistribution = org.apache.commons.math3.distribution.PoissonDistribution(randomGenerator, p, epsilon, maxIterations)

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
        // TODO
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