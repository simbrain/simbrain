package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

class ParetoDistribution(slope: Double = 2.0, min: Double = 1.0): ProbabilityDistribution() {

    @UserParameter(
        label = "Slope (\u03B1)",
        useSetter = true,
        description = "The power of the distribution.",
        order = 1)
    var slope = slope
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ParetoDistribution(randomGenerator, value, min)
        }

    @UserParameter(
        label = "Minimum",
        useSetter = true,
        description = "The minimum value the distribution will produce. "
                + "Note that floor should never be lower than minimum.",
        order = 2
    )
    var min = min
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ParetoDistribution(randomGenerator, slope, value)
        }

    @Transient
    var dist: AbstractRealDistribution =
        org.apache.commons.math3.distribution.ParetoDistribution(randomGenerator, slope, min)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    override fun deepCopy(): ParetoDistribution {
        val copy = ParetoDistribution()
        copy.randomSeed = randomSeed
        copy.slope = slope
        copy.min = min
        return copy
    }

    override fun getName(): String {
        return "Pareto"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}