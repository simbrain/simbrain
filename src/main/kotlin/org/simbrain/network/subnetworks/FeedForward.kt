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
import org.simbrain.network.core.randomizeBiases
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.NormalDistribution
import java.awt.geom.Point2D

/**
 * A standard feed-forward network, as a succession of [NeuronArray] and [WeightMatrix] objects.
 *
 * @author Jeff Yoshimi
 */
open class FeedForward : Subnetwork {

    var betweenLayerInterval = 250

    /**
     * Ordered reference to [NeuronArray]'s maintained in [Subnetwork.modelList]
     */
    val layerList: MutableList<NeuronArray> = ArrayList()

    val wmList: MutableList<WeightMatrix> = ArrayList()

    lateinit var inputLayer: NeuronArray
        private set

    lateinit var outputLayer: NeuronArray
        private set

    constructor(): super()

    /**
     * @param parentNetwork Parent network
     * @param nodesPerLayer Integers 1...n correspond to number of nodes in layers 1..n
     * @param initialPosition Center location for network.
     */
    constructor(nodesPerLayer: IntArray, initialPosition: Point2D?): super() {
        label = "Layered Network"
        inputLayer = NeuronArray(nodesPerLayer[0])
        addModel(inputLayer)
        layerList.add(inputLayer)

        // Memory of last layer created
        var lastLayer = inputLayer

        // Make hidden layers and output layer
        for (i in 1 until nodesPerLayer.size) {
            val hiddenLayer = NeuronArray(nodesPerLayer[i])
            addModel(hiddenLayer)
            layerList.add(hiddenLayer)
            offsetNetworkModel(
                lastLayer,
                hiddenLayer,
                Direction.NORTH,
                (betweenLayerInterval / 2).toDouble(),
                100.0,
                200.0
            )

            // Add weight matrix
            val wm = WeightMatrix(lastLayer, hiddenLayer)
            wm.randomize()
            addModel(wm)
            wmList.add(wm)

            // Reset last layer
            lastLayer = hiddenLayer
        }
        if (initialPosition != null) {
            setLocation(initialPosition.x, initialPosition.y)
        }
        outputLayer = lastLayer
    }

    override val name: String
        get() = "Feedforward"

    override fun onCommit() {}

    override fun randomize(randomizer: ProbabilityDistribution?) {
        wmList.forEach { wm -> wm.randomize(NormalDistribution(0.0, .1)) }
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases(NormalDistribution(0.0, .01))
        }
    }

    context(Network)
    override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {
        inputLayer.update()
        for (i in 1 until layerList.size - 1) {
            layerList[i].accumulateInputs()
            layerList[i].update()
        }
        outputLayer.accumulateInputs()
        outputLayer.update()
    }

    fun hiddenLayers() = layerList.drop(1).take(layerList.size-2)
}