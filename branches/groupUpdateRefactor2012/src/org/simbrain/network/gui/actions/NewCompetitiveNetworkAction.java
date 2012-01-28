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

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.CompetitiveCreationDialog;

/**
 * New competitive network action.
 */
public final class NewCompetitiveNetworkAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;


    /**
     * Create a new new competitive network action with the specified
     * network panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NewCompetitiveNetworkAction(final NetworkPanel networkPanel) {

        super("Competitive Network");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        CompetitiveCreationDialog dialog = new CompetitiveCreationDialog(networkPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

//        if (!dialog.hasUserCancelled()) {
//            Competitive compNet = new Competitive(dialog.getNumberOfNeurons());
//            compNet.setEpsilon(dialog.getEpsilon());
//            this.addNetwork(compNet, "Line");
//       }
//       renderObjects();
    }
}