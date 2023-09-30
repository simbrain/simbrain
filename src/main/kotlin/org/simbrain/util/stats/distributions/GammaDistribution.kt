package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.NegatableDistribution
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

/**
 * https://en.wikipedia.org/wiki/Gamma_distribution
 */
class GammaDistribution(shape: Double = 2.0, scale: Double = 1.0, negate: Boolean = false)
    : NegatableDistribution, ProbabilityDistribution() {

    @UserParameter(
        label = "Shape (k)",
        description = "Shape (k).",
        minimumValue = 0.0001,
        order = 1)
    var shape = shape
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.GammaDistribution(randomGenerator, value, scale)
        }

    @UserParameter(label = "Scale (\u03B8)",
        description = "Scale (\u03B8).",
        minimumValue = 0.0001,
        order = 2)
    var scale = scale
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.GammaDistribution(randomGenerator, shape, value)
        }

    override var negate: Boolean = negate

    @Transient
    var dist: AbstractRealDistribution =
        org.apache.commons.math3.distribution.GammaDistribution(randomGenerator, shape, scale)

    override fun sampleDouble(): Double = dist.sample().conditionalNegate()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n).conditionalNegate()

    override fun sampleInt(): Int = dist.sample().toInt().conditionalNegate()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray().conditionalNegate()

    val mean get() = shape * scale

    val variance get() = shape * scale * scale

    override fun deepCopy(): GammaDistribution {
        val copy = GammaDistribution()
        copy.randomSeed = randomSeed
        copy.shape = shape
        copy.scale = scale
        copy.negate = negate
        return copy
    }

    override val name = "Gamma"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}