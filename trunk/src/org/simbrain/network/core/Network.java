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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.simbrain.network.connections.ConnectNeurons;
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
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.subnetworks.CompetitiveGroup;
import org.simbrain.network.update_actions.CustomUpdate;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimbrainPreferences.PropertyNotFoundException;
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
    private final Set<Synapse> synapseList = new LinkedHashSet<Synapse>();

    /** Since groups span all levels of the hierarchy they are stored here. */
    private final List<Group> groupList = new ArrayList<Group>();

    /** Text objects. */
    private List<NetworkTextObject> textList =
        new ArrayList<NetworkTextObject>();

    /** The update manager for this network. */
    private NetworkUpdateManager updateManager;

    /** The initial time-step for the network. */
    private static final double DEFAULT_TIME_STEP = .1;

    /** Constant value for Math.log(10); used to approximate log 10. */
    private static final double LOG_10 = Math.log(10);

    /** In iterations or msec. */
    private double time = 0;

    /** Time step. */
    private double timeStep = DEFAULT_TIME_STEP;

    /**
     * Two types of time used in simulations.
     */
    public enum TimeType {
        /**
         * Network update iterations are time-steps.
         */
        DISCRETE,

        /**
         * Simulation of real time. Each updates advances time by length
         * {@link timeStep}.
         */
        CONTINUOUS;
    }

    /** Whether this is a discrete or continuous time network. */
    private TimeType timeType = TimeType.DISCRETE;

    /** List of objects registered to observe general network events. */
    private List<NetworkListener> networkListeners =
        new ArrayList<NetworkListener>();

    /** List of objects registered to observe neuron-related network events. */
    private List<NeuronListener> neuronListeners =
        new ArrayList<NeuronListener>();

    /** List of objects registered to observe synapse-related network events. */
    private List<SynapseListener> synapseListeners =
        new ArrayList<SynapseListener>();

    /** List of objects registered to observe group-related network events. */
    private List<GroupListener> groupListeners = new ArrayList<GroupListener>();

    /** List of objects registered to observe text-related network events. */
    private List<TextListener> textListeners = new ArrayList<TextListener>();

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

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

    /**
     * If a subnetwork or synapse group has more than this many synapses, then
     * the initial synapse visibility flag is set false.
     */
    private static int synapseVisibilityThreshold = 200;

    /** Static initializer */
    {
        try {
            synapseVisibilityThreshold = SimbrainPreferences
                .getInt("networkSynapseVisibilityThreshold");
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
        updateManager = new NetworkUpdateManager(this);
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
        for (NetworkUpdateAction action : updateManager.getActionList()) {
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

        for (Neuron neuron : prioritySortedNeuronList) {
            neuron.update();
            neuron.setActivation(neuron.getBuffer());
        }
    }

    /**
     * Calls {@link Neuron#update} for each neuron.
     */
    public void bufferedUpdateAllNeurons() {

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
     * Return the list of synapses. These are "loose" neurons. For the full set
     * of neurons, including neurons inside of subnetworks and groups, use
     * {@link #getFlatNeuronList()}.
     *
     * @return List of neurons in network.
     */
    public List<? extends Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    /**
     * Return the list of synapses. These are "loose" synapses. For the full set
     * of synapses, including synapses inside of subnetworks and groups, use
     * {@link #getFlatSynapseList()}.
     *
     * @return List of synapses in network.
     */
    public Collection<Synapse> getSynapseList() {
        return Collections.unmodifiableCollection(synapseList);
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
     * Gets the Euclidean distance between two neurons' positions in coordinate
     * space.
     *
     * @param n1
     * @param n2
     * @return
     */
    public static double getEuclideanDist(Neuron n1, Neuron n2) {
        double x2 = (n1.getX() - n2.getX());
        x2 *= x2;
        double y2 = (n1.getY() - n2.getY());
        y2 *= y2;
        double z2 = (n1.getZ() - n2.getZ());
        z2 *= z2;
        return Math.sqrt(x2 + y2 + z2);
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
        for (Group group : getFlatGroupList()) {
            if (group.getId().equalsIgnoreCase(id)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Find groups with a given label, or null if none found.
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
        if (returnList.isEmpty()) {
            return null;
        } else {
            return returnList;
        }
    }

    /**
     * Find group with a given label, or null if none found.
     *
     * @param label
     *            label to search for.
     * @return list of groups with that label found, null otherwise
     */
    public Group getGroupByLabel(final String label) {
        List<Group> returnList = getGroupsByLabel(label);
        if (returnList == null) {
            return null;
        } else {
            return returnList.get(0);
        }
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
     * Returns a list of all neuron groups.
     *
     * @return a neuron group list
     */
    public List<NeuronGroup> getFlatNeuronGroupList() {
        ArrayList<NeuronGroup> ngs = new ArrayList<NeuronGroup>();

        for (Group g : groupList) {
            if (g instanceof NeuronGroup) {
                ngs.add(((NeuronGroup) g));
            }
        }

        return ngs;
    }

    /**
     * Returns the synapse group between some source neuron group and some
     * target neuron group, if it exists. Returns null otherwise.
     *
     * @param src
     *            the source neuron group
     * @param targ
     *            the target neuron group
     * @return the synapse group between src and targ, null if there is none
     */
    public SynapseGroup getSynapseGroup(NeuronGroup src, NeuronGroup targ) {

        SynapseGroup s = null;

        for (Group g : groupList) {
            if (g instanceof SynapseGroup) {
                if (src == ((SynapseGroup) g).getSourceNeuronGroup()
                    && targ == ((SynapseGroup) g).getTargetNeuronGroup())
                    s = (SynapseGroup) g;
            }
        }

        return s;

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
    }

    /**
     * Calls {@link Synapse#update} for each weight.
     */
    public void updateAllSynapses() {

        // No Buffering necessary because the values of weights don't depend on
        // one another
        for (Synapse s : synapseList) {
            s.update();
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

        // Remove Connected Synapses
        toDelete.deleteConnectedSynapses();

        // Remove the neuron itself. Either from a parent group that holds it,
        // or from the root network.
        if (toDelete.getParentGroup() != null) {
            ((NeuronGroup) toDelete.getParentGroup())
                .removeNeuron(toDelete);
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
     */
    public void removeSynapse(final Synapse toDelete) {

        // Remove references to this synapse from parent neurons
        if (toDelete.getSource() != null) {
            toDelete.getSource().removeEfferent(toDelete);
        }
        if (toDelete.getTarget() != null) {
            toDelete.getTarget().removeAfferent(toDelete);
        }

        // If this synapse has a parent group, delete that group
        if (toDelete.getParentGroup() != null) {
            SynapseGroup parentGroup = toDelete.getParentGroup();
            parentGroup.removeSynapse(toDelete);
            if (parentGroup.isDisplaySynapses()) {
                fireSynapseRemoved(toDelete);
            }
            if (parentGroup.isEmpty()) {
                removeGroup(toDelete.getParentGroup());
            }
        } else {
            synapseList.remove(toDelete);
            // Notify listeners that this synapse has been deleted
            fireSynapseRemoved(toDelete);
        }
    }

    /**
     * Remove the given neurons from the neuron list (without firing an event)
     * and add them to the provided group.
     *
     * This is only needed in cases where the neurons have already been added,
     * and must be transferred in to a group. This is what happens in converting
     * loose neurons to a neuron group. It also happens to be the way the add
     * neurons dialog does things (it defaults to adding neurons, but if a user
     * wants they can be put in a group) This is not part of the standard neuron
     * group creation process.
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
     * Set the activation level of all neurons to zero.
     */
    public void clearActivations() {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.clear();
        }
    }

    /**
     * Set biases on all neurons with a bias to 0.
     */
    public void clearBiases() {
        for (Neuron neuron : this.getFlatNeuronList()) {
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                ((BiasedUpdateRule) neuron.getUpdateRule()).setBias(0);
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
        return src.getFanOut().get(tar);
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
        return getNeuron(i).getFanOut().get(getNeuron(j));
    }

    /**
     * Add a group to the network. This works for both singular groups like
     * neuron groups and synapse groups as well as for composite groups
     * like any subclass of Subnetwork. NetworkPanel adds the constituent groups
     * of composite groups and therefore it is unnecessary here.
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

        // TODO: Base this on an overridable method?
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
                ret.addAll(group.getAllSynapses());
            } else if (groupList.get(i) instanceof Subnetwork) {
                Subnetwork group = (Subnetwork) groupList.get(i);
                ret.addAll(group.getFlatSynapseList());
            }
        }
        return ret;
    }

    /**
     * Create a "flat" list of groups, which includes the top-level groups plus
     * all subgroups.
     *
     * @return the flat list
     */
    public List<Group> getFlatGroupList() {
        List<Group> ret = new ArrayList<Group>();
        ret.addAll(groupList);
        for (Group group : groupList) {
            if (group instanceof Subnetwork) {
                ret.addAll(((Subnetwork) group).getNeuronGroupList());
                ret.addAll(((Subnetwork) group).getSynapseGroupList());
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
        XStream xstream = new XStream(new DomDriver("UTF-8"));
        xstream.omitField(Network.class, "groupListeners");
        xstream.omitField(Network.class, "neuronListeners");
        xstream.omitField(Network.class, "networkListeners");
        xstream.omitField(Network.class, "synapseListeners");
        xstream.omitField(Network.class, "textListeners");
        xstream.omitField(Network.class, "updateCompleted");
        xstream.omitField(Network.class, "logger");
        xstream.omitField(Network.class, "synapseVisibilityThreshold");

        xstream.omitField(NetworkUpdateManager.class, "listeners");
        xstream.omitField(CustomUpdate.class, "interpreter");
        xstream.omitField(CustomUpdate.class, "theAction");

        xstream.omitField(Neuron.class, "fanOut");
        xstream.omitField(Neuron.class, "fanIn");

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

        // Initialize groups
        // for (Group group : this.getFlatGroupList()) {
        // group.postUnmarshallingInit();
        // }

        // Check for and remove corrupt synapses.
        // This should not happen but as of 1/24/11 I have not
        // determined why it happens, so the check is needed.
        for (Synapse synapse : this.getFlatSynapseList()) {
            if (synapse.getTarget().getFanIn() != null) {
                synapse.getTarget().addAfferent(synapse);
            } else {
                System.out.println("Warning:" + synapse.getId()
                    + " has null fanIn");
                removeSynapse(synapse);
            }
            if (synapse.getSource().getFanOut() != null) {
                synapse.getSource().addEfferent(synapse);
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
            return "" + BigDecimal.valueOf((long) time, getTimeStepPrecision())
                + " " + getUnits()[0];
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
     * TODO: Resolve priority update issue. Here as a hack to make the list
     * available to groups that want to update via priorities WITHIN the
     * group... To be resolved.
     *
     * @return the prioritySortedNeuronList
     */
    public List<Neuron> getPrioritySortedNeuronList() {
        return prioritySortedNeuronList;
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
     *            neuron
     */
    public void fireNeuronChanged(final Neuron changed) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronChanged(new NetworkEvent<Neuron>(this, changed));
        }
    }

    /**
     * Fire a label changed event to all registered model listeners.
     *
     * @param changed
     *            neuron
     */
    public void fireNeuronLabelChanged(final Neuron changed) {
        for (NeuronListener listener : neuronListeners) {
            listener.labelChanged(new NetworkEvent<Neuron>(this, changed));
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
     * Fire a synapse deleted event to all registered model listeners.
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

    @Override
    public String toString() {

        String ret = "Root Network \n================= \n";

        for (Neuron n : this.getNeuronList()) {
            ret += (n + "\n");
        }

        if (this.getSynapseList().size() > 0) {
            Iterator<Synapse> synapseIterator = synapseList.iterator();
            for (int i = 0; i < getSynapseList().size(); i++) {
                Synapse tempRef = synapseIterator.next();
                ret += tempRef;
            }
        }

        for (int i = 0; i < getGroupList().size(); i++) {
            Group group = getGroupList().get(i);
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
     * Remove a network listener.
     *
     * @param networkListener
     *            the observer to remove
     */
    public void removeNetworkListener(NetworkListener networkListener) {
        networkListeners.remove(networkListener);
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
     * Add an update action to the network' action list (the sequence of actions
     * invoked on each iteration of the network).
     *
     * @param action
     *            new action
     */
    public void addUpdateAction(NetworkUpdateAction action) {
        updateManager.addAction(action);
    }

    /**
     * @return the updateManager
     */
    public NetworkUpdateManager getUpdateManager() {
        return updateManager;
    }

    /**
     * Adds a list of network elements to this network. Used in copy / paste.
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
            } else if (object instanceof NeuronGroup) {
                addGroup((NeuronGroup) object);
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

    /**
     * Connect a source neuron group to a target neuron group using a connection
     * object.
     *
     * @param sng
     *            source neuron group
     * @param tng
     *            target neuron group
     * @param connection
     *            conection object
     */
    public void connectNeuronGroups(final NeuronGroup sng,
        final NeuronGroup tng, final ConnectNeurons connection) {

        final SynapseGroup group = SynapseGroup.createSynapseGroup(sng, tng,
                connection);
        addGroup(group);
    }

    /**
     * Freeze or unfreeze all synapses in the network.
     *
     * @param freeze
     *            frozen if true; unfrozen if false
     */
    public void freezeSynapses(final boolean freeze) {
        // Freeze synapses in synapse groups
        for (SynapseGroup group : getSynapseGroups()) {
            group.setFrozen(freeze);
        }
        // Freeze loose synapses
        for (Synapse synapse : this.getSynapseList()) {
            synapse.setFrozen(freeze);
        }
    }

    /**
     * Convenience method to return a list of synapse groups in the network.
     *
     * @return list of all synapse groups
     */
    public List<SynapseGroup> getSynapseGroups() {
        List<SynapseGroup> retList = new ArrayList<SynapseGroup>();
        for (Group group : this.getGroupList()) {
            if (group instanceof SynapseGroup) {
                retList.add((SynapseGroup) group);
            }
        }
        return retList;
    }

    /**
     * Convenience method to return a list of neuron groups in the network.
     *
     * @return list of all neuron groups
     */
    public List<NeuronGroup> getNeuronGroups() {
        List<NeuronGroup> retList = new ArrayList<NeuronGroup>();
        for (Group group : this.getGroupList()) {
            if (group instanceof NeuronGroup) {
                retList.add((NeuronGroup) group);
            }
        }
        return retList;
    }

    /**
     * @return the synapseVisibilityThreshold
     */
    public static int getSynapseVisibilityThreshold() {
        return synapseVisibilityThreshold;
    }

    /**
     * @param synapseVisibilityThreshold
     *            the synapseVisibilityThreshold to set
     */
    public static void setSynapseVisibilityThreshold(int svt) {
        Network.synapseVisibilityThreshold = svt;
    }

}
