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

import java.util.ArrayList;
import java.util.Collection;

import org.simnet.util.UniqueID;


/**
 * <b>Network</b> provides core neural network functionality and is the the main API for external calls. Network
 * objects are sets of neurons and  weights connecting them. Much of the  actual update and  learning logic occurs
 * (currently) in the individual nodes.
 */
public abstract class Network {
    //
    // TODO:     Add a command-line interface for creating and testing simple networks
    //             independently of the GUI.
    //
    // TODO:     Make saving /opening of files possible from here.  Change file format
    //            so that first character indicates what kind of object is being read.
    //
    protected String id = null;
    public static final int DISCRETE = 0;
    public static final int CONTINUOUS = 1;
    protected ArrayList neuronList = new ArrayList();
    protected ArrayList weightList = new ArrayList();
    protected double time = 0; // Keeps track of time
    private double timeStep = .01;
    private int timeType = DISCRETE;
    private boolean roundOffActivationValues = false; // Whether to round off neuron values
    private int precision = 0; // Degree to which to round off values
    private Network parentNet = null; //Only useed for sub-nets of complex networks which have parents
    private boolean clampWeights = false; // Used to temporarily turn off all learning

    public Network() {
        id = UniqueID.get();
    }

    public abstract void update();


    /**
     * Initialize the network.
     */
    public void init() {
        initWeights();
        initParents();
    }

    /**
     * Updates weights with fan-in.  Used when weights have been added.
     */
    public void initWeights() {
        //initialize fan-in and fan-out on each neuron
        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = getWeight(i);
            w.init();
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

        while (net != null) {
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

    public Collection getNeuronList() {
        return this.neuronList;
    }

    public Collection getWeightList() {
        return this.weightList;
    }


    public int getNeuronCount() {
        return neuronList.size();
    }

    public Neuron getNeuron(final int index) {
        return (Neuron) neuronList.get(index);
    }

    public void addNeuron(final Neuron neuron) {
        neuron.setParentNetwork(this);
        neuronList.add(neuron);
    }

    public int getWeightCount() {
        return weightList.size();
    }

    public Synapse getWeight(final int index) {
        return (Synapse) weightList.get(index);
    }

    public double getTime() {
        return time;
    }

    public void setTime(final double i) {
        time = i;
    }

    /**
     * Adds a weight to the neuron network, where that weight already has designated source and target neurons
     *
     * @param weight the weight object to add
     */
    public void addWeight(final Synapse weight) {
        Neuron source = (Neuron) weight.getSource();
        source.addTarget(weight);

        Neuron target = (Neuron) weight.getTarget();
        target.addSource(weight);
        weight.initSpikeResponder();
        weightList.add(weight);
    }

    /**
     * Calls {@link Neuron#update} for each neuron
     */
    public void updateAllNeurons() {

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
     * Calls {@link Weight#update} for each weight
     */
    public void updateAllWeights() {

        if (clampWeights == true ) return;

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

    //Round activatons off to integers; for testing
    public void roundAll() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.round(precision);
        }
    }

    /**
     * Deletes a neuron from the network
     *
     * @param toDelete neuron to delete
     */
    public void deleteNeuron(final Neuron toDelete) {
        if (neuronList.contains(toDelete)) {
            for (int i = 0; i < toDelete.getFanOut().size(); i++) {
                Synapse w = (Synapse) toDelete.getFanOut().get(i);
                deleteWeight(w);
            }

            toDelete.getFanOut().clear();

            for (int i = 0; i < toDelete.getFanIn().size(); i++) {
                Synapse w = (Synapse) toDelete.getFanIn().get(i);
                deleteWeight(w);
            }

            toDelete.getFanIn().clear();
            neuronList.remove(toDelete);
            neuronList.remove(toDelete);
        }
    }

    /**
     * Wipe out the whole network
     */
    public void deleteAllNeurons() {
        weightList.clear();
        neuronList.clear();
    }

    /**
     * Delete a specified weight
     *
     * @param toDelete the  Weight to delete
     */
    public void deleteWeight(final Synapse toDelete) {
        toDelete.getSource().getFanOut().remove(toDelete);
        toDelete.getTarget().getFanIn().remove(toDelete);
        weightList.remove(toDelete);

        if (this.getNetworkParent() != null) {
            getNetworkParent().deleteWeight(toDelete);
        }
    }

    /**
     * Set the activation level of all neurons to zero
     */
    public void setNeuronsToZero() {
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
     * Sets all weight values to zero, effectively eliminating them
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
     * Round a value off to indicated number of decimal places
     *
     * @param value value to round off
     * @param decimalPlace degree of precision
     *
     * @return rounded number
     */
    public static double round(final double value, final int decimalPlace) {
        double power_of_ten = 1;

        while (decimalPlace-- > 0) {
            power_of_ten *= 10.0;
        }

        return Math.round(value * power_of_ten) / power_of_ten;
    }

    /**
     * Sends relevant information about the network to standard output.
     */
    public void debug() {
        if (neuronList.size() > 0) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron tempRef = (Neuron) neuronList.get(i);
                System.out.println(getIndents() + "Neuron " + tempRef.getId() + ": " + tempRef.getActivation());
            }
        }

        if (weightList.size() > 0) {
            for (int i = 0; i < weightList.size(); i++) {
                Synapse tempRef = (Synapse) weightList.get(i);
                System.out.print(getIndents() + "Weight [" + i + "]: " + tempRef.getStrength() + ".");
                System.out.println("  Connects neuron " + tempRef.getSource().getId() + " to neuron "
                                   + tempRef.getTarget().getId());
            }
        }
    }

    public int getPrecision() {
        return precision;
    }

    public boolean getRoundingOff() {
        return roundOffActivationValues;
    }

    public void setPrecision(final int i) {
        precision = i;
    }

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
        System.out.println("-->" + neuronList.size());
        this.neuronList = neuronList;
    }

    /**
     * @param weightList The weightList to set.
     */
    public void setWeightList(final ArrayList weightList) {
        this.weightList = weightList;
    }

    /**
     * Add an array of neurons and set their parents to this
     *
     * @param neurons list of neurons to add
     */
    public void addNeuronList(final ArrayList neurons) {
        for (int i = 0; i < neurons.size(); i++) {
            Neuron n = (Neuron) neurons.get(i);
            n.setParentNetwork(this);
            neuronList.add(n);
        }
    }

    public void setUpperBounds(final double u) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(u);
        }
    }

    public void setLowerBounds(final double l) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(l);
        }
    }

    /**
     * Returns a reference to the synapse connecting two neurons, or null if there is none
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
     * Replace one neuron with another
     *
     * @param old_neuron out with the old
     * @param new_neuron in with the new...
     */
    public static void changeNeuron(final Neuron old_neuron, final Neuron new_neuron) {
        new_neuron.setId(old_neuron.getId());
        new_neuron.setInput(old_neuron.isInput());
        new_neuron.setFanIn(old_neuron.getFanIn());
        new_neuron.setFanOut(old_neuron.getFanOut());
        new_neuron.setParentNetwork(old_neuron.getParentNetwork());

        for (int i = 0; i < old_neuron.getFanIn().size(); i++) {
            ((Synapse) old_neuron.getFanIn().get(i)).setTarget(new_neuron);
        }

        for (int i = 0; i < old_neuron.getFanOut().size(); i++) {
            ((Synapse) old_neuron.getFanOut().get(i)).setSource(new_neuron);
        }

        old_neuron.getParentNetwork().getNeuronList().remove(old_neuron);
        old_neuron.getParentNetwork().getNeuronList().add(new_neuron);
        new_neuron.getParentNetwork().initParents();

        // If the neuron is a spiker, add spikeResponders to target weights, else remove them
        for (int i = 0; i < new_neuron.getFanOut().size(); i++) {
            ((Synapse) new_neuron.getFanOut().get(i)).initSpikeResponder();
        }
    }

    /**
     * Change synapse type / replace one synapse with another
     *
     * @param old_synapse out with the old
     * @param new_synapse in with the new...
     */
    public static void changeSynapse(final Synapse old_synapse, final Synapse new_synapse) {
        new_synapse.setTarget(old_synapse.getTarget());
        new_synapse.setSource(old_synapse.getSource());
        new_synapse.getTarget().getParentNetwork().deleteWeight(old_synapse);
        new_synapse.getTarget().getParentNetwork().addWeight(new_synapse);
    }

    public void initParents() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = getNeuron(i);
            n.setParentNetwork(this);
        }
    }

    /**
     * If there is a single continuous neuron in the network, consider this a continuous network
     */
    public void updateTimeType() {
        timeType = DISCRETE;

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = getNeuron(i);

            if (n.getTimeType() == CONTINUOUS) {
                timeType = CONTINUOUS;
            }
        }
    }

    /**
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete
     * network
     */
    public void updateTime() {
        if (timeType == CONTINUOUS) {
            time += this.getTimeStep();
        } else {
            time += 1;
        }
    }

    //TODO: Either fix this or make its assumptions explicit
    public Synapse getWeight(final int i, final int j) {
        return (Synapse) getNeuron(i).getFanOut().get(j);
    }

    /**
     * @return Returns the parentNet.
     */
    public Network getNetworkParent() {
        return parentNet;
    }

    /**
     * @param parentNet The parentNet to set.
     */
    public void setNetworkParent(final Network parentNet) {
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

    public static String[] getUnits() {
        String[] units = {"Seconds", "Iterations" };

        return units;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTimeLabel() {
        if (timeType == DISCRETE) {
            return getUnits()[1];
        } else {
            return getUnits()[0];
        }
    }

    public int getTimeType() {
        return timeType;
    }

    public void setTimeType(final int timeType) {
        this.timeType = timeType;
    }

    public boolean getClampWeights() {
        return clampWeights;
    }

    public void setClampWeights(final boolean clampWeights) {
        this.clampWeights = clampWeights;
    }
}
