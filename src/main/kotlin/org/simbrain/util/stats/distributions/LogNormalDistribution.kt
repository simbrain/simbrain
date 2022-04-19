package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

class LogNormalDistribution(location: Double = 1.0, scale: Double = .5): ProbabilityDistribution() {

    @UserParameter(
        label = "Location (\u03BC)",
        description = "The mean of the logarithm of this distribution.",
        order = 1
    )
    private var location = 1.0
    // TODO

    @UserParameter(
        label = "Scale (\u03C3)",
        description = "The standard deviation of the logarithm of this distribution.",
        order = 2
    )
    private var scale = 0.5
    // TODO

    @Transient
    var dist: AbstractRealDistribution =
        org.apache.commons.math3.distribution.LogNormalDistribution(randomGenerator, scale, location)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    override fun deepCopy(): LogNormalDistribution {
        val copy = LogNormalDistribution()
        copy.randomSeed = randomSeed
        copy.location = location
        copy.scale = scale
        return copy
    }

    override fun getName(): String {
        return "Log-Normal"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}