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
package org.simbrain.network.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.network.listeners.TextListener;
import org.simbrain.network.update_actions.CustomUpdate;
import org.simbrain.network.util.CopyPaste;
import org.simbrain.network.util.SynapseRouter;
import org.simbrain.util.SimpleId;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>Network</b> provides core neural network functionality and is the the main
 * API for external calls. Network objects are sets of neurons and weights
 * connecting them. Most update and learning logic occurs in the neurons and
 * weights themselves, as well as in special groups.
 */
public class Network {

    /** Logger. */
    private Logger logger = Logger.getLogger(Network.class);

    /** Array list of neurons. */
    private final List<Neuron> neuronList = new ArrayList<Neuron>();

    /** Array list of synapses. */
    private final List<Synapse> synapseList = new ArrayList<Synapse>();

    /** Since groups span all levels of the hierarchy they are stored here. */
    private final List<Group> groupList = new ArrayList<Group>();

    /** Text objects. */
    private List<NetworkTextObject> textList = new ArrayList<NetworkTextObject>();

    /** The update manager for this network. */
    private UpdateManager updateManager;

    /** Object which routes synapses to synapse groups. */
    private final SynapseRouter synapseRouter;

    /** The initial time-step for the network. */
    private static final double DEFAULT_TIME_STEP = .01;

    /** Constant value for Math.log(10); used to approximate log 10. */
    private static final double LOG_10 = Math.log(10);

    /** In iterations or msec. */
    private double time = 0;

    /** Time step. */
    private double timeStep = DEFAULT_TIME_STEP;

    /**
     * Two types of time used in simulations. DISCRETE: Network update
     * iterations are time-steps CONTINUOUS: Simulation of real time. Each
     * updates advances time by length {@link timeStep}
     */
    public enum TimeType {
        DISCRETE, CONTINUOUS
    }

    /** Whether this is a discrete or continuous time network. */
    private TimeType timeType = TimeType.DISCRETE;

    /** List of objects registered to observe general network events. */
    private List<NetworkListener> networkListeners = new ArrayList<NetworkListener>();

    /** List of objects registered to observe neuron-related network events. */
    private List<NeuronListener> neuronListeners = new ArrayList<NeuronListener>();

    /** List of objects registered to observe synapse-related network events. */
    private List<SynapseListener> synapseListeners = new ArrayList<SynapseListener>();

    /** List of objects registered to observe group-related network events. */
    private List<GroupListener> groupListeners = new ArrayList<GroupListener>();

    /** List of objects registered to observe text-related network events. */
    private List<TextListener> textListeners = new ArrayList<TextListener>();

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** Used to temporarily turn off all learning. */
    private boolean clampWeights = false;

    /** Used to temporarily hold weights at their current value. */
    private boolean clampNeurons = false;

    /**
     * List of neurons sorted by their update priority. Used in priority based
     * update.
     */
    private List<Neuron> prioritySortedNeuronList;

    /** Comparator used for sorting the priority sorted neuron list. */
    private PriorityComparator priorityComparator = new PriorityComparator();

    /** Neuron Id generator. */
    private SimpleId neuronIdGenerator = new SimpleId("Neuron", 1);

    /** Synapse Id generator. */
    private SimpleId synapseIdGenerator = new SimpleId("Synapse", 1);

    /** Group Id generator. */
    private SimpleId groupIdGenerator = new SimpleId("Group", 1);

    /** Whether to round off neuron values. */
    private boolean roundOffActivationValues = false;

    /** Degree to which to round off values. */
    private int precision = 0;

    /**
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
        updateManager = new UpdateManager(this);
        synapseRouter = new SynapseRouter();
        prioritySortedNeuronList = new ArrayList<Neuron>();
    }
    
    /**
     * The core update function of the neural network. Calls the current update
     * function on each neuron, decays all the neurons, and checks their bounds.
     */
    public void update() {

        // Update Time
        updateTime();

        // Perform update
        for (UpdateAction action : updateManager.getActionList()) {
            action.invoke();
        }

        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();
    }

    /**
     * Update all neuron groups and other groups.
     */
    public void updateAllGroups() {
        // Update group lists
        if (getGroupList() != null) {
            for (Group group : getGroupList()) {
                group.update();
            }
        }
    }

    /**
     * Update the priority list used for priority based update.
     */
    void updatePriorityList() {
        prioritySortedNeuronList = this.getFlatNeuronList();
        resortPriorities();
    }

    /**
     * Resort the neurons according to their update priorities.
     */
    void resortPriorities() {
        Collections.sort(prioritySortedNeuronList, priorityComparator);
    }

    /**
     * This function is used to update the neuron and sub-network activation
     * values if the user chooses to set different priority values for a subset
     * of neurons and sub-networks. The priority value determines the order in
     * which the neurons and sub-networks get updated - smaller priority value
     * elements will be updated before larger priority value elements.
     */
    public void updateNeuronsByPriority() {

        if (this.getClampNeurons() == true) {
            return;
        }

        for (Neuron neuron : prioritySortedNeuronList) {
            neuron.update();
            neuron.setActivation(neuron.getBuffer());
            // System.out.println("Priority:" + neuron.getUpdatePriority());
        }
    }

    /**
     * Clears out input values of network nodes, which otherwise linger and
     * cause problems.
     */
    public void clearInputs() {

        // TODO: Is there a more efficient way to handle this?
        // i.e. a way to get a list of neurons that (1) are coupled or better,
        // (2) have input values which consume.
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.setInputValue(0);
        }
    }

    /**
     * @return List of neurons in network.
     */
    public List<? extends Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    /**
     * @return List of synapses in network.
     */
    public List<Synapse> getSynapseList() {
        return Collections.unmodifiableList(synapseList);
    }

    /**
     * @return Number of neurons in network.
     */
    public int getNeuronCount() {
        return neuronList.size();
    }

    /**
     * @return Number of weights in network
     */
    public int getSynapseCount() {
        return synapseList.size();
    }

    /**
     * Returns distance between centers of two neurons.
     *
     * @param neuron1
     *            first neuron
     * @param neuron2
     *            second neuron
     * @return distance
     */
    public static double getDistance(final Neuron neuron1, final Neuron neuron2) {
        return Math.sqrt(Math.pow(neuron2.getX() - neuron1.getX(), 2)
                + Math.pow(neuron2.getY() - neuron1.getY(), 2));
    }

    /**
     * @param index
     *            Number of neuron in array list.
     * @return Neuron at the point of the index
     */
    public Neuron getNeuron(final int index) {
        return neuronList.get(index);
    }

    /**
     * Return a list of neurons in a specific radius of a specified neuron.
     *
     * @param source
     *            the source neuron.
     * @param radius
     *            the radius to search within.
     * @return list of neurons in the given radius.
     */
    public ArrayList<Neuron> getNeuronsInRadius(final Neuron source,
            final double radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        for (Neuron neuron : neuronList) {
            if (getDistance(source, neuron) < radius) {
                ret.add(neuron);
            }
        }
        return ret;
    }

    /**
     * Find a neuron with a given string id.
     *
     * @param id
     *            id to search for.
     * @return neuron with that id, null otherwise
     */
    public Neuron getNeuron(final String id) {
        for (Neuron n : getFlatNeuronList()) {
            if (n.getId().equalsIgnoreCase(id)) {
                return n;
            }
        }
        return null;
    }

    /**
     * Find a group with a given string id.
     *
     * @param id
     *            id to search for.
     * @return group with that id, null otherwise
     */
    public Group getGroup(final String id) {
        for (Group group : getGroupList()) {
            if (group.getId().equalsIgnoreCase(id)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Find groups with a given label.
     *
     * @param label
     *            label to search for.
     * @return list of groups with that label found, null otherwise
     */
    public List<Group> getGroupsByLabel(final String label) {
        List<Group> returnList = new ArrayList<Group>();
        for (Group group : getGroupList()) {
            if (group.getLabel().equalsIgnoreCase(label)) {
                returnList.add(group);
            }
        }
        return returnList;
    }

    /**
     * Returns the group list.
     *
     * @return the groupList
     */
    public List<? extends Group> getGroupList() {
        return Collections.unmodifiableList(groupList);
    }

    /**
     * Find a synapse with a given string id.
     *
     * @param id
     *            id to search for.
     * @return synapse with that id, null otherwise
     */
    public Synapse getSynapse(final String id) {
        for (Synapse s : getFlatSynapseList()) {
            if (s.getId().equalsIgnoreCase(id)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Adds a new neuron.
     *
     * @param neuron
     *            Type of neuron to add
     */
    public void addNeuron(final Neuron neuron) {
        neuronList.add(neuron);
        neuron.setId(getNeuronIdGenerator().getId());
        updatePriorityList();
        fireNeuronAdded(neuron);
        neuron.init();
    }

    /**
     * @param index
     *            Number of weight in array list.
     * @return Weight at the point of the index
     */
    public Synapse getSynapse(final int index) {
        return synapseList.get(index);
    }

    /**
     * Adds a weight to the neuron network, where that weight already has
     * designated source and target neurons.
     *
     * @param synapse
     *            the weight object to add
     */
    public void addSynapse(final Synapse synapse) {
        synapse.initSpikeResponder();
        synapseList.add(synapse);
        synapse.setId(getSynapseIdGenerator().getId());
        fireSynapseAdded(synapse);
        // TODO: Possibly optimize so that this is only called if
        // at least one neuron group / or synapse group exists.
        getSynapseRouter().routeSynapse(synapse);
    }

    /**
     * Calls {@link Neuron#update} for each neuron.
     */
    public void bufferedUpdateAllNeurons() {

        if (getClampNeurons()) {
            return;
        }

        // First update the activation buffers
        for (Neuron n : neuronList) {
            n.update(); // update neuron buffers
        }

        // Then update the activations themselves
        for (Neuron n : neuronList) {
            n.setActivation(n.getBuffer());
        }
    }

    /**
     * Calls {@link Synapse#update} for each weight.
     */
    public void updateAllSynapses() {

        if (getClampWeights()) {
            return;
        }

        // No Buffering necessary because the values of weights don't depend on
        // one another
        for (Synapse s : synapseList) {
            s.update();
        }
    }

    /**
     * Calls {@link Neuron#checkBounds} for each neuron, which makes sure the
     * neuron has not exceeded its upper bound or gone below its lower bound.
     * TODO: Add or replace with normalization within bounds?
     */
    public void checkAllBounds() {
        for (Neuron n : neuronList) {
            n.checkBounds();
        }

        for (int i = 0; i < synapseList.size(); i++) {
            Synapse w = (Synapse) synapseList.get(i);
            w.checkBounds();
        }
    }

    /**
     * Round activations off to integers; for testing.
     */
    public void roundAll() {
        for (Neuron n : neuronList) {
            n.round(precision);
        }
    }

    /**
     * Deletes a neuron from the network.
     *
     * @param toDelete
     *            neuron to delete
     */
    public void removeNeuron(final Neuron toDelete) {

        // Update priority list
        updatePriorityList();

        // Remove outgoing synapses
        while (toDelete.getFanOut().size() > 0) {
            List<Synapse> fanOut = toDelete.getFanOut();
            Synapse s = fanOut.get(fanOut.size() - 1);
            removeSynapse(s);
        }

        // Remove incoming synapses
        while (toDelete.getFanIn().size() > 0) {
            List<Synapse> fanIn = toDelete.getFanIn();
            Synapse s = fanIn.get(fanIn.size() - 1);
            removeSynapse(s);
        }

        // Remove the neuron itself. Either from a parent group that holds it,
        // or from the root network.
        if (toDelete.getParentGroup() != null) {
            if (toDelete.getParentGroup() instanceof NeuronGroup) {
                ((NeuronGroup) toDelete.getParentGroup())
                        .removeNeuron(toDelete);
            }
            if (toDelete.getParentGroup().isEmpty()) {
                removeGroup(toDelete.getParentGroup());
            }
        } else {
            neuronList.remove(toDelete);
        }

        // Notify listeners that this neuron has been deleted
        fireNeuronRemoved(toDelete);

    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete
     *            the weight to delete
     * @param notify
     *            whether to fire a synapse deleted event
     */
    public void removeSynapse(final Synapse toDelete) {

        // Remove references to this synapse from parent neurons
        if (toDelete.getSource() != null) {
            toDelete.getSource().removeTarget(toDelete);
        }
        if (toDelete.getTarget() != null) {
            toDelete.getTarget().removeSource(toDelete);
        }

        // If this synapse has a parent group, delete that group
        if (toDelete.getParentGroup() != null) {
            Group parentGroup = toDelete.getParentGroup();
            if (parentGroup instanceof SynapseGroup) {
                ((SynapseGroup) parentGroup).removeSynapse(toDelete);
            }
            if (parentGroup.isEmpty() && parentGroup.isDeleteWhenEmpty()) {
                removeGroup(toDelete.getParentGroup());
            }
        } else {
            synapseList.remove(toDelete);
        }

        // Notify listeners that this synapse has been deleted
        fireSynapseRemoved(toDelete);

    }

    /**
     * Remove the given neurons from the neuron list (without firing an event)
     * and add them to the provided group.
     *
     * @param list
     *            the list of neurons to transfer
     * @param group
     *            the group to transfer them to
     */
    public void transferNeuronsToGroup(List<Neuron> list, NeuronGroup group) {
        for (Neuron neuron : list) {
            neuronList.remove(neuron);
            group.addNeuron(neuron, false);
        }
    }

    /**
     * Remove the given synapses from the synapse list (without firing an event)
     * and add them to the specified group.
     *
     * @param list
     *            the list of synapses to transfer
     * @param group
     *            the group to transfer them to
     */
    public void transferSynapsesToGroup(List<Synapse> list, SynapseGroup group) {
        for (Synapse synapse : list) {
            transferSynapseToGroup(synapse, group);
        }
    }

    /**
     * Remove the given synapse from the synapse list (without firing an event)
     * and add it to the specified group.
     *
     * @param list
     *            the synapse to transfer
     * @param group
     *            the group to transfer them to
     */
    public void transferSynapseToGroup(Synapse synapse, SynapseGroup group) {
        synapseList.remove(synapse);
        group.addSynapse(synapse, false);
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    public void clearActivations() {
        setActivations(0);
    }

    /**
     * Set biases on all neurons with a bias to 0.
     */
    public void clearBiases() {
        for (Neuron neuron : this.getFlatNeuronList()) {
            if (neuron.getUpdateRule() instanceof BiasedNeuron) {
                ((BiasedNeuron) neuron.getUpdateRule()).setBias(0);
            }
        }
        fireNetworkChanged();
    }

    /**
     * Sets all neurons to a specified value.
     *
     * @param value
     *            value to set
     */
    public void setActivations(final double value) {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.setActivation(value);
        }
        fireNetworkChanged();
    }

    /**
     * Sets neuron activations using values in an array of doubles. Currently
     * these activations are applied to the network in whatever order the
     * neurons were added.
     *
     * @param activationArray
     *            array of values to apply to network
     */
    public void setActivations(final double[] activationArray) {
        // TODO: Sort by id
        int i = 0;
        for (Neuron neuron : this.getFlatNeuronList()) {
            if (activationArray.length == i) {
                return;
            }
            neuron.setActivation(activationArray[i++]);
        }
    }

    /**
     * Sets all weights to a specified value.
     *
     * @param value
     *            value to set
     */
    public void setWeights(final double value) {
        for (Synapse synapse : this.getFlatSynapseList()) {
            synapse.setStrength(value);
        }
    }

    /**
     * Returns the "state" of the network--the activation level of its neurons.
     * Used by the gauge component.
     *
     * @return an array representing the activation levels of all the neurons in
     *         this network
     */
    public double[] getState() {
        double[] ret = new double[this.getNeuronCount()];

        for (int i = 0; i < this.getNeuronCount(); i++) {
            Neuron n = getNeuron(i);
            ret[i] = (int) n.getActivation();
        }

        return ret;
    }

    /**
     * Sets all weight values to zero, effectively eliminating them.
     */
    public void setWeightsToZero() {
        for (Synapse s : synapseList) {
            s.setStrength(0);
        }
    }

    /**
     * Randomizes all neurons.
     */
    public void randomizeNeurons() {
        for (Neuron n : neuronList) {
            n.randomize();
        }
    }

    /**
     * Randomizes all weights.
     */
    public void randomizeWeights() {
        for (Synapse s : synapseList) {
            s.randomize();
        }
    }

    /**
     * Randomize all biased neurons.
     *
     * @param lower
     *            lower bound for randomization.
     * @param upper
     *            upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : neuronList) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Round a value off to indicated number of decimal places.
     *
     * @param value
     *            value to round off
     * @param decimalPlace
     *            degree of precision
     *
     * @return rounded number
     */
    public static double round(final double value, final int decimalPlace) {
        return new BigDecimal(value).setScale(decimalPlace,
                BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * @return Degree to which to round off values.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * @return Whether to round off neuron values.
     */
    public boolean getRoundingOff() {
        return roundOffActivationValues;
    }

    /**
     * Sets the degree to which to round off values.
     *
     * @param i
     *            Degeree to round off values
     */
    public void setPrecision(final int i) {
        precision = i;
    }

    /**
     * Whether to round off neuron values.
     *
     * @param b
     *            Round off
     */
    public void setRoundingOff(final boolean b) {
        roundOffActivationValues = b;
    }

    /**
     * @return Returns the roundOffActivationValues.
     */
    public boolean isRoundOffActivationValues() {
        return roundOffActivationValues;
    }

    /**
     * @param roundOffActivationValues
     *            The roundOffActivationValues to set.
     */
    public void setRoundOffActivationValues(
            final boolean roundOffActivationValues) {
        this.roundOffActivationValues = roundOffActivationValues;
    }

    /**
     * Add an array of neurons and set their parents to this.
     *
     * @param neurons
     *            list of neurons to add
     */
    protected void addNeuronList(final ArrayList<Neuron> neurons) {
        for (Neuron n : neurons) {
            addNeuron(n);
        }
    }

    /**
     * Sets the upper bounds.
     *
     * @param u
     *            Upper bound
     */
    public void setUpperBounds(final double u) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(u);
        }
    }

    /**
     * Sets the lower bounds.
     *
     * @param l
     *            Lower bound
     */
    public void setLowerBounds(final double l) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(l);
        }
    }

    /**
     * Returns a reference to the synapse connecting two neurons, or null if
     * there is none.
     *
     * @param src
     *            source neuron
     * @param tar
     *            target neuron
     *
     * @return synapse from source to target
     */
    public static Synapse getSynapse(final Neuron src, final Neuron tar) {
        for (Synapse s : src.getFanOut()) {
            if (s.getTarget() == tar) {
                return s;
            }
        }

        return null;
    }

    /**
     * Gets the synapse at particular point.
     *
     * @param i
     *            Neuron number
     * @param j
     *            Weight to get
     * @return Weight at the points defined
     */
    // TODO: Either fix this or make its assumptions explicit
    public Synapse getWeight(final int i, final int j) {
        return (Synapse) getNeuron(i).getFanOut().get(j);
    }

    /**
     * Add a group to the network.
     *
     * @param group
     *            group of network elements
     */
    public void addGroup(final Group group) {
        // Generate group id
        String id = getGroupIdGenerator().getId();
        group.setId(id);
        if (group.getLabel() == null) {
            group.setLabel(id.replaceAll("_", " "));
        }

        // Special creation for subnetworks
        if (group instanceof Subnetwork) {
            for (NeuronGroup neuronGroup : ((Subnetwork) group)
                    .getNeuronGroupList()) {
                addGroup(neuronGroup);
            }
            for (SynapseGroup synapseGroup : ((Subnetwork) group)
                    .getSynapseGroupList()) {
                addGroup(synapseGroup);
            }
        }

        if (group.isTopLevelGroup()) {
            groupList.add(group);
        }
        fireGroupAdded(group);
    }

    /**
     * Remove the specified group.
     *
     * @param toDelete
     *            the group to delete.
     */
    public void removeGroup(final Group toDelete) {

        // Remove from the group list
        groupList.remove(toDelete);

        // Call delete method on this group being deleted
        toDelete.delete();

        // Notify listeners that this group has been deleted.
        fireGroupRemoved(toDelete);
    }

    /**
     * Returns true if all objects are gone from this network.
     *
     * @return true if everything's gone.
     */
    public boolean isEmpty() {
        boolean neuronsGone = false;
        boolean networksGone = false;
        if (this.getNeuronCount() == 0) {
            neuronsGone = true;
        }
        return (neuronsGone && networksGone);
    }

    /**
     * Create "flat" list of neurons, which includes the top-level neurons plus
     * all group neurons.
     *
     * @return the flat list
     */
    public List<Neuron> getFlatNeuronList() {

        List<Neuron> ret = new ArrayList<Neuron>();
        ret.addAll(neuronList);

        //TODO: Base this on an overridable method?
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i) instanceof NeuronGroup) {
                NeuronGroup group = (NeuronGroup) groupList.get(i);
                ret.addAll(group.getNeuronList());
            } else if (groupList.get(i) instanceof Subnetwork) {
                Subnetwork group = (Subnetwork) groupList.get(i);
                ret.addAll(group.getFlatNeuronList());
            }
        }

        return ret;
    }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses
     * plus all subnet synapses.
     *
     * @return the flat list
     */
    public List<Synapse> getFlatSynapseList() {
        List<Synapse> ret = new ArrayList<Synapse>();
        ret.addAll(synapseList);
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i) instanceof SynapseGroup) {
                SynapseGroup group = (SynapseGroup) groupList.get(i);
                ret.addAll(group.getSynapseList());
            } else if (groupList.get(i) instanceof Subnetwork) {
                Subnetwork group = (Subnetwork) groupList.get(i);
                ret.addAll(group.getFlatSynapseList());
            }
        }
        return ret;
    }

    /**
     * Returns the precision of the current time step.
     *
     * @return the precision of the current time step.
     */
    private int getTimeStepPrecision() {
        int logVal = (int) Math.round((Math.log(this.getTimeStep()) / LOG_10));
        if (logVal < 0) {
            return Math.abs(logVal);
        } else {
            return 0;
        }
    }

    /**
     * @return Returns the timeStep.
     */
    public double getTimeStep() {
        return timeStep;
    }

    /**
     * Convenience method for asynchronously updating a set of neurons, by
     * calling each neuron's update function (which sets a buffer), and then
     * setting each neuron's activation to the buffer state.
     *
     * @param neuronList
     *            the list of neurons to be updated
     */
    public static void updateNeurons(List<Neuron> neuronList) {
        // TODO: Update by priority if priority based update?
        for (Neuron neuron : neuronList) {
            neuron.update();
        }
        for (Neuron neuron : neuronList) {
            neuron.setActivation(neuron.getBuffer());
        }
    }

    /**
     * Get the activations associated with a list of neurons.
     *
     * @param neuronList
     *            the neurons whose activations to get.
     * @return vector of activations
     */
    public static double[] getActivationVector(List<Neuron> neuronList) {
        double[] ret = new double[neuronList.size()];

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = neuronList.get(i);
            ret[i] = n.getActivation();
        }

        return ret;
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(Network.class, "logger");
        xstream.omitField(Network.class, "component");
        xstream.omitField(Network.class, "customRule");
        xstream.omitField(Network.class, "groupListeners");
        xstream.omitField(Network.class, "neuronListeners");
        xstream.omitField(Network.class, "networkListeners");
        xstream.omitField(Network.class, "subnetworkListeners");
        xstream.omitField(Network.class, "synapseListeners");
        xstream.omitField(Network.class, "textListeners");
        xstream.omitField(Network.class, "updateCompleted");
        xstream.omitField(Network.class, "networkThread");

        xstream.omitField(UpdateManager.class, "listeners");
        xstream.omitField(CustomUpdate.class, "interpreter");
        xstream.omitField(CustomUpdate.class, "theAction");

        xstream.omitField(Network.class, "logger");
        xstream.omitField(Neuron.class, "fanOut");
        xstream.omitField(Neuron.class, "fanIn");
        xstream.omitField(Neuron.class, "readOnlyFanOut");
        xstream.omitField(Neuron.class, "readOnlyFanIn");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {

        // Initialize listeners

        networkListeners = new ArrayList<NetworkListener>();
        neuronListeners = new ArrayList<NeuronListener>();
        synapseListeners = new ArrayList<SynapseListener>();
        textListeners = new ArrayList<TextListener>();
        groupListeners = new ArrayList<GroupListener>();

        // Initialize update manager
        updateManager.postUnmarshallingInit();

        // Initialize neurons
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.postUnmarshallingInit();
        }

        // Check for and remove corrupt synapses.
        // This should not happen but as of 1/24/11 I have not
        // determined why it happens, so the check is needed.
        for (Synapse synapse : this.getFlatSynapseList()) {
            if (synapse.getTarget().getFanIn() != null) {
                synapse.getTarget().getFanIn().add(synapse);
            } else {
                System.out.println("Warning:" + synapse.getId()
                        + " has null fanIn");
                removeSynapse(synapse);
            }
            if (synapse.getSource().getFanOut() != null) {
                synapse.getSource().getFanOut().add(synapse);
            } else {
                System.out.println("Warning:" + synapse.getId()
                        + " has null fanOut");
                removeSynapse(synapse);
            }
        }

        return this;
    }

    /**
     * @return Units by which to count.
     */
    public static String[] getUnits() {
        String[] units = { "msec", "iterations" };
        return units;
    }

    /**
     * Returns the current time.
     *
     * @return the current time
     */
    public double getTime() {
        return time;
    }

    /**
     * Set the current time.
     *
     * @param i
     *            the current time
     */
    public void setTime(final double i) {
        time = i;
    }

    /**
     * @return String string version of time, with units.
     */
    public String getTimeLabel() {
        if (timeType == TimeType.DISCRETE) {
            return "" + (int) time + " " + getUnits()[1];
        } else {
            return "" + round(time, getTimeStepPrecision()) + " "
                    + getUnits()[0];
        }
    }

    /**
     * @return The representation of time used by this network.
     */
    public TimeType getTimeType() {
        return timeType;
    }

    /**
     * If there is a single continuous neuron in the network, consider this a
     * continuous network.
     */
    public void updateTimeType() {
        timeType = TimeType.DISCRETE;

        for (Neuron n : getNeuronList()) {
            if (n.getTimeType() == TimeType.CONTINUOUS) {
                timeType = TimeType.CONTINUOUS;
            }
        }
        time = 0;
    }

    /**
     * Increment the time counter, using a different method depending on whether
     * this is a continuous or discrete. network.
     */
    public void updateTime() {
        if (timeType == TimeType.CONTINUOUS) {
            time += this.getTimeStep();
        } else {
            time += 1;
        }
    }

    /**
     * Comparator for sorting neurons by update priority.
     */
    protected class PriorityComparator implements Comparator<Neuron> {
        public int compare(Neuron neuron1, Neuron neuron2) {
            Integer priority1 = neuron1.getUpdatePriority();
            Integer priority2 = neuron2.getUpdatePriority();
            return priority1.compareTo(priority2);
        }
    }

    /**
     * @param timeStep
     *            The timeStep to set.
     */
    public void setTimeStep(final double timeStep) {
        this.timeStep = timeStep;
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted
     *            neuron which has been deleted
     */
    public void fireNeuronRemoved(final Neuron deleted) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronRemoved(new NetworkEvent<Neuron>(this, deleted));
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     */
    public void fireNetworkChanged() {
        for (NetworkListener listener : networkListeners) {
            listener.networkChanged();
        }
    }

    /**
     * Fire a neuron clamp toggle event to all registered model listeners.
     */
    public void fireNeuronClampToggle() {

        for (NetworkListener listener : networkListeners) {
            listener.neuronClampToggled();
        }
    }

    /**
     * Fire a neuron synapse toggle event to all registered model listeners.
     */
    public void fireSynapseClampToggle() {

        for (NetworkListener listener : networkListeners) {
            listener.synapseClampToggled();
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     *
     * @param moved
     *            Neuron that has been moved
     */
    public void fireNeuronMoved(final Neuron moved) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronMoved(new NetworkEvent<Neuron>(this, moved));
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added
     *            neuron which was added
     */
    public void fireNeuronAdded(final Neuron added) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronAdded(new NetworkEvent<Neuron>(this, added));
        }
    }

    /**
     * Fire a neuron type changed event to all registered model listeners.
     *
     * @param old
     *            the old update rule
     * @param changed
     *            the new update rule
     */
    public void fireNeuronTypeChanged(final NeuronUpdateRule old,
            final NeuronUpdateRule changed) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronTypeChanged(new NetworkEvent<NeuronUpdateRule>(this,
                    old, changed));
        }
    }

    /**
     * Fire a neuron changed event to all registered model listeners.
     *
     * @param changed
     *            new, changed neuron
     */
    public void fireNeuronChanged(final Neuron changed) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronChanged(new NetworkEvent<Neuron>(this, changed));
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added
     *            synapse which was added
     */
    public void fireSynapseAdded(final Synapse added) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseAdded(new NetworkEvent<Synapse>(this, added));
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted
     *            synapse which was deleted
     */
    public void fireSynapseRemoved(final Synapse deleted) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseRemoved(new NetworkEvent<Synapse>(this, deleted));
        }
    }

    /**
     * Fire a synapse changed event to all registered model listeners.
     *
     * @param changed
     *            new, changed synapse
     */
    public void fireSynapseChanged(final Synapse changed) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseChanged(new NetworkEvent<Synapse>(this, changed));
        }
    }

    /**
     * Fire a synapse type changed event to all registered model listeners.
     *
     * @param oldRule
     *            old synapse, before the change
     * @param learningRule
     *            new, changed synapse
     */
    public void fireSynapseTypeChanged(final SynapseUpdateRule oldRule,
            final SynapseUpdateRule learningRule) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseTypeChanged(new NetworkEvent<SynapseUpdateRule>(
                    this, oldRule, learningRule));
        }
    }

    /**
     * Fire a text added event to all registered model listeners.
     *
     * @param added
     *            text which was deleted
     */
    public void fireTextAdded(final NetworkTextObject added) {
        for (TextListener listener : textListeners) {
            listener.textAdded(added);
        }
    }

    /**
     * Fire a text deleted event to all registered model listeners.
     *
     * @param deleted
     *            text which was deleted
     */
    public void fireTextRemoved(final NetworkTextObject deleted) {
        for (TextListener listener : textListeners) {
            listener.textRemoved(deleted);
        }
    }

    /**
     * Fire a text changed event to all registered model listeners.
     *
     * TODO: Not currently used.
     *
     * @param changed
     *            text which was changed
     */
    public void fireTextChanged(final NetworkTextObject changed) {
        for (TextListener listener : textListeners) {
            listener.textRemoved(changed);
        }
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @return whether the network has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @param b
     *            whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
    }

    /**
     * Fire a group added event to all registered model listeners.
     *
     * @param added
     *            Group that has been added
     */
    public void fireGroupAdded(final Group added) {
        for (GroupListener listener : groupListeners) {
            listener.groupAdded(new NetworkEvent<Group>(this, added));
        }
    }

    /**
     * Fire a group deleted event to all registered model listeners.
     *
     * @param deleted
     *            Group to be deleted
     */
    public void fireGroupRemoved(final Group deleted) {
        for (GroupListener listener : groupListeners) {
            listener.groupRemoved(new NetworkEvent<Group>(this, deleted));
        }
    }

    /**
     * Fire a group changed event to all registered model listeners. A string
     * desription describes the change and is used by listeners to handle the
     * event. Old group is not currently used but may be in the future.
     *
     * @param old
     *            Old group
     * @param changed
     *            New changed group
     * @param changeDescription
     *            A description of the
     */
    public void fireGroupChanged(final Group old, final Group changed,
            final String changeDescription) {

        for (GroupListener listener : groupListeners) {
            listener.groupChanged(new NetworkEvent<Group>(this, old, changed),
                    changeDescription);
        }
    }

    /**
     * This version of fireGroupChanged fires a pre-set event, which may have an
     * auxiliary object set.
     *
     * @param event
     *            the network changed event.
     * @param changeDescription
     *            A description of the
     */
    public void fireGroupChanged(final NetworkEvent<Group> event,
            final String changeDescription) {

        for (GroupListener listener : groupListeners) {
            listener.groupChanged(event, changeDescription);
        }
    }

    /**
     * Fire a group parameters changed event.
     *
     * @param group
     *            reference to group whose parameters changed
     */
    public void fireGroupParametersChanged(final Group group) {
        for (GroupListener listener : groupListeners) {
            listener.groupParameterChanged(new NetworkEvent<Group>(this, group,
                    group));
        }
    }

    /**
     * @return Clamped weights.
     */
    public boolean getClampWeights() {
        return clampWeights;
    }

    /**
     * Sets weights to clamped values.
     *
     * @param clampWeights
     *            Weights to set
     */
    public void setClampWeights(final boolean clampWeights) {
        this.clampWeights = clampWeights;
        this.fireSynapseClampToggle();
    }

    /**
     * @return Clamped neurons.
     */
    public boolean getClampNeurons() {
        return clampNeurons;
    }

    /**
     * Sets neurons to clamped values.
     *
     * @param clampNeurons
     *            Neurons to set
     */
    public void setClampNeurons(final boolean clampNeurons) {
        this.clampNeurons = clampNeurons;
        this.fireNeuronClampToggle();
    }

    @Override
    public String toString() {

        String ret = "Root Network \n================= \n";

        for (Neuron n : this.getNeuronList()) {
            ret += (n + "\n");
        }

        if (this.getSynapseList().size() > 0) {
            for (int i = 0; i < getSynapseList().size(); i++) {
                Synapse tempRef = (Synapse) getSynapseList().get(i);
                ret += tempRef;
            }
        }

        for (int i = 0; i < getGroupList().size(); i++) {
            Group group = (Group) getGroupList().get(i);
            ret += group.toString();
        }

        for (NetworkTextObject text : textList) {
            ret += (text + "\n");
        }

        return ret;
    }

    /**
     * Return the generator for neuron ids.
     *
     * @return the generator
     */
    public SimpleId getNeuronIdGenerator() {
        return neuronIdGenerator;
    }

    /**
     * Return the generator for synapse ids.
     *
     * @return the generator.
     */
    public SimpleId getSynapseIdGenerator() {
        return synapseIdGenerator;
    }

    /**
     * Register a network listener.
     *
     * @param listener
     *            the observer to register
     */
    public void addNetworkListener(final NetworkListener listener) {
        networkListeners.add(listener);
    }

    /**
     * Register a neuron listener.
     *
     * @param listener
     *            the observer to register
     */
    public void addNeuronListener(final NeuronListener listener) {
        neuronListeners.add(listener);
    }

    /**
     * Register a synapse listener.
     *
     * @param listener
     *            the observer to register
     */
    public void addSynapseListener(final SynapseListener listener) {
        synapseListeners.add(listener);
    }

    /**
     * Register a text listener.
     *
     * @param listener
     *            the observer to register
     */
    public void addTextListener(final TextListener listener) {
        textListeners.add(listener);
    }

    /**
     * Remove a synapse listener.
     *
     * @param synapseListener
     *            the observer to remove
     */
    public void removeSynapseListener(SynapseListener synapseListener) {
        synapseListeners.remove(synapseListener);
    }

    /**
     * Register a group listener.
     *
     * @param listener
     *            the observer to register
     */
    public void addGroupListener(final GroupListener listener) {
        groupListeners.add(listener);
    }

    /**
     * Remove a group listener.
     *
     * @param listener
     *            the observer to remove
     */
    public void removeGroupListener(final GroupListener listener) {
        groupListeners.remove(listener);
    }

    /**
     * Search for a neuron by label. If there are more than one with the same
     * label only the first one found is returned.
     *
     * @param inputString
     *            label of neuron to search for
     * @return list of matched neurons, or null if none are found
     */
    public List<Neuron> getNeuronsByLabel(String inputString) {
        ArrayList<Neuron> foundNeurons = new ArrayList<Neuron>();
        for (Neuron neuron : this.getFlatNeuronList()) {
            if (neuron.getLabel().equalsIgnoreCase(inputString)) {
                foundNeurons.add(neuron);
            }
        }
        if (foundNeurons.size() == 0) {
            return null;
        } else {
            return foundNeurons;
        }
    }

    /**
     * Returns the first neuron in the array returned by getNeuronsByLabel.
     *
     * @param inputString
     *            label of neuron to search for
     * @return matched Neuron, if any
     */
    public Neuron getNeuronByLabel(String inputString) {
        List<Neuron> foundNeurons = getNeuronsByLabel(inputString);
        if (foundNeurons == null) {
            return null;
        } else {
            return foundNeurons.get(0);
        }
    }

    /**
     * @return the groupIdGenerator
     */
    public SimpleId getGroupIdGenerator() {
        return groupIdGenerator;
    }

    /**
     * Add a network text object.
     *
     * @param text
     *            text object to add.
     */
    public void addText(final NetworkTextObject text) {
        textList.add(text);
        this.fireTextAdded(text);
    }

    /**
     * Delete a network text object.
     *
     * @param text
     *            text object to add
     */
    public void deleteText(final NetworkTextObject text) {
        textList.remove(text);
        this.fireTextRemoved(text);
    }

    /**
     * @return the textList
     */
    public List<NetworkTextObject> getTextList() {
        if (textList == null) {
            textList = new ArrayList<NetworkTextObject>();
        }
        return textList;
    }

    /**
     * @return the updateManager
     */
    public UpdateManager getUpdateManager() {
        return updateManager;
    }

    /**
     * @return the synapseRouter
     */
    public SynapseRouter getSynapseRouter() {
        return synapseRouter;
    }

    /**
     * Adds a list of network elements to this network. Used in copy paste.
     *
     * @param toAdd
     *            list of objects to add.
     */
    public void addObjects(final List<?> toAdd) {
        for (Object object : toAdd) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                addNeuron(neuron);
            } else if (object instanceof Synapse) {
                Synapse synapse = (Synapse) object;
                addSynapse(synapse);
            } else if (object instanceof NetworkTextObject) {
                addText((NetworkTextObject) object);
            }
        }
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX
     *            x offset for translation.
     * @param offsetY
     *            y offset for translation.
     */
    public void translate(final double offsetX, final double offsetY) {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.setX(neuron.getX() + offsetX);
            neuron.setY(neuron.getY() + offsetY);
        }
    }
}
