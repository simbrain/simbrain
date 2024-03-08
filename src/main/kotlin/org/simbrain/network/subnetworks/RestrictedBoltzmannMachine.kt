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

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.*
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment.VERTICAL
import org.simbrain.network.util.Direction.NORTH
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix

/**
 * RestrictedBoltzmannMachine implements restricted a Boltzman Machine
 *
 * See https://www.cs.toronto.edu/~hinton/absps/guideTR.pdf
 *
 * @author Makenzy Gilbert
 * @author Jeff Yoshimi
 */
class RestrictedBoltzmannMachine(numVisibleNodes: Int, numHiddenNodes: Int) : Subnetwork(), UnsupervisedNetwork {

    // Set of patterns for visible layer. Rows are inputs to visible layer.
    var trainingPatterns: Matrix

    val hiddenLayer: NeuronArray

    val visibleLayer: NeuronArray

    override var inputData: Matrix = Matrix(10, numVisibleNodes)

    override val inputLayer: NeuronArray
        get() = visibleLayer

    val hiddenToVisible: WeightMatrix

    val visibleToHidden: WeightMatrix

    val infoText: InfoText

    init {
        this.label = "Restricted Boltzmann Machine"

        // TODO: numrows variable
        trainingPatterns = Matrix.rand(10, numVisibleNodes)

        visibleLayer = NeuronArray(numVisibleNodes).apply {
            label = "Visible layer"
            (updateRule as LinearRule).apply {
                upperBound = 1.0
                lowerBound = 0.0
            }
        }
        this.addModel(visibleLayer)

        // Something like a softmax may be used? See 13.1
        hiddenLayer = NeuronArray(numHiddenNodes).apply {
            label = "Hidden Layer"
            updateRule = SigmoidalRule()
        }
        this.addModel(hiddenLayer)

        visibleToHidden = WeightMatrix(visibleLayer, hiddenLayer)
        hiddenToVisible = WeightMatrix(hiddenLayer, visibleLayer)
        this.addModels(visibleToHidden, hiddenToVisible)

        alignNetworkModels(visibleLayer, hiddenLayer, VERTICAL)
        offsetNetworkModel(visibleLayer, hiddenLayer, NORTH, 400.0, 100.0, 100.0)

        infoText = InfoText(stateInfoText)

    }

    val stateInfoText: String
        get() = "Energy: "

    fun updateStateInfoText() {
        infoText.text = stateInfoText
        events.customInfoUpdated.fireAndBlock()
    }

    override val customInfo: NetworkModel
        get() = infoText

    fun trainOnCurrentPattern() {
        // Contrastive divergence
        // Set "k"
    }

    context(Network)
    override fun update() {

        // "Positive phase"
        hiddenLayer.updateInputs()
        hiddenLayer.update()
        // updateWithSampling(hiddenLayer.neuronList)

        // This is where training starts

        // Negative phase / "reconstruction" of visible
        // visibleLayer.updateInputs()
        // visibleLayer.update()

        // Sample code: visibleLayer.outputs.mm(hiddenLayer.outputs)
        // visibleLayer.activations.outerProduct(hiddenLayer.activations)

        // TODO: Positive gradient is outer product of visible and hidden states

        // Now go BACK to hidden using reconstructed visible, this is reconstructed hidden
        // negative gradient is outer product of reconstructed_visible, reconstructed_hidden_states

        //  Weight update
        // weights += learning_rate * (positive_gradient - negative_gradient)
        // visible_bias += learning_rate * (visible - reconstructed_visible)
        // hidden_bias += learning_rate * (hidden_states - reconstructed_hidden_states)

        // updateWithSampling(visibleLayer.neuronList)
        updateStateInfoText()
    }


    /**
     * Helper class for creating new RBM's nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class RBMCreator : EditableObject {
        @UserParameter(label = "Number of visible inputs")
        var numVisible: Int = 25

        @UserParameter(label = "Number of hidden units neurons")
        var numHidden: Int = 20

        fun create(): RestrictedBoltzmannMachine {
            return RestrictedBoltzmannMachine(numVisible, numHidden)
        }
    }
}

fun updateWithSampling(neurons: List<Neuron>) {
    neurons.forEach {n ->
        n.activation = if (Math.random() < n.activation) 1.0 else 0.0
    }
}

// Todo: move to test
fun main() {
    // val rbm = RestrictedBoltzmannMachine(2,2)
    repeat(10) {
        val n1 = Neuron().apply { activation = 0.0 }
        val n2 = Neuron().apply { activation = 0.5 }
        val n3 = Neuron().apply { activation = 1.0 }
        // Expecting 0, ?, 1 each run
        updateWithSampling(listOf(n1, n2, n3))
        println("${n1.activation}, ${n2.activation}, ${n3.activation}")
    }
}
