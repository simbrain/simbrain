package org.simbrain.util.decayfunctions

class LinearDecayFunction @JvmOverloads constructor(dispersion: Double = 70.0) : DecayFunction(dispersion) {

    override fun getScalingFactor(distance: Double): Double {
        val dist = distanceFromPeak(distance)
        return if (dist > dispersion) 0.0 else 1 - dist / dispersion
    }

    override fun copy(): LinearDecayFunction {
        return LinearDecayFunction(dispersion)
            .also {
                it.peakDistance = peakDistance
            }
    }

    override val name = "Linear"

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return DecayFunction.getTypes()
        }
    }
}