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
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNeuronGroup
import java.awt.geom.Point2D

/**
 * A standard feed-forward network, as a succession of [NeuronArray] and [WeightMatrix] objects.
 *
 * @author Jeff Yoshimi
 */
open class SRNNetwork(
    network: Network,
    numInputNodes: Int,
    numHiddenNodes: Int,
    numOutputNodes: Int,
    initialPosition: Point2D?) : Subnetwork(network) {

    var betweenLayerInterval = 250

    var inputLayer: NeuronArray
        private set

    var hiddenLayer: NeuronArray
        private set

    var outputLayer: NeuronArray
        private set

    init {
        label = "Layered Network"

        inputLayer = NeuronArray(network, numInputNodes)
        addModel(inputLayer)
        hiddenLayer = NeuronArray(network, numHiddenNodes)
        addModel(hiddenLayer)
        outputLayer = NeuronArray(network, numOutputNodes)
        addModel(outputLayer)

        offsetNeuronGroup(inputLayer, hiddenLayer, Direction.NORTH,
            (betweenLayerInterval / 2).toDouble(), 100.0, 200.0 )
        offsetNeuronGroup(hiddenLayer, outputLayer, Direction.NORTH,
            (betweenLayerInterval / 2).toDouble(), 100.0, 200.0 )

        val wmInHidden = WeightMatrix(parentNetwork, inputLayer, hiddenLayer)
        wmInHidden.randomize()
        addModel(wmInHidden)

        val wmHiddenOut = WeightMatrix(parentNetwork, hiddenLayer, outputLayer)
        wmHiddenOut.randomize()
        addModel(wmHiddenOut)

        if (initialPosition != null) {
            setLocation(initialPosition.x, initialPosition.y)
        }
    }

    override val name: String
        get() = "SRN"

    override fun onCommit() {}

    override fun update() {
        inputLayer.updateInputs()
        inputLayer.update()
        hiddenLayer.updateInputs()
        hiddenLayer.update()
        outputLayer.updateInputs()
        outputLayer.update()
    }
}