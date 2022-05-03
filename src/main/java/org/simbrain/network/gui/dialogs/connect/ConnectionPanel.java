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
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.simbrain.network.connections.ConnectionUtilitiesKt.*;

/**
 * Panel for editing connection manager, the ratio of inhibition to excitation
 * and the randomization of synapses.
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

    private List<Neuron> sourceNeurons;
    private List<Neuron> targetNeurons;

    /**
     * Whether or not the the connections would be recurrent.
     */
    private boolean rec;

    /**
     * For showing/hiding the connection properties.
     */
    private DropDownTriangle detailTriangle;

    /**
     * Whether or not this is a creation panel or an edit panel.
     */
    private boolean isCreation = true;

    /**
     * Construct the dialog.
     *
     * @param connectionStrategy   the underlying connection object
     */
    public ConnectionPanel(
            final Window parent,
            final ConnectionStrategy connectionStrategy,
            List<Neuron> sourceNeurons,
            List<Neuron> targetNeurons) {
        this.parentFrame = parent;
        this.connectionStrategy = connectionStrategy;
        this.sourceNeurons = sourceNeurons;
        this.targetNeurons = targetNeurons;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel connectionContainer = new JPanel(new GridBagLayout());
        connectionContainer.setBorder(BorderFactory.createTitledBorder("Connection Properties"));
        if (connectionStrategy.getClass() != Sparse.class) {
            connectionStrategyProperties = new AnnotatedPropertyEditor(connectionStrategy);
        } else {
            connectionStrategyProperties = new SparseConnectionPanel((Sparse) connectionStrategy);
        }

        // Set up detail triangle and connection strategy
        detailTriangle = new DropDownTriangle(DropDownTriangle.UpDirection.LEFT,
            true, "Show", "Hide", parentFrame);
        detailTriangle.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                syncPanelToTriangle();
            }

        });
        syncPanelToTriangle();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(2,0,7,7);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        connectionContainer.add(detailTriangle, gbc);
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.insets = new Insets(0,0,0,0);
        connectionContainer.add(connectionStrategyProperties, gbc);
        add(connectionContainer);

        // E/I Ratio and Randomizers
        if (isCreation && !connectionStrategy.getOverridesPolarity()) {
            polarityPanel = SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(connectionStrategy, parentFrame);
            add(polarityPanel);
        }
    }

    /**
     * If detail triangle is down, show the panel; if not hide the panel.
     */
    private void syncPanelToTriangle() {
        connectionStrategyProperties.setVisible(detailTriangle.isDown());
        parentFrame.pack();
        //parentFrame.setLocationRelativeTo(null);
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
        if (isCreation && !connectionStrategy.getOverridesPolarity()) {
            polarityPanel.commitChanges(connectionStrategy);
        }
    }

    /**
     * Commit changes made in this panel to free synapses.
     */
    public void commitChanges(NetworkPanel networkPanel) {

        commitSettings();

        // Make the connections
        List<Synapse> synapses = connectionStrategy.connectNeurons(networkPanel.getNetwork(),
                networkPanel.getSelectionManager().filterSelectedSourceModels(Neuron.class),
                networkPanel.getSelectionManager().filterSelectedModels(Neuron.class));
        if (synapses.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Chosen connection" +
                            " parameters resulted in no synapses being created." +
                            "\nTry using different connection parameters.",
                    "Warning: No Connections Created", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        //TODO: Consider moving the below to connection manager
        if (isCreation && !connectionStrategy.getOverridesPolarity()) {
            // Set the weights to have the desired excitatory-inhibitory ratio
            polarizeSynapses(synapses, polarityPanel.getPercentExcitatory());
            // TODO: Separate these panels out
            if (polarityPanel.exRandomizerEnabled()) {
                // Apply probability distribution to excitatory weights
                randomizeExcitatorySynapses(synapses, polarityPanel.getExRandomizer());
            }
            if (polarityPanel.inRandomizerEnabled()) {
                // Apply probability distribution to inhibitory weights
                randomizeInhibitorySynapses(synapses, polarityPanel.getInRandomizer());
            }
        }
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
        if (isCreation && !connectionStrategy.getOverridesPolarity()) {
            // Calls SynapseGroup.setExcitatoryRatio
            polarityPanel.commitChanges();
            if (polarityPanel.exRandomizerEnabled()) {
                randomizeExcitatorySynapses(
                        synapseGroup.getExcitatorySynapses(),
                        polarityPanel.getExRandomizer());
                synapseGroup.setExcitatoryRandomizer(polarityPanel
                        .getExRandomizer());
            }
            if (polarityPanel.inRandomizerEnabled()) {
                randomizeInhibitorySynapses(
                        synapseGroup.getInhibitorySynapses(),
                        polarityPanel.getInRandomizer());
                synapseGroup.setInhibitoryRandomizer(polarityPanel
                        .getInRandomizer());
            }
            synapseGroup.setExcitatoryRatio(polarityPanel.getPercentExcitatory());
        }
        synapseGroup.setConnectionManager(connectionStrategy);
    }

    public ConnectionStrategy getConnectionStrategy() {
        return connectionStrategy;
    }

    @Override
    public String toString() {
        return connectionStrategy.getName();
    }

}
