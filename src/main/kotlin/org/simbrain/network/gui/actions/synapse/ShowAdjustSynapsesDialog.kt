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
package org.simbrain.network.gui.actions.synapse

import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.createSynapseAdjustmentPanel
import org.simbrain.util.ResourceManager
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JDialog
import javax.swing.KeyStroke

/**
 * Show synapse adjustment dialog.
 */
class ShowAdjustSynapsesDialog(val networkPanel: NetworkPanel) : AbstractAction("Show synapse adjustment dialog...") {

    /**
     * Only enable the action if there is at least one synapse selected.
     */
    private fun updateAction() {
        val atLeastOneSynapseSelected = networkPanel.selectionManager.filterSelectedModels<Synapse>().isNotEmpty()
        isEnabled = atLeastOneSynapseSelected
    }

    /**
     * @param event
     * @see AbstractAction
     */
    override fun actionPerformed(event: ActionEvent) {
        JDialog().apply {
            title = "Adjust selected synapses"
            contentPane = createSynapseAdjustmentPanel(networkPanel.selectionManager.filterSelectedModels<Synapse>())
            pack()
            setLocationRelativeTo(networkPanel)
            isVisible = true
        }
    }

    init {
        putValue(Action.SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"))
        val toolkit = Toolkit.getDefaultToolkit()
        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R, toolkit.menuShortcutKeyMask)
        putValue(Action.ACCELERATOR_KEY, keyStroke)
        updateAction()

        // Add a selection listener to update state based on selection
        networkPanel.selectionManager.events.onSelection { _, _ -> updateAction() }
    }
}