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
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;

/**
 * A collection of neuron groups and synapse groups.
 * 
 * REDO: Rename to subnetwork?
 */
public class SubnetworkGroup extends Group implements UpdatableGroup {
    
    /** List of neuron groups. */
    private final List<NeuronGroup> neuronGroupList = new ArrayList<NeuronGroup>();

    /** List of synapse groups. */
    private final List<SynapseGroup> synapseGroupList = new ArrayList<SynapseGroup>();

    /**
     * Create subnetwork group.
     *
     * @param net parent network.
     */
    public SubnetworkGroup(final RootNetwork net) {
        super(net);
        init();
    }
    
    /**
     * Create subnetwork group with a specified numbers of neuron groups and synapse groups.
     *
     * @param net parent network
     * @param numNeuronGroups number of neuron groups
     * @param numSynapseGroups number of synapse groups
     */
    public SubnetworkGroup(final RootNetwork net, int numNeuronGroups, int numSynapseGroups) {
        super(net);
        init();
        for (int i = 0; i < numNeuronGroups; i++) {
            addNeuronGroup(new NeuronGroup(net));
        }
        for (int i = 0; i < numSynapseGroups; i++) {
            addSynapseGroup(new SynapseGroup(net));
        }
    }
    
    @Override
    public void delete() {
        if (isMarkedForDeletion()) {
            return;
        } else {
            setMarkedForDeletion(true);
        }
//      for (NeuronGroup layer : layers) {
//      layer.setDeleteWhenEmpty(true);
//      getParentNetwork().deleteGroup(layer);            
//  }
//  for (SynapseGroup weightLayer : connections) {
//      weightLayer.setDeleteWhenEmpty(true);
//      getParentNetwork().deleteGroup(weightLayer);            
//  }        

        
//        neuronGroup.setDeleteWhenEmpty(true);
//        getParentNetwork().deleteGroup(neuronGroup);
//        synapseGroup.setDeleteWhenEmpty(true);
//        getParentNetwork().deleteGroup(synapseGroup);        
    }

    /**
     * Initialize the subnetwork;
     */
    private void init() {
        setLabel("Subnetwork");
        //TODO: Think about below
//        neuronGroup.setLabel("Neuron group");
//        neuronGroup.setParentGroup(this);
//        synapseGroup.setLabel("Synapse group");
//        synapseGroup.setParentGroup(this);
//        synapseGroup.setDeleteWhenEmpty(false);
    }
   
    /** @Override. */
    public Network duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEmpty() {
        boolean neuronGroupsEmpty = neuronGroupList.isEmpty();
        boolean synapseGroupsEmpty = synapseGroupList.isEmpty();
        return (neuronGroupsEmpty && synapseGroupsEmpty);
    }

    /**
     * Add a synapse group
     *
     * @param group the synapse group to add
     */
    public void addSynapseGroup(SynapseGroup group) {
//        group.setLabel("Weights " + (connections.size() + 1) + " > "
//                + (connections.size() + 2));
        synapseGroupList.add(group);
        group.setParentGroup(this);
    }
    
    
    /**
     * Add a neuron group.
     *
     * @param group the neuron group to add
     */
    public void addNeuronGroup(NeuronGroup group) {
        neuronGroupList.add(group);
        group.setParentGroup(this);
    }
    
    /**
     * Remove a neuron group.
     *
     * @param neuronGroup group to remove
     */
    public void removeNeuronGroup(NeuronGroup neuronGroup) {
        neuronGroupList.remove(neuronGroup);
        // TODO: Fire event?
    }

    /**
     * Remove a synapse group.
     *
     * @param synapseGroup group to remove
     */
    public void removeSynapseGroup(SynapseGroup synapseGroup) {
        synapseGroupList.remove(synapseGroup);
        // TODO: Fire event?
    }

    /**
     * Update subnetwork. Override for special updating.
     */
    public void invoke() {
        for (NeuronGroup layer : neuronGroupList) {
            layer.updateNeurons();
        }
    }
    
    // TODO: Javadocs
    
    public NeuronGroup getNeuronGroup(int index) {
        return neuronGroupList.get(index);
    }
    public NeuronGroup getNeuronGroup() {
        return neuronGroupList.get(0);
    }
    public int getNeuronGroupCount() {
        return neuronGroupList.size();
    }
    public List<NeuronGroup> getNeuronGroupList() {
        return Collections.unmodifiableList(neuronGroupList);
    }
    
    public SynapseGroup getSynapseGroup(int index) {
        return synapseGroupList.get(index);
    }
    public SynapseGroup getSynapseGroup() {
        return synapseGroupList.get(0);
    }
    public int getSynapseGroupCount() {
        return synapseGroupList.size();
    }
    public List<SynapseGroup> getSynapseGroupList() {
        return Collections.unmodifiableList(synapseGroupList);
    }

    @Override
    public List<Neuron> getFlatNeuronList() {
        List<Neuron> ret = new ArrayList<Neuron>();
        for(NeuronGroup group : neuronGroupList) {
            ret.addAll(group.getFlatNeuronList());
        }
        return Collections.unmodifiableList(ret);
    }

    @Override
    public List<Synapse> getFlatSynapseList() {
        List<Synapse> ret = new ArrayList<Synapse>();
        for(SynapseGroup group : synapseGroupList) {
            ret.addAll(group.getFlatSynapseList());
        }
        return Collections.unmodifiableList(ret);
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Subnetwork with " + neuronGroupList.size() + " neuron list(s),");
        ret += (" " + synapseGroupList.size() + " synapse list(s).");
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enabled) {
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
        for (NeuronGroup neuronGroup : neuronGroupList) {
            neuronGroup.updateNeurons();
        }
    }
}
