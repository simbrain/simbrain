package org.simbrain.util.decayfunctions

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.math.abs

/**
 * Decay functions return a scaling factor between 0 and 1 based on distance.
 *
 * The scaling factor is 1 at the [peakDistance] (default 0) and decreases as distance from the peak increases.
 * Beyond the [dispersion] radius, the scaling factor is 0 (or near 0 for Gaussian).
 *
 * These functions are used to model distance-dependent effects, such as how strongly a spatial object
 * affects a network based on how far away it is. The scaling factor can also be interpreted as a probability.
 *
 * Available implementations:
 * - [StepDecayFunction]: 1 within dispersion, 0 outside
 * - [LinearDecayFunction]: Linear decrease from 1 to 0
 * - [GaussianDecayFunction]: Smooth Gaussian falloff
 */
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
    @UserParameter(label = "Peak distance", description = "Peak value.", order = 2)
    var peakDistance: Double = 0.0,

    /**
     * Scales the output of the decay function. Can be interpreted as a base probability
     * that is further modulated by distance.
     */
    @UserParameter(
        label = "Base Multiplier",
        description = "Scales the output probability (0-1).",
        minimumValue = 0.0,
        maximumValue = 1.0,
        increment = 0.1,
        order = 3
    )
    var baseMultiplier: Double = 1.0,

) : CopyableObject {

    /**
     * Get the scaling factor for the given distance.
     *
     * Returns a value between 0 and 1 that scales with distance. The scaling factor is 1 at [peakDistance]
     * and decreases as distance increases, reaching 0 at or beyond [dispersion] (or near 0 for Gaussian).
     *
     * Can also be treated as a probability.
     */
    abstract fun getScalingFactor(distance: Double): Double

    // TODO: Stub for future implementation of, for example, elliptical decay functions
    // open fun getScalingFactor(relativeLocation: Point2D): Double {
    //     return 0.0
    // }

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
        copy.baseMultiplier = baseMultiplier
        return copy
    }

    class DecayFunctionSelector(decayFunction: DecayFunction = LinearDecayFunction(100.0)) : EditableObject {
        /**
         * The layout for the neurons in this group.
         */
        @UserParameter(label = "Decay Function", tab = "Layout", order = 50)
        var decayFunction: DecayFunction = decayFunction

        override val name = "Decay Function"
    }

    override fun getTypeList() = decayFunctionTypes

}

val decayFunctionTypes = listOf(
    StepDecayFunction::class.java,
    LinearDecayFunction::class.java,
    GaussianDecayFunction::class.java,
)