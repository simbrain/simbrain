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

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.events.SubnetworkEvents;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.simbrain.network.LocatableModelKt.getCenterLocation;
import static org.simbrain.util.GeomKt.minus;

/**
 * A collection of neuron groups and synapse groups which functions as a subnetwork within the main root network, with
 * its own update rules. Note that no neurons or synapses or other objects are contained in a subnet (as of now), it
 * only contains neuron and synapse groups.
 */
public abstract class Subnetwork implements EditableObject, LocatableModel, AttributeContainer {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    /**
     * Id of this group.
     */
    @UserParameter(label = "ID", description = "Id of this subnetwork", order = -1, editable = false)
    protected String id;

    /**
     * Name of this group. Null strings lead to default labeling conventions.
     */
    @UserParameter(label = "Label", description = "Subnetwork label", useSetter = true,
            order = 10)
    private String label = "";

    /**
     * Event support.
     */
    protected transient SubnetworkEvents events = new SubnetworkEvents(this);

    /**
     * List of neuron groups.
     */
    private final List<NeuronGroup> neuronGroupList = new CopyOnWriteArrayList<NeuronGroup>();

    /**
     * List of synapse groups.
     */
    private final List<SynapseGroup> synapseGroupList = new CopyOnWriteArrayList<SynapseGroup>();

    /**
     * List of weight matrices
     */
    private final List<WeightMatrix> weightMatrixList = new CopyOnWriteArrayList<>();

    /**
     * Whether the GUI should display neuron groups contained in this subnetwork. This will usually be true, but in
     * cases where a subnetwork has just one neuron group it is redundant to display both. So this flag indicates to the
     * GUI that neuron groups in this subnetwork need not be displayed.
     */
    private boolean displayNeuronGroups = true;

    /**
     * Create subnetwork group.
     *
     * @param net parent network.
     */
    public Subnetwork(final Network net) {
        parentNetwork = net;
        setLabel("Subnetwork");
        net.getEvents().onNeuronGroupRemoved(this::removeNeuronGroup);
        net.getEvents().onSynapseGroupRemoved(this::removeSynapseGroup);
    }

    /**
     * Delete this subnetwork and its children.
     */
    public void delete() {
        neuronGroupList.forEach(this::removeNeuronGroup);
        synapseGroupList.forEach(this::removeSynapseGroup);
        events.fireDelete();
    }

    /**
     * True if the subnetwork has no neuron groups or synapse groups (or only empty synapse groups).
     */
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
     * Add a synapse group.
     *
     * @param group the synapse group to add
     */
    public void addSynapseGroup(SynapseGroup group) {
        synapseGroupList.add(group);
    }

    /**
     * Add a neuron group.
     *
     * @param group the neuron group to add
     */
    public void addNeuronGroup(NeuronGroup group) {
        neuronGroupList.add(group);
    }

    /**
     * Connects one group of neurons to another group of neurons using an All to All connection.
     *
     * @param source the source group
     * @param target the target group
     * @return the new neuron group
     */
    public SynapseGroup connectNeuronGroups(NeuronGroup source, NeuronGroup target) {
        SynapseGroup newGroup = connectNeuronGroups(source, target, new AllToAll(true));
        return newGroup;
    }

    /**
     * Connects two groups of neurons according to some connection style.
     *
     * @param source     the source group
     * @param target     the target group
     * @param connection the type of connection desired between the two groups
     * @return the new group
     */
    public SynapseGroup connectNeuronGroups(NeuronGroup source, NeuronGroup target, ConnectionStrategy connection) {
        SynapseGroup newGroup = connectNeuronGroups(source, target, "" + (getIndexOfNeuronGroup(source) + 1), "" + (getIndexOfNeuronGroup(target) + 1), connection);
        return newGroup;
    }

    /**
     * Connects two groups of neurons according to some connection style, and allows for custom labels of the neuron
     * groups within the weights label.
     *
     * @param source      the source group
     * @param target      the target group
     * @param sourceLabel the name of the source group in the weights label
     * @param targetLabel the name of the target group in the weights label
     * @param connection  the type of connection desired between the two groups
     * @return the new group
     */
    public SynapseGroup connectNeuronGroups(NeuronGroup source, NeuronGroup target, String sourceLabel, String targetLabel, ConnectionStrategy connection) {
        SynapseGroup newGroup = SynapseGroup.createSynapseGroup(source, target, connection);
        addSynapseGroup(newGroup);
        setSynapseGroupLabel(source, target, newGroup, sourceLabel, targetLabel);
        return newGroup;
    }

    /**
     * Utility method for labeling synapse groups based on the neuron groups they connect. A forward arrow is used for
     * feed-forward synapse groups, a circular arrow for recurrent synapse groups.
     *
     * @param source      source neuron group
     * @param target      target neuron group
     * @param sg          synapse group
     * @param sourceLabel source label
     * @param targetLabel target label
     */
    private void setSynapseGroupLabel(NeuronGroup source, NeuronGroup target, final SynapseGroup sg, final String sourceLabel, final String targetLabel) {
        if (!source.equals(target)) {
            sg.setLabel(sourceLabel + " " + new Character('\u2192') + " " + targetLabel);
        } else {
            sg.setLabel(sourceLabel + " " + new Character('\u21BA'));
        }

    }

    /**
     * Adds an already constructed synapse group to the subnetwork and provides it with an appropriate label.
     *
     * @param synGrp group to add
     */
    public void addAndLabelSynapseGroup(SynapseGroup synGrp) {
        addSynapseGroup(synGrp);
        NeuronGroup source = synGrp.getSourceNeuronGroup();
        NeuronGroup target = synGrp.getTargetNeuronGroup();
        setSynapseGroupLabel(source, target, synGrp, source.getLabel(), target.getLabel());
    }

    /**
     * Remove a neuron group.
     *
     * @param neuronGroup group to remove
     */
    public void removeNeuronGroup(NeuronGroup neuronGroup) {
        neuronGroupList.remove(neuronGroup);
        neuronGroup.delete();
        if (isEmpty()) {
            delete();
        }
    }

    /**
     * Remove a synapse group.
     *
     * @param synapseGroup group to remove
     */
    public void removeSynapseGroup(SynapseGroup synapseGroup) {
        synapseGroupList.remove(synapseGroup);
        synapseGroup.delete();
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
     * Find neuron group with a given label, or null if none found.
     *
     * @param label label to search for.
     * @return neurongroup with that label found, null otherwise
     */
    public NeuronGroup getNeuronGroupByLabel(final String label) {
        for (NeuronGroup group : getNeuronGroupList()) {
            if (group.getLabel().equalsIgnoreCase(label)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Find synapse group with a given label, or null if none found.
     *
     * @param label label to search for.
     * @return synapsegroup with that label found, null otherwise
     */
    public SynapseGroup getSynapseGroupByLabel(final String label) {
        for (SynapseGroup group : getSynapseGroupList()) {
            if (group.getLabel().equalsIgnoreCase(label)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Get the first neuron group in the list. Convenience method when there is just one neuron group.
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
    public int getIndexOfNeuronGroup(NeuronGroup group) {
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
     * Get the first synapse group in the list. Convenience method when there is just one synapse group.
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
     * Unomodifiable weight matrix list.
     */
    public List<WeightMatrix> getWeightMatrixList() {
        return Collections.unmodifiableList(weightMatrixList);
    }

    /**
     * Add a weight matrix to the subnet.
     */
    public void addWeightMatrix(WeightMatrix wm) {
        weightMatrixList.add(wm);
        parentNetwork.getEvents().fireWeightMatrixAdded(wm);
        parentNetwork.getEvents().onWeightMatrixRemoved(w -> {
            weightMatrixList.remove(w);
        });
    }

    /**
     * Return a "flat" list containing every neuron in every neuron group in this subnetwork.
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
     * Returns a "flat" list containing every neuron in every neuron group in this subnetwork. This list <b>is</b>
     * modifiable, but this method is protected... use with care.
     *
     * @return flat neuron list
     */
    protected List<Neuron> getModifiableNeuronList() {
        List<Neuron> ret = new ArrayList<Neuron>();
        for (NeuronGroup group : neuronGroupList) {
            ret.addAll(group.getNeuronList());
        }
        return ret;
    }

    /**
     * Return a "flat" list containing every synapse in every synapse group in this subnetwork.
     *
     * @return the flat synapse list.
     */
    public List<Synapse> getFlatSynapseList() {
        List<Synapse> ret = new ArrayList<Synapse>();
        for (SynapseGroup group : synapseGroupList) {
            ret.addAll(group.getAllSynapses());
        }
        return Collections.unmodifiableList(ret);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subnetwork Group [" + getLabel() + "] Subnetwork with " + neuronGroupList.size() + " neuron " +
                "group(s) and ");
        sb.append(synapseGroupList.size()).append(" synapse group(s) and ")
                .append(weightMatrixList.size()).append(" weight matrices\n");
        neuronGroupList.forEach(sb::append);
        if(synapseGroupList.size() > 0) {
            synapseGroupList.forEach(sb::append);
        }
        if(weightMatrixList.size() > 0) {
            weightMatrixList.forEach(sb::append);
        }
        return sb.toString();
    }

    /**
     * Get long description for info box, formmated in html. Override for more detailed description.
     *
     * @return the long description.
     */
    public String getLongDescription() {
        String ret = new String();
        ret += ("<html>Subnetwork [" + getLabel() + "]<br>" + "Subnetwork with " + neuronGroupList.size() + " neuron group(s) and ");
        ret += (synapseGroupList.size() + " synapse group(s)");
        if ((getNeuronGroupCount() + getSynapseGroupCount()) > 0) {
            ret += "<br>";
        }
        for (NeuronGroup neuronGroup : neuronGroupList) {
            ret += "Neuron Group:  " + neuronGroup.getLabel() + "<br>";
        }
        for (SynapseGroup synapseGroup : synapseGroupList) {
            ret += "Synapse Group:   " + synapseGroup.getLabel() + "<br>";
        }
        ret += "</html>";
        return ret;
    }

    public boolean getEnabled() {
        return false;
    }

    public void setEnabled(boolean enabled) {
    }

    /**
     * Default subnetwork update just updates all neuron and synapse groups. Subclasses with custom update should
     * override this.
     */
    public void update() {

        neuronGroupList.forEach(NeuronGroup::update);
        synapseGroupList.forEach(SynapseGroup::update);
        weightMatrixList.forEach(WeightMatrix::update);

    }

    /**
     * Set all activations to 0.
     */
    public void clearActivations() {
        for (Neuron n : this.getFlatNeuronList()) {
            n.clear();
        }
    }

    /**
     * If this subnetwork is trainable, then add the current activation of the "input" neuron group to the input data of
     * the training set. Assumes the input neuron group is that last in the list of neuron groups (as is the case with
     * Hopfield, Competitive, and SOM). Only makes sense with unsupervised learning, since only input data (and no
     * target data) are added.
     */
    public void addRowToTrainingSet() {
        if (this instanceof Trainable) {
            ((Trainable) this).getTrainingSet().addRow(getNeuronGroupList().get(getNeuronGroupList().size() - 1).getActivations());
        }
    }

    //@Override
    //public void initializeId() {
    //    // Set id for subnetwork
    //    super.initializeId();
    //    // Set ids for neuron groups
    //    for (Group ng : neuronGroupList) {
    //        ng.initializeId();
    //    }
    //    // Set ids for synapse groups
    //    for (Group sg : synapseGroupList) {
    //        sg.initializeId();
    //    }
    //}

    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        String oldLabel = this.label;
        this.label = label;
        events.fireLabelChange(oldLabel , label);
    }

    @Producible(defaultVisibility = false)
    public String getLabel() {
        return label;
    }

    public Network getParentNetwork() {
        return parentNetwork;
    }
    
    public void postUnmarshallingInit() {
        events = new SubnetworkEvents(this);
        neuronGroupList.forEach(AbstractNeuronCollection::postUnmarshallingInit);
        synapseGroupList.forEach(SynapseGroup::postUnmarshallingInit);
    }

    @Override
    public void setLocation(@NotNull Point2D location) {
        Point2D delta = minus(location, getLocation());
        neuronGroupList.forEach(ng -> ng.offset(delta.getX(), delta.getY()));
    }

    @NotNull
    @Override
    public Point2D getLocation() {
        return getCenterLocation(neuronGroupList);
    }

    public SubnetworkEvents
    getEvents() {
        return events;
    }

    @Override
    public void setBufferValues() {
        // TODO
    }

    @Override
    public void applyBufferValues() {
        // TODO
    }

}
