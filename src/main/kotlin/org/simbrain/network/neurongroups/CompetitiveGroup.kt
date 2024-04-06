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
package org.simbrain.network.neurongroups

import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution

/**
 * Implements a simple competitive network.
 *
 * Current implementations include Rummelhart-Zipser (PDP, 151-193), and
 * Alvarez-Squire 1994, PNAS, 7041-7045.
 *
 * @author Jeff Yoshimi
 */
open class CompetitiveGroup @JvmOverloads constructor(
    neurons: List<Neuron>,
    params: CompetitiveGroupParams = CompetitiveGroupParams()
) : NeuronGroup() {

    var params by GuiEditable(
        label = "Competitive Group Parameters",
        description = "Parameters for the Competitive Group",
        initValue = params.apply { creationMode = false },
        order = 50
    )
    constructor(numNeurons: Int) : this(List(numNeurons) { Neuron() })

    @XStreamConstructor
    private constructor(): this(listOf())

    init {
        addNeurons(neurons)
    }

    private var max = 0.0
    private var activation = 0.0
    private var winner = 0

    /**
     * Specific implementation of competitive learning.
     */
    enum class UpdateMethod {
        /**
         * Rummelhart-Zipser.
         */
        RUMM_ZIPSER {
            override fun toString(): String {
                return "Rummelhart-Zipser"
            }
        },

        /**
         * Alvarez-Squire.
         */
        ALVAREZ_SQUIRE {
            override fun toString(): String {
                return "Alvarez-Squire"
            }
        }
    }

    override fun copy() = CompetitiveGroup(neuronList.map { it.copy() }, params.copy()).also {
        it.max = this.max
        it.activation = this.activation
    }

    context(Network)
    override fun update() {

        neuronList.forEach { it.updateInputs() }
        neuronList.forEach { it.update() }

        max = Double.MIN_VALUE
        winner = 0

        // Determine Winner
        for (i in neuronList.indices) {
            val n = neuronList[i]
            if (n.activation > max) {
                max = n.activation
                winner = i
            }
        }

        // Update weights on winning neuron
        for (i in neuronList.indices) {
            val neuron = neuronList[i]
            if (i == winner) {
                neuron.activation = params.winValue
                neuron.isSpike = neuron.isSpike
                if (params.updateMethod === UpdateMethod.RUMM_ZIPSER) {
                    rummelhartZipser(neuron)
                } else if (params.updateMethod === UpdateMethod.ALVAREZ_SQUIRE) {
                    squireAlvarezWeightUpdate(neuron)
                    decayAllSynapses()
                }
            } else {
                neuron.activation = params.loseValue
                neuron.isSpike = neuron.isSpike
                if (params.useLeakyLearning) {
                    leakyLearning(neuron)
                }
            }
        }
        // normalizeIncomingWeights();
    }

    /**
     * Update winning neuron's weights in accordance with Alvarez and Squire
     * 1994, eq 2. TODO: rate is unused... in fact everything before
     * "double deltaw = learningRate" (line 200 at time of writing) cannot
     * possibly change any variables in the class.
     *
     * @param neuron winning neuron.
     */
    private fun squireAlvarezWeightUpdate(neuron: Neuron) {
        for (synapse in neuron.fanIn) {
            val deltaw =
                params.learningRate * synapse.target.activation * (synapse.source.activation - synapse.target.averageInput)
            synapse.strength = synapse.clip(synapse.strength + deltaw)
        }
        events.fanInUpdated.fireAndBlock()
    }

    /**
     * Update winning neuron's weights in accordance with PDP 1, p. 179.
     *
     * @param neuron winning neuron.
     */
    private fun rummelhartZipser(neuron: Neuron) {
        val sumOfInputs = neuron.totalInput
        // Apply learning rule
        for (synapse in neuron.fanIn) {
            activation = synapse.source.activation

            // Normalize the input values
            if (params.normalizeInputs) {
                if (sumOfInputs != 0.0) {
                    activation /= sumOfInputs
                }
            }
            val deltaw = params.learningRate * (activation - synapse.strength)
            synapse.strength = synapse.clip(synapse.strength + deltaw)
        }
        events.fanInUpdated.fireAndBlock()
    }

    /**
     * Decay attached synapses in accordance with Alvarez and Squire 1994, eq 3.
     */
    private fun decayAllSynapses() {
        for (n in neuronList) {
            for (synapse in n.fanIn) {
                synapse.decay(params.synpaseDecayPercent)
            }
        }
        events.fanInUpdated.fireAndBlock()
    }

    /**
     * Apply leaky learning to provided learning.
     *
     * @param neuron neuron to apply leaky learning to
     */
    private fun leakyLearning(neuron: Neuron) {
        val sumOfInputs = neuron.totalInput
        for (incoming in neuron.fanIn) {
            activation = incoming.source.activation
            if (params.normalizeInputs) {
                if (sumOfInputs != 0.0) {
                    activation /= sumOfInputs
                }
            }
            incoming.strength = incoming.strength + params.leakyLearningRate * (activation - incoming.strength)
        }
        events.fanInUpdated.fireAndBlock()
    }

    /**
     * Normalize weights coming in to this network, separately for each neuron.
     */
    fun normalizeIncomingWeights() {
        for (n in neuronList) {
            val normFactor = n.summedIncomingWeights
            for (s in n.fanIn) {
                s.strength /= normFactor
            }
        }
        events.fanInUpdated.fireAndBlock()
    }

    /**
     * Randomize all weights coming in to this network.
     * TODO: Add gaussian option...
     */
    override fun randomizeIncomingWeights(randomizer: ProbabilityDistribution?) {
        val i: Iterator<Neuron> = neuronList.iterator()
        while (i.hasNext()) {
            val n = i.next()
            for (s in n.fanIn) {
                s.randomize()
            }
        }
        events.fanInUpdated.fireAndBlock()
    }

    /**
     * Returns the sum of all incoming weights to this network.
     *
     * @return the sum of all incoming weights to this network.
     */
    private val summedIncomingWeights: Double
        get() {
            var ret = 0.0
            val i: Iterator<Neuron> = neuronList.iterator()
            while (i.hasNext()) {
                val n = i.next()
                ret += n.summedIncomingWeights
            }
            return ret
        }

    /**
     * Randomize and normalize weights.
     */
    override fun randomize(randomizer: ProbabilityDistribution?) {
        randomizeIncomingWeights(randomizer)
        normalizeIncomingWeights()
    }

    /**
     * Convenience method for setting update style from scripts.
     *
     * @param updateMethod string name of method: "RZ" for Rummelhart Zipser;
     * "AS" for Alvarez-Squire
     */
    fun setUpdateMethod(updateMethod: String) {
        if (updateMethod.equals("RZ", ignoreCase = true)) {
            params.updateMethod = UpdateMethod.RUMM_ZIPSER
        } else if (updateMethod.equals("AS", ignoreCase = true)) {
            params.updateMethod = UpdateMethod.ALVAREZ_SQUIRE
        }
    }

}

@CustomTypeName("Competitive Group")
class CompetitiveGroupParams : NeuronGroupParams() {
    val DEFAULT_LEARNING_RATE = .1

    val DEFAULT_WIN_VALUE = 1.0

    val DEFAULT_LOSE_VALUE = 0.0

    val DEFAULT_NORM_INPUTS = true

    val DEFAULT_USE_LEAKY = false

    val DEFAULT_LEAKY_RATE = DEFAULT_LEARNING_RATE / 4

    val DEFAULT_DECAY_PERCENT = .0008

    val DEFAULT_UPDATE_METHOD = CompetitiveGroup.UpdateMethod.RUMM_ZIPSER

    @UserParameter(label = "Update method", order = 30)
    var updateMethod = DEFAULT_UPDATE_METHOD

    @UserParameter(label = "Learning rate", order = 40)
    var learningRate = DEFAULT_LEARNING_RATE

    @UserParameter(label = "Winner Value", order = 50)
    var winValue = DEFAULT_WIN_VALUE

    @UserParameter(label = "Lose Value", order = 60)
    var loseValue = DEFAULT_LOSE_VALUE

    @UserParameter(label = "Normalize inputs", order = 70)
    var normalizeInputs = DEFAULT_NORM_INPUTS

    @UserParameter(label = "Use Leaky learning", order = 80)
    var useLeakyLearning = DEFAULT_USE_LEAKY

    var leakyLearningRate by GuiEditable(
        initValue = DEFAULT_LEAKY_RATE,
        conditionallyEnabledBy = CompetitiveGroupParams::useLeakyLearning,
        order = 90
    )

    @UserParameter(label = "Decay percent", description = "Percentage by which to decay synapses on each update for Alvarez-Squire update.", order = 100)
    var synpaseDecayPercent = DEFAULT_DECAY_PERCENT

    override fun create(): CompetitiveGroup {
        return CompetitiveGroup(List(numNeurons) { Neuron() }, this)
    }

    override fun copy(): CompetitiveGroupParams {
        return CompetitiveGroupParams().also {
            commonCopy(it)
            it.updateMethod = updateMethod
            it.learningRate = learningRate
            it.winValue = winValue
            it.loseValue = loseValue
            it.normalizeInputs = normalizeInputs
            it.useLeakyLearning = useLeakyLearning
            it.leakyLearningRate = leakyLearningRate
            it.synpaseDecayPercent = synpaseDecayPercent
        }
    }
}