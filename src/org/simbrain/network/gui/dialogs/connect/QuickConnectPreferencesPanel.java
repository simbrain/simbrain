/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.connect;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.QuickConnectPreferences;
import org.simbrain.network.connections.QuickConnectPreferences.ConnectType;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>QuickConnectPreferencesPanel</b> is a dialog box for setting connection
 * types and properties.
 */
public class QuickConnectPreferencesPanel extends JPanel {

    /** Select connection type. */
    private JComboBox<ConnectType> cbConnectionType =
        new JComboBox<ConnectType>(
            QuickConnectPreferences.getConnectiontypes());

    /** Main dialog box. */
    private Box mainPanel = Box.createVerticalBox();

    /** Panel for setting connection type. */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /** Panel for setting connection properties. */
    private AbstractConnectionPanel optionsPanel;

    /** Reference to network panel. */
    private NetworkPanel panel;

    /** Parent dialog. */
    private Window parentWindow;

    /**
     * Connection dialog default constructor.
     */
    public QuickConnectPreferencesPanel(NetworkPanel panel, Window parentWindow) {
        this.panel = panel;
        this.parentWindow = parentWindow;

        // Set up combo box
        cbConnectionType.setSelectedItem(QuickConnectPreferences
            .getCurrentConnection());
        cbConnectionType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                initPanel();
            }
        });
        typePanel.addItem("Connection Type", cbConnectionType);

        // Set up main panel
        initPanel();
        mainPanel.add(typePanel);
        // mainPanel.add(optionsPanel);
        this.add(mainPanel);

        // Set up help button. TODO
        // ShowHelpAction helpAction = new ShowHelpAction(
        // "Pages/Network/connections.html");
        // parentDialog.addButton(new JButton(helpAction));
    }

    /**
     * Initialize the connection panel based upon the current connection type.
     */
    private void initPanel() {
        ConnectNeurons connection = ((ConnectType) cbConnectionType
            .getSelectedItem()).getDefaultInstance();
        // if (connection instanceof AllToAll) {
        // clearOptionPanel();
        // optionsPanel = new AllToAllPanel((AllToAll) connection);
        // optionsPanel.fillDefaultFieldValues();
        // mainPanel.add(optionsPanel);
        // } else if (connection instanceof OneToOne) {
        // clearOptionPanel();
        // optionsPanel = new OneToOnePanel((OneToOne) connection);
        // optionsPanel.fillDefaultFieldValues();
        // mainPanel.add(optionsPanel);
        // } else if (connection instanceof Radial) {
        // clearOptionPanel();
        // optionsPanel = new RadialPanel((Radial) connection);
        // optionsPanel.fillDefaultFieldValues();
        // mainPanel.add(optionsPanel);
        // } else if (connection instanceof Sparse) {
        // clearOptionPanel();
        // optionsPanel = new SparsePanel((Sparse) connection, panel);
        // optionsPanel.fillDefaultFieldValues();
        // mainPanel.add(optionsPanel);
        // }
        parentWindow.pack();
        parentWindow.setLocationRelativeTo(null);
    }

    /**
     * Remove current panel, if any.
     */
    private void clearOptionPanel() {
        if (optionsPanel != null) {
            mainPanel.remove(optionsPanel);
        }
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        QuickConnectPreferences
            .setCurrentConnection((ConnectType) cbConnectionType
                .getSelectedItem());
        // optionsPanel.commitChanges();
    }

    /**
     * Helper class for embedding this panel in a dialog.
     */
    public class QuickConnectDialog extends StandardDialog {

        /** Reference to the preferences panel. */
        private QuickConnectPreferencesPanel panel;

        /**
         * Construct the dialog.
         *
         * @param panel
         *            the embedded panel
         */
        public QuickConnectDialog(QuickConnectPreferencesPanel panel) {
            this.panel = panel;
            setTitle("Quick Connect Propeties");

        }

        @Override
        protected void closeDialogOk() {
            super.closeDialogOk();
            panel.commitChanges();
        }

    }

}
