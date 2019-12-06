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
package org.simbrain.network.gui.actions.synapse;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.NetworkSelectionEvent;
import org.simbrain.network.gui.NetworkSelectionListener;
import org.simbrain.network.gui.dialogs.synapse.SynapseAdjustmentPanel;
import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Show synapse adjustment dialog.
 */
public final class ShowAdjustSynapsesDialog extends AbstractAction {

    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Construct the action.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ShowAdjustSynapsesDialog(final NetworkPanel networkPanel) {

        super("Show synapse adjustment dialog...");
        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"));
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_R, toolkit.getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, keyStroke);

        this.networkPanel = networkPanel;
        updateAction();

        // Add a selection listener to update state based on selection
        networkPanel.addSelectionListener(new NetworkSelectionListener() {
            /** @see NetworkSelectionListener */
            public void selectionChanged(final NetworkSelectionEvent event) {
                updateAction();
            }
        });

    }

    /**
     * Only enable the action if there is at least one synapse selected.
     */
    private void updateAction() {
        boolean atLeastOneSynapseSelected = (networkPanel.getSelectedModels(Synapse.class).size() > 0);
        if (atLeastOneSynapseSelected) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    /**
     * @param event
     * @see AbstractAction
     */
    public void actionPerformed(final ActionEvent event) {

        final SynapseAdjustmentPanel synapsePanel = SynapseAdjustmentPanel.createSynapseAdjustmentPanel(networkPanel, networkPanel.getSelectedModels(Synapse.class));
        JDialog dialog = new JDialog();
        dialog.setTitle("Adjust selected synapses");
        dialog.setContentPane(synapsePanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }
}