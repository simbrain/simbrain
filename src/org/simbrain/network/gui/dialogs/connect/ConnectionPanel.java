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

import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel for editing connection managers.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public final class ConnectionPanel extends JPanel {

    /**
     * Parent frame so pack can be called when combo box changed.
     */
    private final Window parentFrame;

    /**
     * Template synapse properties
     */
    private SynapsePropertiesPanel synapseProperties;

    /**
     * The excitatory-inhibitory ratio and randomizer panel.
     */
    private SynapsePolarityAndRandomizerPanel polarityPanel;

    /**
     * To edit the properties of the connection object
     */
    private EditablePanel connectionStrategyProperties;

    /**
     * The connection object used to connect source to target neurons.
     */
    private ConnectionStrategy connectionStrategy;

    /**
     * Number of target neurons, can be 0
     */
    private int noTar;

    /**
     * Whether or not the the connections would be recurrent.
     */
    private boolean rec;

    /**
     * Whether or not this is a creation panel or an edit panel.
     */
    private boolean isCreation = true;

    /**
     * Construct the dialog.
     *
     * @param connectionManager   the underlying connection object
     */
    public ConnectionPanel(final Window parent, final ConnectionStrategy connectionManager, int noTar, boolean rec,
                           boolean isCreation) {
        this.parentFrame = parent;
        this.connectionStrategy = connectionManager;
        this.noTar = noTar;
        this.rec = rec;
        this.isCreation = isCreation;
        init();
    }

    /**
     * Initialize the connection panel.
     */
    private void init() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel connectionContainer = new JPanel(new GridBagLayout());
        connectionContainer.setBorder(BorderFactory.createTitledBorder("Connection Properties"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 3;
        gbc.weighty = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER;
        if (connectionStrategy.getClass() != Sparse.class) {
            connectionStrategyProperties = new AnnotatedPropertyEditor(connectionStrategy);

        } else {
            if (((Sparse) connectionStrategy).getSynapseGroup() != null) {
                connectionStrategyProperties = SparseConnectionPanel.createSparsityAdjustmentEditor((Sparse) connectionStrategy);
            } else {
                connectionStrategyProperties = SparseConnectionPanel.createSparsityAdjustmentPanel(
                        (Sparse) connectionStrategy, noTar, rec);
            }
        }
        connectionContainer.add(connectionStrategyProperties, gbc);
        add(connectionContainer);

        // E/I Ratio and Randomizers
        if (isCreation) {
            polarityPanel = SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(connectionStrategy, parentFrame);
            add(polarityPanel);
        }
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
     * Update the {@link ConnectionStrategy} object associated with this panel.
     */
    public void commitSettings() {
        connectionStrategyProperties.commitChanges();
        if (isCreation) {
            polarityPanel.commitChanges(connectionStrategy);
        }
    }

    /**
     * Commit changes made in this panel to a loose network.
     *
     * @param networkPanel
     */
    public void commitChanges(NetworkPanel networkPanel) {

        commitSettings();
        List<Synapse> synapses = connectionStrategy.connectNeurons(networkPanel.getNetwork(), networkPanel.getSourceModelNeurons(), networkPanel.getSelectedModelNeurons());
        if (synapses.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Chosen connection" +
                            " parameters resulted in no synapses being created." +
                            "\nTry using different connection parameters.",
                    "Warning: No Connections Created", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        //TODO: Consider moving the below to connection manager
        if (isCreation) {
            ConnectionUtilities.polarizeSynapses(synapses, polarityPanel.getPercentExcitatory());
            if (polarityPanel.exRandomizerEnabled()) {
                ConnectionUtilities.randomizeExcitatorySynapses(synapses, polarityPanel.getExRandomizer());
            }
            if (polarityPanel.inRandomizerEnabled()) {
                ConnectionUtilities.randomizeInhibitorySynapses(synapses, polarityPanel.getInRandomizer());
            }
        }
        networkPanel.getNetwork().fireSynapsesUpdated(synapses);
    }

    /**
     * Commit changes made to a synapse group's connectivity.
     *
     * @param synapseGroup the group to change
     */
    public void commitChanges(SynapseGroup synapseGroup) {
        synapseGroup.clear();
        connectionStrategyProperties.commitChanges();
        connectionStrategy.connectNeurons(synapseGroup);
        if (isCreation) {
            polarityPanel.commitChanges();
            if (polarityPanel.exRandomizerEnabled()) {
                ConnectionUtilities.randomizeExcitatorySynapses(
                        synapseGroup.getExcitatorySynapses(),
                        polarityPanel.getExRandomizer());
                synapseGroup.setExcitatoryRandomizer(polarityPanel
                        .getExRandomizer());
            }
            if (polarityPanel.inRandomizerEnabled()) {
                ConnectionUtilities.randomizeInhibitorySynapses(
                        synapseGroup.getInhibitorySynapses(),
                        polarityPanel.getInRandomizer());
                synapseGroup.setInhibitoryRandomizer(polarityPanel
                        .getInRandomizer());
            }
            synapseGroup.setExcitatoryRatio(polarityPanel.getPercentExcitatory());
        }
        synapseGroup.setConnectionManager(connectionStrategy);
    }

    public SynapsePropertiesPanel getSynapseProperties() {
        return synapseProperties;
    }

    public SynapsePolarityAndRandomizerPanel getPolarityPanel() {
        return polarityPanel;
    }

    public ConnectionStrategy getConnectionStrategy() {
        return connectionStrategy;
    }

    @Override
    public String toString() {
        return connectionStrategy.getName();
    }

}
