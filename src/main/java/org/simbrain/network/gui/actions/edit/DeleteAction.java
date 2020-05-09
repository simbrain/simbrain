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
package org.simbrain.network.gui.actions.edit;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.ConditionallyEnabledAction;
import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Delete action.
 */
public final class DeleteAction extends ConditionallyEnabledAction {

    /**
     * Create a new delete action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public DeleteAction(final NetworkPanel networkPanel) {
        super(networkPanel, "Delete", EnablingCondition.ALLITEMS);

        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/DeleteNeuron.png"));
        putValue(SHORT_DESCRIPTION, "Delete selected node(s) (\"Backspace\" or \"Delete\")");
        networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), this);
        networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), this);
        networkPanel.getActionMap().put(this, this);

    }

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(final ActionEvent event) {
        SwingUtilities.invokeLater(getNetworkPanel()::deleteSelectedObjects);
    }

}