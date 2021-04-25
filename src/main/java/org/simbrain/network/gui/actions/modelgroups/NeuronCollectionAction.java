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
package org.simbrain.network.gui.actions.modelgroups;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.ConditionallyEnabledAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Add selected neurons to a neuron collection.
 */
public final class NeuronCollectionAction extends ConditionallyEnabledAction {

    /**
     * Create a new new neuron collection with the specified network
     * panel.
     *
     * @param networkPanel networkPanel, must not be null
     */
    public NeuronCollectionAction(final NetworkPanel networkPanel) {
        super(networkPanel, "Add Neurons to  Collection", EnablingCondition.NEURONS);

        putValue(NAME, "Add Neurons to Collection");
        putValue(SHORT_DESCRIPTION, "Add selected neurons to a neuron collction (Shift-G)");
        networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('G'), this);
        networkPanel.getActionMap().put(this, this);

    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        List<Neuron> neuronList = getNetworkPanel().getSelectionManager().filterSelectedModels(Neuron.class);
        if (neuronList.size() > 0) {
            NeuronCollection nc = getNetworkPanel().getNetwork().createNeuronCollection(neuronList);
            if (nc != null) {
                getNetworkPanel().getNetwork().addNetworkModel(nc);
            }
        }
    }

}