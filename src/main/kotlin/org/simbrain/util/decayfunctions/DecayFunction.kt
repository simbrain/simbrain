package org.simbrain.util.decayfunctions

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.math.abs

abstract class DecayFunction(

    /**
     * If outside of this radius the object should have no effect on the network.
     */
    @UserParameter(
        label = "Dispersion",
        description = "If outside of this radius the object has no affect on the network.",
        minimumValue = 0.0,
        order = 1
    )
    var dispersion: Double = 70.0,

    /**
     * The "center" of the decay function where it takes its maximal value.
     */
    @UserParameter(label = "Peak Distance", description = "Peak value", order = 2)
    var peakDistance: Double = 0.0,


) : CopyableObject {

    /**
     * Get the decay amount for the given distance.
     *
     * Returns a number between 0 and 1 that can also be treated as a probability.
     */
    // TODO: But note these are not normalized to be probability density functions
    abstract fun getScalingFactor(distance: Double): Double

    /**
     * Distance from peak.
     *
     * Note that when peak = 0, this is just distance
     */
    fun distanceFromPeak(distance: Double): Double {
        return abs(distance - peakDistance)
    }

    fun copy(copy: DecayFunction): DecayFunction {
        copy.dispersion = dispersion
        copy.peakDistance = peakDistance
        return copy
    }

    class DecayFunctionSelector(decayFunction: DecayFunction = LinearDecayFunction(100.0)) : EditableObject {
        /**
         * The layout for the neurons in this group.
         */
        @UserParameter(label = "Decay Function", isObjectType = true, tab = "Layout", order = 50)
        var decayFunction: DecayFunction = decayFunction

        override val name = "Decay Function"
    }

    companion object {

        /**
         * Decay functions for drop-down list used by [org.simbrain.util.propertyeditor.ObjectTypeEditor] to set a
         * type of probability distribution.
         */
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return listOf(
                StepDecayFunction::class.java,
                LinearDecayFunction::class.java,
                ExponentialDecayFunction::class.java,
            )
        }
    }
}