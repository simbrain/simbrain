package org.simbrain.util.decayfunctions

import kotlin.math.exp

class ExponentialDecayFunction @JvmOverloads constructor(dispersion: Double = 70.0): DecayFunction() {

    // @UserParameter(
    //     label = "Rate (\u03BB)",
    //     useSetter = true,
    //     description = "The rate of exponential decay. The mean is 1/λ. For higher λ the mean is closer to 0 " +
    //             "For lower λ the mean is farther from 0 and the tail is longer.",
    //     minimumValue = 0.00001,
    //     increment = .1,
    //     order = 1
    // )
    // var lambda = 1.0

    override fun getScalingFactor(distance: Double): Double {
        // lambda = 1/dispersion
        // So max value we can get is 1/dispersion, at the peak
        // this is a reflected version of the exponential distribution
        // it is not a true pdf except when limited to non-negative values
        val x = distanceFromPeak(distance)
        return (1/dispersion) * exp((-1/dispersion) * x)
    }


    override fun copy(): ExponentialDecayFunction {
        return ExponentialDecayFunction(dispersion)
            .also {
                it.peakDistance = dispersion
            }
    }

    override fun getName(): String {
        return "Gaussian"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return DecayFunction.getTypes()
        }
    }
}