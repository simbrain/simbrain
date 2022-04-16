package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractIntegerDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toDoubleArray

class UniformIntegerDistribution(floor:Int = 0, ceil: Int = 1) : ProbabilityDistribution() {

    @UserParameter(label = "Ceiling", description = "Highest integer possible.", order = 1)
    var ceil = ceil
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.UniformIntegerDistribution(randomGenerator, floor, value)
        }

    @UserParameter(label = "Floor", description = "Smallest integer possible.", order = 2)
    var floor = floor
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.UniformIntegerDistribution(randomGenerator, value, ceil)
        }

    @Transient
    var dist: AbstractIntegerDistribution = org.apache.commons.math3.distribution.UniformIntegerDistribution(randomGenerator, floor, ceil)

    override fun sampleDouble(): Double = dist.sample().toDouble()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n).toDoubleArray()

    override fun sampleInt(): Int = dist.sample()

    override fun sampleInt(n: Int) = dist.sample(n)

    override fun getName(): String {
        return "Uniform (Integer)"
    }

    override fun deepCopy(): UniformIntegerDistribution {
        val cpy = UniformIntegerDistribution()
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