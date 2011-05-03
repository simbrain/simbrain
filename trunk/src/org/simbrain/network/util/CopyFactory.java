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
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

/**
 * <b>CopyFactory</b> provides utilities for creating copies of arbitrary collections
 * of network objects (neurons, synapses, networks, groups, text objects, etc.)
 */
public class CopyFactory {

    /**
     * Creates a copy of a list of network model elements:
     *  neurons, synapses, networks, etc.
     *
     * @param items the list of items to copy.
     * @return an arraylist of model elements.
     */
    public static ArrayList<Object> getCopy(final RootNetwork newRoot, final ArrayList<Object> items) {
        ArrayList<Object> ret = new ArrayList<Object>();
        // Match new to old neurons for synapse adding
        Hashtable<Neuron, Neuron> neuronMappings = new Hashtable<Neuron, Neuron>();
        ArrayList<Synapse> synapses = new ArrayList<Synapse>();

        // Copy neurons first
        for (Object item : items) {
            if (item instanceof Neuron) {
                Neuron oldNeuron = ((Neuron) item);
                if (!isPartOfNetwork(items, oldNeuron)) {
                    Neuron newNeuron = new Neuron(newRoot, oldNeuron);
                    ret.add(newNeuron);
                    neuronMappings.put(oldNeuron, newNeuron);
                }
            } else if (item instanceof Synapse) {
                if (!isStranded(items, (Synapse) item)) {
                    synapses.add((Synapse) item);
                }
            } else if (item instanceof Network) {
                Network oldNet = (Network) item;
                Network newNet = oldNet.duplicate();
                ret.add(newNet);
                Iterator oldNeuronIterator = oldNet.getFlatNeuronList().iterator();
                for (Neuron newNeuron : newNet.getFlatNeuronList()) {
                    neuronMappings.put((Neuron) oldNeuronIterator.next(), newNeuron);
                }
            }
        }

        // Copy synapses
        for (Synapse synapse : synapses) {
            Synapse newSynapse = new Synapse(
                    (Neuron) neuronMappings.get(synapse.getSource()),
                    (Neuron) neuronMappings.get(synapse.getTarget()), synapse
                            .getLearningRule().deepCopy(), synapse);
            ret.add(newSynapse);
        }

        return ret;
    }

    /**
     * Reurns true if the neuron is contained in one of the listed networks.
     *
     * @param allItems objects to check
     * @param toCheck neuron to check
     * @return true if the neuron to check is in the list of items
     */
    private static boolean isPartOfNetwork(final ArrayList allItems, final Neuron toCheck) {
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
     * Returns true if this synapse is not connected to two neurons
     * (i.e. is "stranded"), false otherwise.
     *
     * @param allItems includes neurons to check
     * @param synapse synapse to check
     * @return true if this synapse is stranded, false otherwise
     */
    private static boolean isStranded(final ArrayList allItems, final Synapse synapse) {

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

        if (check.contains(synapse.getSource()) && (check.contains(synapse.getTarget()))) {
            return false;
        }
        return true;
    }
}
