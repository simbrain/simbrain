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
import org.simbrain.network.gui.WeightMatrixViewer
import javax.swing.JPanel
import javax.swing.JScrollPane


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
    private var expandedNode: SynapseGroup2NodeExpanded? = null

    /**
     * PNode that represents a single one-directional green arrow from
     * one neuron group to another.
     */
    private var directedNode: SynapseGroup2NodeDirected? = null

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
        interactionBox.setText(synapseGroup.label)
        addChild(interactionBox)
        synapseGroup.source.events.onLocationChange { layoutChildren() }
        synapseGroup.target.events.onLocationChange { layoutChildren() }

        // Handle events
        val events = synapseGroup.events
        events.onDeleted { s: NetworkModel -> removeFromParent() }
        events.onLabelChange { o: String, n: String -> updateText() }
        events.onVisibilityChange {
            setVisibility()
        }
        events.onSynapseAdded { s ->
            this@SynapseGroup2Node.networkPanel.createNode(s)
            refreshVisible()
        }
        events.onSynapseRemoved { s ->
            s.delete()
            refreshVisible()
        }
        events.onSynapseListChanged() {
            setVisibility()
            refreshVisible()
        }
        setVisibility()
    }

    private fun removeEverythingButInteractionBox() {
        removeChild(directedNode)
        removeChild(expandedNode)
        removeChild(recurrentNode)
    }

    private fun refreshVisible() {
        removeChild(expandedNode)
        expandedNode = null
        setVisibility()
    }

    fun setVisibility() {
        if (synapseGroup.displaySynapses) {
            removeEverythingButInteractionBox()
            if (expandedNode == null) {
                expandedNode = SynapseGroup2NodeExpanded(networkPanel, this)
            }
            addChild(expandedNode)
            interactionBox.raiseAbove(expandedNode)
            currentNode = expandedNode
        } else {
            removeEverythingButInteractionBox()
            if (synapseGroup.isRecurrent()) {
                if (recurrentNode == null) {
                    recurrentNode = SynapseGroup2NodeRecurrent(this)
                }
                addChild(recurrentNode)
                interactionBox.raiseAbove(recurrentNode)
                currentNode = recurrentNode
            } else {
                if (directedNode == null) {
                    directedNode = SynapseGroup2NodeDirected(this)
                }
                addChild(directedNode)
                interactionBox.raiseAbove(directedNode)
                currentNode = directedNode
            }
        }
        raiseToTop()
    }

    override fun layoutChildren() {
        currentNode!!.layoutChildren()
    }

    /**
     * Update the text in the interaction box.
     */
    fun updateText() {
        interactionBox.setText(synapseGroup.label)
    }

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

    fun weightMatrixViewer() : JScrollPane {
        val weightMatrixPanel = JPanel()
        val matrixScrollPane = JScrollPane(weightMatrixPanel)
        matrixScrollPane.border = null
        weightMatrixPanel.add(
            WeightMatrixViewer.getWeightMatrixPanel(
                WeightMatrixViewer(
                    synapseGroup.source.neuronList,
                    synapseGroup.target.neuronList,
                    networkPanel
                )
            )
        )
        return matrixScrollPane
    }

}