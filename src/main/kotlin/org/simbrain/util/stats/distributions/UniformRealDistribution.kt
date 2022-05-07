package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray
import java.lang.Math.sqrt

class UniformRealDistribution(floor:Double = 0.0, ceil: Double = 1.0) : ProbabilityDistribution() {

    @UserParameter(
        label = "Ceiling",
        useSetter = true,
        description = "Max of the uniform distribution.",
        order = 1)
    var ceil = ceil
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.UniformRealDistribution(randomGenerator, floor, value)
        }

    @UserParameter(
        label = "Floor",
        useSetter = true,
        description = "Min of the uniform distribution.",
        order = 2)
    var floor = floor
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.UniformRealDistribution(randomGenerator, value, ceil)
        }

    @Transient
    var dist: AbstractRealDistribution = org.apache.commons.math3.distribution.UniformRealDistribution(randomGenerator, floor, ceil)

    override fun sampleDouble(): Double = dist.sample()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n)

    override fun sampleInt(): Int = dist.sample().toInt()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray()

    val mean get() =  (ceil + floor)/2

    val stdev get() = (ceil - floor)/sqrt(12.0)

    val variance get() = Math.pow(stdev, 2.0)

    override fun getName(): String {
        return "Uniform (Real)"
    }

    override fun deepCopy(): UniformRealDistribution {
        val copy = UniformRealDistribution()
        copy.randomSeed = randomSeed
        copy.ceil = ceil
        copy.floor = floor
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