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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.QuickConnectPreferences;
import org.simbrain.network.connections.Radial;
import org.simbrain.network.connections.Sparse;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>ConnectionDialog</b> is a dialog box for setting connection types and
 * properties.
 */
public class QuickConnectPreferencesDialog extends StandardDialog implements
        ActionListener {

    /** Select connection type. */
    private JComboBox cbConnectionType = new JComboBox(
            QuickConnectPreferences.getConnectiontypes());

    /** Main dialog box. */
    private Box mainPanel = Box.createVerticalBox();

    /** Panel for setting connection type. */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /** Panel for setting connection properties. */
    private AbstractConnectionPanel optionsPanel;

    /**
     * Connection dialog default constructor.
     */
    public QuickConnectPreferencesDialog() {
        init();
    }

    /**
     * Initialize default constructor.
     */
    private void init() {
        setTitle("Quick Connect Propeties");

        cbConnectionType.addActionListener(this);
        typePanel.addItem("Connection Type", cbConnectionType);
        cbConnectionType.setSelectedItem(QuickConnectPreferences
                .getCurrentConnection());
        initPanel();
        mainPanel.add(typePanel);
        mainPanel.add(optionsPanel);
        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initialize the connection panel based upon the current connection type.
     */
    private void initPanel() {
        ConnectNeurons connection = (ConnectNeurons) cbConnectionType
                .getSelectedItem();
        if (connection instanceof AllToAll) {
            clearOptionPanel();
            optionsPanel = new AllToAllPanel((AllToAll) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        } else if (connection instanceof OneToOne) {
            clearOptionPanel();
            optionsPanel = new OneToOnePanel((OneToOne) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        } else if (connection instanceof Radial) {
            clearOptionPanel();
            optionsPanel = new RadialPanel((Radial) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        } else if (connection instanceof Sparse) {
            clearOptionPanel();
            optionsPanel = new SparsePanel((Sparse) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        }
        pack();
        setLocationRelativeTo(null);
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
     * Respond to neuron type changes.
     *
     * @param e Action event.
     */
    public void actionPerformed(final ActionEvent e) {
        initPanel();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        QuickConnectPreferences
                .setCurrentConnection((ConnectNeurons) cbConnectionType
                        .getSelectedItem());
        optionsPanel.commitChanges();
    }
}
