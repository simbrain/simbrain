package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.NegatableDistribution
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

/**
 * https://en.wikipedia.org/wiki/Pareto_distribution
 */
class ParetoDistribution(shape: Double = 3.0, scale: Double = 1.0, negate: Boolean = false)
    : NegatableDistribution, ProbabilityDistribution() {

    @UserParameter(
        label = "Shape (α)",
        useSetter = true,
        description = "The power of the distribution.",
        minimumValue = 0.0001,
        order = 1)
    var shape = shape
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ParetoDistribution(randomGenerator, scale, value)
        }

    @UserParameter(
        label = "Scale (x)",
        useSetter = true,
        description = "The minimum value the distribution will produce.",
        minimumValue = 0.0001,
        order = 2
    )
    var scale = scale
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.ParetoDistribution(randomGenerator, value, shape)
        }

    override var negate: Boolean = negate

    @Transient
    var dist: AbstractRealDistribution =
        org.apache.commons.math3.distribution.ParetoDistribution(randomGenerator, scale, shape)

    override fun sampleDouble(): Double = dist.sample().conditionalNegate()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n).conditionalNegate()

    override fun sampleInt(): Int = dist.sample().toInt().conditionalNegate()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray().conditionalNegate()

    override fun deepCopy(): ParetoDistribution {
        val copy = ParetoDistribution()
        copy.randomSeed = randomSeed
        copy.shape = shape
        copy.scale = scale
        copy.negate = negate
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