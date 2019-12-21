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

import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.dl4j.ArrayConnectable;
import org.simbrain.network.dl4j.MultiLayerNet;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.groups.*;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimpleId;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * <b>Network</b> provides core neural network functionality and is the the
 * main
 * <p>
 * API for external calls. Network objects are sets of neurons and weights connecting them. Most update and learning
 * logic occurs in the neurons and weights themselves, as well as in special groups.
 */
public class Network {

    /**
     * The initial time-step for the network.
     */
    private static final double DEFAULT_TIME_STEP = SimbrainPreferences.getDouble("networkDefaultTimeStep");

    /**
     * Constant value for Math.log(10); used to approximate log 10.
     */
    private static final double LOG_10 = Math.log(10);

    /**
     * If a subnetwork or synapse group has more than this many synapses, then the initial synapse visibility flag is
     * set false.
     */
    private transient static int synapseVisibilityThreshold = SimbrainPreferences.getInt("networkSynapseVisibilityThreshold");

    /**
     * Two types of time used in simulations.
     */
    public enum TimeType {
        /**
         * Network update iterations are time-steps.
         */
        DISCRETE,

        /**
         * Simulation of real time. Each updates advances time by length.
         */
        CONTINUOUS;
    }

    /**
     * List of "loose neurons" (as opposed to neurons in neuron groups)
     */
    private final List<Neuron> looseNeurons = new ArrayList<Neuron>();

    /**
     * Array list of "loose synapses" (as opposed to synapses in synapse groups)
     */
    private final Set<Synapse> looseSynapses = new LinkedHashSet<Synapse>();

    //TODO: Set all below back to final when backwards compatibility issue fixed
    /**
     * Set of weight matrices.
     */
    private Set<WeightMatrix> weightMatrices = new HashSet<>();

    /**
     * Neuron Collections. Can contain overlapping neurons. A set is used
     * to prevent identical sets from being created.
     */
    private HashSet<NeuronCollection> neuronCollectionSet = new HashSet();

    //TODO
    private final List<MultiLayerNet> multiLayerNetworks = new ArrayList<>();
    private List<NeuronGroup> neuronGroups  = new ArrayList<>();
    private List<SynapseGroup> synapseGroups  = new ArrayList<>();
    private List<Subnetwork> subnetworks  = new ArrayList<>();

    /**
     * Text objects.
     */
    private List<NetworkTextObject> textList = new ArrayList<NetworkTextObject>();

    /**
     * Neuron Array objects (nd4j). Not yet implemented.
     */
    private final List<NeuronArray> naList = new ArrayList();

    /**
     * The update manager for this network.
     */
    private NetworkUpdateManager updateManager;

    /**
     * In iterations or msec.
     */
    private double time = 0;

    /**
     * Time step.
     */
    private double timeStep = DEFAULT_TIME_STEP;

    /**
     * Local thread flag for starting and stopping the network
     */
    private final AtomicBoolean isRunning = new AtomicBoolean();

    /**
     * Whether this is a discrete or continuous time network.
     */
    private TimeType timeType = TimeType.DISCRETE;

    /**
     * Whether network has been updated yet; used by thread.
     */
    private transient AtomicBoolean updateCompleted = new AtomicBoolean(false);

    /**
     * List of neurons sorted by their update priority. Used in priority based update.
     */
    private List<Neuron> prioritySortedNeuronList;

    /**
     * Comparator used for sorting the priority sorted neuron list.
     */
    private PriorityComparator priorityComparator = new PriorityComparator();

    /**
     * Neuron Id generator.
     */
    private SimpleId neuronIdGenerator = new SimpleId("Neuron", 1);

    /**
     * Synapse Id generator.
     */
    private SimpleId synapseIdGenerator = new SimpleId("Synapse", 1);

    /**
     * Group Id generator.
     */
    private SimpleId groupIdGenerator = new SimpleId("Group", 1);

    /**
     * Collection Id generator.
     */
    private SimpleId collectionIdGenerator = new SimpleId("Collection", 1);

    /**
     * Array Id generator.
     */
    private SimpleId arrayIdGenerator = new SimpleId("Array", 1);

    /**
     * Weight Matrix Id generator.
     */
    private SimpleId weightMatrixGenerator = new SimpleId("Weight Matrix", 1);

    /**
     * A variable telling the network not to fire events to any listeners during update.
     */
    private volatile boolean fireUpdates = true;

    /**
     * An internal id giving networks unique numbers within the same simbrain session.
     */
    private static int current_id = 0;

    /**
     * An optional name for the network that defaults to "Network[current_id]".
     */
    private String name;

    /**
     * A counter for the total number of iterations run by this network.
     */
    private int iterCount = 0;

    /**
     * How frequently this network should fire events.
     */
    private int updateFreq = 1;

    /**
     * A special flag for if the network is being run for a one-time single iteration.
     */
    private boolean oneOffRun = false;

    /**
     * Support for property change events.
     */
    private transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
        name = "Network" + current_id;
        current_id++;
        updateManager = new NetworkUpdateManager(this);
        prioritySortedNeuronList = new ArrayList<Neuron>();
    }

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    public void update() {

        // TODO: Why is this here and not at end?
        changeSupport.firePropertyChange("updateCompleted", null, false);

        // Main update
        updateManager.invokeAllUpdates();

        // TODO
        neuronGroups.forEach(NeuronGroup::update);
        subnetworks.forEach(Subnetwork::update);
        naList.forEach(NeuronArray::update);
        neuronCollectionSet.forEach(NeuronCollection::update);
        synapseGroups.forEach(SynapseGroup::update);

        //clearInputs();
        updateTime();
        changeSupport.firePropertyChange("updateTimeDisplay", null, false);
        iterCount++;
        setUpdateCompleted(true);

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
     * This function is used to update the neuron and sub-network activation values if the user chooses to set different
     * priority values for a subset of neurons and sub-networks. The priority value determines the order in which the
     * neurons and sub-networks get updated - smaller priority value elements will be updated before larger priority
     * value elements.
     */
    public void updateNeuronsByPriority() {
        for (Neuron neuron : prioritySortedNeuronList) {
            neuron.update();
            neuron.setToBufferVals();
        }
    }

    /**
     * Calls {@link Neuron#update} for each neuron.
     */
    public void bufferedUpdateAllNeurons() {

        // First update the activation buffers
        looseNeurons.forEach(Neuron::update);

        // Then update the activations themselves
        looseNeurons.forEach(Neuron::setToBufferVals);

    }

    /**
     * Clears out input values of network nodes, which otherwise linger and cause problems.
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
     * @return Number of neurons in network.
     */
    public int getNeuronCount() {
        return looseNeurons.size();
    }

    /**
     * @return Number of weights in network
     */
    public int getSynapseCount() {
        return looseSynapses.size();
    }

    /**
     * Return the neuron at the specified index of the internal list storing neurons.
     *
     * @param neuronIndex index of the neuron
     * @return the neuron at that index
     */
    public Neuron getLooseNeuron(int neuronIndex) {
        return looseNeurons.get(neuronIndex);
    }

    /**
     * Find a neuron with a given string id.
     *
     * @param id id to search for.
     * @return neuron with that id, null otherwise
     */
    public Neuron getLooseNeuron(String id) {
        for (Neuron n : getFlatNeuronList()) {
            if (n.getId().equalsIgnoreCase(id)) {
                return n;
            }
        }
        return null;
    }


    ///**
    // * Find groups with a given label, or null if none found.
    // *
    // * @param label label to search for.
    // * @return list of groups with that label.
    // */
    //public List<Group> getGroupsByLabel(String label) {
    //    List<Group> returnList = new ArrayList<>();
    //    for (Group group : getFlatGroupList()) {
    //        if (group.getLabel().equalsIgnoreCase(label)) {
    //            returnList.add(group);
    //        }
    //    }
    //    return returnList;
    //}

    //todo
    ///**
    // * Find group with a given label, or null if none found.
    // *
    // * @param label label to search for.
    // * @return list of groups with that label found, null otherwise
    // */
    //public Group getGroupByLabel(String label) {
    //    List<Group> returnList = getGroupsByLabel(label);
    //    if (returnList.isEmpty()) {
    //        return null;
    //    } else {
    //        return returnList.get(0);
    //    }
    //}

    /**
     * Returns a list of all neuron groups.
     *
     * @return a neuron group list
     */
    public List<NeuronGroup> getFlatNeuronGroupList() {
        ArrayList<NeuronGroup> ret = new ArrayList<>();
        ret.addAll(neuronGroups);
        subnetworks.forEach(net -> ret.addAll(net.getNeuronGroupList()));
        return neuronGroups;
    }

    /**
     * Find a synapse with a given string id.
     *
     * @param id id to search for.
     * @return synapse with that id, null otherwise
     */
    public Synapse getLooseSynapse(String id) {
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
     * @param neuron Type of neuron to add
     */
    public void addLooseNeuron(Neuron neuron) {
        looseNeurons.add(neuron);
        updatePriorityList();
        fireNeuronAdded(neuron);
    }

    /**
     * Add an ND4J array object.
     */
    public void addNeuronArray(NeuronArray na) {
        naList.add(na);
        changeSupport.firePropertyChange("neuronArrayAdded", null, na);
    }

    public void addDL4JMultiLayerNetwork(MultiLayerNet network) {
        multiLayerNetworks.add(network);
        changeSupport.firePropertyChange("multiLayerNetworkAdded", null, network);
    }

    /**
     * Adds a weight to the neuron network, where that weight already has designated source and target neurons.
     *
     * @param synapse the weight object to add
     */
    public void addLooseSynapse(Synapse synapse) {
        synapse.initSpikeResponder();
        looseSynapses.add(synapse);
        synapse.setId(getSynapseIdGenerator().getId());
        fireSynapseAdded(synapse);
    }

    /**
     * Calls {@link Synapse#update} for each weight.
     */
    public void updateLooseSynapses() {
        // No Buffering necessary because the values of weights don't depend on one another
        for (Synapse s : looseSynapses) {
            s.update();
        }
    }

    public void updateNeuronArrayConnections() {
        weightMatrices.forEach(WeightMatrix::update);
    }

    /**
     * Remove a neuron.
     *
     * @param toDelete  the neuron to remove
     * @param fireEvent whether to fire an event
     */
    public void removeNeuron(final Neuron toDelete, boolean fireEvent) {

        // Update priority list
        updatePriorityList();

        // Remove Connected Synapses
        toDelete.deleteConnectedSynapses();

        // Remove the neuron itself. Either from a parent group that holds it,
        // or from the root network.
        if (toDelete.getParentGroup() != null) {
            toDelete.getParentGroup().removeNeuron(toDelete);
            if (toDelete.getParentGroup().isEmpty()) {
                removeNeuronGroup(toDelete.getParentGroup());
            }
        } else {
            looseNeurons.remove(toDelete);
        }

        // Notify listeners that this neuron has been deleted
        if (fireEvent) {
            changeSupport.firePropertyChange("neuronRemoved", toDelete, null);
        }
    }

    public void removeNeuronGroup(NeuronGroup ng) {
        neuronGroups.remove(ng);
        ng.delete();
        changeSupport.firePropertyChange("ngRemoved", ng, null);
    }

    public void removeSynapseGroup(SynapseGroup sg) {
        neuronGroups.remove(sg);
        sg.delete();
        changeSupport.firePropertyChange("sgRemoved", sg, null);
    }

    public void removeSubnetwork(Subnetwork subnet) {
        subnetworks.remove(subnet);
        subnet.delete();
        changeSupport.firePropertyChange("subnetRemoved", subnet, null);
    }


    /**
     * Remove a neuron collection.
     *
     * @param nc the collection to remove
     */
    public void removeNeuronCollection(NeuronCollection nc) {
        neuronCollectionSet.remove(nc);
        nc.delete();
        changeSupport.firePropertyChange("ncRemoved", nc, null);
    }

    /**
     * Remove a neuron array.
     */
    public void removeNeuronArray(NeuronArray na) {
        naList.remove(na);
        changeSupport.firePropertyChange("naRemoved", na, null);
    }

    /**
     * Remove a weight matrix
     */
    public void removeWeightMatrix(WeightMatrix wm) {
        if (wm == null) {
            return;
        }
        weightMatrices.remove(wm);
        changeSupport.firePropertyChange("wmRemoved", wm, null);
    }

    /**
     * Deletes a neuron from the network.
     *
     * @param toDelete neuron to delete
     */
    public void removeNeuron(final Neuron toDelete) {
        removeNeuron(toDelete, true);
    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete the weight to delete
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
            // TODO
            //if (parentGroup.isEmpty()) {
            //    removeGroup(toDelete.getParentGroup());
            //}
        } else {
            looseSynapses.remove(toDelete);
            // Notify listeners that this synapse has been deleted
            fireSynapseRemoved(toDelete);
        }
    }

    /**
     * Create a {@link NeuronCollection) from a provided list of neurons
     *
     * @param neuronList list of neurons to add to a neuron collection.
     */
    public void createNeuronCollection(List<Neuron> neuronList) {

        // Filter out loose neurons (a neuron is loose if its parent group is null)
        List<Neuron> loose = neuronList.stream()
                //.filter(n -> n.getParentGroup() == null)  // TODO
                .collect(Collectors.toList());

        // Only make the neuron collection if some neurons have been selected
        if (!loose.isEmpty()) {

            // Creating a neuron collection increments the id counter so don't
            // even create it if it's a duplicate
            int hashCode = loose.stream().mapToInt(n -> n.hashCode()).sum();
            for (NeuronCollection nc : neuronCollectionSet) {
                if (hashCode == nc.getSummedNeuronHash()) {
                    return;
                }
            }

            // Make the collection
            NeuronCollection nc = new NeuronCollection(this, loose);
            addNeuronCollection(nc);
        }
    }

    /**
     * Add a neuron collection to the network
     * @param nc the neuron collection to add
     */
    public void addNeuronCollection(NeuronCollection nc) {
        // Don't add duplicates
        int hashCode = nc.getSummedNeuronHash();
        for (NeuronCollection other : neuronCollectionSet) {
            if (hashCode == other.getSummedNeuronHash()) {
                return;
            }
        }
        neuronCollectionSet.add(nc);
        changeSupport.firePropertyChange("ncAdded", null, nc);
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
        changeSupport.firePropertyChange("neuronsUpdated", null, this.getFlatNeuronList());
    }

    /**
     * Sets all neurons to a specified value.
     *
     * @param value value to set
     */
    public void setActivations(final double value) {
        for (Neuron neuron : this.getFlatNeuronList()) {
            //TODO
            neuron.setActivation(value);
        }
    }

    /**
     * Sets neuron activations using values in an array of doubles. Currently these activations are applied to the
     * network in whatever order the neurons were added.
     *
     * @param activationArray array of values to apply to network
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
     * @param value value to set
     */
    public void setWeights(final double value) {
        for (Synapse synapse : this.getFlatSynapseList()) {
            synapse.setStrength(value);
        }
    }

    /**
     * Returns the "state" of the network: the activation level of its neurons. An activation vector.
     *
     * @return an array representing the activation levels of all the neurons in this network
     */
    public double[] getState() {
        double[] ret = new double[this.getNeuronCount()];

        for (int i = 0; i < this.getNeuronCount(); i++) {
            Neuron n = getLooseNeuron(i);
            ret[i] = (int) n.getActivation();
        }

        return ret;
    }

    /**
     * Sets all weight values to zero, effectively eliminating them.
     */
    public void setWeightsToZero() {
        for (Synapse s : looseSynapses) {
            s.setStrength(0);
        }
    }

    /**
     * Randomizes all loose neurons.
     */
    public void randomizeLooseNeurons() {
        for (Neuron n : looseNeurons) {
            n.randomize();
        }
    }

    /**
     * Randomize loose neurons. Keeping this for legacy code.
     */
    public void randomizeNeurons() {
        randomizeLooseNeurons();
    }

    /**
     * Randomizes all loose weights.
     */
    public void randomizeLooseWeights() {
        for (Synapse s : looseSynapses) {
            s.randomize();
        }
    }

    /**
     * Randomize loose weights. Keeping this for legacy code.
     */
    public void randomizeWeights() {
        randomizeLooseWeights();
    }

    /**
     * Randomize all biased loose neurons.
     *
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBiasesLooseNeurons(double lower, double upper) {
        for (Neuron neuron : looseNeurons) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Returns a reference to the synapse connecting two neurons, or null if there is none.
     *
     * @param src source neuron
     * @param tar target neuron
     * @return synapse from source to target
     */
    public static Synapse getLooseSynapse(final Neuron src, final Neuron tar) {
        return src.getFanOut().get(tar);
    }

    /**
     * Gets the synapse at particular point.
     *
     * @param i Neuron number
     * @param j Weight to get
     * @return Weight at the points defined
     */
    // TODO: Either fix this or make its assumptions explicit
    public Synapse getLooseWeight(final int i, final int j) {
        return getLooseNeuron(i).getFanOut().get(getLooseNeuron(j));
    }

    public void addSynapseGroup(final SynapseGroup sg) {
        synapseGroups.add(sg);
        changeSupport.firePropertyChange("sgAdded", null, sg);
    }

    public void addNeuronGroup(final NeuronGroup ng) {
        neuronGroups.add(ng);
        changeSupport.firePropertyChange("ngAdded", null, ng);
    }

    public void addSubnetwork(Subnetwork net) {
        subnetworks.add(net);
        changeSupport.firePropertyChange("subnetAdded", null, net);
    }

    ///**
    // * Remove the specified group.
    // *
    // * @param toDelete the group to delete.
    // */
    //public void removeGroup(final Group toDelete) {
    //
    //    // Remove from the group list
    //    groupList.remove(toDelete);
    //
    //    // Call delete method on this group being deleted
    //    toDelete.delete();
    //
    //    // Notify listeners that this group has been deleted.
    //    fireGroupRemoved(toDelete);
    //}

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
     * Create "flat" list of neurons, which includes the top-level neurons plus all group neurons.
     *
     * @return the flat list
     */
    public List<Neuron> getFlatNeuronList() {
        List<Neuron> ret = new ArrayList<Neuron>();
        ret.addAll(looseNeurons);
        neuronGroups.forEach(ng -> ret.addAll(ng.getNeuronList()));
        neuronCollectionSet.forEach(nc -> ret.addAll(nc.getNeuronList()));
        subnetworks.forEach(s -> s.getNeuronGroupList().forEach(
                        ng -> ret.addAll(ng.getNeuronList())));
        return ret;
    }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses.
     *
     * @return the flat list
     */
    public List<Synapse> getFlatSynapseList() {
        List<Synapse> ret = new ArrayList<Synapse>(10000);
        ret.addAll(looseSynapses);
        synapseGroups.forEach(sg -> ret.addAll(sg.getAllSynapses()));
        subnetworks.forEach(s -> s.getSynapseGroupList().forEach(
                sg -> ret.addAll(sg.getAllSynapses())));
        return ret;
    }

    /**
     * Returns the precision of the current time step.
     *
     * @return the precision of the current time step.
     */
    private int getTimeStepPrecision() {
        int logVal = (int) Math.ceil((Math.log(this.getTimeStep()) / LOG_10));
        if (logVal < 0) {
            return Math.abs(logVal) + 1;
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
     * Convenience method for asynchronously updating a set of neurons, by calling each neuron's update function (which
     * sets a buffer), and then setting each neuron's activation to the buffer state.
     *
     * @param neuronList the list of neurons to be updated
     */
    public static void updateNeurons(List<Neuron> neuronList) {
        // TODO: Update by priority if priority based update?
        for (Neuron neuron : neuronList) {
            neuron.update();
        }
        for (Neuron neuron : neuronList) {
            neuron.setToBufferVals();
        }

    }

    /**
     * Get the activations associated with a list of neurons.
     *
     * @param neuronList the neurons whose activations to get.
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
     * Returns a copy of this network based on its xml rep.
     *
     * @return the copied network.
     */
    public Network copy() {
        preSaveInit();
        String xml_rep = Utils.getSimbrainXStream().toXML(this);
        postSaveReInit();
        return (Network) Utils.getSimbrainXStream().fromXML(xml_rep);
    }

    /**
     * Standard method call made to objects after they are deserialized. See: http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {

        fireUpdates = true;

        // TODO: Temp code to handle xstream backwards compatibility issues
        if(weightMatrices == null) {
            weightMatrices = new HashSet<>();
        }
        if (neuronCollectionSet == null) {
            neuronCollectionSet = new HashSet<>();
        }
        if (synapseGroups == null) {
            synapseGroups = new ArrayList<>();
        }
        if (neuronGroups == null) {
            neuronGroups = new ArrayList<>();
        }
        if (subnetworks == null) {
            subnetworks = new ArrayList<>();
        }

        changeSupport = new PropertyChangeSupport(this);

        // Initialize update manager
        updateManager.postUnmarshallingInit();

        getFlatNeuronList().forEach(Neuron::postUnmarshallingInit);
        textList.forEach(NetworkTextObject::postUnmarshallingInit);
        synapseGroups.forEach(SynapseGroup::postUnmarshallingInit);
        neuronGroups.forEach(AbstractNeuronCollection::postUnmarshallingInit);
        neuronCollectionSet.forEach(AbstractNeuronCollection::postUnmarshallingInit);
        subnetworks.forEach(Subnetwork::postUnmarshallingInit);

        // Re-populate fan-in / fan-out for loose synapses
        getLooseSynapses().forEach(Synapse::postUnmarshallingInit);

        updateCompleted = new AtomicBoolean(false);
        return this;
    }

    /**
     * Perform operations required before saving a network. Post-opening operations occur in {@link #readResolve()}.
     */
    public void preSaveInit() {
        for (SynapseGroup group : this.getSynapseGroups()) {
            group.preSaveInit();
        }
    }

    /**
     * Returns synapse groups to a usable state after a save is performed.
     */
    public void postSaveReInit() {
        for (SynapseGroup group : this.getSynapseGroups()) {
            group.postSaveReInit();
        }
    }

    /**
     * @return Units by which to count.
     */
    public static String[] getUnits() {
        String[] units = {"msec", "iterations"};
        return units;
    }

    /**
     * Returns the current time in ms based on the network {@link #timeStep}.
     *
     * @return the current time
     */
    public double getTime() {
        return time;
    }

    /**
     * Returns the current number of iterations.
     *
     * @return the number of update iterations which have been run since the network was created.
     */
    public long getIterations() {
        return (long) (time / timeStep);
    }

    /**
     * Set the current time.
     *
     * @param i the current time
     */
    public void setTime(final double i) {
        if (i < time) {
            for (Neuron n : this.getFlatNeuronList()) {
                NeuronUpdateRule nur = n.getUpdateRule();
                if (nur.isSpikingNeuron()) {
                    SpikingNeuronUpdateRule snur = (SpikingNeuronUpdateRule) nur;
                    double diff = i - (time - snur.getLastSpikeTime());
                    snur.setLastSpikeTime(diff < 0 ? 0 : diff);
                }
            }
        }
        time = i;
    }

    /**
     * @return String string version of time, with units.
     */
    public String getTimeLabel() {
        if (timeType == TimeType.DISCRETE) {
            return "" + (int) getIterations() + " " + getUnits()[1];
        } else {
            return SimbrainMath.roundDouble(getTime(), getTimeStepPrecision() + 1) + " " + getUnits()[0];
        }
    }

    /**
     * @return The representation of time used by this network.
     */
    public TimeType getTimeType() {
        return timeType;
    }

    /**
     * If there is a single continuous neuron in the network, consider this a continuous network.
     */
    public void updateTimeType() {
        timeType = TimeType.DISCRETE;
        for (Neuron n : getFlatNeuronList()) {
            if (n.getTimeType() == TimeType.CONTINUOUS) {
                timeType = TimeType.CONTINUOUS;
            }
        }
    }

    /**
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete.
     * network.
     */
    public void updateTime() {
        setTime(time + timeStep);
    }

    /**
     * TODO: Resolve priority update issue. Here as a hack to make the list available to groups that want to update via
     * priorities WITHIN the group... To be resolved.
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
     * @param timeStep The timeStep to set.
     */
    public void setTimeStep(final double timeStep) {
        double oldTimeStep = this.timeStep;
        this.timeStep = timeStep;
        for (Synapse s : getFlatSynapseList()) {
            int newDelay = (int) (s.getDelay() * oldTimeStep / timeStep);
            if (s.getDelay() != 0 && newDelay == 0) {
                s.setDelay(1);
            } else {
                s.setDelay(newDelay);
            }
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added neuron which was added
     */
    public void fireNeuronAdded(final Neuron added) {
        changeSupport.firePropertyChange("neuronAdded", null, added);
    }

    /**
     * Fire this event when the visible state of a neuron (e.g. its activation) has been changed and this should be
     * reflected in the GUI.
     */
    public void fireNeuronsUpdated() {
        changeSupport.firePropertyChange("neuronsUpdated", null, getLooseNeurons());
    }

    /**
     * Fire this event when the visible state of a specified list of neuron (e.g. their activations) has been changed
     * and this should be reflected in the GUI.
     *
     * @param neurons the neurons whose state has changed
     */
    public void fireNeuronsUpdated(Collection<Neuron> neurons) {
        changeSupport.firePropertyChange("neuronsUpdated", null, neurons);
    }

    /**
     * Fire a synapse added event to all registered model listeners.
     *
     * @param added synapse which was added
     */
    public void fireSynapseAdded(final Synapse added) {
        changeSupport.firePropertyChange("synapseAdded", null, added);
    }

    /**
     * Fire a synapse deleted event to all registered model listeners.
     *
     * @param deleted synapse which was deleted
     */
    public void fireSynapseRemoved(final Synapse deleted) {
        changeSupport.firePropertyChange("synapseRemoved", deleted, null);
    }

    /**
     * Fire this event when the visible state of a synapse (e.g. its strength) has been changed and this should be
     * reflected in the GUI.
     */
    public void fireSynapsesUpdated() {
        changeSupport.firePropertyChange("synapsesUpdated", null, getLooseSynapses());
    }

    /**
     * Fire this event when the visible state of a specified list of synapses (e.g. their strengths) has been changed
     * and this should be reflected in the GUI.
     *
     * @param synapses the synapses whose state has changed
     */
    public void fireSynapsesUpdated(Collection<Synapse> synapses) {
        changeSupport.firePropertyChange("synapsesUpdated", null, synapses);
    }

    /**
     * Fire a text added event to all registered model listeners.
     *
     * @param added text which was deleted
     */
    public void fireTextAdded(final NetworkTextObject added) {
        changeSupport.firePropertyChange("textAdded", null, added);
    }

    /**
     * Fire a text deleted event to all registered model listeners.
     *
     * @param deleted text which was deleted
     */
    public void fireTextRemoved(final NetworkTextObject deleted) {
        changeSupport.firePropertyChange("textRemoved", deleted, null);
    }

    ///**
    // * Fire a group added event to all registered model listeners.
    // *
    // * @param added Group that has been added
    // */
    //public void fireGroupAdded(final Group added) {
    //    changeSupport.firePropertyChange("groupAdded", null, added);
    //}
    //
    ///**
    // * Fire a group deleted event to all registered model listeners.
    // *
    // * @param deleted Group to be deleted
    // */
    //public void fireGroupRemoved(final Group deleted) {
    //    changeSupport.firePropertyChange("groupRemoved", deleted, null);
    //}

    /**
     * Used by Network thread to ensure that an update cycle is complete before updating again.
     *
     * @return whether the network has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted.get();
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before updating again.
     *
     * @param b whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted.set(b);
    }

    @Override
    public String toString() {

        final StringBuilder ret = new StringBuilder("Root Network \n================= \n");
        looseNeurons.forEach(ret::append);
        looseSynapses.forEach(ret::append);
        neuronGroups.forEach(ret::append);
        synapseGroups.forEach(ret::append);
        subnetworks.forEach(ret::append);
        naList.forEach(ret::append);
        weightMatrices.forEach(ret::append);
        textList.forEach(ret::append);
        return ret.toString();
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
     * Search for a neuron by label. If there are more than one with the same label only the first one found is
     * returned.
     *
     * @param inputString label of neuron to search for
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
     * @param inputString label of neuron to search for
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

    public SimpleId getGroupIdGenerator() {
        return groupIdGenerator;
    }

    public SimpleId getCollectionIdGenerator() {
        return collectionIdGenerator;
    }

    public SimpleId getArrayIdGenerator() {
        return arrayIdGenerator;
    }

    public SimpleId getWeightMatrixGenerator() {
        return weightMatrixGenerator;
    }

    /**
     * Add a network text object.
     *
     * @param text text object to add.
     */
    public void addText(final NetworkTextObject text) {
        textList.add(text);
        this.fireTextAdded(text);
    }

    /**
     * Delete a network text object.
     *
     * @param text text object to add
     */
    public void deleteText(final NetworkTextObject text) {
        textList.remove(text);
        this.fireTextRemoved(text);
    }

    /**
     * Returns the list of text objects
     */
    public List<NetworkTextObject> getTextList() {
        if (textList == null) {
            textList = new ArrayList<NetworkTextObject>();
        }
        return textList;
    }

    /**
     * Add an update action to the network' action list (the sequence of actions invoked on each iteration of the
     * network).
     *
     * @param action new action
     */
    public void addUpdateAction(NetworkUpdateAction action) {
        updateManager.addAction(action);
    }

    public NetworkUpdateManager getUpdateManager() {
        return updateManager;
    }

    /**
     * Adds a list of network elements to this network. Used in copy / paste.
     *
     * @param toAdd list of objects to add.
     */
    public void addObjects(final List<?> toAdd) {
        for (Object object : toAdd) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                addLooseNeuron(neuron);
            } else if (object instanceof Synapse) {
                Synapse synapse = (Synapse) object;
                addLooseSynapse(synapse);
            } else if (object instanceof NetworkTextObject) {
                addText((NetworkTextObject) object);
            } else if (object instanceof NeuronGroup) {
                addNeuronGroup((NeuronGroup) object);
            } else if (object instanceof NeuronArray) {
                addNeuronArray((NeuronArray) object);
            }
        }
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void translate(final double offsetX, final double offsetY) {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.setX(neuron.getX() + offsetX);
            neuron.setY(neuron.getY() + offsetY);
        }
    }

    /**
     * Connect a source neuron group to a target neuron group using a connection object.
     *
     * @param sng        source neuron group
     * @param tng        target neuron group
     * @param connection connection object
     */
    public void connectNeuronGroups(final NeuronGroup sng, final NeuronGroup tng, final ConnectionStrategy connection) {
        final SynapseGroup group = SynapseGroup.createSynapseGroup(sng, tng, connection);
        addSynapseGroup(group);
    }

    /**
     * Add a weight matrix between two {@link ArrayConnectable}'s.
     * Can "adapt" a neuron collection to an ND4J array, or be a weight
     * matrix between those arrays.
     *
     * @param source source neuron collection or nd4j array
     * @param target target neuron collection or nd4j array
     */
    public WeightMatrix addWeightMatrix(ArrayConnectable source, ArrayConnectable target) {
        if (target.getIncomingWeightMatrix() != null) {
            removeWeightMatrix(target.getIncomingWeightMatrix());
        }

        // Don't allow weight matrices between neuron collections and neuron groups
        if (((source instanceof NeuronCollection || source instanceof NeuronGroup)
                && (target instanceof NeuronCollection || target instanceof NeuronGroup))) {
            return null;
        }
        WeightMatrix newMatrix = new WeightMatrix(this, source, target);
        newMatrix.initializeId();
        weightMatrices.add(newMatrix);
        //changeSupport.firePropertyChange("weightMatrixAdded", null, newMatrix);
        return newMatrix;
    }

    /**
     * Freeze or unfreeze all synapses in the network.
     *
     * @param freeze frozen if true; unfrozen if false
     */
    public void freezeSynapses(final boolean freeze) {
        // Freeze synapses in synapse groups
        for (SynapseGroup group : getSynapseGroups()) {
            group.setFrozen(freeze, Polarity.BOTH);
        }
        // Freeze loose synapses
        for (Synapse synapse : this.getLooseSynapses()) {
            synapse.setFrozen(freeze);
        }
    }

    public static int getSynapseVisibilityThreshold() {
        return synapseVisibilityThreshold;
    }

    public static void setSynapseVisibilityThreshold(int svt) {
        Network.synapseVisibilityThreshold = svt;
    }

    public boolean isFireUpdates() {
        return fireUpdates;
    }

    public void setFireUpdates(boolean fireUpdates) {
        this.fireUpdates = fireUpdates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return whether the network is currently running.
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Set whether the network is currently running.
     */
    public void setRunning(boolean value) {
        isRunning.set(value);
    }

    public int getUpdateFreq() {
        return updateFreq;
    }

    public void setUpdateFreq(int updateFreq) {
        this.updateFreq = updateFreq;
    }

    public boolean isRedrawTime() {
        return oneOffRun || iterCount % updateFreq == 0;
    }

    public void setOneOffRun(boolean _oneOffRun) {
        this.oneOffRun = _oneOffRun;
    }

    public boolean isOneOffRun() {
        return oneOffRun;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public List<MultiLayerNet> getMultiLayerNetworks() {
        return  Collections.unmodifiableList(multiLayerNetworks);
    }

    public List<Subnetwork> getSubnetworks() {
        return Collections.unmodifiableList(subnetworks);
    }

    public List<SynapseGroup> getSynapseGroups() {
        return Collections.unmodifiableList(synapseGroups);
    }

    public List<NeuronGroup> getNeuronGroups() {
        return Collections.unmodifiableList(neuronGroups);
    }

    public List<NeuronArray> getNaList() {
        return Collections.unmodifiableList(naList);
    }

    public Set<WeightMatrix> getWeightMatrices() {
        return Collections.unmodifiableSet(weightMatrices);
    }

    /**
     * Return the list of synapses. These are "loose" neurons. For the full set of neurons, including neurons inside of
     * subnetworks and groups, use {@link #getFlatNeuronList()}.
     */
    public List<? extends Neuron> getLooseNeurons() {
        return Collections.unmodifiableList(looseNeurons);
    }

    /**
     * Return the list of synapses. These are "loose" synapses. For the full set of synapses, including synapses inside
     * of subnetworks and groups, use {@link #getFlatSynapseList()}.
     */
    public Collection<Synapse> getLooseSynapses() {
        return Collections.unmodifiableCollection(looseSynapses);
    }

    public HashSet<NeuronCollection> getNeuronCollectionSet() {
        return neuronCollectionSet;
    }

}
