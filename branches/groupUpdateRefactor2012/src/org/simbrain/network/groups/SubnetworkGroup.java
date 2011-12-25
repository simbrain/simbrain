/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.util.Comparators;

/**
 * 
 */
public class SubnetworkGroup extends Group {

    /** Set of neurons. */
    private final List<Neuron> neuronList = new ArrayList<Neuron>();

    /** Set of synapses. */
    private Set<Synapse> synapseList = new HashSet<Synapse>();

    /** @see Group */
    public SubnetworkGroup(final RootNetwork net, final List<Neuron> neurons) {
        super(net);
        for (Neuron neuron : neurons) {
            neuronList.add(neuron);
        }
        Collections.sort(neuronList, Comparators.X_ORDER);
    }

    public SubnetworkGroup(RootNetwork root) {
        super(root);
    }

    /**
     * Randomize fan-in for all neurons in group.
     */
    public void randomizeIncomingWeights() {
        for (Neuron neuron : neuronList) {
            neuron.randomizeFanIn();
        }
    }

    /**
     * Randomize all neurons in group.
     */
    public void randomize() {
        for (Neuron neuron : neuronList) {
            neuron.randomize();
        }
    }

    /**
     * Randomize bias for all neurons in group.
     * 
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : neuronList) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /** @Override. */
    public Network duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Add neuron.
     * 
     * @param neuron neuron to add
     */
    public void addNeuron(Neuron neuron) {
        neuronList.add(neuron);
    }

    /**
     * Delete a neuron.
     * 
     * @param toDelete neuron to delete
     */
    public void deleteNeuron(Neuron toDelete) {
        neuronList.remove(toDelete);
        // parent.fireGroupChanged(this, this);
    }

    /**
     * @return a list of neurons
     */
    public List<Neuron> getNeuronList() {
        return neuronList;
    }

    /**
     * Update all neurons.
     */
    public void updateNeurons() {
        for (Neuron neuron : neuronList) {
            neuron.update();
        }
    }

    @Override
    public boolean isEmpty() {
        boolean neuronsGone = neuronList.isEmpty();
        boolean synapsesGone = synapseList.isEmpty();
        return (neuronsGone && synapsesGone);
    }

    /**
     * Returns the number of neurons and synapses in this group.
     * 
     * @return the number of neurons and synapses in this group.
     */
    public int getElementCount() {
        return neuronList.size() + synapseList.size();
    }

    /**
     * Returns a debug string.
     * 
     * @return the debug string.
     */
    public String debugString() {
        String ret = new String();
        ret += ("Group with " + this.getNeuronList().size() + " neuron(s),");
        ret += (" " + this.getSynapseList().size() + " synapse(s).");
        return ret;
    }

    /**
     * Add synapse.
     * 
     * @param synapse synapse to add
     */
    public void addSynapse(Synapse synapse) {
        synapseList.add(synapse);
    }

    /**
     * Delete a synapse.
     * 
     * @param toDelete synapse to delete
     */
    public void deleteSynapse(Synapse toDelete) {
        synapseList.remove(toDelete);
        getParentNetwork().fireGroupChanged(this, this);
    }

    /**
     * @return a list of weights
     */
    public List<Synapse> getSynapseList() {
        return new ArrayList<Synapse>(synapseList);
    }

    /**
     * Update group. Override for special updating.
     */
    public void update() {
        updateNeurons();
        updateAllSynapses();
    }

    /**
     * Update all synapses.
     */
    public void updateAllSynapses() {
        for (Synapse synapse : synapseList) {
            synapse.update();
        }
    }

}
