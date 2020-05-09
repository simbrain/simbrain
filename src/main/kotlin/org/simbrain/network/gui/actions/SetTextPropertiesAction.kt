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

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.gui.showTextPropertyDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

/**
 * Set neuron properties.
 */
class SetTextPropertiesAction(private val networkPanel: NetworkPanel, private val textNodes: List<TextNode>)
    : AbstractAction("Text Properties...") {

    /**
     * Set action text based on number of selected neurons.
     */
    private fun updateAction() {
        // TODO: why is this empty?
    }

    override fun actionPerformed(event: ActionEvent) {
        networkPanel.showTextPropertyDialog(textNodes)
    }

    init {
        putValue(Action.SHORT_DESCRIPTION, "Set the properties of this text, e.g. font and size")
        updateAction()
        networkPanel.selectionManager.events.onSelection { _, _ -> updateAction() }
    }
}