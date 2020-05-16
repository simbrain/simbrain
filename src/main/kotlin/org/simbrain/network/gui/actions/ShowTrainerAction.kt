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
package org.simbrain.network.gui.actions

import org.simbrain.network.core.Neuron
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.ResourceManager
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

/**
 * Show Trainer object for training selected source and target neurons.
 */
class ShowTrainerAction(val networkPanel: NetworkPanel) : AbstractAction("Show Trainer...") {

    /**
     * Only enable the action if there is at least one source and one target neuron.
     */
    private fun updateAction() {
        val atLeastOneSourceSelected = networkPanel.selectionManager.filterSelectedSourceModels<Neuron>().isNotEmpty()
        val atLeastOneTargetSelected = networkPanel.selectionManager.selectedModels.isNotEmpty()
        isEnabled = atLeastOneSourceSelected && atLeastOneTargetSelected
    }

    /**
     * @param event
     * @see AbstractAction
     */
    override fun actionPerformed(event: ActionEvent) {
        // Trainer trainer = new Trainer(networkPanel.getNetwork(),
        // networkPanel.getSourceModels(Neuron.class),
        // networkPanel.getSelectedModels(Neuron.class), new Backprop());
        // TrainerPanel trainerPanel = new TrainerPanel(networkPanel, trainer);
        // GenericFrame frame = networkPanel.displayPanel(trainerPanel,
        // "Trainer panel");
        // trainerPanel.setFrame(frame);
    }

    init {
        putValue(Action.SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Trainer.png"))
        updateAction()
        networkPanel.selectionManager.events.onSelection { _, _ -> updateAction() }
    }
}