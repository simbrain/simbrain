package org.simbrain.util.decayfunctions

class StepDecayFunction @JvmOverloads constructor(dispersion: Double = 70.0) : DecayFunction(dispersion) {

    override fun getScalingFactor(distance: Double): Double {
        return if (distanceFromPeak(distance) > dispersion) {
            0.0
        } else {
            baseMultiplier
        }
    }

    override fun copy(): StepDecayFunction {
        return StepDecayFunction(dispersion).also {
            it.peakDistance = peakDistance
            it.baseMultiplier = baseMultiplier
        }
    }

    override val name = "Step"

}