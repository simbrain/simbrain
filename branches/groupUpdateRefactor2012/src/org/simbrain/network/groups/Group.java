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
package org.simbrain.network.groups;

import java.util.List;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;


/**
 * <b>Group</b>: a logical group of neurons and synapses (and perhaps other
 * items later). In some cases this is useful for custom updating. In other
 * cases it is useful simply as a logical grouping of nodes (e.g. to represent
 * the "layers" of a feedforward network) Its gui representation is
 * {@link org.simbrain.network.gui.nodes.GroupNode}.
 *
 * Possibly add a flag to have the group visible or not.
 */
public abstract class Group {

    /** Reference to the network this group is a part of. */
    private final RootNetwork parentNetwork;

//    /** Whether this Group should be active or not. */
//    private boolean isOn = true;

    /** Name of this group. */
    private String id;

    /** Name of this group. */
    private String label;

    /** Whether this group should be deleted when all its components are deleted. */
    private boolean deleteWhenEmpty = true;
    
    /** Flag which prevents infinite loops when deleting composite groups. */
    private boolean markedForDeletion = false;

    /**
     * Parent group of this group, or null if it has none. For group types which
     * have a hierarchy of groups.
     */
    private Group parentGroup;

    /**
     * Construct a model group with a reference to its root network.
     *
     * @param net reference to root network.
     */
    public Group(final RootNetwork net) {
        parentNetwork = net;
    }
       
    //REDO: Kind of weird that removeNeuron/Synapse are here but not add... 
    
    /**
     * Remove a neuron.  Subclasses with neuron lists should override this.
     * 
     * @param neuron neuron to delete
     */
    public void deleteNeuron(Neuron neuron) {};
    
    /**
     * Remove a synapse.  Subclasses with synapse lists should override this.
     * 
     * @param synapse synapse to delete
     */
    public void removeSynapse(Synapse synapse) {};
        
    /**
     * Whether this group is empty or not.
     *
     * @return true if the group is empty, false otherwise.
     */
    public abstract boolean isEmpty();

    /**
     * Perform necessary deletion cleanup.
     */
    public abstract void delete();


//
//    /**
//     * Turn the group on or off.  When off, the group update function
//     * should not be called.
//     */
//    public void toggleOnOff() {
//        if (isOn) {
//            isOn = false;
//        } else {
//            isOn = true;
//        }
//    }
//
//    /**
//     * @return whether the group is "on" or not.
//     */
//    public boolean isOn() {
//        return isOn;
//    }
//

    
    @Override
    public String toString() {
        if (label != null) {
            return label;
        } else if (id != null){
            return id;
        } else {
            return super.toString();
        }
    }

    /**
     * @return the parent
     */
    public RootNetwork getParentNetwork() {
        return parentNetwork;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
        parentNetwork.fireGroupParametersChanged(this);
    }

    /**
     * @return the parentGroup
     */
    public Group getParentGroup() {
        return parentGroup;
    }

    /**
     * @param parentGroup the parentGroup to set
     */
    protected void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }
    
    /**
     * Override to produce a list of all neurons contained in a group.
     */
    public List<Neuron> getFlatNeuronList() {
        return null;
    }

    /**
     * Override to produce a list of all synapses contained in a group.
     */
    public List<Synapse> getFlatSynapseList() {
        return null;
    }

    /**
     * @return the deleteWhenEmpty
     */
    public boolean isDeleteWhenEmpty() {
        return deleteWhenEmpty;
    }

    /**
     * @param deleteWhenEmpty the deleteWhenEmpty to set
     */
    protected void setDeleteWhenEmpty(boolean deleteWhenEmpty) {
        this.deleteWhenEmpty = deleteWhenEmpty;
    }

    /**
     * @return the markedForDeletion
     */
    protected boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    /**
     * @param markedForDeletion the markedForDeletion to set
     */
    protected void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

}