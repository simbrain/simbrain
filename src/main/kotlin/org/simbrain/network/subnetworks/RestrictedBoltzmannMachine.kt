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

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.InfoText
import org.simbrain.network.core.Network
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.getEnergy
import org.simbrain.network.neurongroups.CompetitiveGroup
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.util.Alignment.VERTICAL
import org.simbrain.network.util.Direction.NORTH
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.UserParameter
import org.simbrain.util.format
import org.simbrain.util.propertyeditor.EditableObject

/**
 * RestrictedBoltzmannMachine implements restricted a Boltzman Machine
 *
 * See https://www.cs.toronto.edu/~hinton/absps/guideTR.pdf
 *
 * @author Makenzy Gilbert
 * @author Jeff Yoshimi
 */
public class RestrictedBoltzmannMachine(numVisibleNodes: Int, numHiddenNodes: Int) : Subnetwork() {

    val hiddenLayer: CompetitiveGroup

    val visibleLayer: NeuronGroup

    val hiddenToVisible: SynapseGroup

    val visibleToHidden: SynapseGroup

    val infoText: InfoText

    init {
        this.label = "Restricted Boltzmann Machine"

        visibleLayer = NeuronGroup(numVisibleNodes)
        this.addModel(visibleLayer)
        visibleLayer.label = "Visible layer"
        visibleLayer.setUpperBound(1.0)
        visibleLayer.setLowerBound(0.0)
        visibleLayer.setClamped(true)
        visibleLayer.setLayoutBasedOnSize()

        hiddenLayer = CompetitiveGroup(numHiddenNodes)
        hiddenLayer.label = "Hidden Layer"
        hiddenLayer.setUpperBound(1.0)
        hiddenLayer.setLowerBound(0.0)

        this.addModel(hiddenLayer)
        hiddenLayer.setLayoutBasedOnSize()

        visibleToHidden = SynapseGroup(visibleLayer, hiddenLayer)
        hiddenToVisible = SynapseGroup(hiddenLayer, visibleLayer)
        this.addModels(visibleToHidden, hiddenToVisible)

        alignNetworkModels(visibleLayer, hiddenLayer, VERTICAL)
        offsetNeuronCollections(visibleLayer, hiddenLayer, NORTH, 400.0)

        infoText = InfoText(stateInfoText)

    }

    val stateInfoText: String
        get() = "Energy: " + (hiddenLayer.neuronList + visibleLayer.neuronList).getEnergy().format(4)

    fun updateStateInfoText() {
        infoText.text = stateInfoText
        events.customInfoUpdated.fireAndBlock()
    }

    override val customInfo: NetworkModel
        get() = infoText

    context(Network)
    override fun update() {
        super.update()
        println("TODO: Custom update")
        updateStateInfoText()
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
