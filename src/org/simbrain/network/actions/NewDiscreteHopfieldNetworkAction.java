/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.DiscreteHopfieldDialog;

/**
 * New discrete hopfield network action.
 */
public final class NewDiscreteHopfieldNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new new discrete hopfield network action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewDiscreteHopfieldNetworkAction(final NetworkPanel networkPanel) {

        super("Discrete Hopfield");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }



    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        DiscreteHopfieldDialog dialog = new DiscreteHopfieldDialog(networkPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            if (dialog.getType() == HopfieldDialog.DISCRETE) {
//                DiscreteHopfield hop = new DiscreteHopfield(dialog
//                        .getNumUnits());
//                this.addNetwork(hop, dialog.getCurrentLayout());
//            } else if (dialog.getType() == HopfieldDialog.CONTINUOUS) {
//                ContinuousHopfield hop = new ContinuousHopfield(dialog
//                        .getNumUnits());
//                this.addNetwork(hop, dialog.getCurrentLayout());
//            }
//        }
//
//        repaint();

    }
}