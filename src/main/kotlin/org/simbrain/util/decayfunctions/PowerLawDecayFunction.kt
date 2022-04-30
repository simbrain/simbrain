package org.simbrain.util.decayfunctions

// TODO: Finish implementation.
//  Add parameters (constant, exponent). Relation to quadratic
class PowerLawDecayFunction(): DecayFunction(){

    override fun getScalingFactor(distance: Double): Double {
        val dist = distanceFromPeak(distance)
        return if (dist > dispersion) {
            0.0
        } else {
            return 1 -  Math.pow(dist / dispersion, 2.0)
        }
    }

    override fun copy(): PowerLawDecayFunction {
        return PowerLawDecayFunction().also {
            it.dispersion = dispersion
            it.peakDistance = peakDistance
            it.addNoise = addNoise
            it.randomizer = randomizer.deepCopy()
        }
    }

    override fun getName(): String {
        return "Quadratic"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return DecayFunction.getTypes()
        }
    }
}