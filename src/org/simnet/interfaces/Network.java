/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.simbrain.workspace.Workspace;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
import org.simbrain.world.WorldListener;
import org.simnet.NetworkThread;
import org.simnet.coupling.Coupling;
import org.simnet.coupling.InteractionMode;
import org.simnet.synapses.SignalSynapse;
import org.simnet.util.CopyFactory;


/**
 * <b>Network</b> provides core neural network functionality and is the the main API for external calls. Network
 * objects are sets of neurons and  weights connecting them. Much of the  actual update and  learning logic occurs
 * (currently) in the individual nodes.
 */
public abstract class Network {

    /** Reference to root network. */
    private RootNetwork rootNetwork = null;

    /** Id of this network; used in persistence. */
    private String id;

    /** Array list of neurons. */
    private ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

    /** Array list of weights. */
    private ArrayList<Synapse> weightList = new ArrayList<Synapse>();

    /** Array list of sub-networks. */
    private ArrayList<Network> networkList = new ArrayList<Network>();

    /** In iterartions or seconds. */
    private double time = 0;

    /** Time step. */
    private double timeStep = .01;

    /** Whether to round off neuron values. */
    private boolean roundOffActivationValues = false;

    /** Degree to which to round off values. */
    private int precision = 0;

    /** Only used for sub-nets of complex networks which have parents. */
    private Network parentNet = null;

    /** Provides default initialization to network ids. */
    private static int counter = 0;

    /** sequence in which the update function should be called
     *  for this sub-network. By default, this is set to 0 for all
     *  the sub-networks. If you want a subset of sub-networks to fire
     *  before others, assign it a higher priority value.
     */
    private int updatePriority = 0;


    /**
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
        id = "temp_id" + counter++;
    }

    /**
     * Update the network.
     */
    public abstract void update();

    /**
     * Updates all networks.
     */
    public void updateAllNetworks() {
        Iterator i = networkList.iterator();

        while (i.hasNext()) {
            ((Network) i.next()).update();
        }
    }

     /**
     * Initialize the network.
     */
    public void init(RootNetwork root) {
        setRootNetwork(root);
        initWeights();
        initNeurons();
        initParents();
        initSubnets(root);
        rootNetwork.updateTimeType();
        rootNetwork.fireNetworkChanged();
        rootNetwork.fireClampChanged();
    }

    /**
     * @return a duplicate network.
     */
    public abstract Network duplicate();


    /**
     * Finish creating a duplicate network.  This copies over most of the
     * objects.  The subclass method takes care of type specific parameters.
     *
     * @param newNetwork thenew network to finish duplicating.
     * @return the new network to finish copying.
     */
    public Network duplicate(final Network newNetwork) {
        newNetwork.setRootNetwork(this.getRootNetwork());
        ArrayList copy = CopyFactory.getCopy(this.getObjectList());
        newNetwork.addObjects(copy, true);
        newNetwork.setUpdatePriority(this.getUpdatePriority());
        return newNetwork;
    }

    /**
     * Adds a list of network elements to this network.
     *
     * @param toAdd list of objects to add.
     * @param notify whether to fire a notification event.
     */
    private void addObjects(final ArrayList toAdd, final boolean notify) {
        for (Object object : toAdd) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                addNeuron(neuron, notify);
            } else if (object instanceof Synapse) {
                Synapse synapse = (Synapse) object;
                addWeight(synapse, notify);
            } else if (object instanceof Network) {
                Network net = (Network) object;
                addNetwork(net, notify);
            }
        }
    }

    /**
     * Adds a list of objects and fires a notification event for views, etc.
     *
     * @param toAdd objects to add.
     */
    public void addObjects(final ArrayList toAdd) {
        addObjects(toAdd, true);
    }

    /**
     * Adds a list of objects and fires a notification event for views, etc.
     *
     * @param toAdd objects to add.
     */
    public void addObjectReferences(final ArrayList toAdd) {
        addObjects(toAdd, false);
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
     * Initialize subnetworks.
     */
    private void initSubnets(RootNetwork root) {
        for (Network network : networkList) {
            network.init(root);
            network.setParentNetwork(this);
        }
    }
    
    /**
     * Perform intialization required after opening saved networks.
     */
    public void initCastor(){
        if(this.updatePriority != 0)
            this.getRootNetwork().setPriorityUpdate(this.updatePriority);
    }

    /**
     * Updates weights with fan-in.  Used when weights have been added.
     */
    public void initWeights() {
        //initialize fan-in and fan-out on each neuron
        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = (Synapse) weightList.get(i);
            w.init();
        }
    }

    /**
     * Inits neurons.
     */
    public void initNeurons() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron neuron = (Neuron) neuronList.get(i);
            neuron.setParentNetwork(this);
        }
    }

    /**
     * @return the name of the class of this network
     */
    public String getType() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * @return how many subnetworks down this is
     */
    public int getDepth() {
        Network net = this;
        int n = 0;

        while (!(net instanceof RootNetwork)) {
            net = net.getNetworkParent();
            n++;
        }

        return n;
    }

    /**
     * @return a string of tabs for use in indenting debug info accroding to the depth of a subnet
     */
    public String getIndents() {
        String ret = new String("");

        for (int i = 0; i < (this.getDepth() - 1); i++) {
            ret = ret.concat("\t");
        }

        return ret;
    }

    /**
     * @return List of neurons in network.
     */
    public ArrayList<Neuron> getNeuronList() {
        return this.neuronList;
    }

    /**
     * @return List of weights in network.
     */
    public ArrayList getWeightList() {
        return this.weightList;
    }

    /**
     * @return Number of neurons in network.
     */
    public int getNeuronCount() {
        return neuronList.size();
    }

    /**
     * Returns distance between centers of two neurons.
     * @param neuron1 first neuron
     * @param neuron2 second neuron
     * @return distance
     */
    public static double getDistance(final Neuron neuron1, final Neuron neuron2) {
        return Math.sqrt(Math.pow(neuron2.getX() - neuron1.getX(), 2)
                      + Math.pow(neuron2.getY() - neuron1.getY(), 2));
    }

    /**
     * @param index Number of neuron in array list.
     * @return Neuron at the point of the index
     */
    public Neuron getNeuron(final int index) {
        return (Neuron) neuronList.get(index);
    }

    /**
     * Return a list of neurons in a specific radius of a specified neuron.
     * @param source the source neuron.
     * @param radius the radius to search within.
     * @return list of neurons in the given radius.
     */
    public ArrayList<Neuron> getNeuronsInRadius(final Neuron source, final double radius) {
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
     * @param id id to search for.
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
     * Find a synapse with a given string id.
     *
     * @param id id to search for.
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
     * @param neuron Type of neuron to add
     */
    public void addNeuron(final Neuron neuron) {
        addNeuron(neuron, true);
    }

    /**
     * Adds a new neuron.
     *
     * @param neuron Type of neuron to add
     * @param notify whether to fire a synapse added event
     */
    private void addNeuron(final Neuron neuron, final boolean notify) {
        neuron.setParentNetwork(this);
        neuronList.add(neuron);
        if ((rootNetwork != null) && (notify)) {
            rootNetwork.fireNeuronAdded(neuron);
        }
        neuron.init();
    }

    /**
     * @return Number of weights in network
     */
    public int getWeightCount() {
        return weightList.size();
    }

    /**
     * @param index Number of weight in array list.
     * @return Weight at the point of the indesx
     */
    public Synapse getWeight(final int index) {
        return (Synapse) weightList.get(index);
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
     * Adds a weight to the neuron network, where that weight already has designated source and target neurons.
     *
     * @param weight the weight object to add
     * @param notify whether to fire a synapse added event
     */
    private void addWeight(final Synapse weight, final boolean notify) {

        Neuron source = (Neuron) weight.getSource();
        source.addTarget(weight);
        weight.setParent(this);
        Neuron target = (Neuron) weight.getTarget();
        target.addSource(weight);

        if (weight instanceof SignalSynapse) {
            target.setTargetValueSynapse((SignalSynapse) weight);
        }

        weight.initSpikeResponder();
        weightList.add(weight);
        if ((rootNetwork != null) && (notify)) {
            rootNetwork.fireSynapseAdded(weight);
        }
    }

    /**
     * Adds a weight to the neuron network, where that weight already has designated source and target neurons.
     *
     * @param weight the weight object to add
     */
    public void addWeight(final Synapse weight) {
        addWeight(weight, true);
    }

    /**
     * Calls {@link Neuron#update} for each neuron.
     */
    public void updateAllNeurons() {

        if (rootNetwork.getClampNeurons()) {
            return;
        }

        // First update the activation buffers
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.update(); // update neuron buffers
        }

        // Then update the activations themselves
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.setActivation(n.getBuffer());
        }
    }

    /**
     * Calls {@link Synapse#update} for each weight.
     */
    public void updateAllWeights() {

        if (rootNetwork.getClampWeights()) {
            return;
        }

        // No Buffering necessary because the values of weights don't depend on one another
        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = (Synapse) weightList.get(i);
            w.update();
        }
    }

    /**
     * Calls {@link Neuron#checkBounds} for each neuron, which makes sure the neuron has not exceeded its upper bound
     * or gone below its lower bound.   TODO: Add or replace with normalization within bounds?
     */
    public void checkAllBounds() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.checkBounds();
        }

        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = (Synapse) weightList.get(i);
            w.checkBounds();
        }
    }

    /**
     * Round activations of to intergers; for testing.
     */
    public void roundAll() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.round(precision);
        }
    }

    /**
     * Deletes a neuron from the network.
     *
     * @param toDelete neuron to delete
     */
    public void deleteNeuron(final Neuron toDelete) {

        if (toDelete.getParentNetwork().getNeuronList().contains(toDelete)) {

            Group group = getRootNetwork().containedInGroup(toDelete);
            if (group != null) {
                group.deleteNeuron(toDelete);
                if (group.isEmpty()) {
                    this.getRootNetwork().deleteGroup(group);
                }
            }

            // Remove outgoing synapses
            while (toDelete.getFanOut().size() > 0) {
                Synapse s = (Synapse) toDelete.getFanOut().get(toDelete.getFanOut().size() - 1);
                deleteWeight(s);
            }

            // Remove incoming synapses
            while (toDelete.getFanIn().size() > 0) {
                
              Synapse s = (Synapse) toDelete.getFanIn().get(toDelete.getFanIn().size() - 1);
              deleteWeight(s);
            }

            // Remove the neuron itself
            toDelete.getParentNetwork().getNeuronList().remove(toDelete);

            // Notify listeners (views) that this neuron has been deleted
            rootNetwork.fireNeuronDeleted(toDelete);
        }

        //If we just removed the last neuron of a network, remove that network
        Network parent = toDelete.getParentNetwork();
        if (!(parent instanceof RootNetwork)) {
            if (parent.isEmpty()) {
                parent.getNetworkParent().deleteNetwork(parent);
            }
        }
    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete the weight to delete
     * @param notify whether to fire a synapse deleted event
     */
    private void deleteWeight(final Synapse toDelete, final boolean notify) {

        // Notify first since some methods need to refer to the dying
        //   synapse before its death.
        if (notify) {
            rootNetwork.fireSynapseDeleted(toDelete);
        }

        // ABOVE NOT CALLED IN MAIN NETWORK WHEN CALLED FROM ROOT NET

        Group group = getRootNetwork().containedInGroup(toDelete);
        if (group != null) {
            group.deleteWeight(toDelete);
            if (group.isEmpty()) {
                this.getRootNetwork().deleteGroup(group);
            }
        }

        toDelete.getSource().getFanOut().remove(toDelete);
        toDelete.getTarget().getFanIn().remove(toDelete);
        toDelete.getParent().getWeightList().remove(toDelete);
    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete the weight to delete
     */
    public void deleteWeight(final Synapse toDelete) {
        if (toDelete == toDelete.getTarget().getTargetValueSynapse()) {
            toDelete.getTarget().setTargetValueSynapse(null);
        }
        deleteWeight(toDelete, true);
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    public void clearActivations() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.setActivation(0);
        }
    }

    /**
     * Returns the "state" of the network--the activation level of its neurons.  Used by the gauge component
     *
     * @return an array representing the activation levels of all the neurons in this network
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
        for (int i = 0; i < weightList.size(); i++) {
            Synapse temp = (Synapse) weightList.get(i);
            temp.setStrength(0);
        }
    }

    /**
     * Randomizes all neurons.
     */
    public void randomizeNeurons() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.randomize();
        }
    }

    /**
     * Randomizes all weights.
     */
    public void randomizeWeights() {
        for (int i = 0; i < weightList.size(); i++) {
            Synapse temp = (Synapse) weightList.get(i);
            temp.randomize();
        }
        //Must make this symmetrical
    }

    /**
     * Round a value off to indicated number of decimal places.
     *
     * @param value value to round off
     * @param decimalPlace degree of precision
     *
     * @return rounded number
     */
    public static double round(final double value, final int decimalPlace) {
        double powerOfTen = 1;
        int place = decimalPlace;

        while (place-- > 0) {
            powerOfTen *= 10.0;
        }

        return Math.round(value * powerOfTen) / powerOfTen;
    }

    /**
     * @see Object
     */
    public String toString() {
        String ret = new String();
        if (neuronList.size() > 0) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron tempRef = (Neuron) neuronList.get(i);
                ret += (getIndents() + tempRef + "\n");
            }
        }

        if (weightList.size() > 0) {
            for (int i = 0; i < weightList.size(); i++) {
                Synapse tempRef = (Synapse) weightList.get(i);
                ret += (getIndents() + tempRef);
            }
        }

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ret += ("\n" + getIndents() + "Sub-network " + (i + 1) + " (" + net.getType() + ")");
            ret += (getIndents() + "--------------------------------\n");
            ret += net.toString();
        }

        return ret;
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
     * @param i Degeree to round off values
     */
    public void setPrecision(final int i) {
        precision = i;
    }

    /**
     * Whether to round off neuron values.
     * @param b Round off
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
     * @param roundOffActivationValues The roundOffActivationValues to set.
     */
    public void setRoundOffActivationValues(final boolean roundOffActivationValues) {
        this.roundOffActivationValues = roundOffActivationValues;
    }

    /**
     * @param neuronList The neuronList to set.
     */
    public void setNeuronList(final ArrayList neuronList) {
        this.neuronList = neuronList;
    }

    /**
     * @param weightList The weightList to set.
     */
    public void setWeightList(final ArrayList weightList) {
        this.weightList = weightList;
    }

    /**
     * Add an array of neurons and set their parents to this.
     *
     * @param neurons list of neurons to add
     * @notify whether to notify listeners that these neurons were added.
     */
    protected void addNeuronList(final ArrayList<Neuron> neurons, boolean notify) {
        for (Neuron n : neurons) {
            n.setParentNetwork(this);
            addNeuron(n,  notify);
        }
    }

    /**
     * Add an array of neurons and set their parents to this.
     *
     * @param neurons list of neurons to add
     */
    public void addNeuronList(final ArrayList neurons) {
        addNeuronList(neurons, true);
    }

    /**
     * Sets the upper bounds.
     * @param u Upper bound
     */
    public void setUpperBounds(final double u) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(u);
        }
    }

    /**
     * Sets the lower bounds.
     * @param l Lower bound
     */
    public void setLowerBounds(final double l) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(l);
        }
    }

    /**
     * Returns a reference to the synapse connecting two neurons, or null if there is none.
     *
     * @param src source neuron
     * @param tar target neuron
     *
     * @return synapse from source to target
     */
    public static Synapse getWeight(final Neuron src, final Neuron tar) {
        for (int i = 0; i < src.fanOut.size(); i++) {
            Synapse s = (Synapse) src.fanOut.get(i);

            if (s.getTarget() == tar) {
                return s;
            }
        }

        return null;
    }

    /**
     * Replace one neuron with another.
     *
     * @param oldNeuron out with the old
     * @param newNeuron in with the new...
     */
    public void changeNeuron(final Neuron oldNeuron, final Neuron newNeuron) {
        newNeuron.setId(oldNeuron.getId());
        newNeuron.setFanIn(oldNeuron.getFanIn());
        newNeuron.setFanOut(oldNeuron.getFanOut());
        newNeuron.setParentNetwork(this);

        rootNetwork.fireNeuronChanged(oldNeuron, newNeuron);

        for (int i = 0; i < oldNeuron.getFanIn().size(); i++) {
            ((Synapse) oldNeuron.getFanIn().get(i)).setTarget(newNeuron);
        }

        for (int i = 0; i < oldNeuron.getFanOut().size(); i++) {
            ((Synapse) oldNeuron.getFanOut().get(i)).setSource(newNeuron);
        }

        newNeuron.setSensoryCoupling(oldNeuron.getSensoryCoupling());
        newNeuron.setMotorCoupling(oldNeuron.getMotorCoupling());

        getNeuronList().remove(oldNeuron);
        getNeuronList().add(newNeuron);
        initParents();

        // If the neuron is a spiker, add spikeResponders to target weights, else remove them
        for (int i = 0; i < newNeuron.getFanOut().size(); i++) {
            ((Synapse) newNeuron.getFanOut().get(i)).initSpikeResponder();
        }

        rootNetwork.updateTimeType();
    }

    /**
     * Change synapse type / replace one synapse with another.
     *
     * @param oldSynapse out with the old
     * @param newSynapse in with the new...
     */
    public void changeSynapse(final Synapse oldSynapse, final Synapse newSynapse) {
        newSynapse.setTarget(oldSynapse.getTarget());
        newSynapse.setSource(oldSynapse.getSource());
        deleteWeight(oldSynapse, false);
        addWeight(newSynapse, false);

        rootNetwork.fireSynapseChanged(oldSynapse, newSynapse);
    }

    /**
     * Initializes parent networks.
     */
    public void initParents() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = getNeuron(i);
            n.setParentNetwork(this);
        }
        for (int i = 0; i < weightList.size(); i++) {
            Synapse s = getWeight(i);
            s.setParent(this);
        }
    }

    /**
     * Gets the weight at particular point.
     * @param i Neuorn number
     * @param j Weight to get
     * @return Weight at the points defined
     */
    //TODO: Either fix this or make its assumptions explicit
    public Synapse getWeight(final int i, final int j) {
        return (Synapse) getNeuron(i).getFanOut().get(j);
    }

    /**
     * @return Returns the parentNet.
     */
    public Network getNetworkParent() {
        if (parentNet == null) {
            return rootNetwork;
        } else {
            return parentNet;            
        }
    }

    /**
     * @param parentNet The parentNet to set.
     */
    public void setParentNetwork(final Network parentNet) {
        this.parentNet = parentNet;
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
     * Return the id of this neuron.
     *
     * @return this neuron's id
     */
    public String getId() {
        return id;
    }

    /**
     * The id of this neuron; used in persistence.
     *
     * @param id the new id.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Adds a new network.
     * @param n Network type to add.
     * @param notify whether to fire a synapse added event
     */
    private void addNetwork(final Network n, final boolean notify) {
        networkList.add(n);
        n.setParentNetwork(this);
        n.setRootNetwork(rootNetwork);
        if (notify) {
            getRootNetwork().fireSubnetAdded(n);
        }
    }

    /**
     * Add a new network.
     *
     * @param network network to add.
     */
    public void addNetwork(final Network network) {
        addNetwork(network, true);
    }

    /**
     * @param i Network number to get.
     * @return network
     */
    public Network getNetwork(final int i) {
        return (Network) networkList.get(i);
    }

    /**
     * Delete network.
     *
     * @param toDelete Network to be deleted
     */
    public void deleteNetwork(final Network toDelete) {

        Group group = getRootNetwork().containedInGroup(toDelete);
        if (group != null) {
            group.deleteNetwork(toDelete);
            if (group.isEmpty()) {
                this.getRootNetwork().deleteGroup(group);
            }
        }

        // Remove all neurons (and the synapses with them)
        while (toDelete.getNeuronList().size() > 0) {
            toDelete.deleteNeuron(toDelete.getNeuron(0));
        }

        // Remove all subnets
        while (toDelete.getNetworkList().size() > 0) {
            toDelete.deleteNetwork(toDelete.getNetwork(0));
        }

        // Remove the network
        if (toDelete.getNetworkParent() != null) {
            toDelete.getNetworkParent().getNetworkList().remove(toDelete);
        }

        //If we just removed the last neuron of a network, remove that network
        Network parent = toDelete.getNetworkParent();
        if (!(parent instanceof RootNetwork)) {
            if (parent.isEmpty()) {
                parent.getNetworkParent().deleteNetwork(parent);
            }
        }

        // Notify listeners
        rootNetwork.fireSubnetDeleted(toDelete);
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
        if (this.getNetworkList().isEmpty()) {
            networksGone = true;
        }
        return (neuronsGone && networksGone);
    }

    /**
     * Add an array of networks and set their parents to this.
     *
     * @param networks list of neurons to add
     */
    public void addNetworkList(final ArrayList networks) {
        for (int i = 0; i < networks.size(); i++) {
            Network n = (Network) networks.get(i);
            addNetwork(n);
        }
    }

    /**
     * @return Returns the networkList.
     */
    public ArrayList<Network> getNetworkList() {
        return networkList;
    }

    /**
     * @param networkList The networkList to set.
     */
    public void setNetworkList(final ArrayList<Network> networkList) {
        this.networkList = networkList;
    }

    /**
     * Create "flat" list of neurons, which includes the top-level neurons plus all subnet neurons.
     *
     * @return the flat list
     */
    public ArrayList<Neuron> getFlatNeuronList() {
        ArrayList ret = new ArrayList();
        ret.addAll(neuronList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList toAdd;

            toAdd = (ArrayList) ((Network) net).getFlatNeuronList();

            ret.addAll(toAdd);
        }

        return ret;
    }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses.
     *
     * @return the flat list
     */
    public ArrayList<Synapse> getFlatSynapseList() {
        ArrayList ret = new ArrayList();
        ret.addAll(weightList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList toAdd;

            toAdd = (ArrayList) net.getFlatSynapseList();

            ret.addAll(toAdd);
        }

        return ret;
    }

    public ArrayList getObjectList() {
        ArrayList ret = new ArrayList();
        ret.addAll(getNeuronList());
        ret.addAll(getWeightList());
        ret.addAll(getNetworkList());
        return ret;
    }

    /**
     * Create "flat" list of all subnetworks.
     *
     * @return the flat list
     */
    public ArrayList getFlatNetworkList() {
        ArrayList ret = new ArrayList();
        ret.addAll(networkList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList toAdd;

            toAdd = (ArrayList) net.getFlatNetworkList();

            ret.addAll(toAdd);
        }

        return ret;
    }

    /**
     * Update all ids. Used in for persistences before writing net file.
     */
    public void updateIds() {

        if (this instanceof RootNetwork) {
            setId("root_net");
        }

        // Update neteworkids
        int netIndex = 1;
        for (Iterator networks = getNetworkList().iterator(); networks.hasNext(); netIndex++) {
            Network network = (Network) networks.next();
            network.setId("net_" + netIndex);
        }

        // Update neuron ids
        int nIndex = 1;
        for (Iterator neurons = getFlatNeuronList().iterator(); neurons.hasNext(); nIndex++) {
            Neuron neuron = (Neuron) neurons.next();
            neuron.setId("n_" + nIndex);
        }

        // Update synapse ids
        int sIndex = 1;
        for (Iterator synapses = getFlatSynapseList().iterator(); synapses.hasNext(); sIndex++) {
            Synapse synapse = (Synapse) synapses.next();
            synapse.setId("s_" + sIndex);
        }
    }

    /**
     * Returns all Input Neurons.
     *
     * @return list of input neurons;
     */
    public Collection getInputNeurons() {
        ArrayList inputs = new ArrayList();
        for (Iterator i = this.getFlatNeuronList().iterator(); i.hasNext(); ) {
            Neuron neuron = (Neuron) i.next();
            if (neuron.isInput()) {
                inputs.add(neuron);
            }
        }
        return inputs;
    }

    /**
     * Returns all Output Neurons.
     *
     * @return list of output neurons;
     */
    public Collection getOutputNeurons() {
        ArrayList outputs = new ArrayList();
        for (Iterator i = this.getFlatNeuronList().iterator(); i.hasNext(); ) {
            Neuron neuron = (Neuron) i.next();
            if (neuron.isOutput()) {
                outputs.add(neuron);
            }
        }
        return outputs;
    }

    /**
     * @return Returns the rootNetwork.
     */
    public RootNetwork getRootNetwork() {
        return rootNetwork;
    }

    /**
     * @param rootNetwork The rootNetwork to set.
     */
    public void setRootNetwork(RootNetwork rootNetwork) {
        this.rootNetwork = rootNetwork;
    }


    /**
     * @return updatePriority for the sub-network
     */
    public int getUpdatePriority() {
        return updatePriority;
    }

    /**
     * @param updatePriority to set.
     */
   public void setUpdatePriority(final int updatePriority) {
       this.updatePriority = updatePriority;
        if (this.updatePriority != 0 && this.getRootNetwork() != null) {
            // notify the rootNetwork
            this.getRootNetwork().setPriorityUpdate(updatePriority);
        }
   }
}
