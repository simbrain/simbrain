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

import java.util.List;

import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

/**
 * REDO: Rename to subnetwork?
 */
public class SubnetworkGroup extends Group {
    
    /** List of neurons. */
    private final NeuronGroup neuronGroup;

    /** List of synapses. */
    private final SynapseGroup synapseGroup;

    
    // REDO: Javadocs

    public SubnetworkGroup(final RootNetwork net) {
        super(net);
        neuronGroup = new NeuronGroup(net);
        synapseGroup = new SynapseGroup(net);
        init();
    }
    
    public SubnetworkGroup(final RootNetwork net, final List<Neuron> neurons) {
        super(net);
        neuronGroup = new NeuronGroup(net, neurons);      
        synapseGroup = new SynapseGroup(net);
        init();
    }

    public SubnetworkGroup(final RootNetwork net, final List<Neuron> neurons, final List<Synapse> synapses) {
        super(net);
        neuronGroup = new NeuronGroup(net, neurons);      
        synapseGroup = new SynapseGroup(net, synapses);
        init();
    }

    /**
     * Initialize the subnet.
     */
    private void init() {
        setLabel("Subnetwork");
        neuronGroup.setLabel("Neuron group");
        neuronGroup.setParentGroup(this);
        synapseGroup.setLabel("Synapse group");
        synapseGroup.setParentGroup(this);
        synapseGroup.setDeleteWhenEmpty(false);
    }

    
//    /**
//     * Randomize fan-in for all neurons in group.
//     */
//    public void randomizeIncomingWeights() {
//        for (Neuron neuron : neuronList) {
//            neuron.randomizeFanIn();
//        }
//    }
//
//    /**
//     * Randomize all neurons in group.
//     */
//    public void randomize() {
//        for (Neuron neuron : neuronList) {
//            neuron.randomize();
//        }
//    }
//
//    /**
//     * Randomize bias for all neurons in group.
//     * 
//     * @param lower lower bound for randomization.
//     * @param upper upper bound for randomization.
//     */
//    public void randomizeBiases(double lower, double upper) {
//        for (Neuron neuron : neuronList) {
//            neuron.randomizeBias(lower, upper);
//        }
//    }

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
        neuronGroup.addNeuron(neuron);
        //neuron.setParentGroup(this); REDO: Think... Is this the parent?
    }

    @Override
    public void removeNeuron(Neuron toDelete) {
        neuronGroup.removeNeuron(toDelete);
        //REDO
        //getParent().fireGroupChanged(this, this);
    }
    
    /**
     * Add synapse.
     * 
     * @param synapse synapse to add
     */
    public void addSynapse(Synapse synapse) {
        synapseGroup.addSynapse(synapse);
        getParentNetwork().fireGroupChanged(this, this, "synapseAdded");
    }

    @Override
    public void removeSynapse(Synapse toDelete) {
        synapseGroup.removeSynapse(toDelete);
        getParentNetwork().fireGroupChanged(this, this, "synapseRemoved");
    }

//
//    /**
//     * @return a list of neurons
//     */
//    public List<Neuron> getNeuronList() {
//        return neuronList;
//    }
//
//    /**
//     * Update all neurons.
//     */
//    public void updateNeurons() {
//        for (Neuron neuron : neuronList) {
//            neuron.update();
//        }
//    }

    @Override
    public boolean isEmpty() {
        boolean neuronsGone = neuronGroup.isEmpty();
        boolean synapsesGone = synapseGroup.isEmpty();
        return (neuronsGone && synapsesGone);
    }

    /**
     * Returns the number of neurons and synapses in this group.
     * 
     * @return the number of neurons and synapses in this group.
     */
    public int getElementCount() {
        return neuronGroup.getNeuronList().size() + synapseGroup.getSynapseList().size();
    }

    /**
     * Update group. Override for special updating.
     */
    public void update() {
        neuronGroup.updateNeurons(); // TODO: Justmake it update in neurons, and dump the whole update interface
        synapseGroup.update();
    }

//    /**
//     * Update all synapses.
//     */
//    public void updateAllSynapses() {
//        for (Synapse synapse : synapseList) {
//            synapse.update();
//        }
//    }

    
    @Override
    public String toString() {
        String ret = new String();
        ret += ("Subnetwork with " + neuronGroup.getNeuronList().size() + " neuron(s),");
        ret += (" " + synapseGroup.getSynapseList().size() + " synapse(s).");
        return ret;
    }

    /**
     * @return the neuronGroup
     */
    public NeuronGroup getNeuronGroup() {
        return neuronGroup;
    }

    /**
     * @return the synapseGroup
     */
    public SynapseGroup getSynapseGroup() {
        return synapseGroup;
    }
}
