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

import org.simbrain.network.connections.QuickConnectionManager;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>QuickConnectPreferencesPanel</b> is a dialog box for setting the
 * properties of the quick connection manager, which is used when creating
 * connections on loose neurons using key commands.
 */
public class QuickConnectPreferencesPanel extends JPanel {

    /**
     * Select connection type.
     */
    private JComboBox<ConnectionPanel2> cbConnectionType;

    /**
     * Main dialog box.
     */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * Panel for setting connection type.
     */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /**
     * Reference to network panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Parent dialog.
     */
    private final Window parentWindow;

    /**
     * Panel which holds the connection panels (for use in resizing).
     */
    private JPanel connectionPanelHolder = new JPanel(new CardLayout());

    /**
     * List of connection panels.
     */
    private List<ConnectionPanel2> connectorPanels = new ArrayList();

    /**
     * Connection dialog default constructor.
     *
     * @param panel parent network panel
     * @param parentWindow parent window
     */
    public QuickConnectPreferencesPanel(NetworkPanel panel, final Window parentWindow) {
        this.networkPanel = panel;
        this.parentWindow = parentWindow;

        cbConnectionType = new JComboBox();
        typePanel.addItem("Quick Connection Type", cbConnectionType);

        // Set up connection holder
        QuickConnectionManager quickConnector = panel.getQuickConnector();
        connectorPanels.add(new ConnectionPanel2(parentWindow, quickConnector.getAllToAll()));
        connectorPanels.add(new ConnectionPanel2(parentWindow, quickConnector.getOneToOne()));
        connectorPanels.add(new ConnectionPanel2(parentWindow, quickConnector.getRadialSimple()));
        connectorPanels.add(new ConnectionPanel2(parentWindow, quickConnector.getRadial()));
        connectorPanels.add(new ConnectionPanel2(parentWindow, quickConnector.getSparse()));

        // Set up main panel
        JLabel infoLabel = new JLabel("Set preferences for making \"Quick connections\" using keyboard shortucts");
        infoLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(infoLabel);
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(typePanel);
        mainPanel.add(connectionPanelHolder);
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        this.add(mainPanel);

        // Set up dropdown box
        ConnectionPanel2 initialSelection = connectorPanels.get(0);
        for (ConnectionPanel2  cp : connectorPanels) {
            cbConnectionType.addItem(cp);
            if(cp.getConnection() == networkPanel.getQuickConnector().getCurrentConnector()) {
                initialSelection = cp;
            }
            connectionPanelHolder.add(cp, cp.getConnection().getName());
        }
        cbConnectionType.setSelectedItem(initialSelection);
        cbConnectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                CardLayout cl = (CardLayout)(connectionPanelHolder.getLayout());
                ConnectionPanel2 cp =  (ConnectionPanel2)cbConnectionType.getSelectedItem();
                cl.show(connectionPanelHolder, cp.getConnection().getName());
            }
        });

    }

    /**
     * Fill field values (used e.g. when restoring defaults so the panel
     * properly displays changed values).
     */
    public void fillFieldValues() {
        //TODO
        // connectorPanels[0].fillFieldValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        QuickConnectionManager quickConnector = networkPanel.getQuickConnector();
        ConnectionPanel2 cp =  (ConnectionPanel2)cbConnectionType.getSelectedItem();
        quickConnector.setCurrentConnector(cp.getConnection());
        cp.getConnectionProperties().commitChanges();
        quickConnector.setExcitatoryRatio(cp.getPolarityPanel().getPercentExcitatory());
        quickConnector.setExRandomizer(cp.getPolarityPanel().getExRandomizer());
        quickConnector.setInRandomizer(cp.getPolarityPanel().getInRandomizer());
        quickConnector.setUseExcitatoryRandomization(cp.getPolarityPanel().exRandomizerEnabled());
        quickConnector.setUseInhibitoryRandomization(cp.getPolarityPanel().inRandomizerEnabled());
    }

}
