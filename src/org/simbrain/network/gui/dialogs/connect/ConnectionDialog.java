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

import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 *
 * Dialog wrapper for all connection panels.
 *
 * @author jyoshimi
 * @author ztosi
 *
 */
@SuppressWarnings("serial")
public class ConnectionDialog extends StandardDialog {

    private final NetworkPanel networkPanel;

    /** The connection panel wrapped in this dialog. */
    private AbstractConnectionPanel connectionPanel;

    /** The connection object associated with the connection panel. */
    private ConnectNeurons connection;

    private JPanel mainPanel;

    private ConnectionSynapsePropertiesPanel propertiesPanel;

    private SynapsePolarityAndRandomizerPanel eirPanel;

    /**
     *
     * @param optionsPanel
     * @param connection
     * @param networkPanel
     * @return
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
     *
     * @param networkPanel
     * @param optionsPanel
     * @param connection
     */
    private ConnectionDialog(final AbstractConnectionPanel optionsPanel,
        final ConnectNeurons connection, final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        this.connectionPanel = optionsPanel;
        this.connection = connection;
    }

    /**
     *
     */
    private void init() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(connectionPanel);
        propertiesPanel = ConnectionSynapsePropertiesPanel
            .createSynapsePropertiesPanel(this);
        mainPanel.add(propertiesPanel);
        eirPanel = SynapsePolarityAndRandomizerPanel
            .createPolarityRatioPanel(this);
        mainPanel.add(eirPanel);
        fillFrame();
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
        List<Neuron> source = networkPanel.getSourceModelNeurons();
        List<Neuron> target = networkPanel.getSelectedModelNeurons();
        List<Synapse> synapses = connectionPanel.commitChanges(source, target);
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
        networkPanel.revalidate();
        networkPanel.repaint();
    }

    public AbstractConnectionPanel getOptionsPanel() {
        return connectionPanel;
    }

    public void setOptionsPanel(AbstractConnectionPanel optionsPanel) {
        this.connectionPanel = optionsPanel;
    }

    public ConnectNeurons getConnection() {
        return connection;
    }

    public void setConnection(ConnectNeurons connection) {
        this.connection = connection;
    }
}