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
package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.createTrainOnPatternAction
import org.simbrain.network.gui.dialogs.getUnsupervisedTrainingPanel
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.StandardDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JPopupMenu

/**
 * PNode representation of competitive network.
 *
 * @author Jeff Yoshimi
 */
class CompetitiveNetworkNode(networkPanel: NetworkPanel?, val competitiveNetwork: CompetitiveNetwork) :
    SubnetworkNode(networkPanel, competitiveNetwork) {
    /**
     * Create a competitive Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the competitive network
     */
    init {
        setContextMenu()
    }

    /**
     * Sets custom menu for Competitive Network node.
     */
    private fun setContextMenu() {
        val menu = JPopupMenu()
        editAction.putValue("Name", "Edit / Train Competitive...")
        menu.add(editAction)
        menu.add(renameAction)
        menu.add(removeAction)
        menu.addSeparator()
        menu.add(with(networkPanel.network) { competitiveNetwork.createTrainOnPatternAction() })
        menu.addSeparator()
        val randomizeNet: Action = object : AbstractAction("Randomize synapses") {
            override fun actionPerformed(event: ActionEvent) {
                competitiveNetwork.randomize()
            }
        }
        menu.add(randomizeNet)
        setContextMenu(menu)
    }

    private fun makeTrainerPanel() = with(networkPanel) {
        getUnsupervisedTrainingPanel(competitiveNetwork) {
            competitiveNetwork.trainOnCurrentPattern()
        }
    }

    override val propertyDialog: StandardDialog
        get() = makeTrainerPanel()
}
