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

import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.XStreamConstructor
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
import java.awt.geom.Point2D

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
        label = "SRN"

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

        setLocation(initialPosition.x, initialPosition.y)

        weightMatrixTree = WeightMatrixTree(listOf(inputLayer, contextLayer), outputLayer)
    }


    @XStreamConstructor
    protected constructor() : super()

    override var trainer: SRNTrainer = SRNTrainer()

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