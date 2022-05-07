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
package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.gui.NetworkPanel
import java.beans.PropertyChangeEvent


class SynapseGroup2Node(networkPanel: NetworkPanel, val synapseGroup: SynapseGroup2) :
    ScreenElement(networkPanel) {

    /**
     * Reference to the currently used PNode type.
     */
    private var currentNode: Arrow? = null

    /**
     * PNode that represents an aggregate of visible "loose" [SynapseNode]s.
     * when [SynapseGroup.isDisplaySynapses] is true.
     */
    private var visibleNode: SynapseGroup2NodeVisible? = null

    /**
     * PNode that represents a single one-directional green arrow from
     * one neuron group to another.
     */
    private var simpleNode: SynapseGroup2NodeSimple? = null

    /**
     * PNode that represents a recurrent arrow from a neuron group to itself.
     */
    private var recurrentNode: SynapseGroup2NodeRecurrent? = null

    /**
     * The interaction box for this neuron group.
     */
    var interactionBox: SynapseGroup2InteractionBox

    init {
        // Note the children pnodes to outlined objects are created in
        // networkpanel and added externally to outlined objects
        interactionBox = SynapseGroup2InteractionBox(networkPanel, synapseGroup, this)
        interactionBox.setText(synapseGroup.getLabel())
        addChild(interactionBox)
        toggleSynapseVisibility()
        synapseGroup.source.events.onLocationChange { layoutChildren() }
        synapseGroup.target.events.onLocationChange { layoutChildren() }

        // Handle events
        val events = synapseGroup.events
        events.onDeleted { s: NetworkModel -> removeFromParent() }
        events.onLabelChange { o: String, n: String -> updateText() }
        events.onVisibilityChange { toggleSynapseVisibility() }
        events.onSynapseAdded { s ->
            this@SynapseGroup2Node.networkPanel.createNode(s)
            refreshVisible()
        }
        events.onSynapseRemoved { s ->
            this@SynapseGroup2Node.networkPanel.createNode(s)
            refreshVisible()
        }
    }

    private fun removeEverythingButInteractionBox() {
        removeChild(simpleNode)
        removeChild(visibleNode)
        removeChild(recurrentNode)
    }

    private fun refreshVisible() {
        removeChild(visibleNode)
        visibleNode = null
        toggleSynapseVisibility()
    }


    private fun initializeArrow() {
        if (synapseGroup.displaySynapses) {
            if (visibleNode == null) {
                visibleNode = SynapseGroup2NodeVisible(networkPanel, this)
            }
            currentNode = visibleNode
            return
        }
        if (synapseGroup.isRecurrent()) {
            if (recurrentNode == null) {
                recurrentNode = SynapseGroup2NodeRecurrent(this)
            }
            currentNode = recurrentNode
        } else {
            if (simpleNode == null) {
                simpleNode = SynapseGroup2NodeSimple(this)
            }
            currentNode = simpleNode
        }
    }

    fun toggleSynapseVisibility() {
        if (synapseGroup.displaySynapses) {
            removeEverythingButInteractionBox()
            if (visibleNode == null) {
                visibleNode = SynapseGroup2NodeVisible(networkPanel, this)
            }
            addChild(visibleNode)
            currentNode = visibleNode
        } else {
            removeEverythingButInteractionBox()
            if (synapseGroup.isRecurrent()) {
                if (recurrentNode == null) {
                    recurrentNode = SynapseGroup2NodeRecurrent(this)
                }
                addChild(recurrentNode)
                currentNode = recurrentNode
            } else {
                if (simpleNode == null) {
                    simpleNode = SynapseGroup2NodeSimple(this)
                }
                addChild(simpleNode)
                currentNode = simpleNode
            }
        }
        lowerToBottom()
        interactionBox.raiseToTop()
    }

    override fun layoutChildren() {
        currentNode!!.layoutChildren()
    }

    fun propertyChange(evt: PropertyChangeEvent?) {
        // This is needed for synapse groups within subnetworks
        // to be updated properly when neuron groups are moved.
        layoutChildren()
    }

    /**
     * Update the text in the interaction box.
     */
    fun updateText() {
        interactionBox.setText(synapseGroup.getLabel())
    }

    // /**
    //  * Show randomization dialog
    //  */
    // fun showRandomizationDialog() {
    //     val dialog = StandardDialog()
    //     dialog.contentPane = SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(dialog, synapseGroup)
    //     dialog.pack()
    //     dialog.setLocationRelativeTo(null)
    //     dialog.isVisible = true
    // }

    override fun isSelectable(): Boolean {
        return false
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun getModel(): SynapseGroup2 {
        return synapseGroup
    }

    /**
     * Interface for all PNodes used in as the main representation for a synapse group.
     */
    interface Arrow {
        fun layoutChildren()
    }

}