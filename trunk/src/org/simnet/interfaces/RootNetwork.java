/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.simbrain.workspace.Workspace;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
import org.simbrain.world.WorldListener;
import org.simnet.NetworkThread;
import org.simnet.coupling.Coupling;
import org.simnet.coupling.InteractionMode;

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
public class RootNetwork extends Network implements WorldListener {

    /** Reference to Workspace, which maintains a list of all worlds and gauges. */
    private Workspace workspace;

    /** Default interaction mode. */
    private static final InteractionMode DEFAULT_INTERACTION_MODE = InteractionMode.BOTH_WAYS;

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** Current interaction mode. */
    private InteractionMode interactionMode = DEFAULT_INTERACTION_MODE;

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
    
    /** Boolean flag indicating if priority based update is required 
     * for neurons and sub-networks*/
    private boolean priorityUpdate = false;
    
    /** The updatePriority valuse used by neurons and sub-layers 
     *  is stored in this set
     */
    private SortedSet<Integer> updatePriorities = null;

    /**
     * Used to create an instance of network (Default constructor).
     */
    public RootNetwork() {
        super();
        setRootNetwork(this);
    }


    /**
     * Externally called update function which coordiantes input and output neurons and
     * connections with worlds and gauges.
     */
    public void updateRootNetwork() {


        if (customUpdateScript != null) {
            runScript();
            return;
        } 

        //Update Time
        updateTime();

        // Get stimulus vector from world and update input nodes
        updateInputs();

        // Call root network update function
        update();

        // Update coupled worlds
        updateWorlds();     

        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();

        // For thread
        updateCompleted = true;
    }
    
    public void updateRootNetworkStandard() {


        //Update Time
        updateTime();

       // Call root network update function
        update();

        // Update coupled worlds
        updateWorlds();            

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

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
	if(this.priorityUpdate == false){
            updateAllNeurons();
            updateAllWeights();
            updateAllNetworks();
	}else{
	    updateByPriority();
	    updateAllWeights();
	}
        
        for (Group n : this.getGroupList()) {
            n.update();
        }
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
        if ((interactionMode.isWorldToNetwork() || interactionMode.isBothWays())) {
            return;
        }

        Iterator it = getInputNeurons().iterator();

        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();
            n.setInputValue(0);
        }
    }
    
    /**
     * Update input nodes of the network based on the state of the world.
     */
    public void updateInputs() {
        if (!(interactionMode.isWorldToNetwork() || interactionMode.isBothWays())) {
            return;
        }

        Iterator it = getInputNeurons().iterator();
        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();
            if (n.getSensoryCoupling().getAgent() != null) {
                double val = n.getSensoryCoupling().getAgent().getStimulus(
                        n.getSensoryCoupling().getSensorArray());
                n.setInputValue(val);
            } else {
                n.setInputValue(0);
            }
        }

        Iterator agents = this.getWorkspace().getAgentList().iterator();
        while (agents.hasNext()) {
            ((Agent) agents.next()).completedInputRound();
        }
    }

    /**
     * Go through each output node and send the associated output value to the
     * world component.
     */
    public void updateWorlds() {

        if (!(interactionMode.isNetworkToWorld() || interactionMode.isBothWays())) {
            return;
        }

        Iterator it = getOutputNeurons().iterator();
        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();

            if (n.getMotorCoupling().getAgent() != null) {
                n.getMotorCoupling().getAgent().setMotorCommand(
                        n.getMotorCoupling().getCommandArray(),
                        n.getActivation());
            }
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
    public RootNetwork getNetworkParent() {
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
            listener.clampChanged();
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
     * Check if any input or output neurons are coupled to a given world, and stop
     * listening to that world if none are.
     *
     * @param toCheck the world which should be checked for live couplings.
     */
    public void updateWorldListeners(final World toCheck) {
        boolean stopListening = false;
        for (Iterator i = getCouplingList().iterator(); i.hasNext(); ) {
            Coupling coupling = (Coupling) i.next();
            if (coupling.getWorld() != null) {
                if (coupling.getWorld() == toCheck) {
                    stopListening = true;
                }
            }
        }
        if (stopListening) {
            toCheck.removeWorldListener(this);
        }
    }

    /**
     * Notify any objects observing this network that it has closed.
     */
    public void close() {
        // Only consider this a close if no one is listening to this network
        if (getListenerList().size() == 0) {
            // Remove world listeners
            for (Iterator i = getCouplingList().iterator(); i.hasNext(); ) {
                Coupling coupling = (Coupling) i.next();
                if (coupling.getWorld() != null) {
                    coupling.getWorld().removeWorldListener(this);
                }
            }
            if (this.getNetworkThread() != null) {
                this.getNetworkThread().setRunning(false);
            }
        }
    }

    /**
     * Set the current interaction mode for this network panel to <code>interactionMode</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param interactionMode interaction mode for this network panel, must not be null
     */
    public void setInteractionMode(final InteractionMode interactionMode) {
        if (interactionMode == null) {
            throw new IllegalArgumentException("interactionMode must not be null");
        }

        this.interactionMode = interactionMode;
    }

    /**
     * Return the current interaction mode for this network panel.
     *
     * @return the current interaction mode for this network panel
     */
    public InteractionMode getInteractionMode() {
        return interactionMode;
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
     * @return Returns the workspace.
     */
    public Workspace getWorkspace() {
        if (workspace == null) {
            return this.getNetworkParent().getWorkspace();
        }
        return workspace;
    }

    /**
     * @param workspace The workspace to set.
     */
    public void setWorkspace(final Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Returns a list of all couplings associated with neurons in this network.
     *
     * @return couplings in this network.
     */
    public ArrayList getCouplingList() {
        ArrayList ret = new ArrayList();
        Iterator i = getNeuronList().iterator();
        while (i.hasNext()) {
            Neuron neuron = (Neuron) i.next();

            Coupling c = neuron.getSensoryCoupling();
            if (c != null) {
                ret.add(c);
            }

            c = neuron.getMotorCoupling();
            if (c != null) {
                ret.add(c);
            }
        }

        return ret;
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

    public void fireGroupAdded(final Group added) {
        for (NetworkListener listener : getListenerList()) {
            listener.groupAdded(new NetworkEvent(this, added));
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
    public void setCustomUpdateScript(File customUpdateScript) {
        this.customUpdateScript = customUpdateScript;
    }

    /**
     * @return the priorityUpdate
     */
    public boolean isPriorityUpdate() {
        return priorityUpdate;
    }

    /**
     * @param priorityUpdate to set
     */
    public void setPriorityUpdate(int priority) {
	if(priority == 0) return;
	if(this.updatePriorities == null){
            this.updatePriorities = new TreeSet<Integer>();
            this.updatePriorities.add(new Integer(0));
	}
	this.updatePriorities.add(new Integer(priority));
        this.priorityUpdate = true;        
    }
    
    /**
     * @return the set of updatePriority values
     */    
    public SortedSet<Integer> getUpdatePriorities(){
	return this.updatePriorities;
    }

}
