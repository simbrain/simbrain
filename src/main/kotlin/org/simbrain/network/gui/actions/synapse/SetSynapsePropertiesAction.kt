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
import org.simbrain.network.gui.showSelectedSynapseProperties
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.KeyStroke

/**
 * Set synapse properties.
 */
class SetSynapsePropertiesAction(val networkPanel: NetworkPanel) : AbstractAction("Synapse Properties...") {

    /**
     * Set action text based on number of selected neurons.
     */
    private fun updateAction() {
        val numSynapses = networkPanel.selectionManager.filterSelectedModels<Synapse>().size
        isEnabled = if (numSynapses > 0) {
            putValue(Action.NAME, "Edit $numSynapses Selected ${if (numSynapses > 1) "Synapses" else "Synapse"}")
            true
        } else {
            putValue(Action.NAME, "Edit Selected Synapse(s)")
            false
        }
    }

    override fun actionPerformed(event: ActionEvent) {
        networkPanel.showSelectedSynapseProperties()
    }

    init {
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().menuShortcutKeyMask))
        networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('e'), this)
        putValue(Action.SHORT_DESCRIPTION, "Set the properties of selected synapses")
        updateAction()

        // add a selection listener to update state based on selection
        networkPanel.selectionManager.events.onSelection { _, _ -> updateAction() }
    }
}