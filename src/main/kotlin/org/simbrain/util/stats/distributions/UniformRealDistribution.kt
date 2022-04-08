package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

class UniformRealDistribution(floor:Double = 0.0, ceil: Double = 1.0) : ProbabilityDistribution() {

    @UserParameter(label = "Ceiling", description = "Max of the uniform distribution.", order = 1)
    var ceil = ceil
        set(value) {
            field = value
            dist = UniformRealDistribution(randomGenerator, floor, value)
        }

    @UserParameter(label = "Floor", description = "Min of the uniform distribution.", order = 2)
    var floor = floor
        set(value) {
            field = value
            dist = UniformRealDistribution(randomGenerator, value, ceil)
        }

    @Transient
    var dist: AbstractRealDistribution = UniformRealDistribution(randomGenerator, floor, ceil)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    override fun getName(): String {
        return "Uniform (Real)"
    }

    override fun readResolve(): Any {
        super.readResolve()
        dist = UniformRealDistribution(this.randomGenerator, this.floor, this.ceil)
        return this
    }

    override fun deepCopy(): org.simbrain.util.stats.distributions.UniformRealDistribution {
        val cpy = org.simbrain.util.stats.distributions.UniformRealDistribution()
        cpy.ceil = ceil
        cpy.floor = floor
        return cpy
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}