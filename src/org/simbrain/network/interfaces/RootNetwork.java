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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.util.SimpleId;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

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

    /** Since groups span all levels of the hierarchy they are stored here. */
    private ArrayList<Group> groupList = new ArrayList<Group>();

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

    /** Network Component. */
    private NetworkComponent component;
    
    /**
     * Returns reference to parent.
     *
     * @return reference to parent
     */
    public NetworkComponent getParent() {
        return component;
    }
    
    /**
     * Used to create an instance of network (Default constructor).
     */
    public RootNetwork(final NetworkComponent parent) {
        super();
        this.component = parent;
        init();
    }
    
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
     * Only to be called by NetworkComponent.
     */
    public void setParent(final NetworkComponent component) {
        this.component = component;
    }
    
    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(RootNetwork.class, "logger");
        xstream.omitField(RootNetwork.class, "component");
        xstream.omitField(RootNetwork.class, "listenerList");
        xstream.omitField(RootNetwork.class, "updateCompleted");
        xstream.omitField(RootNetwork.class, "networkThread");
        xstream.omitField(Network.class, "logger");
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
        // listenerList = new HashSet<NetworkListener>();
        this.updatePriorities = new TreeSet<Integer>();
        this.updatePriorities.add(new Integer(0));
        return this;
    }

    /**
     * Externally called update function which coordinates input and output neurons and
     * connections with worlds and gauges.
     */
    public void updateRootNetwork() {
        logger.debug("updateRootNetwork called");

        //Update Time
        updateTime();

        // Call root network update function
        update();

        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();
    }

    /**
     * Helper method for custom scripts, to handle basic non-logical updates.
     *
     */
    public void updateRootNetworkStandard() {

        //Update Time
        updateTime();

        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();

        // For thread
        this.setUpdateCompleted(true);
    }

    /**
     * The core update function of the neural network. Calls the current update
     * function on each neuron, decays all the neurons, and checks their bounds.
     */
    public void update() {
        logger.debug("update called");
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
        if (groupList != null) {
            for (Group n : groupList) {
                n.update();
            }
        }
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
        for (Integer i : this.getUpdatePriorities()) {
            //System.out.print(i.intValue() + "\n");
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
            // update sub-networks with priority level i
            for (Network n : this.getNetworkList()) {
                if (n.getUpdatePriority() == i.intValue()) {
                    n.update();
                }
            }
        }
    }

    /**
     * Add a new group of network elements.
     * @param group group of network elements
     */
    public void addGroup(final Group group) {
        groupList.add(group);
        fireGroupAdded(group);
    }

    /**
     * Remove the specified group.
     *
     * @param toDelete the group to delete.
     */
    public void deleteGroup(final Group toDelete) {
        fireGroupDeleted(toDelete);
        groupList.remove(toDelete);
    }

    /**
     * Returns the group, if any, a specified object is contained in.
     *
     * @param object the object to check
     * @return the group, if any, containing that object
     */
    public Group containedInGroup(final Object object) {
        for (Group group : groupList) {
            if (object instanceof Neuron) {
                if (group.getFlatNeuronList().contains(object)) {
                    return group;
                }
            } else if (object instanceof Synapse) {
                if (group.getFlatSynapseList().contains(object)) {
                    return group;
                }
            } else if (object instanceof Network) {
                if (group.getFlatNetworkList().contains(object)) {
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
        for (NetworkListener listener : component.getListeners()) {
            for (ConsumingAttribute<?> attribute : deleted.consumingAttributes()) {
                listener.attributeRemoved(deleted, attribute);
            }
            
            for (ProducingAttribute<?> attribute : deleted.producingAttributes()) {
                listener.attributeRemoved(deleted, attribute);
            }
            
            listener.neuronRemoved(new NetworkEvent<Neuron>(this, deleted));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     */
    public void fireNetworkChanged() {
        for (NetworkListener listener : component.getListeners()) {
            listener.networkChanged();
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     * @param moved Neuron that has been moved
     */
    public void fireNeuronMoved(final Neuron moved) {
        for (NetworkListener listener : component.getListeners()) {
            listener.neuronMoved(new NetworkEvent<Neuron>(this, moved));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a clamp changed event to all registered model listeners.
     */
    public void fireClampChanged() {
//        for (NetworkListener listener : component.getListeners()) {
//            listener.clampMenuChanged();
//            listener.clampBarChanged();
//        }
//        if (getParent() != null) {
//            getParent().setChangedSinceLastSave(true);
//        }
    }


    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added neuron which was added
     */
    public void fireNeuronAdded(final Neuron added) {
        for (NetworkListener listener : component.getListeners()) {
            listener.neuronAdded(new NetworkEvent<Neuron>(this, added));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a neuron changed event to all registered model listeners.
     *
     * @param old the previous neuron, before the change
     * @param changed the new, changed neuron
     */
    public void fireNeuronChanged(final Neuron old, final Neuron changed) {
        for (NetworkListener listener : component.getListeners()) {
            listener.neuronChanged(new NetworkEvent<Neuron>(this, old, changed));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added synapse which was added
     */
    public void fireSynapseAdded(final Synapse added) {
        for (NetworkListener listener : component.getListeners()) {
            listener.synapseAdded(new NetworkEvent<Synapse>(this, added));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted synapse which was deleted
     */
    public void fireSynapseDeleted(final Synapse deleted) {
        for (NetworkListener listener : component.getListeners()) {
            listener.synapseRemoved(new NetworkEvent<Synapse>(this, deleted));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param old old synapse, before the change
     * @param changed new, changed synapse
     */
    public void fireSynapseChanged(final Synapse old, final Synapse changed) {
        for (NetworkListener listener : component.getListeners()) {
            listener.synapseChanged(new NetworkEvent<Synapse>(this, old, changed));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
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
        for (NetworkListener listener : component.getListeners()) {
            listener.subnetAdded(new NetworkEvent<Network>(this, added));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a subnetwork deleted event to all registered model listeners.
     *
     * @param deleted synapse which was deleted
     */
    public void fireSubnetDeleted(final Network deleted) {
        for (NetworkListener listener : component.getListeners()) {
            listener.subnetRemoved(new NetworkEvent<Network>(this, deleted));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a subnetwork added event to all registered model listeners.
     *
     * @param added synapse which was added
     */
    public void fireSubnetAdded(final Network added) {
        for (NetworkListener listener : component.getListeners()) {
            listener.subnetAdded(new NetworkEvent<Network>(this, added));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a group added event to all registered model listeners.
     *
     * @param added Group that has been added
     */
    public void fireGroupAdded(final Group added) {
        for (NetworkListener listener : component.getListeners()) {
            listener.groupAdded(new NetworkEvent<Group>(this, added));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a group changed event to all registered model listeners.
     *
     * @param old Old group
     * @param changed New changed group
     */
    public void fireGroupChanged(final Group old, final Group changed) {
        for (NetworkListener listener : component.getListeners()) {
            listener.groupChanged(new NetworkEvent<Group>(this, old, changed));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
        }
    }

    /**
     * Fire a group changed event to all registered model listeners.
     *
     * @param deleted Group to be deleted
     */
    public void fireGroupDeleted(final Group deleted) {
        for (NetworkListener listener : component.getListeners()) {
            listener.groupRemoved(new NetworkEvent<Group>(this, deleted));
        }
        if (getParent() != null) {
            getParent().setChangedSinceLastSave(true);
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
        fireClampChanged();
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
        fireClampChanged();
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
        for (int i = 0; i < groupList.size(); i++) {
            Group group = (Group) groupList.get(i);
            ret += ("\n" + getIndents() + "Group " + (i + 1));
            ret += (getIndents() + "--------------------------------\n");
            ret += group.toString();
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public List<Consumer> getConsumers() {
        return new ArrayList<Consumer>(getFlatNeuronList());
    }

    /**
     * {@inheritDoc}
     */
//    public List<Coupling> getCouplings() {
//        return couplings;
//    }

    /**
     * {@inheritDoc}
     */
    public List<Producer> getProducers() {
        return new ArrayList<Producer>(getFlatNeuronList());
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

}
