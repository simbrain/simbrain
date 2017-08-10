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
package org.simbrain.network.gui.dialogs.connect;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog wrapper for all connection panels.
 *
 * @author jyoshimi
 * @author ztosi
 */
@SuppressWarnings("serial")
public class ConnectionDialog extends StandardDialog {

    /** Parent network panel. */
    private final NetworkPanel networkPanel;

    /** The connection panel wrapped in this dialog. */
    private AbstractConnectionPanel connectionPanel;

    /** The main panel. */
    private JPanel mainPanel;

    /** The connection properties panel. */
    private ConnectionSynapsePropertiesPanel propertiesPanel;

    /** The excitatory ratio and randomizer panel. */
    private SynapsePolarityAndRandomizerPanel eirPanel;

    /** Drop down triangle for synapse properties. */
    private DropDownTriangle detailTriangle;

    /**
     * Create an instance of a connection dialog.
     *
     * @param optionsPanel the connection panel
     * @param connection the connection object
     * @param networkPanel the parent panel
     * @return the constructed dialog
     */
    public static ConnectionDialog createConnectionDialog(
            final AbstractConnectionPanel optionsPanel,
            final ConnectNeurons connection, final NetworkPanel networkPanel) {
        ConnectionDialog cd = new ConnectionDialog(optionsPanel, connection,
                networkPanel);
        cd.init();
        return cd;
    }

    /**
     * Construct the dialog.
     *
     * @param networkPanel parent panel
     * @param optionsPanel the option panel for this connection type
     * @param connection the underlyign connection object
     */
    private ConnectionDialog(final AbstractConnectionPanel optionsPanel,
            final ConnectNeurons connection, final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        this.connectionPanel = optionsPanel;
    }

    /**
     * Initialize the connection panel.
     */
    private void init() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(connectionPanel);
        detailTriangle = new DropDownTriangle(UpDirection.RIGHT, false,
                "Synapse Properties", "Synapse Properties", this);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(leftJustify(detailTriangle));
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        propertiesPanel = ConnectionSynapsePropertiesPanel
                .createSynapsePropertiesPanel(this);
        mainPanel.add(propertiesPanel);
        eirPanel = SynapsePolarityAndRandomizerPanel
                .createPolarityRatioPanel(this);
        mainPanel.add(eirPanel);
        detailTriangle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateDetailTriangle();
            }
        });
        fillFrame();
        updateDetailTriangle();
    }

    /**
     * Update state of detail triangle.
     */
    private void updateDetailTriangle() {
        propertiesPanel.setVisible(detailTriangle.isDown());
        propertiesPanel.repaint();
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * https://stackoverflow.com/questions/8335997/
     * how-can-i-add-a-space-in-between-two-buttons-in-a-boxlayout
     */
    private Component leftJustify(final JPanel panel) {
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalStrut(10));
        b.add(panel);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    /**
     * Fills the standard dialog with the connection panel and a help button.
     */
    public void fillFrame() {
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Network/connections.html");
        addButton(new JButton(helpAction));
        setContentPane(mainPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        connectionPanel.commitChanges();
        List<Neuron> source = networkPanel.getSourceModelNeurons();
        List<Neuron> target = networkPanel.getSelectedModelNeurons();
        List<Synapse> synapses = connectionPanel.applyConnection(source,
                target);
        ConnectionUtilities.polarizeSynapses(synapses,
                eirPanel.getPercentExcitatory());
        propertiesPanel.commitChanges();
        ConnectionUtilities.conformToTemplates(synapses,
                propertiesPanel.getTemplateExcitatorySynapse(),
                propertiesPanel.getTemplateInhibitorySynapse());
        eirPanel.commitChanges();
        if (eirPanel.exRandomizerEnabled()) {
            ConnectionUtilities.randomizeExcitatorySynapses(synapses,
                    eirPanel.getExRandomizer());
        }
        if (eirPanel.inRandomizerEnabled()) {
            ConnectionUtilities.randomizeInhibitorySynapses(synapses,
                    eirPanel.getInRandomizer());
        }
        networkPanel.getNetwork().fireSynapsesUpdated(synapses);
    }

}