package org.simbrain.util.decayfunctions

class StepDecayFunction () : DecayFunction() {

    override fun getScalingFactor(distance: Double): Double {
        return if (distanceFromPeak(distance) > dispersion) {
            0.0
        } else {
            1.0
        }
    }

    override fun copy(): StepDecayFunction {
        return StepDecayFunction().also {
            it.dispersion = dispersion
            it.peakDistance = dispersion
            it.addNoise = addNoise
            it.randomizer = randomizer.deepCopy()
        }
    }

    override fun getName(): String {
        return "Step"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return DecayFunction.getTypes()
        }
    }
}