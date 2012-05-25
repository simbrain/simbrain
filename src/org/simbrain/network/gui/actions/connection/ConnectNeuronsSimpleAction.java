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
package org.simbrain.network.gui.actions.connection;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect neurons action. Connects a set of source neurons to a set of target
 * neurons.
 *
 * Not currently used.
 */
public class ConnectNeuronsSimpleAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /** Network panel. */
    private final NetworkPanel networkPanel;

    /** Source neurons. */
    private Collection<NeuronNode> sourceNeurons;

    /** Target neuron. */
    private NeuronNode targetNeuron;

    /**
     * Create a new connect neurons action. Connects a set of source neurons to
     * a set of target neurons.
     *
     * @param networkPanel network panel, must not be null
     * @param sourceNeurons NeuronNodes to connect from
     * @param targetNeuron NeuronNodes to connect to
     */
    public ConnectNeuronsSimpleAction(final NetworkPanel networkPanel,
            final Collection<NeuronNode> sourceNeurons,
            final NeuronNode targetNeuron) {

        super("Connect Simple");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        this.sourceNeurons = sourceNeurons;
        this.targetNeuron = targetNeuron;

    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent arg0) {

        if (sourceNeurons.isEmpty() || targetNeuron == null) {
            return;
        }

        for (NeuronNode source : sourceNeurons) {
            networkPanel.getNetwork().addSynapse(
                    new Synapse(source.getNeuron(), targetNeuron.getNeuron(),
                            new ClampedSynapse()));
        }

    }

}
