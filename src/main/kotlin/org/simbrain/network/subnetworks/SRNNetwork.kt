/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.subnetworks

import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.UserParameter
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Point2D
import kotlin.math.ceil

/**
 *  Implements a simple recurrent network (See, e.g, Elman 1991).
 *
 * @author Jeff Yoshimi
 */
class SRNNetwork: FeedForward, SupervisedNetwork {

    lateinit var hiddenLayer: NeuronArray

    lateinit var contextLayer: NeuronArray

    lateinit var contextToHidden: WeightMatrix

    override lateinit var trainingSet: MatrixDataset

    override lateinit var testingSet: MatrixDataset

    lateinit var weightMatrixTree: WeightMatrixTree

    constructor(
        numInputNodes: Int = 10,
        numHiddenNodes: Int = 10,
        numOutputNodes: Int = 10,
        initialPosition: Point2D = point(0, 0)
    ): super(
        intArrayOf(numInputNodes, numHiddenNodes, numOutputNodes),
        initialPosition
    ) {

        hiddenLayer = layerList[1].also {
            it.updateRule = SigmoidalRule()
        }

        contextLayer = NeuronArray(numHiddenNodes).apply {
            updateRule = LinearRule()
        }
        contextLayer.fillActivations(.5)
        addModels(contextLayer)

        inputLayer.isClamped = true
        contextLayer.isClamped = true

        outputLayer.updateRule = SigmoidalRule()

        alignNetworkModels(inputLayer, contextLayer, Alignment.HORIZONTAL)
        offsetNetworkModel(inputLayer, contextLayer, Direction.EAST,
            100.0, 100.0, 200.0)

        contextToHidden = WeightMatrix(contextLayer, hiddenLayer)
        contextToHidden.randomize()
        addModels(contextToHidden)

        trainingSet = createDiagonalDataset(numInputNodes, numOutputNodes, shiftAmount = 1)
        testingSet = MatrixDataset(
            inputs = Matrix(ceil(trainingSet.inputs.nrow() * 0.2).toInt(), trainingSet.inputs.ncol()),
            targets = Matrix(ceil(trainingSet.targets.nrow() * 0.2).toInt(), trainingSet.targets.ncol())
        )

        setLocation(initialPosition.x, initialPosition.y)

        weightMatrixTree = WeightMatrixTree(listOf(inputLayer, contextLayer), outputLayer)
    }


    @XStreamConstructor
    protected constructor() : super()

    override var trainerConfig = SRNTrainerConfig(lossFunctionProvider = ::possibleLossFunctions)

    override val name: String
        get() = "SRN"

    override fun onCommit() {}

    context(Network) override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {
        inputLayer.update()
        hiddenLayer.accumulateInputs()
        hiddenLayer.update()
        contextLayer.activations = hiddenLayer.activations.clone()
        outputLayer.accumulateInputs()
        outputLayer.update()
        // Since it's expected, updating weight matrices in case learning rules have been added. In the normal case
        // there is no such rule and these calls are bypassed.
        wmList.forEach { it.update() }
        contextToHidden.update()
    }

    // Forwarded from output layer
    @Producible
    fun getOutputs(): DoubleArray {
        return outputLayer.activationArray
    }

    // Forwards to input layer
    @Consumable
    open fun addInputs(inputs: DoubleArray) {
        inputLayer.addInputs(inputs)
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        super.randomize(randomizer)
        contextToHidden.randomize(randomizer)
    }

    override fun initWeights() {
        (wmList + contextToHidden).forEach { wm -> trainerConfig.weightInitializationStrategy.initializeWeights(wm) }
    }

    override fun initBiases() {
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases()
        }
    }

    override fun copy(): SRNNetwork {
        val copy = SRNNetwork(inputLayer.size, hiddenLayer.size, outputLayer.size)

        // Copy base FeedForward structure
        copy.layerList.zip(layerList).forEach { (copyLayer, originalLayer) ->
            copyLayer.copyFrom(originalLayer)
        }
        copy.wmList.zip(wmList).forEach { (copyWeightMatrix, originalWeightMatrix) ->
            copyWeightMatrix.copyFrom(originalWeightMatrix)
        }

        // Copy context layer
        copy.contextLayer.copyFrom(contextLayer)

        // Copy context to hidden connections
        copy.contextToHidden.copyFrom(contextToHidden)

        // Copy training related properties
        copy.trainingSet = MatrixDataset(
            inputs = trainingSet.inputs.clone(),
            targets = trainingSet.targets.clone()
        )
        copy.testingSet = MatrixDataset(
            inputs = testingSet.inputs.clone(),
            targets = testingSet.targets.clone()
        )
        copy.trainerConfig = SRNTrainerConfig(lossFunctionProvider = ::possibleLossFunctions).copy()

        return copy
    }

    /**
     * Helper class for creating SRN Networks.
     */
    class SRNCreator(val initialPosition: Point2D) : EditableObject {

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 5

        @UserParameter(label = "Number of hidden", order = 20)
        var nhidden = 5

        @UserParameter(label = "Number of outputs",  order = 30)
        var nout = 5

        //TODO: Node type

        override val name = "SRN Network"

        fun create(): SRNNetwork {
            return SRNNetwork(nin, nhidden, nout, initialPosition)
        }

    }
}
