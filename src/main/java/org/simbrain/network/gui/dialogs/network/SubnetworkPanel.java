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
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.Subnetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * Default subnetwork panel that displays basic info. Most subnetworks should
 * have custom panels so this should not typically be seen.
 *
 * @author Jeff Yoshimi
 */
public class SubnetworkPanel extends JPanel {


    /**
     * Parent network panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Subnetwork.
     */
    private Subnetwork subnetwork;

    /**
     * Constructor for case where an existing subnetwork is being
     * edited.
     *
     * @param np           Parent network panel
     * @param sn           subnetwork being edited
     * @param parentDialog parent dialog containing this panel.
     */
    public SubnetworkPanel(final NetworkPanel np, final Subnetwork sn, final StandardDialog parentDialog) {
        this.networkPanel = np;
        this.subnetwork = sn;
        initPanel(parentDialog);
    }

    /**
     * Initialize the panel.
     *
     * @param parentDialog the parent window
     */
    private void initPanel(final StandardDialog parentDialog) {

        // Set title
        parentDialog.setTitle("Edit " + subnetwork.getClass().getSimpleName());

        // Set up help button
        Action helpAction;
        helpAction = new ShowHelpAction("Pages/Network/subnetwork.html");
        parentDialog.addButton(new JButton(helpAction));
    }

}
