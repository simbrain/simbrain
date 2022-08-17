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
package org.simbrain.network.gui.dialogs;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * <b>NetworkDialog</b> is a dialog box for setting the properties of the
 * Network GUI. If the user presses ok, values become default values. Restore
 * defaults restores to original values. When canceling out the values prior to
 * making any changes are restored.
 */
public class NetworkDialog extends StandardDialog {

    /**
     * Network panel.
     */
    protected NetworkPanel networkPanel;

    /**
     * Main properties panel.
     */
    protected NetworkPropertiesPanel networkPropertiesPanel;

    /**
     * This method is the default constructor.
     *
     * @param np reference to <code>NetworkPanel</code>.
     */
    public NetworkDialog(final NetworkPanel np) {
        networkPanel = np;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {

        // Initialize Dialog
        setTitle("Network Dialog");

        // Main properties tab
        networkPropertiesPanel = new NetworkPropertiesPanel(networkPanel);
        setContentPane(networkPropertiesPanel);
        // Add help button
        JButton helpButton = new JButton("Help");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Network/network_prefs.html");
        helpButton.setAction(helpAction);
        this.addButton(helpButton);
    }

    /**
     * Commits changes not handled in action performed.
     */
    private void commitChanges() {
        networkPropertiesPanel.commitChanges();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

}
