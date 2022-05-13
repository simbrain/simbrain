package org.simbrain.util.stats.distributions;

import org.simbrain.util.UserParameter
import org.simbrain.util.stats.ProbabilityDistribution

/**
 * Returns one of two values based on a probability. A Bernoulli distribution where the sample space can be any two
 * numbers.
 */
class TwoValued(
    @UserParameter(label = "Lower value", useSetter = true, order = 1)
    var lowerValue: Double = -1.0,

    @UserParameter(label = "Upper value", useSetter = true, order = 2)
    var upperValue: Double = 1.0,

    @UserParameter(
        label = "Probability",
        description = "Probabiliy of selecting the upper value",
        order = 3)
    var p: Double = .5
) : ProbabilityDistribution() {

    override fun sampleDouble(): Double {
        return if (randomGenerator.nextDouble() > p) lowerValue else upperValue
    }

    override fun sampleInt(): Int {
        return sampleDouble().toInt()
    }

    override fun sampleDouble(n: Int): DoubleArray {
        return DoubleArray(n) {sampleDouble()}
    }

    override fun sampleInt(n: Int): IntArray {
        return IntArray(n) {sampleInt()}
    }

    val mean get() = p * upperValue + (1-p) * lowerValue

    val variance get() = .5 * (p * p + ((1-p) * (1-p))) * ((upperValue - lowerValue) * (upperValue - lowerValue))

    override fun deepCopy(): TwoValued {
        val copy = TwoValued()
        copy.randomSeed = randomSeed
        copy.lowerValue = lowerValue
        copy.upperValue = upperValue
        copy.p = p
        return copy
    }

    override fun getName(): String {
        return "Two valued"
    }

}