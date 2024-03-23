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

import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.neurongroups.CompetitiveGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * **CompetitiveNetwork** is a small network encompassing a Competitive
 * group. An input layer and input data have been added so that the SOM can be
 * easily trained using existing Simbrain GUI tools
 *
 * @author Jeff Yoshimi
 */
class CompetitiveNetwork : Subnetwork {

    lateinit var competitive: CompetitiveGroup

    lateinit var inputLayer: NeuronGroup

    lateinit var weights: SynapseGroup

    constructor(numInputNeurons: Int, numCompetitiveNeurons: Int): super() {
        this.label = "Competitive Network"

        competitive = CompetitiveGroup(numCompetitiveNeurons)
        competitive.label = "Competitive Group"
        this.addModel(competitive)
        competitive.setLayoutBasedOnSize()

        inputLayer = NeuronGroup(numInputNeurons)
        this.addModel(inputLayer)
        inputLayer.label = "Input layer"
        inputLayer.setClamped(true)
        inputLayer.setLayoutBasedOnSize()
        inputLayer.setLowerBound(0.0)

        weights = SynapseGroup(inputLayer, competitive)
        weights.randomize(UniformRealDistribution(0.0, 1.0))
        this.addModel(weights)
        weights.synapses.forEach { it.lowerBound = 0.0 }

        alignNetworkModels(inputLayer, competitive, Alignment.VERTICAL)
        offsetNeuronCollections(inputLayer, competitive, Direction.NORTH, 200.0)
    }

    @XStreamConstructor
    constructor(): super()

    /**
     * Helper class for creating new competitive nets using [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    class CompetitiveCreator : EditableObject {
        @UserParameter(label = "Number of inputs")
        var numIn: Int = 20

        @UserParameter(label = "Number of competitive neurons")
        var numComp: Int = 20

        fun create(): CompetitiveNetwork {
            return CompetitiveNetwork(numIn, numComp)
        }
    }
}
