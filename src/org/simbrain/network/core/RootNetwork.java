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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.network.groups.Group;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SubnetworkListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.network.listeners.TextListener;
import org.simbrain.network.update_actions.CustomUpdate;
import org.simbrain.network.util.SynapseRouter;
import org.simbrain.util.SimpleId;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>RootNetwork</b> is the top level of the network hierarchy. All networks
 * are instances are Network.java. But a network can itself have sub-networks.
 * This is the root node in the network-subnetwork hierarchy. Acts as a
 * "container" for all subsequent networks. Gui networks and NetworkComponents
 * (workspace level wrappers for networks) always have a single unique root
 * networks. Most network related event notifications are broadcast from here.
 * Time is kept track of here.
 */
public class RootNetwork extends Network {

    public static final String NEURON_ID_PREFIX = "Neuron";

    /** Log4j logger. */
    private Logger logger = Logger.getLogger(RootNetwork.class);

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** In iterations or msec. */
    private double time = 0;

    /** Time step. */
    private double timeStep = .01;

    /** Constant value for Math.log(10); used to approximate log 10. */
    private static final double LOG_10 = Math.log(10);

    /** Used to temporarily turn off all learning. */
    private boolean clampWeights = false;

    /** Used to temporarily hold weights at their current value. */
    private boolean clampNeurons = false;

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

    /** Text objects. */
    private List<NetworkTextObject> textList = new ArrayList<NetworkTextObject>();

    /**
     * List of neurons sorted by their update priority. Used in priority based
     * update.
     */
    private List<Neuron> prioritySortedNeuronList;

    /** Comparator used for sorting the priority sorted neuron list. */
    private PriorityComparator priorityComparator = new PriorityComparator();

    /**
     * The update manager for this network.
     */
    private UpdateManager updateManager;

    /** Object which routes synpases to synpase groups. */
    private final SynapseRouter synapseRouter;

    /** Network Id generator. */
    private SimpleId networkIdGenerator = new SimpleId("Network", 1);

    /** Neuron Id generator. */
    private SimpleId neuronIdGenerator = new SimpleId(NEURON_ID_PREFIX, 1);

    /** Synapse Id generator. */
    private SimpleId synapseIdGenerator = new SimpleId("Synapse", 1);

    /** Group Id generator. */
    private SimpleId groupIdGenerator = new SimpleId("Group", 1);

    /** List of objects registered to observe general network events. */
    private List<NetworkListener> networkListeners = new ArrayList<NetworkListener>();

    /** List of objects registered to observe neuron-related network events. */
    private List<NeuronListener> neuronListeners = new ArrayList<NeuronListener>();

    /** List of objects registered to observe synapse-related network events. */
    private List<SynapseListener> synapseListeners = new ArrayList<SynapseListener>();

    /** List of objects registered to observe subnetwork-related network events. */
    private List<SubnetworkListener> subnetworkListeners = new ArrayList<SubnetworkListener>();

    /** List of objects registered to observe group-related network events. */
    private List<GroupListener> groupListeners = new ArrayList<GroupListener>();

    /** List of objects registered to observe text-related network events. */
    private List<TextListener> textListeners = new ArrayList<TextListener>();

    /**
     * When using from a console.
     *
     * @param id String id of this network
     */
    public RootNetwork() {
        updateManager = new UpdateManager(this);
        synapseRouter  = new SynapseRouter();
        setRootNetwork(this);
        prioritySortedNeuronList = new ArrayList<Neuron>();
    }

    /**
     * Returns a properly initialized xstream object.
     * 
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(RootNetwork.class, "logger");
        xstream.omitField(RootNetwork.class, "component");
        xstream.omitField(RootNetwork.class, "customRule");
        xstream.omitField(RootNetwork.class, "groupListeners");
        xstream.omitField(RootNetwork.class, "neuronListeners");
        xstream.omitField(RootNetwork.class, "networkListeners");
        xstream.omitField(RootNetwork.class, "subnetworkListeners");
        xstream.omitField(RootNetwork.class, "synapseListeners");
        xstream.omitField(RootNetwork.class, "textListeners");
        xstream.omitField(RootNetwork.class, "updateCompleted");
        xstream.omitField(RootNetwork.class, "networkThread");

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
        logger = Logger.getLogger(RootNetwork.class);
        
        // Initialize listeners
        networkListeners = new ArrayList<NetworkListener>();
        neuronListeners = new ArrayList<NeuronListener>();
        synapseListeners = new ArrayList<SynapseListener>();
        subnetworkListeners = new ArrayList<SubnetworkListener>();
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
     * The core update function of the neural network. Calls the current update
     * function on each neuron, decays all the neurons, and checks their bounds.
     */
    public void update() {

        logger.debug("update called");

        // Update Time
        updateTime();

        // Perform update
        for(UpdateAction action : updateManager.getActionList()) {
            action.invoke();
        }        
        
//        switch (this.updateMethod) {
//        case BUFFERED:
//            logger.debug("default update");
//            updateAllNeurons();
//            updateAllSynapses();
//            updateAllNetworks();
//            updateAllGroups();
//            break;
//        case PRIORITYBASED:
//            logger.debug("priority-based update");
//            updateNeuronsByPriority();
//            updateAllSynapses();
//            updateAllGroups();
//            break;
//        case CUSTOM:
//            logger.debug("custom update");
//            if (customRule != null) {
//                customRule.update(this);
//            }
//            break;
//        default:
//            updateAllNeurons();
//            updateAllSynapses();
//            updateAllNetworks();
//            updateAllGroups();
//            break;
//        }

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
     * Returns the group, if any, a specified object is contained in.
     * 
     * @param object the object to check
     * @return the group, if any, containing that object
     */
    public Group containedInGroup(final Object object) {
        for (Group group : getGroupList()) {
            //REDO
//            if (object instanceof Neuron) {
//                if (group.getNeuronList().contains(object)) {
//                    return group;
//                }
//            } else if (object instanceof Synapse) {
//                if (group.getSynapseList().contains(object)) {
//                    return group;
//                }
//            }
        }
        return null;
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
     * @param i the current time
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
     * @return Returns the parentNet.
     */
    public RootNetwork getParentNetwork() {
        return this;
    }

    /**
     * @return Returns the timeStep.
     */
    public double getTimeStep() {
        return timeStep;
    }

    /**
     * @param timeStep The timeStep to set.
     */
    public void setTimeStep(final double timeStep) {
        this.timeStep = timeStep;
    }

    /**
     * @return Units by which to count.
     */
    public static String[] getUnits() {
        String[] units = { "msec", "iterations" };
        return units;
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     * 
     * @param deleted neuron which has been deleted
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
     * @param moved Neuron that has been moved
     */
    public void fireNeuronMoved(final Neuron moved) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronMoved(new NetworkEvent<Neuron>(this, moved));
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     * 
     * @param added neuron which was added
     */
    public void fireNeuronAdded(final Neuron added) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronAdded(new NetworkEvent<Neuron>(this, added));
        }
    }

    /**
     * Fire a neuron type changed event to all registered model listeners.
     * 
     * @param old the old update rule
     * @param changed the new update rule
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
     * @param changed new, changed neuron
     */
    public void fireNeuronChanged(final Neuron changed) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronChanged(new NetworkEvent<Neuron>(this, changed));
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     * 
     * @param added synapse which was added
     */
    public void fireSynapseAdded(final Synapse added) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseAdded(new NetworkEvent<Synapse>(this, added));
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     * 
     * @param deleted synapse which was deleted
     */
    public void fireSynapseRemoved(final Synapse deleted) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseRemoved(new NetworkEvent<Synapse>(this, deleted));
        }
    }

    /**
     * Fire a synapse changed event to all registered model listeners.
     * 
     * @param changed new, changed synapse
     */
    public void fireSynapseChanged(final Synapse changed) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseChanged(new NetworkEvent<Synapse>(this, changed));
        }
    }

    /**
     * Fire a synapse type changed event to all registered model listeners.
     * 
     * @param oldRule old synapse, before the change
     * @param learningRule new, changed synapse
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
     * @param added text which was deleted
     */
    public void fireTextAdded(final NetworkTextObject added) {
        for (TextListener listener : textListeners) {
            listener.textAdded(added);
        }
    }

    /**
     * Fire a text deleted event to all registered model listeners.
     * 
     * @param deleted text which was deleted
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
     * @param changed text which was changed
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
     * @param b whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
    }

    /**
     * Fire a subnetwork added event to all registered model listeners.
     * 
     * @param added synapse which was added
     */
    public void fireSubnetAdded(final RootNetwork added) {
        for (SubnetworkListener listener : subnetworkListeners) {
            listener.subnetAdded(new NetworkEvent<Network>(this, added));
        }
    }

    /**
     * Fire a subnetwork deleted event to all registered model listeners.
     * 
     * @param deleted synapse which was deleted
     */
    public void fireSubnetDeleted(final Network deleted) {
        for (SubnetworkListener listener : subnetworkListeners) {
            listener.subnetRemoved(new NetworkEvent<Network>(this, deleted));
        }
    }

    /**
     * Fire a subnetwork added event to all registered model listeners.
     * 
     * @param added synapse which was added
     */
    public void fireSubnetAdded(final Network added) {
        for (SubnetworkListener listener : subnetworkListeners) {
            listener.subnetAdded(new NetworkEvent<Network>(this, added));
        }
    }

    /**
     * Fire a group added event to all registered model listeners.
     * 
     * @param added Group that has been added
     */
    public void fireGroupAdded(final Group added) {
        for (GroupListener listener : groupListeners) {
            listener.groupAdded(new NetworkEvent<Group>(this, added));
        }
    }

    /**
     * Fire a group deleted event to all registered model listeners.
     * 
     * @param deleted Group to be deleted
     */
    public void fireGroupRemoved(final Group deleted) {
        for (GroupListener listener : groupListeners) {
            listener.groupRemoved(new NetworkEvent<Group>(this, deleted));
        }
    }

    /**
     * Fire a group changed event to all registered model listeners.  A string desription describes
     * the change and is used by listeners to handle the event.   Old group is not currently used
     * but may be in the future.
     * 
     * @param old Old group
     * @param changed New changed group
     * @param changeDescription A description of the 
     */
    public void fireGroupChanged(final Group old, final Group changed, final String changeDescription) {

        for (GroupListener listener : groupListeners) {
            listener.groupChanged(new NetworkEvent<Group>(this, old, changed),
                    changeDescription);
        }
    }
    
    /**
     * This version of fireGroupChanged fires a pre-set event, which may have an
     * auxiliary object set.
     * 
     * @param event the network changed event.
     * @param changeDescription A description of the
     */
    public void fireGroupChanged(final NetworkEvent<Group> event, final String changeDescription) {

        for (GroupListener listener : groupListeners) {
            listener.groupChanged(event, changeDescription);
        }
    }


    /**
     * Fire a group parameters changed event.
     * 
     * @param group reference to group whose parameters changed
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
     * @param clampWeights Weights to set
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
     * @param clampNeurons Neurons to set
     */
    public void setClampNeurons(final boolean clampNeurons) {
        this.clampNeurons = clampNeurons;
        this.fireNeuronClampToggle();
    }


    @Override
    public String toString() {

        String ret = "Root Network \n================= \n";

        for (Neuron n : this.getNeuronList()) {
            ret += (getIndents() + n + "\n");
        }

        if (this.getSynapseList().size() > 0) {
            for (int i = 0; i < getSynapseList().size(); i++) {
                Synapse tempRef = (Synapse) getSynapseList().get(i);
                ret += (getIndents() + tempRef);
            }
        }

        for (int i = 0; i < getNetworkList().size(); i++) {
            Network net = (Network) getNetworkList().get(i);
            ret += ("\n" + getIndents() + "Sub-network " + (i + 1) + " ("
                    + net.getType() + ")");
            ret += (getIndents() + "--------------------------------\n");
            ret += net.toString();
        }

        for (int i = 0; i < getGroupList().size(); i++) {
            Group group = (Group) getGroupList().get(i);
            ret += group.toString();
        }

        for (NetworkTextObject text : textList) {
            ret += (getIndents() + text + "\n");
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
     * Return the generator for network ids.
     * 
     * @return the generator.
     */
    public SimpleId getNetworkIdGenerator() {
        return networkIdGenerator;
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
     * @param listener the observer to register
     */
    public void addNetworkListener(final NetworkListener listener) {
        networkListeners.add(listener);
    }

    /**
     * Register a neuron listener.
     * 
     * @param listener the observer to register
     */
    public void addNeuronListener(final NeuronListener listener) {
        neuronListeners.add(listener);
    }

    /**
     * Register a synapse listener.
     * 
     * @param listener the observer to register
     */
    public void addSynapseListener(final SynapseListener listener) {
        synapseListeners.add(listener);
    }

    /**
     * Register a text listener.
     * 
     * @param listener the observer to register
     */
    public void addTextListener(final TextListener listener) {
        textListeners.add(listener);
    }

    /**
     * Remove a synapse listener.
     * 
     * @param synapseListener the observer to remove
     */
    public void removeSynapseListener(SynapseListener synapseListener) {
        synapseListeners.remove(synapseListener);
    }

    /**
     * Register a subnetwork listener.
     * 
     * @param listener the observer to register
     */
    public void addSubnetworkListener(final SubnetworkListener listener) {
        subnetworkListeners.add(listener);
    }

    /**
     * Register a group listener.
     * 
     * @param listener the observer to register
     */
    public void addGroupListener(final GroupListener listener) {
        groupListeners.add(listener);
    }

    /**
     * Remove a group listener.
     * 
     * @param listener the observer to remove
     */
    public void removeGroupListener(final GroupListener listener) {
        groupListeners.remove(listener);
    }

    /**
     * Search for a neuron by label. If there are more than one with the same
     * label only the first one found is returned.
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

    /**
     * @return the groupIdGenerator
     */
    public SimpleId getGroupIdGenerator() {
        return groupIdGenerator;
    }

    /**
     * Comparator for sorting neurons by update priority.
     */
    private class PriorityComparator implements Comparator<Neuron> {
        public int compare(Neuron neuron1, Neuron neuron2) {
            Integer priority1 = neuron1.getUpdatePriority();
            Integer priority2 = neuron2.getUpdatePriority();
            return priority1.compareTo(priority2);
        }
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
}
