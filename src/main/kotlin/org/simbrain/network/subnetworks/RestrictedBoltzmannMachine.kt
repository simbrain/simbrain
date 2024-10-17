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
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedTrainer
import org.simbrain.network.trainers.updateBiases
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.*
import org.simbrain.util.math.SigmoidFunctions
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import smile.math.matrix.Matrix

/**
 * RestrictedBoltzmannMachine implements restricted a Boltzman Machine
 *
 * See https://www.cs.toronto.edu/~hinton/absps/guideTR.pdf
 *
 * @author Makenzy Gilbert
 * @author Jeff Yoshimi
 */
class RestrictedBoltzmannMachine : Subnetwork, UnsupervisedNetwork {

    lateinit var hiddenLayer: NeuronArray

    lateinit var visibleLayer: NeuronArray

    val defaultRowsInputData = 10

    override lateinit var inputData: Matrix

    override val inputLayer: NeuronArray
        get() = visibleLayer

    lateinit var visibleToHidden: WeightMatrix

    override lateinit var customInfo: InfoText

    override val trainer = UnsupervisedTrainer()

    constructor(numVisibleNodes: Int, numHiddenNodes: Int): super() {
        this.label = "Restricted Boltzmann Machine"
        this.inputData = Matrix.rand(defaultRowsInputData, numVisibleNodes)
        visibleLayer = NeuronArray(numVisibleNodes).apply {
            label = "Visible layer"
            gridMode = true
            isShowBias = false
            updateRule = SigmoidalRule()
        }
        this.addModel(visibleLayer)

        // Something like a softmax may be used? See 13.1
        hiddenLayer = NeuronArray(numHiddenNodes).apply {
            label = "Hidden Layer"
            gridMode = true
            isShowBias = false
            updateRule = SigmoidalRule()
        }
        this.addModel(hiddenLayer)

        visibleToHidden = WeightMatrix(visibleLayer, hiddenLayer)
        this.addModel(visibleToHidden)
        visibleToHidden.randomize()
        alignNetworkModels(visibleLayer, hiddenLayer, Alignment.HORIZONTAL)
        offsetNetworkModel(visibleLayer, hiddenLayer, Direction.EAST, 200.0, 258.0, 126.0)

        customInfo = InfoText(stateInfoText)
        customInfo.location = point(0, -100)
    }

    @XStreamConstructor()
    constructor(): super()

    // See eq 1 https://www.cs.toronto.edu/~hinton/absps/guideTR.pdf
    val stateInfoText: String
        get() {
            val visE = (visibleLayer.activations * visibleLayer.biases).sum()
            val hidE = (hiddenLayer.activations * hiddenLayer.biases).sum()
            val wtsE = hiddenLayer.activations.mm(visibleLayer.activations.transpose()).mul(visibleToHidden.weightMatrix).sum()
            return "Energy: ${(-visE - hidE - wtsE).roundToString(2)}"
        }

    fun updateStateInfoText() {
        customInfo.text = stateInfoText
        events.customInfoUpdated.fire()
    }

    context(Network) override fun accumulateInputs() {
        visibleLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {

        // "Positive phase": visible -> hidden
        hiddenLayer.accumulateInputs()
        hiddenLayer.update()
        updateWithSampling(hiddenLayer)
        
        // Negative phase: hidden -> visible "backwards" through weights
        // Note this is a "reconstructed visible" state, but for Simbrain we are setting the gui visible layer to the reconstructed values
        // Make the hidden layer a row vector and left multiply with the matrix, the make the result back into a column vector
        visibleLayer.addInputs(hiddenLayer.activations.transpose().mm(visibleToHidden.weightMatrix).transpose())
        visibleLayer.update()
        updateWithSampling(visibleLayer)

        updateStateInfoText()
    }

    context(Network)
    override fun trainOnInputData() {
        inputData.toArray().forEach { row ->
            visibleLayer.activations = row.toMatrix()
            trainOnCurrentPattern()
        }
    }

    context(Network)
    override fun trainOnCurrentPattern() {

        val learningRate = trainer.learningRate

        // "Positive phase"
        hiddenLayer.accumulateInputs()
        hiddenLayer.update()
        updateWithSampling(hiddenLayer)

        // Get "reconstructed" activations
        var reconstructedVisible = hiddenLayer.activations.transpose().mm(visibleToHidden.weightMatrix).transpose()
        reconstructedVisible += visibleLayer.biases
        updateWithSampling(reconstructedVisible)
        var reconstructedHidden = visibleToHidden.weightMatrix.mm(reconstructedVisible)
        reconstructedHidden += hiddenLayer.biases
        updateWithSampling(reconstructedHidden)

        // Positive gradient: hidden layer outer product visible layer
        val positiveGradient = hiddenLayer.activations.mm(visibleLayer.activations.transpose())
        // Negative gradient: same as positive but use reconstructions
        val negativeGradient = reconstructedHidden.mm(reconstructedVisible.transpose())

        //  Weight updates
        visibleToHidden.setMatrixValues(visibleToHidden.weightMatrix + (positiveGradient - negativeGradient) * learningRate)

        //  Bias updates.
        visibleLayer.updateBiases(visibleLayer.activations - reconstructedVisible, learningRate)
        hiddenLayer.updateBiases(hiddenLayer.activations - reconstructedHidden, learningRate)

        updateStateInfoText()

    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        visibleToHidden.randomize(NetworkPreferences.weightRandomizer)
        visibleLayer.randomizeBiases(NetworkPreferences.biasesRandomizer)
        hiddenLayer.randomizeBiases(NetworkPreferences.biasesRandomizer)
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

/**
 * Apply logistic sigmoid to components, then treat the resulting values as probabilities and use those
 * probabilities to replace with 0 or 1.
 */
private fun updateWithSampling(array: Matrix) {
    array.validateColumnVector()
    (0 until array.nrow()).forEach { i ->
        array.set(i, 0, SigmoidFunctions.logistic(array.get(i, 0)))
        array.set(i, 0, if (Math.random() < array.get(i, 0)) 1.0 else 0.0)
    }
}

/**
 * Same as above but the NeuronArray is assumed to have been updated and sigmoidal which would apply the sigmoid.
 */
private fun updateWithSampling(na: NeuronArray) {
    na.activations.validateColumnVector()
    (0 until na.activations.nrow()).forEach{ i ->
        na.activations.set(i, 0, if (Math.random() < na.activations.get(i,0)) 1.0 else 0.0)
    }
}



