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

import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.gui.getDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import javax.swing.*


class SynapseGroup2InteractionBox(
    networkPanel: NetworkPanel,
    val synapseGroup: SynapseGroup2,
    val synapseGroupNode: SynapseGroup2Node
) : InteractionBox(networkPanel) {

    override fun getPropertyDialog(): JDialog {
       return synapseGroupNode.getDialog()
    }

    override fun getModel(): SynapseGroup2 {
        return synapseGroup
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun getContextMenu(): JPopupMenu {

        val menu = JPopupMenu()

        // Edit
        menu.add(networkPanel.createAction(name = "Edit synapse group...") {
            synapseGroupNode.getDialog().makeVisible()
        })
        menu.add(networkPanel.createAction(
            iconPath = "menu_icons/RedX_small.png",
            name = "Delete synapse group"
        ) {
            synapseGroup.delete()
        })
        menu.add(networkPanel.createAction(
            name = "Rename synapse group..."
        ) {
            val newName = JOptionPane.showInputDialog("Name:", synapseGroup.label)
            synapseGroup.label = newName
        })

        // Selection stuff
        menu.addSeparator()
        menu.add(networkPanel.createAction(name = "Select synapses") {
            synapseGroup.synapses.forEach{it.select()}
        })
        menu.add(networkPanel.createAction(name = "Select incoming neurons") {
            synapseGroup.source.neuronList.forEach{it.select()}
        })
        menu.add(networkPanel.createAction(name = "Select outgoing neurons") {
            synapseGroup.target.neuronList.forEach{it.select()}
        })

        // Weight adjustment
        menu.addSeparator()
        menu.add(JMenuItem(networkPanel.createAction(
            iconPath = "menu_icons/grid.png",
            name = "Show weight matrix...",
        ) {
            JDialog().also{
                it.add(synapseGroupNode.weightMatrixViewer())
            }.display()
        }))

        // Freezing actions
        menu.addSeparator()
        menu.add(networkPanel.createAction(
            name = "Freeze synapses",
            description = "Freeze all synapses in this group (prevent learning)")
        {
            synapseGroup.synapses.forEach{it.isFrozen = true}
        })
        menu.add(networkPanel.createAction(
            name = "Unfreeze synapses",
            description = "Unfreeze all synapses in this group (allow learning)")
        {
            synapseGroup.synapses.forEach{it.isFrozen = false}
        })

        // Synapse Enabling actions
        menu.addSeparator()
        menu.add(networkPanel.createAction(
            name = "Disable synapses",
            description = "Enable all synapses in this group (allow activation to pass through synapses)")
        {
            synapseGroup.synapses.forEach{it.isEnabled = false}
        })
        menu.add(networkPanel.createAction(
            name = "Enable synapses",
            description = "Enable all synapses in this group (allow activation to pass through synapses)")
        {
            synapseGroup.synapses.forEach{it.isEnabled = true}
        })

        // Synapse Visibility
        menu.addSeparator()
        menu.add(networkPanel.networkActions.createSynapseGroupVisibilityAction())

        // Coupling menu
        val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(synapseGroup)
        menu.addSeparator()
        menu.add(couplingMenu)
        return menu
    }

    override fun getToolTipText(): String {
        return "Synapses: " + synapseGroup.size() + " Density: " + synapseGroup.size()
            .toDouble() / (synapseGroup.source.size() * synapseGroup.target.size())
    }

}