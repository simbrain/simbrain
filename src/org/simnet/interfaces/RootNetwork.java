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
package org.simnet.interfaces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;
import org.simnet.NetworkThread;

import bsh.EvalError;
import bsh.Interpreter;


/**
 * <b>RootNetwork</b> is the top level of the network hierarchy.
 * Subject for all observers.
 * Time is kept track of here.
 * Also keeps track, currently, of couplings to other workspace components.
 * When instantiating a view (including when using Simbrain as an API, or from a command-line, a root network must
 * first be created.  Acts as a "container" for all subsequent networks.
 */
public class RootNetwork extends Network implements CouplingContainer {

    /** Log4j logger. */
    private Logger logger = Logger.getLogger(RootNetwork.class);

    /** Since groups span all levels of the hierarcy they are stored here. */
    private ArrayList<Group> groupList = new ArrayList<Group>();

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** List of observers. */
    private HashSet<NetworkListener> listenerList = new HashSet<NetworkListener>();

    /** The thread that runs the network. */
    private NetworkThread networkThread;

    /** Whether this is a discrete or continuous time network. */
    private int timeType = DISCRETE;

    /** If this is a discrete-time network. */
    public static final int DISCRETE = 0;

    /** If this is a continuous-time network. */
    public static final int CONTINUOUS = 1;

    /** In iterartions or seconds. */
    private double time = 0;

    /** Time step. */
    private double timeStep = .01;

    /** Constant value for Math.lg(10); used to approxomate log 10. */
    private static final double LOG_10 = Math.log(10);

    /** Used to temporarily turn off all learning. */
    private boolean clampWeights = false;

    /** Used to temporarily hold weights at their current value. */
    private boolean clampNeurons = false;

    /** Custom update script written in beanshell (www.beanshell.org). */
    private File customUpdateScript = null;

    /** Whether network files should use tabs or not. */
    private boolean usingTabs = true;

    /** Enumeration for the update methods
     *  DEFAULT: default update method
     *  PRIORITYBASED: user sets the priority for each neuron,
     *  sub-neuron and synapse. Default priority value is 0.
     *  Elements with smaller priority value are updated first.
     *  SCRIPT: update is handled by a script
     */
    public enum UpdateMethod { PRIORITYBASED, SCRIPTBASED, DEFAULT }

    /** Current update method. */
    private UpdateMethod updateMethod = UpdateMethod.DEFAULT;

    /**
     * The updatePriority valuse used by neurons and sub-layers
     *  is stored in this set.
     */
    private SortedSet<Integer> updatePriorities = null;

    /** List of couplings. */
    private ArrayList<Coupling> couplings = new ArrayList<Coupling>();


    /**
     * Used to create an instance of network (Default constructor).
     */
    public RootNetwork() {
        super();
        setRootNetwork(this);
        this.updatePriorities = new TreeSet<Integer>();
        this.updatePriorities.add(new Integer(0));
    }

    /**
     * Perform intialization required after opening saved networks.
     */
    public void postUnmarshallingInit(NetworkListener listener) {

        logger = Logger.getLogger(RootNetwork.class);

        if (this instanceof RootNetwork) {
            listenerList = new HashSet<NetworkListener>();
            this.addNetworkListener(listener);
        }
        super.postUnmarshallingInit();
        // Only add top level networks
        for (Network subnet : getNetworkList()) {
            this.fireSubnetAdded(subnet);
        }
    }

    /**
     * Externally called update function which coordiantes input and output neurons and
     * connections with worlds and gauges.
     */
    public void updateRootNetwork() {
        logger.debug("updateRootNetwork called");

        if (this.updateMethod == UpdateMethod.SCRIPTBASED) {
            logger.debug("script-based update method");
            runScript();
            return;
        }

        //Update Time
        updateTime();

        // Call root network update function
        update();

        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();

        // For thread
        updateCompleted = true;
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
        updateCompleted = true;
    }

    /**
     * Run a user specified script written in beanshell.
     */
    private void runScript() {
        Interpreter i = new Interpreter(); // Construct an interpreter
        try {
            i.set("network", this);
            i.source(customUpdateScript.getAbsolutePath());
            i.eval("iterate()");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (EvalError e) {
            e.printStackTrace();
        }
    }

    public void updateCouplings() {
        logger.debug("updateCouplings called");
        for (Coupling coupling : getCouplings()) {
            logger.debug("updating coupling: " + coupling);
            coupling.update();
        }
    }
    
    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        logger.debug("update called");
        updateCouplings();
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

    /** this function is used to update the neuron
     * and sub-network activation values if the user
     * chooses to set different priority values for
     * a subset of neurons and sub-networks. The
     * priority value determines the order in which
     * the neurons and sub-networks get updated - smaller
     * priority value elements will be updated before larger
     * priority value elements
     */
    public void updateByPriority() {
        if (this.getUpdatePriorities() == null) {
            return;
        }
        for (Integer i : this.getUpdatePriorities()) {
            System.out.print(i.intValue() + "\n");
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
     * Respond to worldChanged event.
     */
    public void worldChanged() {
        updateRootNetwork();
    }

    /**
     * Clears out input values of network nodes, which otherwise linger and
     * cause problems.
     */
    public void clearInputs() {

        Iterator it = getInputNeurons().iterator();

        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();
            n.setInputValue(0);
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
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete.
     * network
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
        String[] units = {"Seconds", "Iterations" };

        return units;
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted neuron which has been deleted
     */
    public void fireNeuronDeleted(final Neuron deleted) {
        for (NetworkListener listener : getListenerList()) {
            listener.neuronRemoved(new NetworkEvent(this, deleted));
        }
    }

    /**
     * Fire a coupling changed event to all registered model listeners.
     *
     * @param n the Neuron whose coupling has changed.
     */
    public void fireCouplingChanged(final Neuron n) {
        for (NetworkListener listener : getListenerList()) {
            listener.couplingChanged(new NetworkEvent(this, n));
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     */
    public void fireNetworkChanged() {
        for (NetworkListener listener : getListenerList()) {
            listener.networkChanged();
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     * @param moved Neuron that has been moved
     */
    public void fireNeuronMoved(final Neuron moved) {
        for (NetworkListener listener : getListenerList()) {
            listener.neuronMoved(new NetworkEvent(this, moved));
        }
    }

    /**
     * Fire a clamp changed event to all registered model listeners.
     */
    public void fireClampChanged() {
        for (NetworkListener listener : getListenerList()) {
            listener.clampMenuChanged();
            listener.clampBarChanged();
        }
    }


    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added neuron which was added
     */
    public void fireNeuronAdded(final Neuron added) {
        for (NetworkListener listener : getListenerList()) {
            listener.neuronAdded(new NetworkEvent(this, added));
        }
    }

    /**
     * Fire a neuron changed event to all registered model listeners.
     *
     * @param old the previous neuron, before the change
     * @param changed the new, changed neuron
     */
    public void fireNeuronChanged(final Neuron old, final Neuron changed) {
        for (NetworkListener listener : getListenerList()) {
            listener.neuronChanged(new NetworkEvent(this, old, changed));
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     *
     * @param added synapse which was added
     */
    public void fireSynapseAdded(final Synapse added) {
        for (NetworkListener listener : getListenerList()) {
            listener.synapseAdded(new NetworkEvent(this, added));
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted synapse which was deleted
     */
    public void fireSynapseDeleted(final Synapse deleted) {
        for (NetworkListener listener : getListenerList()) {
            listener.synapseRemoved(new NetworkEvent(this, deleted));
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param old old synapse, before the change
     * @param changed new, changed synapse
     */
    public void fireSynapseChanged(final Synapse old, final Synapse changed) {
        for (NetworkListener listener : getListenerList()) {
            listener.synapseChanged(new NetworkEvent(this, old, changed));
        }
    }

    /**
     * Notify any objects observing this network that it has closed.
     */
    public void close() {
        if (this.getNetworkThread() != null) {
            this.getNetworkThread().setRunning(false);
        }
        // Only consider this a close if no one is listening to this network
//        if (getListenerList().size() == 0) {
            // Remove world listeners
//            for (Iterator i = getCouplingList().iterator(); i.hasNext(); ) {
//                Coupling coupling = (Coupling) i.next();
//                if (coupling.getWorld() != null) {
//                    coupling.getWorld().removeWorldListener(this);
//                }
//            }
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
     * @return Returns the networkThread.
     */
    public NetworkThread getNetworkThread() {
        return networkThread;
    }

    /**
     * @param networkThread The networkThread to set.
     */
    public void setNetworkThread(final NetworkThread networkThread) {
        this.networkThread = networkThread;
    }

    /**
     * Return the top level listener list.
     *
     * @return the top level listener list
     */
    public HashSet<NetworkListener> getListenerList() {
        return listenerList;
    }

    /**
     * Fire a subnetwork added event to all registered model listeners.
     *
     * @param added synapse which was added
     */
    public void fireSubnetAdded(final RootNetwork added) {
        for (NetworkListener listener : getListenerList()) {
            listener.subnetAdded(new NetworkEvent(this, added));
        }
    }

    /**
     * Fire a subnetwork deleted event to all registered model listeners.
     *
     * @param deleted synapse which was deleted
     */
    public void fireSubnetDeleted(final Network deleted) {
        for (NetworkListener listener : getListenerList()) {
            listener.subnetRemoved(new NetworkEvent(this, deleted));
        }
    }

    /**
     * Fire a subnetwork added event to all registered model listeners.
     *
     * @param added synapse which was added
     */
    public void fireSubnetAdded(final Network added) {
        for (NetworkListener listener : getListenerList()) {
            listener.subnetAdded(new NetworkEvent(this, added));
        }
    }

    /**
     * Fire a group added event to all registered model listeners.
     *
     * @param added Group that has been added
     */
    public void fireGroupAdded(final Group added) {
        for (NetworkListener listener : getListenerList()) {
            listener.groupAdded(new NetworkEvent(this, added));
        }
    }

    /**
     * Fire a group changed event to all registered model listeners.
     *
     * @param old Old group
     * @param changed New changed group
     */
    public void fireGroupChanged(final Group old, final Group changed) {
        for (NetworkListener listener : getListenerList()) {
            listener.groupChanged(new NetworkEvent(this, old, changed));
        }
    }

    /**
     * Fire a group changed event to all registered model listeners.
     *
     * @param deleted Group to be deleted
     */
    public void fireGroupDeleted(final Group deleted) {
        for (NetworkListener listener : getListenerList()) {
            listener.groupRemoved(new NetworkEvent(this, deleted));
        }
    }
    /**
     * Add the specified network listener.
     *
     * @param l listener to add
     */
    public void addNetworkListener(final NetworkListener l) {
        listenerList.add(l);
    }

    /**
     * Remove the specified network listener.
     *
     * @param l listener to remove
     */
    public void removeNetworkListener(final NetworkListener l) {
        getListenerList().remove(l);
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


    /**
     * @return the customUpdateScript
     */
    public File getCustomUpdateScript() {
        return customUpdateScript;
    }


    /**
     * @param customUpdateScript the customUpdateScript to set
     */
    public void setCustomUpdateScript(final File customUpdateScript) {
        this.customUpdateScript = customUpdateScript;
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
        String ret = super.toString();
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
    public List<Coupling> getCouplings() {
        return couplings;
    }

    /**
     * {@inheritDoc}
     */
    public List<Producer> getProducers() {
        return new ArrayList<Producer>(getFlatNeuronList());
    }

}
