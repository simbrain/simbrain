package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

class NormalDistribution(mean: Double = 0.0, standardDeviation: Double = 1.0): ProbabilityDistribution() {

    @UserParameter(
        label = "Mean (\u03BC)",
        useSetter = true,
        description = "The expected value or center of the distribution.",
        increment = .1, order = 1
    )
    var mean: Double = mean
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.NormalDistribution(randomGenerator, value, standardDeviation)
        }

    @UserParameter(
        label = "Std. Dev. (\u03C3)",
        useSetter = true,
        description = "The average squared distance from the mean.",
        increment = .1, order = 2)
    var standardDeviation: Double = standardDeviation
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.NormalDistribution(randomGenerator, mean, value)
        }

    @Transient
    var dist: AbstractRealDistribution =
        org.apache.commons.math3.distribution.NormalDistribution(randomGenerator, mean, standardDeviation)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    val variance get() = standardDeviation * standardDeviation

    override val name = "Normal"

    override fun deepCopy(): NormalDistribution {
        val copy = NormalDistribution()
        copy.randomSeed = randomSeed
        copy.mean = mean
        copy.standardDeviation = standardDeviation
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