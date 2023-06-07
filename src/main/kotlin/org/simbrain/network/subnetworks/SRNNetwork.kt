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
import org.simbrain.network.trainers.BackpropTrainer2
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.Trainable2
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNeuronGroup
import org.simbrain.util.UserParameter
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.geom.Point2D

/**
 * A standard feed-forward network, as a succession of [NeuronArray] and [WeightMatrix] objects.
 *
 * @author Jeff Yoshimi
 */
open class SRNNetwork(
    network: Network,
    numInputNodes: Int = 10,
    numHiddenNodes: Int = 10,
    numOutputNodes: Int = 10,
    initialPosition: Point2D = point(0, 0)) :
        BackpropNetwork(network,
            intArrayOf(numInputNodes, numHiddenNodes, numOutputNodes),
            initialPosition), Trainable2 {

    //TODO: Extend Feed forward?

    var hiddenLayer: NeuronArray = NeuronArray(network, numHiddenNodes)

    var contextLayer: NeuronArray = NeuronArray(network, numHiddenNodes)

    override var trainingSet: MatrixDataset = MatrixDataset(numInputNodes, numOutputNodes)

    override val trainer by lazy {
        BackpropTrainer2(this)
    }

    init {
        label = "SRN"

        hiddenLayer = NeuronArray(network, numHiddenNodes)
        contextLayer = NeuronArray(network, numHiddenNodes)
        addModels(inputLayer, hiddenLayer, contextLayer, outputLayer)

        offsetNeuronGroup(inputLayer, hiddenLayer, Direction.NORTH,
            (betweenLayerInterval / 2).toDouble(), 100.0, 200.0 )
        offsetNeuronGroup(hiddenLayer, outputLayer, Direction.NORTH,
            (betweenLayerInterval / 2).toDouble(), 100.0, 200.0 )
        offsetNeuronGroup(inputLayer, contextLayer, Direction.EAST,
            100.0, 100.0, 200.0 )

        val wmInHidden = WeightMatrix(parentNetwork, inputLayer, hiddenLayer)
        wmInHidden.randomize()
        val wmCopy = WeightMatrix(parentNetwork, contextLayer, hiddenLayer)
        wmInHidden.clear()
        val wmHiddenOut = WeightMatrix(parentNetwork, hiddenLayer, outputLayer)
        wmHiddenOut.randomize()
        addModels(wmInHidden, wmCopy, wmHiddenOut)

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
        contextLayer.activations = hiddenLayer.activations
        outputLayer.updateInputs()
        outputLayer.update()
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

    /**
     * Helper class for creating SRN Networks.
     */
    class SRNCreator(proposedLabel: String, val initialPosition: Point2D) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 5

        @UserParameter(label = "Number of hidden", order = 20)
        var nhidden = 5

        @UserParameter(label = "Number of outputs",  order = 30)
        var nout = 4

        //TODO: Node type

        override val name = "SRN Network"

        fun create(net: Network): SRNNetwork {
            return SRNNetwork(net, nin, nhidden, nout, initialPosition)
        }

    }
}