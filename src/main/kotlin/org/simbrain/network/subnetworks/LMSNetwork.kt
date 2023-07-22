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
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.trainers.LMSTrainer2
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.Trainable2
import org.simbrain.network.trainers.createDiagonalDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.geom.Point2D

/**
 * LMS network.
 *
 * @author Jeff Yoshimi
 */
class LMSNetwork(network: Network, nInputs: Int, nOutputs: Int, initialPosition: Point2D? = null) :
    FeedForward(network, intArrayOf(nInputs, nOutputs), initialPosition), Trainable2 {

    override lateinit var trainingSet: MatrixDataset

    override val trainer by lazy {
        LMSTrainer2(this)
    }

    init {
        layerList.forEach { it.updateRule = LinearRule() }
        trainingSet = createDiagonalDataset(nInputs, nOutputs)
        label = "LMS"
    }

    val weightMatrix: WeightMatrix get() = inputLayer.outgoingConnectors.first() as WeightMatrix

    override fun randomize() {
        weightMatrix.randomize()
        // TODO
        // outputLayer.randomizeBiases()
    }

    /**
     * Helper class for creating LMS Networks.
     */
    class LMSCreator(proposedLabel: String, val initialPosition: Point2D?) : EditableObject {

        @UserParameter(label = "Label", order = 5)
        private val label = proposedLabel

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 5

        @UserParameter(label = "Number of outputs (classes)",  order = 20)
        var nout = 4

        //TODO: Node type

        override val name = "LMS Network"

        fun create(net: Network): LMSNetwork {
            return LMSNetwork(net, nin, nout, initialPosition)
        }

    }

}