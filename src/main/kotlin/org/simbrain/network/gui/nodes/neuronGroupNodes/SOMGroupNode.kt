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
package org.simbrain.network.gui.nodes.neuronGroupNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.NeuronGroupNode
import org.simbrain.network.neurongroups.SOMGroup
import org.simbrain.util.Utils
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JMenuItem

/**
 * PNode representation of Self-Organizing Map.
 *
 * @author jyoshimi
 */
class SOMGroupNode(networkPanel: NetworkPanel?, group: SOMGroup?) : NeuronGroupNode(networkPanel, group) {
    /**
     * Create a SOM Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the SOM network
     */
    init {
        // setStrokePaint(Color.green);
        setCustomMenuItems()
        interactionBox = SOMInteractionBox(networkPanel)
        // setOutlinePadding(15f);
        updateText()
    }

    /**
     * Custom interaction box for SOM group node.
     */
    private inner class SOMInteractionBox(net: NetworkPanel?) : NeuronGroupInteractionBox(net) {
        override val toolTipText: String
            get() = "Current learning rate: " + Utils.round(
                (neuronGroup as SOMGroup).learningRate,
                2
            ) + "  Current neighborhood size: " + Utils.round(
                (neuronGroup as SOMGroup).neighborhoodSize, 2
            )
    }

    /**
     * Sets custom menu for SOM node.
     */
    protected fun setCustomMenuItems() {
        super.addCustomMenuItem(JMenuItem(object : AbstractAction("Reset SOM Network") {
            override fun actionPerformed(event: ActionEvent) {
                val group = (neuronGroup as SOMGroup)
                group.reset()
            }
        }))
        super.addCustomMenuItem(JMenuItem(object : AbstractAction("Recall SOM Memory") {
            override fun actionPerformed(event: ActionEvent) {
                val group = (neuronGroup as SOMGroup)
                group.recall()
            }
        }))
        super.addCustomMenuItem(JMenuItem(object : AbstractAction("Randomize SOM Weights") {
            override fun actionPerformed(event: ActionEvent) {
                val group = (neuronGroup as SOMGroup)
                with(networkPanel.network) {
                    group.randomizeIncomingWeights()
                }
            }
        }))
    }
}
