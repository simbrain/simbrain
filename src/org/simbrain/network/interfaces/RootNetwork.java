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
package org.simbrain.network.interfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.listeners.SubnetworkListener;
import org.simbrain.network.listeners.SynapseListener;
import org.simbrain.network.util.SimpleId;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * <b>RootNetwork</b> is the top level of the network hierarchy. Subject for all
 * observers. Time is kept track of here. Also keeps track, currently, of
 * couplings to other workspace components. When instantiating a view (including
 * when using Simbrain as an API, or from a command-line, a root network must
 * first be created. Acts as a "container" for all subsequent networks.
 */
public class RootNetwork extends Network {

    public static final String NEURON_ID_PREFIX = "Neuron";

    /** Log4j logger. */
    private Logger logger = Logger.getLogger(RootNetwork.class);

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** Whether this is a discrete or continuous time network. */
    private int timeType = DISCRETE;

    /** If this is a discrete-time network. */
    public static final int DISCRETE = 0;

    /** If this is a continuous-time network. */
    public static final int CONTINUOUS = 1;

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

    /** Whether network files should use tabs or not. */
    private boolean usingTabs = true;

    /**
     * Enumeration for the update methods.
     *
     * BUFFER: default update method; based on buffering
     *
     * PRIORITYBASED: user sets the priority for each neuron, sub-neuron and
     * synapse. Default priority value is 0. Elements with smaller priority
     * value are updated first.
     *
     */
    public enum UpdateMethod { PRIORITYBASED, BUFFERED }

    /** Current update method. */
    private UpdateMethod updateMethod = UpdateMethod.BUFFERED;

    /**
     * The updatePriority values used by neurons and sub-layers
     *  is stored in this set.
     */
    private SortedSet<Integer> updatePriorities = null;
    
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

    /**
     * When using from a console.
     *
     * @param id String id of this network
     */
    public RootNetwork() {
        init();
    }

    /**
     * Local initialization.
     */
    private void init() {
        setRootNetwork(this);
        this.updatePriorities = new TreeSet<Integer>();
        this.updatePriorities.add(new Integer(0));
        this.setId("Root-network");
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(RootNetwork.class, "logger");
        xstream.omitField(RootNetwork.class, "component");
        xstream.omitField(RootNetwork.class, "groupListeners");
        xstream.omitField(RootNetwork.class, "neuronListeners");
        xstream.omitField(RootNetwork.class, "networkListeners");
        xstream.omitField(RootNetwork.class, "subnetworkListeners");
        xstream.omitField(RootNetwork.class, "synapseListeners");
        xstream.omitField(RootNetwork.class, "updateCompleted");
        xstream.omitField(RootNetwork.class, "networkThread");
        xstream.omitField(Network.class, "logger");
        xstream.omitField(Neuron.class, "fanOut");
        xstream.omitField(Neuron.class, "fanIn");
        xstream.omitField(Neuron.class, "readOnlyFanOut");
        xstream.omitField(Neuron.class, "readOnlyFanIn");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        logger = Logger.getLogger(RootNetwork.class);
        this.updatePriorities = new TreeSet<Integer>();
        this.updatePriorities.add(new Integer(0));
        networkListeners = new ArrayList<NetworkListener>();
        neuronListeners = new ArrayList<NeuronListener>();
        synapseListeners = new ArrayList<SynapseListener>();
        subnetworkListeners = new ArrayList<SubnetworkListener>();
        groupListeners = new ArrayList<GroupListener>();

        for(Neuron neuron : this.getFlatNeuronList()) {
            neuron.postUnmarshallingInit();
        }
        for(Synapse synapse: this.getFlatSynapseList()) {
            synapse.getTarget().getFanIn().add(synapse);
            synapse.getSource().getFanOut().add(synapse);
        }
        return this;
    }

    @Deprecated
    public void updateRootNetwork() {
        update();
    }

    /**
     * The core update function of the neural network. Calls the current update
     * function on each neuron, decays all the neurons, and checks their bounds.
     */
    public void update() {

        logger.debug("update called");

        //Update Time
        updateTime();

        switch (this.updateMethod) {
	        case PRIORITYBASED:
            logger.debug("priority-based update");
            updateByPriority();
            updateAllSynapses();
            break;
        default:
            logger.debug("default update");
            updateAllNeurons();
            updateAllSynapses();
            updateAllNetworks();
        }
        if (getGroupList() != null) {
            for (Group n : getGroupList()) {
                n.update();
            }
        }

        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();
    }

    /**
     * This function is used to update the neuron and sub-network activation
     * values if the user chooses to set different priority values for a subset
     * of neurons and sub-networks. The priority value determines the order in
     * which the neurons and sub-networks get updated - smaller priority value
     * elements will be updated before larger priority value elements.
     */
    public void updateByPriority() {
        if (this.getUpdatePriorities() == null) {
            return;
        }
                
        // TODO: Re-implement update using a <Priority,Neuron> treemap
        for (Integer i : this.getUpdatePriorities()) {
            // System.out.print(i.intValue() + "\n");
            // update neurons with priority level i
            if (!this.getClampNeurons()) {
                // First update the activation buffers
                for (Neuron n : this.getNeuronList()) {
                    if (n.getUpdatePriority() == i.intValue()) {
                        n.update(); // update neuron buffers
                    }
                }

                // Then update the activations themselves
                for (Neuron n : this.getNeuronList()) {
                    if (n.getUpdatePriority() == i.intValue()) {
                        n.setActivation(n.getBuffer());
                    }
                }
            }
            
            // For now just neuron based priorities
//            // update sub-networks with priority level i
//            for (Network n : this.getNetworkList()) {
//                if (n.getUpdatePriority() == i.intValue()) {
//                    n.update();
//                }
//            }
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
            if (object instanceof Neuron) {
                if (group.getNeuronList().contains(object)) {
                    return group;
                }
            } else if (object instanceof Synapse) {
                if (group.getSynapseList().contains(object)) {
                    return group;
                }
            } else if (object instanceof Network) {
                if (group.getNetworkList().contains(object)) {
                    return group;
                }
            }
        }
        return null;
    }

    /**
     * Clears out input values of network nodes, which otherwise linger and
     * cause problems.
     */
    public void clearInputs() {

        //TODO: Is there a more efficient way to handle this?
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
        if (timeType == DISCRETE) {
            return "" + (int) time + " " + getUnits()[1];
        } else {
            return "" + round(time, getTimeStepPrecision()) + " " + getUnits()[0];
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
     * @return integer representation of time type.
     */
    public int getTimeType() {
        return timeType;
    }

    /**
     * If there is a single continuous neuron in the network, consider this a continuous network.
     */
    public void updateTimeType() {
        timeType = DISCRETE;

        for (Neuron n : getNeuronList()) {
            if (n.getTimeType() == CONTINUOUS) {
                timeType = CONTINUOUS;
            }
        }

        time = 0;
    }

    /**
     * Increment the time counter, using a different method depending on whether
     * this is a continuous or discrete. network.
     */
    public void updateTime() {
        if (timeType == CONTINUOUS) {
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
        String[] units = {"msec", "iterations" };

        return units;
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted neuron which has been deleted
     */
    public void fireNeuronDeleted(final Neuron deleted) {
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
     * Fire a network update method changed event to all registered model
     * listeners.
     */
    private void fireNetworkUpdateMethodChanged() {
        for (NetworkListener listener : networkListeners) {
            listener.networkUpdateMethodChanged();
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
     * @param old the previous neuron, before the change
     * @param changed the new, changed neuron
     */
    public void fireNeuronTypeChanged(final Neuron old, final Neuron changed) {
        for (NeuronListener listener : neuronListeners) {
            listener.neuronTypeChanged(new NetworkEvent<Neuron>(this, old, changed));
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
    public void fireSynapseDeleted(final Synapse deleted) {
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
     * @param old old synapse, before the change
     * @param changed new, changed synapse
     */
    public void fireSynapseTypeChanged(final Synapse old, final Synapse changed) {
        for (SynapseListener listener : synapseListeners) {
            listener.synapseTypeChanged(new NetworkEvent<Synapse>(this, old, changed));
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
    public void fireGroupDeleted(final Group deleted) {
        for (GroupListener listener : groupListeners) {
            listener.groupRemoved(new NetworkEvent<Group>(this, deleted));
        }
    }

    /**
     * Fire a group changed event to all registered model listeners.
     *
     * @param old Old group
     * @param changed New changed group
     */
    public void fireGroupChanged(final Group old, final Group changed) {

        for (GroupListener listener : groupListeners) {
            listener.groupChanged(new NetworkEvent<Group>(this, old, changed));
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
     * @param clampNeurons Neurons to set
     */
    public void setClampNeurons(final boolean clampNeurons) {
        this.clampNeurons = clampNeurons;
        this.fireNeuronClampToggle();
    }

    @Override
    public Network duplicate() {
        // TODO Auto-generated method stub
        return null;
    }
    /**
     * @return Returns the isUsingTabs.
     */
    public boolean getUsingTabs() {
        return usingTabs;
    }

    /**
     * @param usingTabs The isUsingTabs to set.
     */
    public void setUsingTabs(final boolean usingTabs) {
        this.usingTabs = usingTabs;
    }

    /**
     * @param priority to set.
     */
    public void setPriorityUpdate(final int priority) {
       if (priority == 0) {
           return;
       }
       this.updatePriorities.add(new Integer(priority));
    }

    /**
     * @return the set of updatePriority values
     */
    public SortedSet<Integer> getUpdatePriorities() {
        return this.updatePriorities;
    }

    /**
     * @return the updateMethod
     */
    public UpdateMethod getUpdateMethod() {
        return updateMethod;
    }

    /**
     * @param updateMethod to set
     */
    public void setUpdateMethod(final UpdateMethod updateMethod) {
        this.updateMethod = updateMethod;
        this.fireNetworkUpdateMethodChanged();
    }

    /**
     * @see Object
     */
    public String toString() {

        String ret = "Root Network \n================= \n";
        ret += "Update method: " + this.getUpdateMethod() + "\t Iterations:"
                + this.getTime() + "\n";

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
            ret += ("\n" + getIndents() + "Sub-network " + (i + 1) + " (" + net.getType() + ")");
            ret += (getIndents() + "--------------------------------\n");
            ret += net.toString();
        }

        for (int i = 0; i < getGroupList().size(); i++) {
            Group group = (Group) getGroupList().get(i);
            ret += ("\n" + getIndents() + "Group " + (i + 1));
            ret += (getIndents() + "--------------------------------\n");
            ret += group.toString();
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

//    public Attribute getAttribute(String id) {
//        System.out.println("id: " + id);
//        
//        Matcher matcher = Pattern.compile('(' + NEURON_ID_PREFIX + "_\\d+):(\\w+)").matcher(id);
//        
//        if (!matcher.matches()) return null;
//        
//        String parent = matcher.group(1);
//        String attribute = matcher.group(2);
//        
//        System.out.println("parent: " + parent);
//        System.out.println("attribute: " + attribute);
//        
//        for (Neuron n : getFlatNeuronList()) {
//            if (n.getId().equals(parent)) {
//                for (Attribute a : n.consumingAttributes()) {
//                    System.out.println("\tchecking: " + a.getAttributeDescription());
//                    if (a.getAttributeDescription().equals(attribute)) return a;
//                }
//                
//                for (Attribute a : n.producingAttributes()) {
//                    System.out.println("\tchecking: " + a.getAttributeDescription());
//                    if (a.getAttributeDescription().equals(attribute)) return a;
//                }
//            }
//        }
//        
//        return null;
//    }

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
     * Search for a neuron by label. If there are more than one with the same
     * label only the first one found is returned.
     *
     * @param inputString
     *            label of neuron to search for
     * @return list of matched neurons, or null if none are found
     */
    public List<Neuron> getNeuronByLabel(String inputString) {
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
     * @return the groupIdGenerator
     */
    public SimpleId getGroupIdGenerator() {
        return groupIdGenerator;
    }

}
