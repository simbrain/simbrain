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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * A group of synapses. Must connect a source and target neuron group.
 */
public class SynapseGroup extends Group {

    /** The synapses in this group. */
    private final List<Synapse> synapseList =
            new CopyOnWriteArrayList<Synapse>();
//
//    private final Set<Synapse> synapseSet =
//            Collections.synchronizedSet(new HashSet<Synapse>());

    /** A map of the neuron pairs which already have a synapse between them. */
    private final HashMap<Neuron, HashSet<Neuron>> pairMap;

    /** Reference to source neuron group. */
    private final NeuronGroup sourceNeuronGroup;

    /** Reference to target neuron group. */
    private final NeuronGroup targetNeuronGroup;

    /**
     * Create a new synapse group.
     *
     * @param net parent network
     * @param source source neuron group
     * @param target target neuron group
     */
    public SynapseGroup(final Network net, final NeuronGroup source,
            final NeuronGroup target) {
        super(net);
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        pairMap = new HashMap<Neuron, HashSet<Neuron>>((int) 1.5
                * sourceNeuronGroup.getNeuronList().size());
        for (Neuron n : sourceNeuronGroup.getNeuronList()) {
            pairMap.put(n, new HashSet<Neuron>((int) (targetNeuronGroup.
                    getNeuronList().size() * 1.5)));
        }
        AllToAll connection = new AllToAll(net);
        List<Synapse> synapses = connection.connectNeurons(net,
                sourceNeuronGroup.getNeuronList(),
                targetNeuronGroup.getNeuronList(), true);
        for (Synapse synapse : synapses) {
            addSynapse(synapse);
        }
    }

    /**
     * Create a new synapse group using a connection object.
     *
     * @param net parent network
     * @param source source neuron group
     * @param target target neuron group
     * @param connection the connection object to use.
     */
    public SynapseGroup(final Network net, final NeuronGroup source,
            final NeuronGroup target, final ConnectNeurons connection) {
        super(net);
        this.sourceNeuronGroup = source;
        this.targetNeuronGroup = target;
        pairMap = new HashMap<Neuron, HashSet<Neuron>>((int) 1.5
                * sourceNeuronGroup.getNeuronList().size());
        for (Neuron n : sourceNeuronGroup.getNeuronList()) {
            pairMap.put(n, new HashSet<Neuron>((int) (targetNeuronGroup.
                    getNeuronList().size() * 1.5)));
        }
        List<Synapse> synapses = connection.connectNeurons(net,
                sourceNeuronGroup.getNeuronList(),
                targetNeuronGroup.getNeuronList(), false);
        for (Synapse syn : synapses) {
            if (!pairMap.get(syn.getSource()).contains(syn.getTarget())) {
                pairMap.get(syn.getSource()).add(syn.getTarget());
                addSynapse(syn);
            }
        }
    }

    @Override
    public void delete() {
        if (isMarkedForDeletion()) {
            return;
        } else {
            setMarkedForDeletion(true);
        }
        for (Synapse synapse : synapseList) {
            getParentNetwork().removeSynapse(synapse);
        }
        if (hasParentGroup()) {
            if (getParentGroup() instanceof Subnetwork) {
                ((Subnetwork) getParentGroup()).removeSynapseGroup(this);
            }
            if (getParentGroup().isEmpty()) {
                // System.out.println("SynapseGroup.delete");
                getParentNetwork().removeGroup(getParentGroup());
            }
        }
    }

    /**
     * @return a list of weights
     */
    public List<Synapse> getSynapseList() {
        return Collections.unmodifiableList(synapseList);
    }

    /**
     * Add a synapse to this synapse group.
     *
     * @param synapse synapse to add
     */
    public void addSynapse(final Synapse synapse) {
        // Don't add the synapse if it conflicts with an existing synapse.
//        if (conflictsWithExistingSynapse(synapse)) {
//            return false;
//        }
        synapseList.add(synapse);
        if (getParentNetwork() != null) {
            synapse.setId(getParentNetwork().getSynapseIdGenerator().getId());
            synapse.setParentGroup(this);
        }
    }

//    /**
//     * Returns true if a synapse with the same source and parent neurons
    //already
//     * exists in the synapse group.
//     *
//     * @param toCheck the synapse to check
//     * @return true if a synapse connecting the same neurons already exists,
//     *         false otherwise
//     */
//    private boolean conflictsWithExistingSynapse(final Synapse toCheck) {
//        for (Synapse synapse : synapseList)
//
//
////        for (Synapse synapse : synapseList) {
////            if (synapse.getSource() == toCheck.getSource()) {
////                if (synapse.getTarget() == toCheck.getTarget()) {
////                    return true;
////                }
////            }
////        }
//        return false;
//    }

    /**
     * Remove the provided synapse.
     *
     * @param toDelete the synapse to delete
     */
    public void removeSynapse(Synapse toDelete) {
        synapseList.remove(toDelete);
        getParentNetwork().fireSynapseRemoved(toDelete);
        getParentNetwork().fireGroupChanged(this, this, "synapseRemoved");
        if (isEmpty()) {
            // System.out.println("SynapseGroup.removeSynapse:" + toDelete);
            delete();
        }
    }

    /**
     * Return a list of source neurons associated with the synapses in this
     * group.
     *
     * @return the source neuron list.
     */
    public List<Neuron> getSourceNeurons() {
        // Use a set to remove repeat source neurons
        Set<Neuron> retList = new HashSet<Neuron>();
        for (Synapse synpase : synapseList) {
            retList.add(synpase.getSource());
        }
        return new ArrayList<Neuron>(retList);
    }

    /**
     * Return a list of target neurons associated with the synapses in this
     * group.
     *
     * @return the target neuron list.
     */
    public List<Neuron> getTargetNeurons() {
        // Use a set to remove repeat source neurons
        Set<Neuron> retList = new HashSet<Neuron>();
        for (Synapse synpase : synapseList) {
            retList.add(synpase.getTarget());
        }
        return new ArrayList<Neuron>(retList);
    }

    /**
     * Update group. Override for special updating.
     */
    public void update() {
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

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Synapse Group [" + getLabel() + "]. Contains "
                + this.getSynapseList().size() + " synapse(s)." + " Connects "
                + getSourceNeuronGroup().getId() + " ("
                + getSourceNeuronGroup().getLabel() + ")" + " to "
                + getTargetNeuronGroup().getId() + " ("
                + getTargetNeuronGroup().getLabel() + ")\n");

        return ret;
    }

    @Override
    public boolean isEmpty() {
        return synapseList.isEmpty();
    }

    /**
     * Determine whether this synpasegroup should have its synapses displayed.
     * For isolated synapse groups check its number of synapses. For
     * synapsegroups inside of subnetworks check the total synapses in the
     * parent subnetwork.
     *
     * @return whether to display synapses or not.
     */
    public boolean displaySynapses() {
        int threshold = getParentNetwork().getSynapseVisibilityThreshold();

        // Isolated synapse group
        if (getParentGroup() == null) {
            if (getSynapseList().size() > threshold) {
                return false;
            }
        } else if (getParentGroup() instanceof Subnetwork) {
            return ((Subnetwork) getParentGroup()).displaySynapses();
        }
        return true;
    }

    /**
     * @return the sourceNeuronGroup
     */
    public NeuronGroup getSourceNeuronGroup() {
        return sourceNeuronGroup;
    }

    /**
     * @return the targetNeuronGroup
     */
    public NeuronGroup getTargetNeuronGroup() {
        return targetNeuronGroup;
    }

    /**
     * Return weight strengths as a double vector.
     *
     * @return weights
     */
    public double[] getWeightVector() {
        double[] retArray = new double[synapseList.size()];
        int i = 0;
        for (Synapse synapse : synapseList) {
            retArray[i++] = synapse.getStrength();
        }
        return retArray;
    }

    /**
     * Set the weights using an array of doubles. Assumes the order of the items
     * in the array should match the order of items in the synapselist.
     *
     * Does not throw an exception if the provided input array and synapse list
     * do not match in size.
     *
     * @param weightVector the weight vector to set.
     */
    public void setWeightVector(double[] weightVector) {
        int i = 0;
        for (Synapse synapse : synapseList) {
            if (i >= weightVector.length) {
                break;
            }
            synapse.setStrength(weightVector[i++]);
        }
    }

    /**
     * Set all weight strengths to a specified value.
     *
     * @param value the value to set the synapses to
     */
    public void setStrengths(final double value) {
        for (Synapse s : getSynapseList()) {
            s.setStrength(value);
        }
    }

    /**
     * Enable or disable all synapses in this group.
     *
     * @param enabled true to enable them all; false to disable them all
     */
    public void setEnabled(final boolean enabled) {
        for (Synapse synapse : this.getSynapseList()) {
            synapse.setEnabled(enabled);
        }
    }

    /**
     * Freeze or unfreeze all synapses in this group.
     *
     * @param freeze true to freeze the group; false to unfreeze it
     */
    public void setFrozen(final boolean freeze) {
        for (Synapse synapse : this.getSynapseList()) {
            synapse.setFrozen(freeze);
        }
    }

    @Override
    public String getUpdateMethodDesecription() {
        return "Update synapses";
    }

}
