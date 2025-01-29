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
package org.simbrain.network.subnetworks

import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix

/**
 * A discrete Hopfield network.
 */
class Hopfield : Subnetwork, UnsupervisedNetwork {

    lateinit var neuronGroup: NeuronGroup

    override val inputLayer
        get() = neuronGroup

    lateinit var weightMatrix: WeightMatrix

    override val trainer = UnsupervisedTrainer()

    override lateinit var inputData: Matrix

    @UserParameter(label = "Update function")
    var updateFunc = HopfieldUpdate.SYNC

    @UserParameter(label = "Learning rate")
    var learningRate = 0.25

    override lateinit var customInfo: InfoText

    constructor(numNeurons: Int): super() {

        this.inputData = Matrix(10, numNeurons).binaryRandomize()

        // Create main neuron group
        neuronGroup = NeuronGroup(numNeurons)
        neuronGroup.label = "Neurons"
        neuronGroup.applyLayout()
        neuronGroup.location = point(0.0, 0.0)
        addModel(neuronGroup)

        // Set neuron rule
        val binary = BinaryRule()
        binary.threshold = 0.0
        binary.setCeiling(1.0)
        binary.setFloor(0.0)
        neuronGroup.setUpdateRule(binary)
        neuronGroup.setIncrement(1.0)

        // Connect the neurons together
        weightMatrix = WeightMatrix(neuronGroup, neuronGroup)
        weightMatrix.label = "weights"
        addModel(weightMatrix)

        // Symmetric randomization
        // randomize() TODO()

        // Create info text
        customInfo = InfoText(stateInfoText)
        reapplyOffsets()
    }

    @XStreamConstructor
    constructor(): super()

    context(Network) override fun trainOnInputData() {
        inputData.toArray().forEach { row ->
            inputLayer.activationArray = row
            trainOnCurrentPattern()
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        weightMatrix.weightMatrix.randomizeSymmetric(randomizer ?: NetworkPreferences.weightRandomizer)
    }

    context(Network)
    override fun accumulateInputs() {
        neuronGroup.accumulateInputs()
    }

    context(Network)
    override fun update() {
        updateFunc.update(this)
        updateStateInfoText()
    }

    val stateInfoText: String
        get() = "Energy: " + neuronGroup.neuronList.getEnergy().format(4)

    fun updateStateInfoText() {
        customInfo.text = stateInfoText
        events.customInfoUpdated.fire()
    }

    /**
     * Convert 0 to -1 in order to convert binary vectors like (0,1) to bipolar vectors like (-1,1)
     */
    fun bipolar(inputVal: Double): Double {
        return if (inputVal == 0.0) -1.0 else inputVal
    }

    context(Network)
    override fun trainOnCurrentPattern() {
        weightMatrix.setMatrixValues(
            neuronGroup.activations
                .applyFunction(::bipolar)
                .mm(neuronGroup.activations.applyFunction(::bipolar).transpose())
                .mul(learningRate)
                .add(weightMatrix.weightMatrix)
        )
        weightMatrix.weightMatrix.zeroDiagonalInPlace()
        weightMatrix.events.updated.fire()
        events.updated.fire()
    }

    fun reapplyOffsets() {
        alignNetworkModels(neuronGroup, customInfo, Alignment.HORIZONTAL)
        val neuronGroupBound = neuronGroup.neuronList.bound
        offsetNetworkModel(neuronGroup,
            customInfo, Direction.NORTH, 40.0, neuronGroupBound.height, neuronGroupBound.width, 24.0, 0.0)
    }

    /**
     * Main forms of Hopfield update rule.
     */
    enum class HopfieldUpdate {
        STOCHASTIC {
            /**
             * Update a single randomly chosen neuron
             */
            context(Network)
            override fun update(hop: Hopfield) {
                val randomIndex = (0 until hop.neuronGroup.size).random()
                hop.neuronGroup.neuronList[randomIndex].activation = hop.weightMatrix.weightMatrix
                    .row(randomIndex)
                    .dot(hop.neuronGroup.activationArray)
                    .let { if (it > 0.0) 1.0 else 0.0 }
            }

            override fun toString(): String {
                return "Stochastic"
            }
        },
        SEQ {
            /**
             * Sequential update of neurons (same sequence every time)
             */
            context(Network)
            override fun update(hop: Hopfield) {
                (0 until hop.neuronGroup.size).forEach {
                    hop.neuronGroup.neuronList[it].activation = hop.weightMatrix.weightMatrix
                        .row(it)
                        .dot(hop.neuronGroup.activationArray)
                        .let { if (it > 0.0) 1.0 else 0.0 }
                }
            }


            override fun toString(): String {
                return "Sequential"
            }
        },
        SYNC {
            context(Network)
            override fun update(hop: Hopfield) {
                hop.neuronGroup.setActivations(
                    hop.weightMatrix.weightMatrix
                        .mm(hop.neuronGroup.activations)
                        .applyFunction { if (it > 0.0) 1.0 else 0.0 }
                        .toDoubleArray()
                )
            }

            override fun toString(): String {
                return "Synchronous"
            }
        };

        context(Network)
        abstract fun update(hop: Hopfield)
    }

    /**
     * Helper class for creating new Hopfield nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class HopfieldCreator : EditableObject {

        /**
         * Default number of neurons.
         */
        val DEFAULT_NUM_UNITS: Int = 36

        @UserParameter(
            label = "Number of neurons",
            description = "How many neurons this Hofield net should have",
            order = -1
        )
        var numNeurons: Int = DEFAULT_NUM_UNITS

        fun create(): Hopfield {
            return Hopfield(numNeurons)
        }
    }


}
