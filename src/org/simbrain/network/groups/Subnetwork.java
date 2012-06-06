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
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * A collection of neuron groups and synapse groups which functions as a
 * subnetwork within the main root network, with its own update rules.
 */
public class Subnetwork extends Group {

    /** List of neuron groups. */
    private final List<NeuronGroup> neuronGroupList = new CopyOnWriteArrayList<NeuronGroup>();

    /** List of synapse groups. */
    private final List<SynapseGroup> synapseGroupList = new CopyOnWriteArrayList<SynapseGroup>();

    /**
     * Create subnetwork group.
     *
     * @param net parent network.
     */
    public Subnetwork(final Network net) {
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
    public Subnetwork(final Network net, int numNeuronGroups,
            int numSynapseGroups) {
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
        for (SynapseGroup synapseGroup : synapseGroupList) {
            synapseGroup.setDeleteWhenEmpty(true);
            getParentNetwork().removeGroup(synapseGroup);
        }
    }

    @Override
    public boolean isEmpty() {
        boolean neuronGroupsEmpty = neuronGroupList.isEmpty();
        boolean synapseGroupsEmpty = synapseGroupList.isEmpty();

        // If synapse groups exist but are empty, treat synapse groups as empty
        boolean allAreEmpty = true;
        for (SynapseGroup synapseGroup : synapseGroupList) {
            if (!synapseGroup.isEmpty()) {
                allAreEmpty = false;
            }
        }
        if (allAreEmpty) {
            synapseGroupsEmpty = true;
        }

        return (neuronGroupsEmpty && synapseGroupsEmpty);
    }

    /**
     * Add a synapse group
     *
     * @param group the synapse group to add
     */
    public void addSynapseGroup(SynapseGroup group) {
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
    public SynapseGroup connectNeuronGroups(NeuronGroup source,
            NeuronGroup target) {
        AllToAll connection = new AllToAll(getParentNetwork(),
                source.getNeuronList(), target.getNeuronList());
        // connection.setPercentExcitatory(1);
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
    public SynapseGroup connectNeuronGroups(NeuronGroup source,
            NeuronGroup target, ConnectNeurons connection) {
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
    public SynapseGroup connectNeuronGroups(NeuronGroup source,
            NeuronGroup target, String sourceLabel, String targetLabel,
            ConnectNeurons connection) {
        List<Synapse> synapses = connection.connectNeurons(getParentNetwork(),
                source.getNeuronList(), target.getNeuronList());
        SynapseGroup newGroup = new SynapseGroup(getParentNetwork());
        getParentNetwork().transferSynapsesToGroup(synapses, newGroup);
        addSynapseGroup(newGroup);
        newGroup.setDeleteWhenEmpty(false);
        setSynapseGroupLabel(source, target, newGroup, sourceLabel, targetLabel);

        // By default set up a synapse routing...
        getParentNetwork().getSynapseRouter()
                .associateSynapseGroupWithNeuronGroupPair(source, target,
                        newGroup);
        return newGroup;
    }

    /**
     * The source and target neuron groups are associated with a synapse group
     * that is initially empty. A routing rule is set up so that the synapse
     * group will be automatically populated when synapses are added which
     * connect the source and parent neuron group.
     *
     * @param source the source neuron group
     * @param target the target neuron group
     */
    public void addEmptySynapseGroup(NeuronGroup source, NeuronGroup target) {
        SynapseGroup sg = new SynapseGroup(getParentNetwork());
        addSynapseGroup(sg);
        sg.setDeleteWhenEmpty(false);
        setSynapseGroupLabel(source, target, sg, ""
                + (indexOfNeuronGroup(source) + 1), ""
                + (indexOfNeuronGroup(target) + 1));

        getParentNetwork().getSynapseRouter()
                .associateSynapseGroupWithNeuronGroupPair(source, target, sg);

    }

    /**
     * Utility method for labeling synapse groups based on the neuron groups
     * they connect. A forward arrow is used for feed-forward synapse groups, a
     * circular arrow for recurrent synapse groups.
     *
     * @param source source neuron group
     * @param target target neuron group
     * @param sg synapse group
     * @param sourceLabel source label
     * @param targetLabel target label
     */
    private void setSynapseGroupLabel(NeuronGroup source, NeuronGroup target,
            final SynapseGroup sg, final String sourceLabel,
            final String targetLabel) {
        if (!source.equals(target)) {
            sg.setLabel("Weights " + sourceLabel + " "
                    + new Character('\u2192') + " " + targetLabel);
        } else {
            sg.setLabel("Weights " + sourceLabel + " "
                    + new Character('\u21BA'));
        }

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
     * Return neuron groups as a list. Used in backprop trainer.
     *
     * @return layers list
     */
    public List<List<Neuron>> getNeuronGroupsAsList() {
        List<List<Neuron>> ret = new ArrayList<List<Neuron>>();
        for (NeuronGroup group : neuronGroupList) {
            ret.add(group.getNeuronList());
        }
        return ret;
    }

    /**
     * Returns the index of a neuron group.
     *
     * @param group the group being queried.
     * @return the index of the group in the list.
     */
    public int indexOfNeuronGroup(NeuronGroup group) {
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
        for (NeuronGroup group : neuronGroupList) {
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
        for (SynapseGroup group : synapseGroupList) {
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

}
