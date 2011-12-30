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
    public void removeNeuron(Neuron neuron) {};
    
    /**
     * Remove a synapse.  Subclasses with synapse lists should override this.
     * 
     * @param synapse synapse to delete
     */
    public void removeSynapse(Synapse synapse) {};
        
    public abstract boolean isEmpty();

    /**
     * Perform necessary deletion cleanup.
     */
    public abstract void delete();


    //    /**
//     * True if the group contains the specified neuron.
//     *
//     * @param n neuron to check for.
//     * @return true if the group contains this neuron, false otherwise
//     */
//    public boolean containsNeuron(final Neuron n) {
//        return neuronList.contains(n);
//    }
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

//    /**
//     * Returns the number of neurons and synapses in this group.
//     *
//     * @return the number of neurons and synapses in this group.
//     */
//    public int getElementCount() {
//        return neuronList.size() + synapseList.size();
//    }
//
//    /**
//     * Returns a debug string.
//     *
//     * @return the debug string.
//     */
//    public String debugString() {
//        String ret =  new String();
//        ret += ("Group with " + this.getNeuronList().size() + " neuron(s),");
//        ret += (" " + this.getSynapseList().size() + " synapse(s).");
//        return ret;
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
    public void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
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
    public void setDeleteWhenEmpty(boolean deleteWhenEmpty) {
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