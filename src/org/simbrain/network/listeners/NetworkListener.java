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
package org.simbrain.network.listeners;

import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * Listener interface for receiving general network events. Classes interested
 * in responding to such events are registered with a Network, which broadcasts
 * those events to registered observer classes.
 */
public interface NetworkListener {

    /**
     * Neuron states have been updated.
     */
    void updateNeurons();

    /**
     * Neuron states in specified neurons have been updated.
     *
     * @param neurons the list of neurons to update.
     */
    void updateNeurons(List<Neuron> neurons);

    /**
     * Synapse states have been updated.
     */
    void updateSynapses();

    /**
     * Synapse states in specified synapses have been updated.
     *
     * @param synapses the list of synapses to update.
     */
    void updateSynapses(List<Synapse> synapses);

}