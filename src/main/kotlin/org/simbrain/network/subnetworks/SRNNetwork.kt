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
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SRNTrainer
import org.simbrain.network.trainers.Trainable
import org.simbrain.network.trainers.createDiagonalDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNeuronGroup
import org.simbrain.util.UserParameter
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D

/**
 *  Implements a simple recurrent network (See, e.g, Elman 1991).
 *
 * @author Jeff Yoshimi
 */
open class SRNNetwork(
    network: Network,
    numInputNodes: Int = 10,
    numHiddenNodes: Int = 10,
    numOutputNodes: Int = 10,
    initialPosition: Point2D = point(0, 0)) :
        FeedForward(network,
            intArrayOf(numInputNodes, numHiddenNodes, numOutputNodes),
            initialPosition), Trainable {

    var hiddenLayer: NeuronArray = layerList[1]

    var contextLayer: NeuronArray = NeuronArray(network, numHiddenNodes)

    val contextToHidden: WeightMatrix

    override var trainingSet: MatrixDataset = createDiagonalDataset(numInputNodes, numOutputNodes, shiftAmount = 1)

    override val trainer by lazy {
        SRNTrainer(this)
    }

    init {
        label = "SRN"

        contextLayer = NeuronArray(network, numHiddenNodes)
        addModels(contextLayer)

        inputLayer.isClamped = true
        contextLayer.isClamped = true

        outputLayer.updateRule = SigmoidalRule()

        offsetNeuronGroup(inputLayer, hiddenLayer, Direction.NORTH,
            (betweenLayerInterval / 2).toDouble(), 100.0, 200.0 )
        offsetNeuronGroup(hiddenLayer, outputLayer, Direction.NORTH,
            (betweenLayerInterval / 2).toDouble(), 100.0, 200.0 )
        offsetNeuronGroup(inputLayer, contextLayer, Direction.EAST,
            100.0, 100.0, 200.0 )

        contextToHidden = WeightMatrix(parentNetwork, contextLayer, hiddenLayer)
        contextToHidden.randomize()
        addModels(contextToHidden)

        setLocation(initialPosition.x, initialPosition.y)
    }

    override val name: String
        get() = "SRN"

    override fun onCommit() {}

    override fun update() {
        inputLayer.updateInputs()
        inputLayer.update()
        hiddenLayer.updateInputs()
        hiddenLayer.update()
        contextLayer.activations = hiddenLayer.activations.clone()
        outputLayer.updateInputs()
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

    override fun randomize() {
        super.randomize()
        contextToHidden.randomize()
    }

    /**
     * Helper class for creating SRN Networks.
     */
    class SRNCreator(proposedLabel: String, val initialPosition: Point2D) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        var label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 5

        @UserParameter(label = "Number of hidden", order = 20)
        var nhidden = 5

        @UserParameter(label = "Number of outputs",  order = 30)
        var nout = 5

        //TODO: Node type

        override val name = "SRN Network"

        fun create(net: Network): SRNNetwork {
            return SRNNetwork(net, nin, nhidden, nout, initialPosition)
        }

    }
}