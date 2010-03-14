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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.network.util.CopyFactory;

/**
 * <b>Network</b> provides core neural network functionality and is the the main
 * API for external calls. Network objects are sets of neurons and weights
 * connecting them. Much of the actual update and learning logic occurs
 * (currently) in the individual nodes.
 */
public abstract class Network {

    /** The initial time-step for the network. */
    private static final double DEFAULT_TIME_STEP = .2;

    /** Logger. */
    private Logger logger = Logger.getLogger(Network.class);

    /** Reference to root network. */
    private RootNetwork rootNetwork = null;

    /** Id of this network; used in persistence. */
    private String id;

    /** Array list of neurons. */
    private ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

    /** Array list of synapses. */
    private ArrayList<Synapse> synapseList = new ArrayList<Synapse>();

    /** Array list of sub-networks. */
    private ArrayList<Network> networkList = new ArrayList<Network>();

    /** Since groups span all levels of the hierarchy they are stored here. */
    private List<Group> groupList = new ArrayList<Group>();

    /** Time step. */
    private double timeStep = DEFAULT_TIME_STEP;

    /** Whether to round off neuron values. */
    private boolean roundOffActivationValues = false;

    /** Degree to which to round off values. */
    private int precision = 0;

    /** Only used for sub-nets of complex networks which have parents. */
    private Network parentNet = null;

    /**
     *  Sequence in which the update function should be called
     *  for this sub-network. By default, this is set to 0 for all
     *  the sub-networks. If you want a subset of sub-networks to fire
     *  before others, assign it a higher priority value.
     */
    private int updatePriority = 0;

    /**
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
    }

    /**
     * Update the network.
     */
    public abstract void update();

    /**
     * Updates all networks.
     */
    public void updateAllNetworks() {
        //TODO: Unable to init logger after deserialize
//        logger.debug("updating " + networkList.size() + " networks");

        for (Network network : networkList) {
  //          logger.debug("updating network: " + network);
            network.update();
        }
    }

    /**
     * @return a duplicate network.
     */
    public abstract Network duplicate();


    /**
     * Finish creating a duplicate network.  This copies over most of the
     * objects.  The subclass method takes care of type specific parameters.
     *
     * @param newNetwork the new network to finish duplicating.
     * @return the new network to finish copying.
     */
    public Network duplicate(final Network newNetwork) {
        newNetwork.setRootNetwork(this.getRootNetwork());
        List<?> copy = CopyFactory.getCopy(this.getObjectList());
        newNetwork.addObjects(copy);
        newNetwork.setUpdatePriority(this.getUpdatePriority());
        return newNetwork;
    }

    /**
     * Adds a list of network elements to this network.
     * Used in copy paste and tuned to that usage.
     *
     * @param toAdd list of objects to add.
     */
    public void addObjects(final List<?> toAdd) {
        for (Object object : toAdd) {
            if (object instanceof Neuron) {
                Neuron neuron = (Neuron) object;
                addNeuron(neuron);
            } else if (object instanceof Synapse) {
                Synapse synapse = (Synapse) object;
                addSynapse(synapse);
            } else if (object instanceof Network) {
                Network net = (Network) object;
                addNetwork(net);
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
            net = net.getParentNetwork();
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
    public List<? extends Neuron> getNeuronList() {
        return this.neuronList;
    }

    /**
     * @return List of synapses in network.
     */
    public ArrayList<Synapse> getSynapseList() {
        return this.synapseList;
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
        return neuronList.get(index);
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
     * Find neurons with a given label.
     *
     * @param label label to search for.
     * @return list of neurons with that label found, null otherwise
     */
    public List<Neuron> getNeuronsByLabel(final String label) {
        ArrayList<Neuron> returnList = new ArrayList<Neuron>();
        for (Neuron neuron : getFlatNeuronList()) {
            if (neuron.getLabel().equalsIgnoreCase(label)) {
                returnList.add(neuron);
            }
        }
        return returnList;
    }

    /**
     * Find a group with a given string id.
     *
     * @param id id to search for.
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
     * @param label label to search for.
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
     * TODO: getFlatGroupList? (not currently adding groups to subnets)
     *
     * @return the groupList
     */
    public List<? extends Group> getGroupList() {
        return groupList;
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
        neuron.setParentNetwork(this);
        neuronList.add(neuron);
        if ((rootNetwork != null)) {
            neuron.setId(getRootNetwork().getNeuronIdGenerator().getId());
            rootNetwork.fireNeuronAdded(neuron);
        }
        neuron.init();
    }

    /**
     * @return Number of weights in network
     */
    public int getSynapseCount() {
        return synapseList.size();
    }

    /**
     * @param index Number of weight in array list.
     * @return Weight at the point of the indesx
     */
    public Synapse getSynapse(final int index) {
        return synapseList.get(index);
    }

    /**
     * Adds a weight to the neuron network, where that weight already has designated
     * source and target neurons.
     *
     * @param synapse the weight object to add
     */
    public void addSynapse(final Synapse synapse) {
        synapse.setParentNetwork(this);

        synapse.initSpikeResponder();
        synapseList.add(synapse);
        if ((rootNetwork != null)) {
            synapse.setId(rootNetwork.getSynapseIdGenerator().getId());
            rootNetwork.fireSynapseAdded(synapse);
        }
    }


    /**
     * Calls {@link Neuron#update} for each neuron.
     */
    public void updateAllNeurons() {

        if (rootNetwork.getClampNeurons()) {
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

        if (rootNetwork.getClampWeights()) {
            return;
        }

        // No Buffering necessary because the values of weights don't depend on one another
        for (Synapse s : synapseList) {
            s.update();
        }
    }

    /**
     * Calls {@link Neuron#checkBounds} for each neuron, which makes sure the neuron has not
     * exceeded its upper bound or gone below its lower bound.   TODO: Add or replace with
     * normalization within bounds?
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
                List<Synapse> fanOut = toDelete.getFanOut();
                Synapse s = fanOut.get(fanOut.size() - 1);
                deleteSynapse(s);
            }

            // Remove incoming synapses
            while (toDelete.getFanIn().size() > 0) {
                List<Synapse> fanIn = toDelete.getFanIn();
                Synapse s = fanIn.get(fanIn.size() - 1);
                deleteSynapse(s);
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
                parent.getParentNetwork().deleteNetwork(parent);
            }
        }
    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete the weight to delete
     * @param notify whether to fire a synapse deleted event
     */
    public void deleteSynapse(final Synapse toDelete) {

        Group group = getRootNetwork().containedInGroup(toDelete);
        if (group != null) {
            group.deleteSynapse(toDelete);
            if (group.isEmpty()) {
                this.getRootNetwork().deleteGroup(group);
            }
        }
        this.getRootNetwork().fireSynapseDeleted(toDelete);
        toDelete.delete();
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    public void clearActivations() {
        setActivations(0);
    }

    /**
     * Sets all neurons to a specified value.
     *
     * @param value value to set
     */
    public void setActivations(final double value) {
        for (Neuron neuron : this.getFlatNeuronList()) {
            neuron.setActivation(value);
        }
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
     * @param value value to set
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
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : neuronList) {
            if (neuron instanceof BiasedNeuron) {
                ((BiasedNeuron) neuron).setBias((upper - lower) * Math.random()
                        + lower);
            }
        }
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
        return new BigDecimal(value).setScale(decimalPlace, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String ret = new String();

        for (Neuron n : neuronList) {
            ret += (getIndents() + n + "\n");
        }

        if (synapseList.size() > 0) {
            for (int i = 0; i < synapseList.size(); i++) {
                Synapse tempRef = (Synapse) synapseList.get(i);
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
    public void setNeuronList(final ArrayList<Neuron> neuronList) {
        this.neuronList = neuronList;
    }

    /**
     * @param weightList The weightList to set.
     */
    public void setWeightList(final ArrayList<Synapse> weightList) {
        this.synapseList = weightList;
    }

    /**
     * Add an array of neurons and set their parents to this.
     *
     * @param neurons list of neurons to add
     */
    protected void addNeuronList(final ArrayList<Neuron> neurons) {
        for (Neuron n : neurons) {
            n.setParentNetwork(this);
            addNeuron(n);
        }
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
    public static Synapse getSynapse(final Neuron src, final Neuron tar) {
        for (Synapse s : src.getFanOut()) {
            if (s.getTarget() == tar) {
                return s;
            }
        }

        return null;
    }

    /**
     * Change neuron type.
     *
     * @param oldNeuron out with the old
     * @param newNeuron in with the new...
     */
    public void changeNeuronType(final Neuron oldNeuron, final Neuron newNeuron) {

        // TODO: The code for changing neuron and synapse types may need to be
        // refactored. Lots of stuff has to happen (here, in the listeners, and
        // in the source for Neuron and Synapses themselves; see their duplicate()
        // method). There is redundant code. Whenever I come back to this stuff I have to spend
        // some time figuring out what's going on. And this is a place bugs periodically emerge. When
        // features are added it's easy to forget about this stuff. So, for these reasons, it
        // smells like a refactor might be useful. Then again it's working, at least for now.
        
        newNeuron.setId(oldNeuron.getId());
        newNeuron.setParentNetwork(this);

        rootNetwork.fireNeuronTypeChanged(oldNeuron, newNeuron);

        for (Synapse s : new ArrayList<Synapse>(oldNeuron.getFanIn())) {
            s.setTarget(newNeuron);
        }

        for (Synapse s : new ArrayList<Synapse>(oldNeuron.getFanOut())) {
            s.setSource(newNeuron);
        }
        neuronList.remove(oldNeuron);
        addNeuron(newNeuron);
        for (Neuron neuron : getNeuronList()) {
            neuron.setParentNetwork(this);
        }

        // If the neuron is a spiker, add spikeResponders to target weights, else remove them
        for (Synapse s : newNeuron.getFanOut()) {
            s.initSpikeResponder();
        }
        rootNetwork.updateTimeType();
    }

    /**
     * Change synapse type; replace one synapse with another.
     * deletes the old synapse
     *
     * @param oldSynapse out with the old
     * @param newSynapse in with the new...
     */
    public void changeSynapseType(final Synapse oldSynapse, final Synapse newSynapse) {
        
        // Initialize the new synapse
        newSynapse.setTarget(oldSynapse.getTarget());
        newSynapse.setSource(oldSynapse.getSource());
        newSynapse.setId(oldSynapse.getId());
        newSynapse.setParentNetwork(this);
        newSynapse.initSpikeResponder();
        synapseList.add(newSynapse);
        
        // Fire event now, before the old synapse is deleted; otherwise
        //      problems in listeners
        rootNetwork.fireSynapseTypeChanged(oldSynapse, newSynapse);

        // Delete the old synapse
        oldSynapse.delete();

    }

    /**
     * Gets the synapse at particular point.
     * @param i Neuron number
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
    public Network getParentNetwork() {
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
     */
    public void addNetwork(final Network n) {
        networkList.add(n);
        n.setParentNetwork(this);
        n.setRootNetwork(rootNetwork);
        getRootNetwork().fireSubnetAdded(n);
        n.setId(getRootNetwork().getNetworkIdGenerator().getId());
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
        if (toDelete.getParentNetwork() != null) {
            toDelete.getParentNetwork().getNetworkList().remove(toDelete);
        }

        //If we just removed the last neuron of a network, remove that network
        Network parent = toDelete.getParentNetwork();
        if (!(parent instanceof RootNetwork)) {
            if (parent.isEmpty()) {
                parent.getParentNetwork().deleteNetwork(parent);
            }
        }

        // Notify listeners
        rootNetwork.fireSubnetDeleted(toDelete);
    }

    /**
     * Add a new group of network elements.
     *
     * @param group group of network elements
     */
    public void addGroup(final Group group) {
        group.setParent(rootNetwork);
        if ((rootNetwork != null)) {
            group.setId(getRootNetwork().getGroupIdGenerator().getId());
            groupList.add(group);
            rootNetwork.fireGroupAdded(group);
        }
    }

    /**
     * Remove the specified group.
     *
     * @param toDelete the group to delete.
     */
    public void deleteGroup(final Group toDelete) {
        rootNetwork.fireGroupDeleted(toDelete);
        groupList.remove(toDelete);
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
    public void addNetworkList(final ArrayList<Network> networks) {
        for (Network n : networks) {
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
     * Create "flat" list of neurons, which includes the top-level neurons plus
     * all subnet neurons.
     *
     * @return the flat list
     */
    public ArrayList<Neuron> getFlatNeuronList() {
        ArrayList<Neuron> ret = new ArrayList<Neuron>();
        ret.addAll(neuronList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList<Neuron> toAdd;

            toAdd = net.getFlatNeuronList();

            ret.addAll(toAdd);
        }

        return ret;
    }

    /**
     * Create "flat" list of synapses, which includes the top-level synapses
     * plus all subnet synapses.
     *
     * @return the flat list
     */
    public ArrayList<Synapse> getFlatSynapseList() {
        ArrayList<Synapse> ret = new ArrayList<Synapse>();
        ret.addAll(synapseList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList<Synapse> toAdd;

            toAdd = net.getFlatSynapseList();

            ret.addAll(toAdd);
        }

        return ret;
    }

    /**
     * Returns a list containing all neurons, synapses and networks.
     *
     * @return A list containing all neurons, synapses and networks.
     */
    public ArrayList<Object> getObjectList() {
        ArrayList<Object> ret = new ArrayList<Object>();
        ret.addAll(getNeuronList());
        ret.addAll(getSynapseList());
        ret.addAll(getNetworkList());
        return ret;
    }

    /**
     * Create "flat" list of all subnetworks.
     *
     * @return the flat list
     */
    public ArrayList<Network> getFlatNetworkList() {
        ArrayList<Network> ret = new ArrayList<Network>();
        ret.addAll(networkList);

        for (int i = 0; i < networkList.size(); i++) {
            Network net = (Network) networkList.get(i);
            ArrayList<Network> toAdd;

            toAdd = net.getFlatNetworkList();

            ret.addAll(toAdd);
        }

        return ret;
    }

    /**
     * @return Returns the rootNetwork.
     */
    public RootNetwork getRootNetwork() {
        return rootNetwork;
    }

    /**
     * Sets the root network.
     *
     * @param rootNetwork The rootNetwork to set.
     */
    public void setRootNetwork(final RootNetwork rootNetwork) {
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
