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
package org.simbrain.network.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.resource.ResourceManager;

/**
 * Clamps weights action.
 */
public final class ClampWeightsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new clamp weights action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public ClampWeightsAction(final NetworkPanel networkPanel) {

        super("Clamp Weights");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;

        putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp_W.png"));
        putValue(SHORT_DESCRIPTION, "Clamp Weights");
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        // Perform action
//        JToggleButton cb = (JToggleButton) event.getSource();
        if (event.getSource() instanceof JToggleButton) {
            JToggleButton cb = (JToggleButton) event.getSource();
            networkPanel.getRootNetwork().setClampWeights(cb.isSelected());
        } else {
            JCheckBoxMenuItem cb = (JCheckBoxMenuItem) event.getSource();
            networkPanel.getRootNetwork().setClampWeights(cb.isSelected());
        }

        // Determine status
//        networkPanel.getRootNetwork().setClampWeights(cb.isSelected());

    }
}