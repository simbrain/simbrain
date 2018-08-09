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

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.DropDownTriangle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel for editing connection objects when creating new synapse groups.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public final class ConnectionPanel2 extends JPanel {

    /**
     * Parent frame so pack can be called when combo box changed.
     */
    private final Window parentFrame;

    /**
     * Template synapse properties
     */
    private ConnectionSynapsePropertiesPanel synapseProperties;

    /**
     * The excitatory ratio and randomizer panel.
     */
    private SynapsePolarityAndRandomizerPanel polarityPanel;

    /**
     * Drop down triangle for synapse properties.
     */
    private DropDownTriangle detailTriangle;

    /**
     * To edit the properties of the connection object
     */
    private AnnotatedPropertyEditor connectionProperties;

    /**
     * The connection object used to connect source to target neurons.
     */
    private ConnectNeurons connection;

    /**
     * Construct the dialog.
     *
     * @param connection   the underlying connection object
     */
    public ConnectionPanel2(final Window parent, final ConnectNeurons connection) {
        this.parentFrame = parent;
        this.connection = connection;
        init();
    }

    /**
     * Initialize the connection panel.
     */
    private void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        connectionProperties = new AnnotatedPropertyEditor(connection);
        add(connectionProperties);
        detailTriangle = new DropDownTriangle(DropDownTriangle.UpDirection.RIGHT, false, "Synapse Properties", "Synapse Properties", parentFrame);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(leftJustify(detailTriangle));
        add(Box.createRigidArea(new Dimension(0, 15)));
        synapseProperties = ConnectionSynapsePropertiesPanel.createSynapsePropertiesPanel(parentFrame);
        add(synapseProperties);
        polarityPanel = SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(parentFrame);
        add(polarityPanel);
        detailTriangle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateDetailTriangle();
            }
        });
        updateDetailTriangle();
    }

    /**
     * Update state of detail triangle.
     */
    private void updateDetailTriangle() {
        synapseProperties.setVisible(detailTriangle.isDown());
        synapseProperties.repaint();
        parentFrame.pack();
    }
    /**
     * https://stackoverflow.com/questions/8335997/ how-can-i-add-a-space-in-between-two-buttons-in-a-boxlayout
     */
    private Component leftJustify(final JPanel panel) {
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalStrut(10));
        b.add(panel);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    /**
     * Commit changes made in this panel to a loose network.
     *
     * @param networkPanel
     */
    public void commitChanges(NetworkPanel networkPanel) {
        connectionProperties.commitChanges();
        List<Synapse> synapses = connection.connectNeurons(networkPanel.getNetwork(), networkPanel.getSourceModelNeurons(), networkPanel.getSelectedModelNeurons());
        ConnectionUtilities.polarizeSynapses(synapses, polarityPanel.getPercentExcitatory());
        ConnectionUtilities.conformToTemplates(synapses, synapseProperties.getTemplateExcitatorySynapse(), synapseProperties.getTemplateInhibitorySynapse());
        polarityPanel.commitChanges();
        if (polarityPanel.exRandomizerEnabled()) {
            ConnectionUtilities.randomizeExcitatorySynapses(synapses, polarityPanel.getExRandomizer());
        }
        if (polarityPanel.inRandomizerEnabled()) {
            ConnectionUtilities.randomizeInhibitorySynapses(synapses, polarityPanel.getInRandomizer());
        }
        networkPanel.getNetwork().fireSynapsesUpdated(synapses);
    }

    /**
     * Commit changes made to a synapse group's connectivity.
     *
     * @param synapseGroup the group to change
     */
    public void commitChanges(SynapseGroup synapseGroup) {
        connectionProperties.commitChanges();
        connection.connectNeurons(synapseGroup);
        // TODO: Polarity, etc.
    }

    public ConnectionSynapsePropertiesPanel getSynapseProperties() {
        return synapseProperties;
    }

    public SynapsePolarityAndRandomizerPanel getPolarityPanel() {
        return polarityPanel;
    }

    public AnnotatedPropertyEditor getConnectionProperties() {
        return connectionProperties;
    }

    public ConnectNeurons getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return connection.getName();
    }

}
