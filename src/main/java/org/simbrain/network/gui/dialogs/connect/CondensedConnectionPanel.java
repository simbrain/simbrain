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

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.SynapsePolarityAndRandomizerPanel.RandBehavior;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that combines a DensityBasedConnectionPanel (Sparse) with a
 * SynapsePolarityAndRandomizerPanel with the ability to create a synapse group
 * with the given properties given a source and target neuron group. Meant for
 * cases where a limited number of properties can be set for a given synapse
 * group.
 * <p>
 * For the very common case where a variable density, variable polarity,
 * random synapse group is desired.
 *
 * @author Zoë Tosi
 */
public class CondensedConnectionPanel {

    /**
     * The connection panel for changing the density.
     */
    private final SparseConnectionPanel connectorPanel;

    /**
     * The panel responsible for chaning polarity ratio and randomizers.
     */
    private final SynapsePolarityAndRandomizerPanel polarityPanel;

    /** */
    private final Box mainPanel = Box.createVerticalBox();

    /**
     * Creates a CondensedConnectionPanel.
     *
     * @param networkPanel the place where the synapse group will go
     * @param parent       for resizing
     * @param numTargs     so the DensityBasedConnectionPanel can estimate
     *                     the number of efferents should the user chose to equalize them.
     */
    public CondensedConnectionPanel(final NetworkPanel networkPanel, final Window parent, final int numTargs) {
        connectorPanel = SparseConnectionPanel.createSparsityAdjustmentPanel(new Sparse(), numTargs, false);
        connectorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5)));
        connectorPanel.setNumTargs(numTargs);
        connectorPanel.setDensity(0.25);
        polarityPanel = SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(parent, RandBehavior.FORCE_ON);
        polarityPanel.setPercentExcitatory(0.5);
        mainPanel.add(connectorPanel);
        mainPanel.add(polarityPanel);
    }

    /**
     * Creates a synapse group from the parameters in the panel and between
     * two neuron groups.
     *
     * @param source the source neuron group
     * @param target the target neuron group
     * @return the synapse group connecting them
     */
    public SynapseGroup createSynapseGroup(NeuronGroup source, NeuronGroup target) {

        connectorPanel.commitChanges();
        Sparse dbc = (Sparse) connectorPanel.getConnection();

        SynapseGroup synGrp = SynapseGroup.createSynapseGroup(source, target, dbc);

        polarityPanel.commitChanges(synGrp);

        synGrp.makeConnections();

        return synGrp;

    }

    /**
     * Returns the density based connection panel so that other classes can
     * set certain properties of the panel independent of this class.
     *
     * @return the density
     */
    public SparseConnectionPanel getConnectorPanel() {
        return connectorPanel;
    }

    /**
     * Returns the main panel where all the components are actually laid out.
     * Doing this so that this class doesn't have to extend a Swing class and
     * thus clutter the JavaDocs.
     *
     * @return
     */
    public Component getMainPanel() {
        return mainPanel;
    }

}
