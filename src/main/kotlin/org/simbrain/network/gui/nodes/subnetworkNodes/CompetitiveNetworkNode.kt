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
import org.simbrain.network.gui.dialogs.makeTrainerPanel
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.JPopupMenu

/**
 * PNode representation of competitive network.
 *
 * @author Jeff Yoshimi
 */
class CompetitiveNetworkNode(networkPanel: NetworkPanel, val competitiveNet: CompetitiveNetwork)
    : SubnetworkNode(networkPanel, competitiveNet) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            with(networkPanel) {
                applyUnsupervisedActions(competitiveNet)
            }
            addSeparator()
            add(CouplingMenu(networkPanel.networkComponent, competitiveNet))
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {competitiveNet.makeTrainerPanel()}
}
