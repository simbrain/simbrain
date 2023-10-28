package org.simbrain.util.decayfunctions

class StepDecayFunction @JvmOverloads constructor(dispersion: Double = 70.0) : DecayFunction() {

    override fun getScalingFactor(distance: Double): Double {
        return if (distanceFromPeak(distance) > dispersion) {
            0.0
        } else {
            1.0
        }
    }

    override fun copy(): StepDecayFunction {
        return StepDecayFunction(dispersion).also {
            it.peakDistance = peakDistance
        }
    }

    override val name = "Step"

}