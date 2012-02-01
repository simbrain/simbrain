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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.SynapseUpdateRule;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.SynapseListener;

/**
 * A collection of neuron groups and synapse groups which essentially functions
 * as a subnetwork within the main root network, with it's own update rules.
 */
public class Subnetwork extends Group {
    
    /** List of neuron groups. */
    private final List<NeuronGroup> neuronGroupList = new CopyOnWriteArrayList<NeuronGroup>();

    /** List of synapse groups. */
    private final List<SynapseGroup> synapseGroupList = new CopyOnWriteArrayList<SynapseGroup>();
    
    /**
     * Associates neuron groups with the "growing" synapse groups that are
     * "attached" to them.
     */
    private final Map<NeuronGroup, SynapseGroup> neuronGroupToSyanpseGroupMap = 
            new HashMap<NeuronGroup, SynapseGroup>();
    
    /** True if "growing synapse groups" are used. */
    private boolean usingGrowingSynapseGroups = false;

    /**
     * Create subnetwork group.
     *
     * @param net parent network.
     */
    public Subnetwork(final RootNetwork net) {
        super(net);
        setLabel("Subnetwork");
    }
    
    /**
     * Create a subnetwork group initialized with a specified numbers of neuron
     * groups and synapse groups.
     *
     * @param net parent network
     * @param numNeuronGroups number of neuron groups
     * @param numSynapseGroups number of synapse groups
     */
    public Subnetwork(final RootNetwork net, int numNeuronGroups, int numSynapseGroups) {
        super(net);
        setLabel("Subnetwork");
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
        for (NeuronGroup neuronGroup : neuronGroupList) {
            neuronGroup.setDeleteWhenEmpty(true);
            getParentNetwork().removeGroup(neuronGroup);
        }
        for (SynapseGroup synapseGroup: synapseGroupList) {
            synapseGroup.setDeleteWhenEmpty(true);
            getParentNetwork().removeGroup(synapseGroup);
        }
        if (usingGrowingSynapseGroups) {
            removeSynapseListener();
        }
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
    private void addSynapseGroup(SynapseGroup group) {
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
     * Connects one group of neurons all to all to another group of neurons.
     *
     * @param source the source group
     * @param target the target group
     */
    public SynapseGroup connectNeuronGroups(NeuronGroup source, NeuronGroup target) {
        AllToAll connection = new AllToAll(getParentNetwork(),
                source.getNeuronList(), target.getNeuronList());
        //connection.setPercentExcitatory(1);
        SynapseGroup newGroup = connectNeuronGroups(source, target, connection);
        return newGroup;
    }
    
    /**
     * Connects two groups of neurons according to some connection style.
     *
     * @param source the source group
     * @param target the target group
     * @param connection the type of connection desired between the two groups
     */
    public SynapseGroup connectNeuronGroups(NeuronGroup source, NeuronGroup target,
    		ConnectNeurons connection) {
        SynapseGroup newGroup = connectNeuronGroups(source, target, ""
                + (indexOfNeuronGroup(source) + 1), ""
                + (indexOfNeuronGroup(target) + 1), connection);
    	return newGroup;
    }
    
    /**
     * Connects two groups of neurons according to some connection style, and 
     * allows for custom labels of the neuron groups within the weights label.
     *
     * @param source the source group
     * @param target the target group
     * @param sourceLabel the name of the source group in the weights label
     * @param targetLabel the name of the target group in the weights label
     * @param connection the type of connection desired between the two groups
     */
    public SynapseGroup connectNeuronGroups(NeuronGroup source, NeuronGroup target,
    		String sourceLabel, String targetLabel, ConnectNeurons connection) {
        List<Synapse> synapses = connection.connectNeurons(getParentNetwork(),
                source.getNeuronList(),target.getNeuronList());
        SynapseGroup newGroup = new SynapseGroup(getParentNetwork());
        getParentNetwork().transferSynapsesToGroup(synapses, newGroup);
        addSynapseGroup(newGroup);
    	if(!source.equals(target)) {
    		newGroup.setLabel("Weights " + sourceLabel + " "
    			+ new Character('\u2192') + " " + targetLabel);
    	} else {
    		newGroup.setLabel("Weights " + sourceLabel + " "
        			+ new Character('\u21BA'));
    	}
        return newGroup;
    }
    
    /**
     * Remove a neuron group.
     *
     * @param neuronGroup group to remove
     */
    public void removeNeuronGroup(NeuronGroup neuronGroup) {
        neuronGroupList.remove(neuronGroup);
        getParentNetwork().fireGroupRemoved(neuronGroup);            
    }

    /**
     * Remove a synapse group.
     *
     * @param synapseGroup group to remove
     */
    public void removeSynapseGroup(SynapseGroup synapseGroup) {
        synapseGroupList.remove(synapseGroup);
        getParentNetwork().fireGroupRemoved(synapseGroup);            
    }

    /**
     * Get a neuron group by index.
     *
     * @param index which neuron group to get
     * @return the neuron group.
     */
    public NeuronGroup getNeuronGroup(int index) {
        return neuronGroupList.get(index);
    }
    
    /**
     * Get the first neuron group in the list. Convenience method when there is
     * just one neuron group.
     *
     * @return the neuron group.
     */
    public NeuronGroup getNeuronGroup() {
        return neuronGroupList.get(0);
    }
    
    /**
     * Get number of neuron groups or "layers" in the list.
     *
     * @return number of neuron groups.
     */
    public int getNeuronGroupCount() {
        return neuronGroupList.size();
    }
    
    /**
     * Returns an unmodifiable version of the neuron group list.
     *
     * @return the neuron group list.
     */
    public List<NeuronGroup> getNeuronGroupList() {
        return Collections.unmodifiableList(neuronGroupList);
    }
    
    /**
     * Returns the index of a neuron group.
     * @param group the group being queried.
     * @return the index of the group in the list.
     */
    public int indexOfNeuronGroup(NeuronGroup group){
    	return getNeuronGroupList().indexOf(group);
    }
    
    /**
     * Get a synapse group by index.
     *
     * @param index which synapse group to get
     * @return the synapse group.
     */
    public SynapseGroup getSynapseGroup(int index) {
        return synapseGroupList.get(index);
    }
    
    /**
     * Get the first synapse group in the list. Convenience method when there is
     * just one synapse group.
     *
     * @return the synapse group.
     */
    public SynapseGroup getSynapseGroup() {
        return synapseGroupList.get(0);
    }

    /**
     * Get number of synapse groups in the list.
     *
     * @return number of synapse groups.
     */
    public int getSynapseGroupCount() {
        return synapseGroupList.size();
    }

    /**
     * Returns an unmodifiable version of the synapse group list.
     *
     * @return the synapse group list.
     */
    public List<SynapseGroup> getSynapseGroupList() {
        return Collections.unmodifiableList(synapseGroupList);
    }

    /**
     * Return a "flat" list containing every neuron in every neuron group in
     * this subnetwork.
     *
     * @return the flat neuron list.
     */
    public List<Neuron> getFlatNeuronList() {
        List<Neuron> ret = new ArrayList<Neuron>();
        for(NeuronGroup group : neuronGroupList) {
            ret.addAll(group.getNeuronList());
        }
        return Collections.unmodifiableList(ret);
    }

    /**
     * Return a "flat" list containing every synapse in every synapse group in
     * this subnetwork.
     *
     * @return the flat synapse list.
     */
    public List<Synapse> getFlatSynapseList() {
        List<Synapse> ret = new ArrayList<Synapse>();
        for(SynapseGroup group : synapseGroupList) {
            ret.addAll(group.getSynapseList());
        }
        return Collections.unmodifiableList(ret);
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Subnetwork Group [" + getLabel() + "] Subnetwork with "
                + neuronGroupList.size() + " neuron group(s) and ");
        ret += (synapseGroupList.size() + " synapse group(s)");
        if ((getNeuronGroupCount() + getSynapseGroupCount()) > 0) {
            ret += "\n";
        }
        for (NeuronGroup neuronGroup : neuronGroupList) {
            ret += "   " + neuronGroup.toString();
        }
        for (SynapseGroup synapseGroup : synapseGroupList) {
            ret += "   " + synapseGroup.toString();
        }
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

    @Override
    public void update() {
        for (NeuronGroup neuronGroup : neuronGroupList) {
            neuronGroup.update();
        }
    }
    
    /**
     * Associates a synapse group with a neuron group. Any synapse that is added
     * to neurons in that group are automatically added to the specified synapse
     * group.
     * 
     * @param synapseGroup the synapse group to attach. It will now "grow"
     *            automatically as neurons are added.
     */
    protected void attachSynapseGroupToNeuronGroup(final SynapseGroup synapseGroup,
            final NeuronGroup neuronGroup) {

        // TODO: If synapse group or neuron group does not exist fire exception
        
        // TODO: Also. Can't have a neuron group associated with multiple
        // synapse groups (at least not now).

        // If this is the first synapse group with a listener, add the listener
        if (!usingGrowingSynapseGroups) {
            usingGrowingSynapseGroups = true;
            addSynapseListener();
        }

        // Don't delete this synapse group when it is empty.  It should grow and shrink
        //  as synapses are added and deleted
        synapseGroup.setDeleteWhenEmpty(false);
        
        // Create association between neuron group and synapse group
        neuronGroupToSyanpseGroupMap.put(neuronGroup, synapseGroup);

    }
        
    /**
     * Add the synapse listener used for growing synapse groups.
     */
    private void addSynapseListener() {
        getParentNetwork().addSynapseListener(synapseListener);
        
    }
    
    /**
     * Remove the synapse listener used for growing synapse groups.
     */
    private void removeSynapseListener() {
        getParentNetwork().removeSynapseListener(synapseListener);
    }

    /**
     * Return the neuron group, if any, this synapse is associated with.
     * Return null if there is no such group.
     * 
     * @param synapse the synapse to check
     * @return the associated neuron group, or null if there is none 
     */
    private NeuronGroup getAssociatedNeuronGroup(final Synapse synapse) {

        NeuronGroup ret = null;
        // Check whether this synapse is contained in any of the neuron groups that
        //  "growing" synapse groups are attached to
        for (NeuronGroup neuronGroup : neuronGroupToSyanpseGroupMap.keySet()) {
            if (neuronGroup.inFanInOfSomeNode(synapse)) {
                ret = neuronGroup;
            }
        }
        return ret;
    }
    
    /**
     * Listen for synapse events and add new synapse when they arrive.
     */
    private final SynapseListener synapseListener = new SynapseListener() {

        public void synapseRemoved(NetworkEvent<Synapse> networkEvent) {
            // This is handled elsewhere
        }

        public void synapseAdded(NetworkEvent<Synapse> networkEvent) {
            
            // A synapse arrives
            Synapse synapse = networkEvent.getObject();

            // Check if the synapse is attached to some neuron group in 
            // this subnetwork
            NeuronGroup parentGroup = getAssociatedNeuronGroup(synapse);
            
            if (parentGroup != null) {
                
                // Find the appropriate synapseGroup
                SynapseGroup synapseGroup = neuronGroupToSyanpseGroupMap.get(parentGroup);

                // Move the synapse from the root network over to this subnet
                getParentNetwork().transferSynapsesToGroup(
                        (List<Synapse>) Collections.singletonList(synapse),
                        synapseGroup);
                
                // Fire Event so network panel knows to add this synapse to appropriate
                //  PNode
                NetworkEvent<Group> event = new NetworkEvent<Group>(
                        getParentNetwork(), Subnetwork.this, Subnetwork.this);
                event.setAuxiliaryObject(synapse);
                getParentNetwork().fireGroupChanged(event,"synapseAdded"); 
            }
        }

        public void synapseChanged(NetworkEvent<Synapse> networkEvent) {
        }

        public void synapseTypeChanged(NetworkEvent<SynapseUpdateRule> networkEvent) {
        }
        
    };   
}
