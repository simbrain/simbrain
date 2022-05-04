package org.simbrain.util.decayfunctions

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

abstract class DecayFunction(

    /**
     * If outside of this radius the object has no affect on the network.
     */
    @UserParameter(
        label = "Dispersion",
        description = "If outside of this radius the object has no affect on the network.",
        order = 1
    )
    var dispersion: Double = 70.0,

    @UserParameter(label = "Peak Distance", description = "Peak value", order = 2)
    var peakDistance: Double = 0.0,

    /**
     * If true, add noise to object's stimulus vector.
     */
    @UserParameter(
        label = "Add noise",
        description = "If true, add noise to object's stimulus vector.",
        order = 99,
        tab = "Noise"
    )
    var addNoise: Boolean = false

) : CopyableObject {

    /**
     * Noise generator for this decay function if [DecayFunction.addNoise] is true.
     */
    @UserParameter(label = "Randomizer", isObjectType = true, order = 1000, tab = "Noise")
    var randomizer: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Get the decay amount for the given distance.
     *
     * Returns a number between 0 and 1 that can also be treated as a probability.
     */
    abstract fun getScalingFactor(distance: Double): Double

    /**
     * Distance from peak.
     *
     * Note that when peak = 0, this is just distance
     */
    fun distanceFromPeak(distance: Double): Double {
        return Math.abs(distance - peakDistance)
    }

    fun getNoise(): Double {
        return if (addNoise) return randomizer.sampleDouble() else 0.0
    }

    fun copy(copy: DecayFunction): DecayFunction {
        copy.dispersion = dispersion
        copy.peakDistance = peakDistance
        copy.randomizer = randomizer.deepCopy()
        return copy
    }

    class DecayFunctionSelector(decayFunction: DecayFunction = LinearDecayFunction(100.0)) : EditableObject {
        /**
         * The layout for the neurons in this group.
         */
        @UserParameter(label = "Decay Function", isObjectType = true, tab = "Layout", order = 50)
        var decayFunction: DecayFunction = decayFunction

        override fun getName(): String {
            return "Decay Function"
        }
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
                PowerLawDecayFunction::class.java
            )
        }
    }
}