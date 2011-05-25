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
package org.simbrain.network.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;

/**
 * <b>CopyFactory</b> provides utilities for creating copies of arbitrary
 * collections of network objects (neurons, synapses, networks, groups, text
 * objects, etc.).
 */
public class CopyFactory {

    // TODO: Add groups, text objects

    /**
     * Creates a copy of a list of network model elements: neurons, synapses,
     * networks, and groups.
     *
     * @param newParent parent network for these objects. May be a root network
     *            or a subnetwork.
     * @param items the list of items to copy.
     * @return the list of copied items.
     */
    public static ArrayList<?> getCopy(final Network newParent,
            final ArrayList<?> items) {

        ArrayList<Object> ret = new ArrayList<Object>();
        // Match new to old neurons for synapse adding
        Hashtable<Neuron, Neuron> neuronMappings =
            new Hashtable<Neuron, Neuron>();
        ArrayList<Synapse> synapses = new ArrayList<Synapse>();

        for (Object item : items) {
            if (item instanceof Neuron) {
                Neuron oldNeuron = ((Neuron) item);
                // Don't make direct copies of neurons inside a subnetwork. They
                // will be copied (with the correct parent network) in the
                // Network copy constructor.
                if (!isContainedInANetwork(oldNeuron, items)) {
                    Neuron newNeuron = new Neuron(newParent, oldNeuron);
                    ret.add(newNeuron);
                    neuronMappings.put(oldNeuron, newNeuron);
                }
            } else if (item instanceof Synapse) {
                if (!isStranded((Synapse) item, items)) {
                    synapses.add((Synapse) item);
                }
            } else if (item instanceof Network) {
                Network oldNet = (Network) item;
                Network newNet = Network.newInstance(
                        newParent.getRootNetwork(), oldNet);
                ret.add(newNet);
                Iterator<Neuron> oldNeuronIterator = oldNet.getFlatNeuronList()
                        .iterator();
                for (Neuron newNeuron : newNet.getFlatNeuronList()) {
                    neuronMappings.put(oldNeuronIterator.next(),
                            newNeuron);
                }
            }
        }

        // Copy synapses
        for (Synapse synapse : synapses) {
            // Parent network for the new synapses inherited from neurons
            Synapse newSynapse = new Synapse(
                    neuronMappings.get(synapse.getSource()),
                    neuronMappings.get(synapse.getTarget()),
                    synapse.getLearningRule().deepCopy(), synapse);
            ret.add(newSynapse);
        }

        return ret;
    }

    /**
     * Returns true if the neuron is contained in some subnetwork included in
     * the list.
     *
     * @param toCheck neuron to check
     * @param allItems list of items, whose subnetworks should be checked
     * @return true if the neuron to check is in the list of items
     */
    private static boolean isContainedInANetwork(final Neuron toCheck, 
            final ArrayList<?> allItems) {
        for (Object object : allItems) {
            if (object instanceof Network) {
                if (((Network) object).getFlatNeuronList().contains(toCheck)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if this synapse is not connected to two neurons (i.e. is
     * "stranded"), false otherwise.
     *
     * @param synapse synapse to check
     * @param allItems includes neurons to check
     * @return true if this synapse is stranded, false otherwise
     */
    private static boolean isStranded(final Synapse synapse,
            final ArrayList<?> allItems) {

        // The list of checked neurons should include neurons in the list
        // as well as all neurons contained in networks in the list
        ArrayList<Neuron> check = new ArrayList<Neuron>();
        for (Object object : allItems) {
            if (object instanceof Neuron) {
                check.add((Neuron) object);
            } else if (object instanceof Network) {
                check.addAll(((Network) object).getFlatNeuronList());
            }
        }

        if (check.contains(synapse.getSource())
                && (check.contains(synapse.getTarget()))) {
            return false;
        }
        return true;
    }
}
