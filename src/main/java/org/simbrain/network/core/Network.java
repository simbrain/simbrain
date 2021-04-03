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

import org.simbrain.network.NetworkModel;
import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.events.NetworkEvents;
import org.simbrain.network.groups.*;
import org.simbrain.network.matrix.ArrayConnectable;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.smile.SmileSVM;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimpleIdManager;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * <b>Network</b> provides core neural network functionality and is the main API for external calls. Network objects
 * are sets of neurons and weights connecting them. Most update and learning logic occurs in the neurons and weights
 * themselves, as well as in special groups.
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
     * Handle network events.
     */
    private transient NetworkEvents events = new NetworkEvents(this);

    /**
     * Main data structure containing all {@link NetworkModel}s: neurons, synapses, etc.
     */
    private final NetworkModelList networkModels = new NetworkModelList();

    /**
     * The update manager for this network.
     */
    private final NetworkUpdateManager updateManager;

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
    private final PriorityComparator priorityComparator = new PriorityComparator();

    /**
     * Manage ids for all network elements.
     */
    private final SimpleIdManager idManager = new SimpleIdManager((clazz) -> networkModels.unsafeGet(clazz).size() + 1);

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
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
        name = "Network" + current_id;
        current_id++;
        updateManager = new NetworkUpdateManager(this);
        prioritySortedNeuronList = new ArrayList<>();
    }

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    public void update() {

        // Main update
        updateManager.invokeAllUpdates();

        //clearInputs();
        updateTime();
        events.fireUpdateTimeDisplay(false);
        iterCount++;
        setUpdateCompleted(true);

        events.fireUpdateCompleted();

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
     * Default asynchronous update method called by {@link org.simbrain.network.update_actions.BufferedUpdate}.
     */
    public void bufferedUpdate() {

        // Only update things in this list, and in this order.
        final var classes = List.of(
                Neuron.class,
                NeuronGroup.class,
                WeightMatrix.class,
                NeuronArray.class,
                NeuronCollection.class,
                Subnetwork.class,
                SmileSVM.class
        );

        // First update the buffers using the models internal update logic
        classes.forEach(cls -> networkModels.get(cls).forEach(NetworkModel::setBufferValues));

        // Then update the states themselves
        classes.forEach(cls -> networkModels.get(cls).forEach(NetworkModel::update));

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
     * Set the activation level of all neurons to zero.
     */
    public void clearActivations() {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.clear();
        }
    }

    /**
     * Return the neuron at the specified index of the internal list storing neurons.
     *
     * @param neuronIndex index of the neuron
     * @return the neuron at that index
     */
    @Deprecated
    public Neuron getLooseNeuron(int neuronIndex) {
        final var iterator = getLooseNeurons().iterator();
        for (int i = 0; i < neuronIndex; i++) {
            iterator.next();
        }
        return iterator.next();
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
     * Create "flat" list of neurons, which includes the top-level neurons plus all group neurons.
     *
     * @return the flat list
     */
    public List<Neuron> getFlatNeuronList() {
        List<Neuron> ret = new ArrayList<>();
        ret.addAll(getLooseNeurons());
        getNeuronGroups().forEach(ng -> ret.addAll(ng.getNeuronList()));
        // TODO: Does this double-count?
        getNeuronCollectionSet().forEach(nc -> ret.addAll(nc.getNeuronList()));
        getSubnetworks().forEach(s -> s.getNeuronGroupList().forEach(
                ng -> ret.addAll(ng.getNeuronList())));
        return ret;
    }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses.
     *
     * @return the flat list
     */
    public List<Synapse> getFlatSynapseList() {
        List<Synapse> ret = new ArrayList<>(10000);
        ret.addAll(getLooseSynapses());
        getSynapseGroups().forEach(sg -> ret.addAll(sg.getAllSynapses()));
        getSubnetworks().forEach(s -> s.getSynapseGroupList().forEach(
                sg -> ret.addAll(sg.getAllSynapses())));
        return ret;
    }

    /**
     * Returns a list of all neuron groups including those in subnetworks.
     */
    public List<NeuronGroup> getFlatNeuronGroupList() {
        ArrayList<NeuronGroup> ret = new ArrayList<>(getNeuronGroups());
        getSubnetworks().forEach(net -> ret.addAll(net.getNeuronGroupList()));
        return ret;
    }

    /**
     * Returns a list of all synapse groups including those in subnetworks.
     */
    public List<SynapseGroup> getFlatSynapseGroupList() {
        ArrayList<SynapseGroup> ret = new ArrayList<>(getSynapseGroups());
        getSubnetworks().forEach(net -> ret.addAll(net.getSynapseGroupList()));
        return ret;
    }

    /**
     * Returns a list of all weight matrices including those in subnetworks.
     */
    public List<WeightMatrix> getFlatWeightMatrixList() {
        ArrayList<WeightMatrix> ret = new ArrayList<>(getWeightMatrices());
        getSubnetworks().forEach(net -> ret.addAll(net.getWeightMatrixList()));
        return ret;
    }

    /**
     * Add a new {@link NetworkModel}. All network models MUST be added using this method.
     */
    public void addNetworkModel(NetworkModel networkModel) {
        if (networkModel.shouldAdd()) {
            networkModels.add(networkModel);
            events.fireModelAdded(networkModel);
            networkModel.setId(idManager.getId(networkModel.getClass()));
        }
    }

    /**
     * Delete a {@link NetworkModel}
     */
    public void delete(final NetworkModel toDelete) {
        networkModels.remove(toDelete);
        toDelete.delete();
        events.fireModelRemoved(toDelete);
    }

    /**
     * Create a {@link NeuronCollection) from a provided list of neurons
     *
     * @param neuronList list of neurons to add to a neuron collection.
     */
    public NeuronCollection createNeuronCollection(List<Neuron> neuronList) {

        // Filter out loose neurons (a neuron is loose if its parent group is null)
        List<Neuron> loose = neuronList.stream()
                //.filter(n -> n.getParentGroup() == null)  // TODO
                .collect(Collectors.toList());

        // Only make the neuron collection if some neurons have been selected
        if (!loose.isEmpty()) {

            // Creating a neuron collection increments the id counter so don't
            // even create it if it's a duplicate
            int hashCode = loose.stream().mapToInt(n -> n.hashCode()).sum();
            for (NeuronCollection nc : getNeuronCollectionSet()) {
                if (hashCode == nc.getSummedNeuronHash()) {
                    return null;
                }
            }

            // Make the collection
            NeuronCollection nc =new NeuronCollection(this, loose);
            nc.setLabel(getIdManager().getProposedId(nc.getClass()));
            return nc;
        }
        return null;
    }


    /**
     * Add a weight matrix between two {@link ArrayConnectable}'s.
     * Can "adapt" a neuron collection to an ND4J array, or be a weight
     * matrix between those arrays.
     *
     * @param source source neuron collection or nd4j array
     * @param target target neuron collection or nd4j array
     */
    public WeightMatrix createWeightMatrix(ArrayConnectable source, ArrayConnectable target) {
        if (target.getIncomingWeightMatrix() != null) {
            delete(target.getIncomingWeightMatrix());
        }

        WeightMatrix newMatrix = new WeightMatrix(this, source, target);
        return newMatrix;
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

        events = new NetworkEvents(this);
        updateCompleted = new AtomicBoolean(false);

        // Initialize update manager
        updateManager.postUnmarshallingInit();

        getFlatNeuronList().forEach(Neuron::postUnmarshallingInit);
        getTextList().forEach(NetworkTextObject::postUnmarshallingInit);
        getSynapseGroups().forEach(SynapseGroup::postUnmarshallingInit);
        getNeuronGroups().forEach(AbstractNeuronCollection::postUnmarshallingInit);
        getNeuronCollectionSet().forEach(AbstractNeuronCollection::postUnmarshallingInit);
        getNeuronArrays().forEach(NeuronArray::postUnmarshallingInit);
        getWeightMatrices().forEach(WeightMatrix::postUnmarshallingInit);
        getSubnetworks().forEach(Subnetwork::postUnmarshallingInit);

        // Re-populate fan-in / fan-out for loose synapses
        getLooseSynapses().forEach(Synapse::postUnmarshallingInit);

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
        ret.append(networkModels.getAll()
                .stream().map((m)-> "[" + m.getId() + "] " + m.toString())
                .collect(Collectors.joining()));
        return ret.toString();
    }

    /**
     * Returns a neuron with a matching label.  If more than one
     * neuron has a matching label, the first found is returned.
     * If there are no matches, a {@link NoSuchElementException} is thrown.
     *
     * @param label label of neuron to search for
     * @return matched Neuron, if any
     */
    public Neuron getNeuronByLabel(String label) {
        return getFlatNeuronList().stream()
                .filter(n -> n.getLabel().equalsIgnoreCase(label))
                .findFirst().get();
    }

    /**
     * Returns a neurongroup with a matching label.  If more than one
     * group has a matching label, the first one found is returned.
     * If there are no matches, a {@link NoSuchElementException} is thrown.
     *
     * @param label label of NeuronGroup to search for
     * @return matched NeuronGroup, if any
     */
    public NeuronGroup getNeuronGroupByLabel(String label) {
        return getNeuronGroups().stream()
                .filter(n -> n.getLabel().equalsIgnoreCase(label))
                .findFirst().get();
    }

    /**
     * Returns the list of text objects
     */
    public LinkedHashSet<NetworkTextObject> getTextList() {
        return networkModels.get(NetworkTextObject.class);
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
    public void addObjects(final List<NetworkModel> toAdd) {
        toAdd.forEach(this::addNetworkModel);
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void translate(final double offsetX, final double offsetY) {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.offset(offsetX, offsetY);
        }
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

    public NetworkEvents getEvents() {
        return events;
    }

    public LinkedHashSet<Subnetwork> getSubnetworks() {
        return networkModels.get(Subnetwork.class);
    }

    public LinkedHashSet<SynapseGroup> getSynapseGroups() {
        return networkModels.get(SynapseGroup.class);
    }

    public LinkedHashSet<NeuronGroup> getNeuronGroups() {
        return networkModels.get(NeuronGroup.class);
    }

    public LinkedHashSet<NeuronArray> getNeuronArrays() {
        return networkModels.get(NeuronArray.class);
    }

    public Set<WeightMatrix> getWeightMatrices() {
        return networkModels.get(WeightMatrix.class);
    }

    /**
     * Return the list of synapses. These are "loose" neurons. For the full set of neurons, including neurons inside of
     * subnetworks and groups, use {@link #getFlatNeuronList()}.
     */
    public LinkedHashSet<? extends Neuron> getLooseNeurons() {
        return networkModels.get(Neuron.class);
    }

    /**
     * Return the list of synapses. These are "loose" synapses. For the full set of synapses, including synapses inside
     * of subnetworks and groups, use {@link #getFlatSynapseList()}.
     */
    public Collection<Synapse> getLooseSynapses() {
        return Collections.unmodifiableCollection(networkModels.get(Synapse.class));
    }

    public HashSet<NeuronCollection> getNeuronCollectionSet() {
        return networkModels.get(NeuronCollection.class);
    }

    public SimpleIdManager getIdManager() {
        return idManager;
    }

    public List<NetworkModel> getModels() {
        return networkModels.getAll();
    }

    public List<NetworkModel> getModelsInDeserializationOrder() {
        return networkModels.getAllInDeserializationOrder();
    }

    /**
     * The main data structure for {@link NetworkModel}s. Wraps a map from classes to ordered sets of those objects.
     */
    private static class NetworkModelList {

        /**
         * Items must be ordered for deserializing. For example neurons but serialized before synapses.
         */
        private final static transient List<Class<? extends NetworkModel>> order = List.of(
                Neuron.class,
                NeuronGroup.class,
                NeuronCollection.class,
                NeuronArray.class,
                Synapse.class,
                WeightMatrix.class,
                SynapseGroup.class,
                Subnetwork.class
        );

        /**
         * Backing for the collection: a map from model types to linked hash sets.
         */
        private final Map<Class<? extends NetworkModel>, LinkedHashSet<NetworkModel>> networkModels = new HashMap<>();

        public <T extends NetworkModel> void put(Class<T> modelClass, T model) {
            networkModels.putIfAbsent(modelClass, new LinkedHashSet<>());
            networkModels.get(modelClass).add(model);
        }

        public <T extends NetworkModel> void putAll(Class<T> modelClass, List<T> model) {
            networkModels.putIfAbsent(modelClass, new LinkedHashSet<>());
            networkModels.get(modelClass).addAll(model);
        }

        private void putAllUnchecked(Class<? extends NetworkModel> modelClass, List<? extends NetworkModel> model) {
            networkModels.putIfAbsent(modelClass, new LinkedHashSet<>());
            networkModels.get(modelClass).addAll(model);
        }

        public void addAll(Collection<? extends NetworkModel> models) {
            models.forEach(this::add);
        }

        public void add(NetworkModel model) {
            if (model instanceof Subnetwork) {
                put(Subnetwork.class, (Subnetwork) model);
            } else {
                networkModels.putIfAbsent(model.getClass(), new LinkedHashSet<>());
                networkModels.get(model.getClass()).add(model);
            }
        }

        /**
         * Returns an ordered set of network models of a specific type.
         */
        @SuppressWarnings("unchecked")
        public <T extends NetworkModel> LinkedHashSet<T> get(Class<T> modelClass) {
            if (networkModels.containsKey(modelClass)) {
                return (LinkedHashSet<T>) networkModels.get(modelClass);
            } else {
                return new LinkedHashSet<>();
            }
        }

        //TODO
        public LinkedHashSet<?> unsafeGet(Class<?> modelClass) {
            if (networkModels.containsKey(modelClass)) {
                return (LinkedHashSet<?>) networkModels.get(modelClass);
            } else {
                return new LinkedHashSet<>();
            }
        }



        public List<NetworkModel> getAll() {
            return networkModels.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }

        /**
         * Returns a list of network models in the order required for proper deserilization.
         */
        public List<NetworkModel> getAllInDeserializationOrder() {
            return order.stream()
                    .filter(networkModels::containsKey)
                    .flatMap(cls -> networkModels.get(cls).stream()).collect(Collectors.toList());
        }

        public void remove(NetworkModel model) {
            if (model instanceof Subnetwork) {
                if (networkModels.containsKey(Subnetwork.class)) {
                    networkModels.get(Subnetwork.class).remove(model);
                }
            } else {
                if (networkModels.containsKey(model.getClass())) {
                    networkModels.get(model.getClass()).remove(model);
                }
            }
        }
    }

}
