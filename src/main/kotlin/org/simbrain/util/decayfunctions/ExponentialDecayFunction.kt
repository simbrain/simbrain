package org.simbrain.util.decayfunctions

import kotlin.math.exp

class ExponentialDecayFunction @JvmOverloads constructor(dispersion: Double = 70.0) : DecayFunction(dispersion) {

    override fun getScalingFactor(distance: Double): Double {
        val dist = distanceFromPeak(distance)
        if (dist >= dispersion) return 0.0
        val lambda = 5.0 / dispersion
        return baseMultiplier * exp(-lambda * dist)
    }

    override fun copy(): ExponentialDecayFunction {
        return ExponentialDecayFunction(dispersion)
            .also {
                it.peakDistance = peakDistance
                it.baseMultiplier = baseMultiplier
            }
    }

    override val name = "Exponential"

}
