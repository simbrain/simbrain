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
import java.util.Iterator;

import org.simbrain.gauge.GaugeSource;
import org.simbrain.util.SimbrainMath;
import org.simbrain.util.Utils;
import org.simbrain.world.Agent;
import org.simnet.NetworkPreferences;
import org.simnet.coupling.MotorCoupling;
import org.simnet.coupling.SensoryCoupling;
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.DecayNeuron;
import org.simnet.neurons.ExponentialDecayNeuron;
import org.simnet.neurons.IACNeuron;
import org.simnet.neurons.IntegrateAndFireNeuron;
import org.simnet.neurons.IzhikevichNeuron;
import org.simnet.neurons.LMSNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.LogisticNeuron;
import org.simnet.neurons.NakaRushtonNeuron;
import org.simnet.neurons.PointNeuron;
import org.simnet.neurons.RandomNeuron;
import org.simnet.neurons.RunningAverageNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.neurons.SinusoidalNeuron;
import org.simnet.neurons.StochasticNeuron;
import org.simnet.neurons.TemporalDifferenceNeuron;
import org.simnet.neurons.ThreeValuedNeuron;
import org.simnet.neurons.TraceNeuron;
import org.simnet.synapses.SignalSynapse;
import org.simnet.util.UniqueID;


/**
 * <b>Neuron</b> represents a node in the neural network.  Most of the "logic" of the neural network occurs here, in
 * the update function.  Subclasses must override update and duplicate (for copy / paste) and cloning generally.
 */
public abstract class Neuron implements GaugeSource {

    /** A unique id for this neuron. */
    private String id = null;

    /** Activation value of the neuron.  The main state variable. */
    protected double activation = NetworkPreferences.getActivation();

    /** Minimum value this neuron can take. */
    protected double lowerBound = NetworkPreferences.getNrnLowerBound();

    /** Maximum value  this neuron can take. */
    protected double upperBound = NetworkPreferences.getNrnUpperBound();

    /** Amount by which to increment or decrement neuron. */
    private double increment = NetworkPreferences.getNrnIncrement();

    /** Represents a coupling between this neuron and an external source of "sensory" input. */
    private SensoryCoupling sensoryCoupling;

    /** Represents a coupling between this neuron and an external source of "motor" output. */
    private MotorCoupling motorCoupling;

    /** Temporary activation value. */
    private double buffer = 0;

    /** Value of any external inputs to neuron. */
    private double inputValue = 0;

    /** Reference to network this neuron is part of. */
    private Network parent = null;

    /** List of synapses this neuron attaches to. */
    protected ArrayList<Synapse> fanOut = new ArrayList<Synapse>();

    /** List of synapses attaching to this neuron. */
    protected ArrayList<Synapse> fanIn = new ArrayList<Synapse>();

    /** x-coordinate of this neuron in 2-space. */
    private double x;

    /** y-coordinate of this neuron in 2-space. */
    private double y;

    /** Sequence in which the update function should be called
     *  for this neuron. By default, this is set to 0 for all
     *  the neurons. If you want a subset of neurons to fire
     *  before other neurons, assign it a higher priority value.
     */
    private int updatePriority = 0;

    /**
     * Target / reward value (not all neurons will use this).
     * "Value" addded to disambiguate from synapse's target neuron.
     */
    private double targetValue = 0;

    /** Signal synapse.  Used for neurons with target values. */
    private SignalSynapse targetValueSynapse = null;

    /** List of neuron types. */
    private static String[] typeList = {AdditiveNeuron.getName(),
            BinaryNeuron.getName(), ClampedNeuron.getName(),
            DecayNeuron.getName(), IACNeuron.getName(),
            IntegrateAndFireNeuron.getName(), IzhikevichNeuron.getName(),
            LinearNeuron.getName(), LMSNeuron.getName(), LogisticNeuron.getName(),
            NakaRushtonNeuron.getName(), PointNeuron.getName(), RandomNeuron.getName(),
            RunningAverageNeuron.getName(), SigmoidalNeuron.getName(), SinusoidalNeuron.getName(),
            StochasticNeuron.getName(), ThreeValuedNeuron.getName(),
            TraceNeuron.getName()};

    /**
     * Default constructor needed for external calls which create neurons then
     * set their parameters.
     */
    public Neuron() {
    }

    /**
     * Initialize a neuron with spatial coordinates.
     *
     * @param x x coordinate of new neuron.
     * @param y y coordinate of new neuron.
     */
    public Neuron(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type.  Only values common to different types of neuron
     * are copied.
     *
     * @param n Neuron
     */
    public Neuron(final Neuron n) {
        setParentNetwork(n.getParentNetwork());
        setActivation(n.getActivation());
        setUpperBound(n.getUpperBound());
        setLowerBound(n.getLowerBound());
        setIncrement(n.getIncrement());
        setInputValue(n.getInputValue());
        setX(n.getX());
        setY(n.getY());
        setTargetValueSynapse(n.getTargetValueSynapse());
    }

    /**
     * Completes duplication of this neuron; used in copy/paste.
     * This does not produce the copy!
     * Matching source and targets is up to you!
     *
     * @param n Neuron to duplicate
     * @return duplicate neuron
     */
    public Neuron duplicate(Neuron n) {
        n.setParentNetwork(this.getParentNetwork());
        n.setActivation(this.getActivation());
        n.setUpperBound(this.getUpperBound());
        n.setLowerBound(this.getLowerBound());
        n.setIncrement(this.getIncrement());
        n.setX(this.getX());
        n.setY(this.getY());
        n.setTargetValueSynapse(this.getTargetValueSynapse());

        return n;
    }

    /**
     * @return the time type.
     */
    public abstract int getTimeType();

    /**
     * @return a duplicate neuron.
     */
    public abstract Neuron duplicate();

    /**
     * Updates network with attached world.
     */
    public abstract void update();


    /**
     * Perform any initialization required when creating a neuron, but after the parent network has been added.
     */
    public void init() {
        this.setId(UniqueID.get());
    }

    /**
     * Perform intialization required after opening saved networks.
     */
    public void initCastor() {
        if (getSensoryCoupling() != null) {
            Agent a = getParentNetwork().getRootNetwork().getWorkspace().findMatchingAgent(getSensoryCoupling());

            if (a != null) {
                setSensoryCoupling(new SensoryCoupling(a, this, getSensoryCoupling().getSensorArray()));
            }
        }

        if (getMotorCoupling() != null) {
            Agent a = getParentNetwork().getRootNetwork().getWorkspace().findMatchingAgent(getMotorCoupling());

            if (a != null) {
                setMotorCoupling(new MotorCoupling(a, this, getMotorCoupling().getCommandArray()));
            }
        }
    }

    /**
     * Utility method to see if an array of names (from the world) contains a target string.
     *
     * @param src the list of Strings
     * @param target the string to check for
     *
     * @return whether src is contained in target or not
     */
    public boolean containsString(final ArrayList src, final String target) {
        boolean ret = false;
        java.util.Iterator it = src.iterator();

        while (it.hasNext()) {
            if (target.equals((String) it.next())) {

            }

            ret = true;
        }

        return ret;
    }

    /**
     * Sets the activation of the neuron.
     * @param act Activation
     */
    public void setActivation(final double act) {
        activation = act;
    }

    /**
     * @return the level of activation.
     */
    public double getActivation() {
        return activation;
    }

    /** @see GaugeSource */
    public double getGaugeValue() {
        return getActivation();
    }

    /**
     * @return ID of neuron.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of the neuron.
     * @param theName Neuron id
     */
    public void setId(final String theName) {
        id = theName;
    }

    /**
     * @return upper bound of the neuron.
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the upper bound of the neuron.
     * @param d Value to set upper bound
     */
    public void setUpperBound(final double d) {
        upperBound = d;
    }

    /**
     * @return lower bound of the neuron.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower bound of the neuron.
     * @param d Value to set lower bound
     */
    public void setLowerBound(final double d) {
        lowerBound = d;
    }

    /**
     * @return the neuron increment.
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * Sets the neuron increment.
     * @param d Value to set increment
     */
    public void setIncrement(final double d) {
        increment = d;
    }

    /**
     * @return the fan in array list.
     */
    public ArrayList<Synapse> getFanIn() {
        return fanIn;
    }

    /**
     * @return the fan out array list.
     */
    public ArrayList<Synapse> getFanOut() {
        return fanOut;
    }

    /**
     * @param fanIn The fanIn to set.
     */
    public void setFanIn(final ArrayList<Synapse> fanIn) {
        this.fanIn = fanIn;
    }

    /**
     * @param fanOut The fanOut to set.
     */
    public void setFanOut(final ArrayList<Synapse> fanOut) {
        this.fanOut = fanOut;
    }

    /**
     * Increment this neuron by increment.
     */
    public void incrementActivation() {
        if (activation < upperBound) {
            activation += increment;
        }
        this.getParentNetwork().getRootNetwork().fireNeuronChanged(null, this);
    }

    /**
     * Decrement this neuron by increment.
     */
    public void decrementActivation() {
        if (activation > lowerBound) {
            activation -= increment;
        }
        this.getParentNetwork().getRootNetwork().fireNeuronChanged(null, this);
    }

    /**
     * Connect this neuron to target neuron via a weight.
     *
     * @param target the connnection between this neuron and a target neuron
     */
    public void addTarget(final Synapse target) {
        fanOut.add(target);
    }

    /**
     * Connect this neuron to source neuron via a weight.
     *
     * @param source the connnection between this neuron and a source neuron
     */
    public void addSource(final Synapse source) {
        fanIn.add(source);
    }

    /**
     * Add specified amount of activation to this neuron.
     *
     * @param amount amount to add to this neuron
     */
    public void addActivation(final double amount) {
        activation += amount;
    }

    /**
     * Sums the weighted signals that are sent to this node.
     *
     * @return weighted input to this node
     */
    public double getWeightedInputs() {
        double wtdSum = inputValue;
        if (fanIn.size() > 0) {
            for (int j = 0; j < fanIn.size(); j++) {
                Synapse w = (Synapse) fanIn.get(j);
                wtdSum += w.getValue();
            }
        }

        return wtdSum;
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomize() {
        setActivation(((upperBound - lowerBound) * Math.random()) + lowerBound);
        this.getParentNetwork().getRootNetwork().fireNeuronChanged(null, this);
//        if (getBias() != 0) {
//            setBias((upperBound - lowerBound) * Math.random() + lowerBound);
//        }
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomizeBuffer() {
        setBuffer(((upperBound - lowerBound) * Math.random()) + lowerBound);
    }

    /**
     * Update all neurons n this neuron is connected to, by adding current activation times the connection-weight  NOT
     * CURRENTLY USED.
     */
    public void updateConnectedOutward() {
        // Update connected weights
        if (fanOut.size() > 0) {
            for (int j = 0; j < fanOut.size(); j++) {
                Synapse w = (Synapse) fanOut.get(j);
                Neuron target = w.getTarget();
                target.setActivation(w.getStrength() * activation);
                target.checkBounds();
            }
        }
    }

    /**
     * Check if this neuron is connected to a given weight.
     *
     * @param w weight to check
     *
     * @return true if this neuron has w in its fan_in or fan_out
     */
    public boolean connectedToWeight(final Synapse w) {
        if (fanOut.size() > 0) {
            for (int j = 0; j < fanOut.size(); j++) {
                Synapse outW = (Synapse) fanOut.get(j);

                if (w.equals(outW)) {
                    return true;
                }
            }
        }

        if (fanIn.size() > 0) {
            for (int j = 0; j < fanIn.size(); j++) {
                Synapse inW = (Synapse) fanIn.get(j);

                if (w.equals(inW)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Round the activation level of this neuron off to a specified precision.
     *
     * @param precision precision to round this neuron's activaion off to
     */
    public void round(final int precision) {
        setActivation(Network.round(getActivation(), precision));
    }

    /**
     * If activation is above or below its bounds set it to those bounds.
     */
    public void checkBounds() {
        if (activation > upperBound) {
            activation = upperBound;
        }

        if (activation < lowerBound) {
            activation = lowerBound;
        }
    }

    /**
     * If value is above or below its bounds set it to those bounds.
     * @param value Value to check
     * @return clip
     */
    public double clip(final double value) {
        double val = value;
        if (val > upperBound) {
            val = upperBound;
        }

        if (val < lowerBound) {
            val = lowerBound;
        }

        return val;
    }

    /**
     * Sends relevant information about the network to standard output. TODO: Change to toString()
     */
    public void debug() {
        System.out.println("neuron " + id);
        System.out.println("fan in");

        for (int i = 0; i < fanIn.size(); i++) {
            Synapse tempRef = (Synapse) fanIn.get(i);
            System.out.println("fanIn [" + i + "]:" + tempRef);
        }

        System.out.println("fan out");

        for (int i = 0; i < fanOut.size(); i++) {
            Synapse tempRef = (Synapse) fanOut.get(i);
            System.out.println("fanOut [" + i + "]:" + tempRef);
        }
    }

    /**
     * @return reference to the Network object this neuron is part of
     */
    public Network getParentNetwork() {
        return parent;
    }

    /**
     * @param network reference to the Network object this neuron is part of.
     */
    public void setParentNetwork(final Network network) {
        parent = network;
    }

    /**
     * Temporary buffer which can be used for algorithms which should not  depend on the order in which  neurons are
     * updated.
     *
     * @param d temporary value
     */
    public void setBuffer(final double d) {
        buffer = d;
    }

    /**
     * @return Returns the current value in the buffer.
     */
    public double getBuffer() {
        return buffer;
    }

    /**
     * @return Returns the inputValue.
     */
    public double getInputValue() {
        return inputValue;
    }

    /**
     * @param inputValue The inputValue to set.
     */
    public void setInputValue(final double inputValue) {
        this.inputValue = inputValue;
    }

    /**
     * @return the name of the class of this network.
     */
    public String getType() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * @return Returns the typeList.
     */
    public static String[] getTypeList() {
        return typeList;
    }

    /**
     * @param typeList The typeList to set.
     */
    public static void setTypeList(final String[] typeList) {
        Neuron.typeList = typeList;
    }

    /**
     * Helper function for combo boxes.  Associates strings with indices.
     * @param type Type of neuron to get index
     * @return neuron type index
     */
    public static int getNeuronTypeIndex(final String type) {
        for (int i = 0; i < typeList.length; i++) {
            if (type.equals(typeList[i])) {
                return i;
            }
        }

        return 0;
    }

    /**
     * Returns the sum of the strengths of the weights attaching to this neuron.
     *
     * @return the sum of the incoming weights to this nueron.
     */
    public double getSummedIncomingWeights() {
        double ret = 0;

        for (int i = 0; i < fanIn.size(); i++) {
            Synapse tempRef = (Synapse) fanIn.get(i);
            ret += tempRef.getStrength();
        }

        return ret;
    }

    /**
     * Returns the number of neurons attaching to this one which have activity above
     * a specified threshold.
     *
     * @param threshold value above which neurons are considered "active."
     * @return number of "active" neurons
     */
    public int getNumberOfActiveInputs(final int threshold) {
        int numActiveLines = 0;
        // Determine number of active (greater than 0) input lines
        for (Iterator j = getFanIn().iterator(); j.hasNext(); ) {
            Synapse incoming = (Synapse) j.next();
            if (incoming.getSource().getActivation() > threshold) {
                numActiveLines++;
            }
        }
        return numActiveLines;
    }

    /**
     * @return the average activation of neurons connecting to this neuron
     */
    public double getAverageInput() {
        return getTotalInput() / fanIn.size();
    }

    /**
     * @return the total activation of neurons connecting to this neuron
     */
    public double getTotalInput() {
        double ret = 0;

        for (int i = 0; i < fanIn.size(); i++) {
            ret += ((Synapse) fanIn.get(i)).getSource().getActivation();
        }

        return ret;
    }

    /**
     * @return Returns the motorCoupling.
     */
    public MotorCoupling getMotorCoupling() {
        return motorCoupling;
    }

    /**
     * @param motorCoupling The motorCoupling to set.
     */
    public void setMotorCoupling(final MotorCoupling motorCoupling) {
        this.motorCoupling = motorCoupling;
        if (getParentNetwork() != null) {
            getParentNetwork().getRootNetwork().fireCouplingChanged(this);
        }
    }

    /**
     * @return Returns the sensoryCoupling.
     */
    public SensoryCoupling getSensoryCoupling() {
        return sensoryCoupling;
    }


    /**
     * @param sc the new SensoryCoupling object.
     */
    public void setSensoryCoupling(final SensoryCoupling sc) {
        inputValue = 0;
        if (sc == null) {
            // If there was a different coupling previously, check whether to stop
            //   observing the coupled world
            if (sensoryCoupling != null) {
                if (sensoryCoupling.getWorld() != null) {
                    getParentNetwork().getRootNetwork().updateWorldListeners(sensoryCoupling.getWorld());
                }
            }
            sensoryCoupling = sc;
        } else {
            sensoryCoupling = sc;
            if (sensoryCoupling.getWorld() != null) {
                sensoryCoupling.getWorld().addWorldListener(getParentNetwork().getRootNetwork());
            }
        }
        if (getParentNetwork() != null) {
            getParentNetwork().getRootNetwork().fireCouplingChanged(this);
        }
    }

//    /**
//     * TODO:
//     * Check if any couplings attach to this world and if there are no none, remove the listener.
//     * @param world
//     */
//    private void removeWorldListener(World world) {
//
//    }

    /**
     * Return true if this neuron has a motor coupling attached.
     *
     * @return true if this neuron has a motor coupling attached
     */
    public boolean isOutput() {
        return (motorCoupling != null);
    }

    /**
     * Return true if this neuron has a sensory coupling attached.
     *
     * @return true if this neuron has a sensory coupling attached
     */
    public boolean isInput() {
        return (sensoryCoupling != null);
    }

    /**
     * True if the synapse is connected to this neuron, false otherwise.
     * @param s the synapse to check.
     * @return true if synapse is connected, false otherwise.
     */
    public boolean isConnected(final Synapse s) {
        return (fanIn.contains(s) || fanOut.contains(s));
     }

    /**
     * @return Returns the x coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * @param x The x coordinate to set.
     */
    public void setX(final double x) {
        this.x = x;
        if (this.getParentNetwork() != null) {
            if (this.getParentNetwork().getRootNetwork() != null) {
                this.getParentNetwork().getRootNetwork().fireNeuronMoved(this);
            }
        }
    }

    /**
     * @param y The y coordinate to set.
     */
    public void setY(final double y) {
        this.y = y;
        if (this.getParentNetwork() != null) {
            if (this.getParentNetwork().getRootNetwork() != null) {
                this.getParentNetwork().getRootNetwork().fireNeuronMoved(this);
            }
        }
    }

    /**
     * @return Returns the y coordinate.
     */
    public double getY() {
        return y;
    }

    /**
     * Delete connected synapses.
     */
    public void deleteConnectedSynapses() {
        deleteFanIn();
        deleteFanOut();
    }

    /**
     * Delete fan in.
     */
    public void deleteFanIn() {
        for (Iterator incoming = fanIn.iterator(); incoming.hasNext(); ) {
            Synapse synapse = (Synapse) incoming.next();
            synapse.getParent().deleteWeight(synapse);
        }
    }

    /**
     * Delete fan out.
     */
    public void deleteFanOut() {
        for (Iterator outgoing = fanOut.iterator(); outgoing.hasNext(); ) {
            Synapse synapse = (Synapse) outgoing.next();
            synapse.getParent().deleteWeight(synapse);
        }
    }

    /**
     * @see Object
     */
    public String toString() {
        String ret = new String();
        ret += ("Neuron " + this.getId());
        ret += ("  Activation = " + this.getActivation());
        ret += ("  Location = (" + this.x +"," + this.y + ")");
        return ret;
    }


    /**
     * Set activation to 0; override for other "clearing" behavior.
     */
    public void clear() {
       activation = 0;
    }

    /**
     * Returns string for tool tip or short description.
     * @return tool tip text
     */
    public String getToolTipText() {
        return " Activation: " + Utils.round(this.getActivation(), 9);
    }

    /**
     * @return the targetValue
     */
    public double getTargetValue() {

        // Use signal synapse for target value
        if (targetValueSynapse != null) {
            return targetValueSynapse.getSource().getActivation();
        }

        // Return externally set target value via coupling...

        return targetValue;
    }

    /**
     * @return the hasTargetValue
     */
    public boolean hasTargetValue() {
        if (targetValueSynapse != null) {
            return true;
        }
        // Add check for external coupling also
        return false;
    }

    /**
     * @return updatePriority for the neuron
     */
    public int getUpdatePriority() {
        return updatePriority;
    }

    /**
     * @param updatePriority to set.
     */
   public void setUpdatePriority(final int updatePriority) {
       this.updatePriority = updatePriority;
        // notify the rootNetwork
        if (this.updatePriority != 0) {
            this.getParentNetwork().getRootNetwork().setPriorityUpdate(updatePriority);
        }
    }

    /**
     * @return the targetValueSynapse
     */
    public SignalSynapse getTargetValueSynapse() {
        return targetValueSynapse;
    }

    /**
     * @param targetValueSynapse the targetValueSynapse to set
     */
    public void setTargetValueSynapse(final SignalSynapse targetValueSynapse) {
        this.targetValueSynapse = targetValueSynapse;
    }
}
