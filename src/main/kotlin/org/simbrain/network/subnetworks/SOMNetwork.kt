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

import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.SOMGroup
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject

/**
 * **SOMNetwork** is a small network encompassing an SOM group. An input
 * layer and input data have been added so that the SOM can be easily trained
 * using existing Simbrain GUI tools
 *
 * @author Jeff Yoshimi
 */
class SOMNetwork(numSOMNeurons: Int, numInputNeurons: Int) : Subnetwork() {

    val som: SOMGroup

    val inputLayer: NeuronGroup

    /**
     * Construct an SOM Network.
     *
     * @param numSOMNeurons   number of neurons in the SOM layer
     * @param numInputNeurons number of neurons in the input layer
     */
    init {
        this.label = "SOM Network"
        som = SOMGroup(numSOMNeurons)
        som.label = "SOM Group"
        this.addModel(som)
        som.applyLayout()

        inputLayer = NeuronGroup(numInputNeurons)
        inputLayer.setLayoutBasedOnSize()
        this.addModel(inputLayer)
        for (neuron in inputLayer.neuronList) {
            neuron.lowerBound = 0.0
        }
        inputLayer.label = "Input layer"
        inputLayer.setClamped(true)


        // Connect layers
        val sg = SynapseGroup(inputLayer, som, AllToAll())
        addModel(sg)

        offsetNeuronCollections(inputLayer, som, Direction.NORTH, 450.0)
    }

    /**
     * Helper class for creating new Hopfield nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class SOMCreator : EditableObject {
        @UserParameter(label = "Number of inputs")
        var numIn: Int = 16

        @UserParameter(label = "Number of som neurons")
        var numSom: Int = 20

        /**
         * Create the som net
         */
        fun create(): SOMNetwork {
            return SOMNetwork(numIn, numSom)
        }
    }
}
