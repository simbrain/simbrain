/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util

import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import java.util.*

/**
 * Stimulus represents a "distal stimulus" in the form of a vector that can decay with distance, have noise added, etc.
 */
class SmellSource : EditableObject {

    @UserParameter(label = "Stimulus vector", description = "Stimulus values associated with this smell", order
    = 1)
    var stimulusVector: DoubleArray?
        private set

    @UserParameter(
        label = "Decay function",
        description = "Method for calculating decay of stimulus as a function of distance",
        showDetails = false,
        order = 5
    )
    var decayFunction: DecayFunction = LinearDecayFunction(70.0)

    @UserParameter(label = "Add noise", description = "If true, add noise to object's stimulus vector.", order = 10)
    var addNoise = false

    var randomizer: ProbabilityDistribution by GuiEditable(
        initValue = UniformRealDistribution(),
        label = "Randomizer",
        description = "Noise generator for this decay function if \"addNoise\" is true.",
        showDetails = false,
        conditionallyEnabledBy = SmellSource::addNoise,
        order = 15
    )

    constructor(distalstim: DoubleArray?) {
        stimulusVector = distalstim
    }

    constructor(numDimensions: Int) {
        stimulusVector = DoubleArray(numDimensions)
        for (i in 0 until numDimensions) {
            stimulusVector!![i] = Math.random()
        }
    }

    /**
     * Calculate what impact the object will have on the creature's receptors
     * (input nodes) based on its distance from this object and its features
     * (whether it is a "noisy object", and how the stimulus decays). That is,
     * calculate the proximal stimulus this distal stimulus gives rise to.
     *
     * @param distance distance of creature from object
     * @return proximal stimulus to creature caused by this object
     */
    fun getStimulus(distance: Double): DoubleArray {
        val scalingFactor = decayFunction.getScalingFactor(distance)
        return Arrays.stream(stimulusVector)
            .map { s: Double -> s * scalingFactor + noise }
            .toArray()
    }

    private val noise: Double
        private get() = if (addNoise) {
            randomizer.sampleDouble()
        } else {
            0.0
        }

    /**
     * Randomize values.
     */
    fun randomize() {
        for (i in 0 until stimulusDimension) {
            stimulusVector!![i] = randomizer.sampleDouble()
        }
    }

    /**
     * Return the number of dimensions in the stimulus vector.
     */
    val stimulusDimension: Int
        get() = if (stimulusVector == null) {
            0
        } else stimulusVector!!.size

    var dispersion: Double
        get() = decayFunction.dispersion
        set(d) {
            decayFunction.dispersion = d
        }

    companion object {
        fun createScalarSource(`val`: Double): SmellSource {
            return SmellSource(doubleArrayOf(`val`))
        }
    }
}