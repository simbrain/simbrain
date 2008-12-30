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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.util.Utils;
import org.simbrain.workspace.AbstractAttribute;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

/**
 * <b>Neuron</b> represents a node in the neural network.  Most of the "logic"
 * of the neural network occurs here, in the update function.  Subclasses must
 * override update and duplicate (for copy / paste) and cloning generally.
 */
public abstract class Neuron implements Producer, Consumer {

    /** The maximum number of digits to display in the tool tip. */
    private static final int MAX_DIGITS = 9;
    
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

    /** Temporary activation value. */
    private double buffer = 0;

    /** Value of any external inputs to neuron. */
    private double inputValue = 0;

    /** Reference to network this neuron is part of. */
    private Network parent = null;

    /** List of synapses this neuron attaches to. */
    private ArrayList<Synapse> fanOut = new ArrayList<Synapse>();

    /** List of synapses attaching to this neuron. */
    private ArrayList<Synapse> fanIn = new ArrayList<Synapse>();

    /** Read only version of the above. */
    private final List<Synapse> readOnlyFanIn = Collections.unmodifiableList(fanIn);

    /** Read only version of the above. */
    private final List<Synapse> readOnlyFanOut = Collections.unmodifiableList(fanOut);

    /** x-coordinate of this neuron in 2-space. */
    private double x;

    /** y-coordinate of this neuron in 2-space. */
    private double y;

    /** If true then do not update this neuron. */
    private boolean clamped = false;
    
    /** Target value. */
    private double targetValue = 0;

    /**
     *  Sequence in which the update function should be called
     *  for this neuron. By default, this is set to 0 for all
     *  the neurons. If you want a subset of neurons to fire
     *  before other neurons, assign it a smaller priority value.
     */
    private int updatePriority = 0;
    
    /** The producing attributes. */
    private ArrayList<ProducingAttribute<?>> producingAttributes
        = new ArrayList<ProducingAttribute<?>>();

    /** The consuming attributes. */
    private ArrayList<ConsumingAttribute<?>> consumingAttributes
        = new ArrayList<ConsumingAttribute<?>>();

    /** The default producing attribute. */
    private ProducingAttribute<?> defaultProducingAttribute;

    /** The default consuming attribute. */
    private ConsumingAttribute<?> defaultConsumingAttribute;

    /**
     * Default constructor needed for external calls which create neurons then
     * set their parameters.
     */
    protected Neuron() {
        setAttributeLists();
    }

    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type.  Only values common to different types of neuron
     * are copied.
     *
     * @param n Neuron
     */
    protected Neuron(final Neuron n) {
        setParentNetwork(n.getParentNetwork());
        setActivation(n.getActivation());
        setUpperBound(n.getUpperBound());
        setLowerBound(n.getLowerBound());
        setIncrement(n.getIncrement());
        setInputValue(n.getInputValue());
        setX(n.getX());
        setY(n.getY());
        setUpdatePriority(n.getUpdatePriority());
        setAttributeLists();
    }

    /**
     * Initialization method called by constructors.
     */
    private void setAttributeLists() {
        
        ActivationAttribute activationAttribute = new ActivationAttribute();
        producingAttributes().add(activationAttribute);
        consumingAttributes().add(activationAttribute);
        defaultProducingAttribute = activationAttribute;
        defaultConsumingAttribute = activationAttribute;

        UpperBoundAttribute upperBoundAttribute = new UpperBoundAttribute();
        producingAttributes().add(upperBoundAttribute);
        consumingAttributes().add(upperBoundAttribute);

        LowerBoundAttribute lowerBoundAttribute = new LowerBoundAttribute();
        producingAttributes().add(lowerBoundAttribute);
        consumingAttributes().add(lowerBoundAttribute);

        TargetValueAttribute targetValueAttribute = new TargetValueAttribute();
        producingAttributes().add(targetValueAttribute);
        consumingAttributes().add(targetValueAttribute);
    }

    /**
     * Completes duplication of this neuron; used in copy/paste.
     * This does not produce the copy!
     * Matching source and targets is up to you!
     *
     * @param n Neuron to duplicate
     * @return duplicate neuron
     */
    protected Neuron duplicate(final Neuron n) {
        n.setParentNetwork(this.getParentNetwork());
        n.setActivation(this.getActivation());
        n.setUpperBound(this.getUpperBound());
        n.setLowerBound(this.getLowerBound());
        n.setIncrement(this.getIncrement());
        n.setX(this.getX());
        n.setY(this.getY());
        n.setUpdatePriority(this.getUpdatePriority());

        return n;
    }

    /**
     * Provides writable access to subclasses.  Avoid making
     * this public.  Subclasses can override this method.
     * 
     * @return the producing attributes for this instance.
     */
    protected List<ConsumingAttribute<?>> consumingAttributes() {
        return consumingAttributes;
    }
    
    /**
     * Provides writable access to subclasses.  Avoid making
     * this public.  Subclasses can override this method.
     * 
     * @return the consuming attributes for this instance.
     */
    protected List<ProducingAttribute<?>> producingAttributes() {
        return producingAttributes;
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
     * Perform any initialization required when creating a neuron, but after
     * the parent network has been added.
     */
    public void postUnmarshallingInit() {
    }

    /**
     * Sets the activation of the neuron.
     * @param act Activation
     */
    public void setActivation(final double act) {
        if (!clamped) {
            activation = act;
        }
    }

    /**
     * @return the level of activation.
     */
    public double getActivation() {
        return activation;
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
    public List<Synapse> getFanIn() {
        return readOnlyFanIn;
    }

    /**
     * @return the fan out array list.
     */
    public List<Synapse> getFanOut() {
        return readOnlyFanOut;
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
    void addTarget(final Synapse target) {
        fanOut.add(target);
    }

    /**
     * Remove this neuron from target neuron via a weight.
     *
     * @param target the connnection between this neuron and a target neuron
     */
    void removeTarget(final Synapse target) {
        fanOut.remove(target);
    }

    /**
     * Connect this neuron to source neuron via a weight.
     *
     * @param source the connnection between this neuron and a source neuron
     */
    void addSource(final Synapse source) {
        fanIn.add(source);
    }

    /**
     * Remove this neuron from source neuron via a weight.
     *
     * @param source the connnection between this neuron and a source neuron
     */
    void removeSource(final Synapse source) {
        fanIn.remove(source);
    }
// not used.  Consider deleting?
//    /**
//     * Add specified amount of activation to this neuron.
//     *
//     * @param amount amount to add to this neuron
//     */
//    public void addActivation(final double amount) {
//        activation += amount;
//    }

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
                if (w.isSendWeightedInput()) {
                    wtdSum += w.getValue();
                }
            }
        }

        return wtdSum;
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomize() {
        setActivation(getRandomValue());
        if (this.getParentNetwork() != null) {
            this.getParentNetwork().getRootNetwork().fireNeuronChanged(null, this);
        }
    }

    /**
     * Returns a random value between the upper and lower bounds of this neuron.
     * @return the random value.
     */
    public double getRandomValue() {
        return (upperBound - lowerBound) * Math.random() + lowerBound;
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomizeBuffer() {
        setBuffer(getRandomValue());
    }

    /*
     * Update all neurons n this neuron is connected to, by adding current activation
     * times the connection-weight  NOT CURRENTLY USED.
     */
//    public void updateConnectedOutward() {
//        // Update connected weights
//        if (fanOut.size() > 0) {
//            for (int j = 0; j < fanOut.size(); j++) {
//                Synapse w = (Synapse) fanOut.get(j);
//                Neuron target = w.getTarget();
//                target.setActivation(w.getStrength() * activation);
//                target.checkBounds();
//            }
//        }
//    }

    /**
     * Check if this neuron is connected to a given weight.
     *
     * @param w weight to check
     *
     * @return true if this neuron has w in its fan_in or fan_out
     */
//    public boolean connectedToWeight(final Synapse w) {
//        if (fanOut.size() > 0) {
//            for (int j = 0; j < fanOut.size(); j++) {
//                Synapse outW = (Synapse) fanOut.get(j);
//
//                if (w.equals(outW)) {
//                    return true;
//                }
//            }
//        }
//
//        if (fanIn.size() > 0) {
//            for (int j = 0; j < fanIn.size(); j++) {
//                Synapse inW = (Synapse) fanIn.get(j);
//
//                if (w.equals(inW)) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }

    /**
     * Round the activation level of this neuron off to a specified precision.
     *
     * @param precision precision to round this neuron's activation off to
     */
    public void round(final int precision) {
        setActivation(Network.round(getActivation(), precision));
    }

    /**
     * If activation is above or below its bounds set it to those bounds.
     */
    public void checkBounds() {
        activation = clip(activation);
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
     * Temporary buffer which can be used for algorithms which should not depend on
     * the order in which  neurons are updated.
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
        // this.targetValue = inputValue; //TODO: This is temporary!
    }

    /**
     * @return the name of the class of this network.
     */
    public String getType() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * Returns the sum of the strengths of the weights attaching to this neuron.
     *
     * @return the sum of the incoming weights to this neuron.
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
        
        
        for (Synapse incoming : fanIn) {
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
        return false;
//        return (motorCoupling != null);
    }

    /**
     * Return true if this neuron has a sensory coupling attached.
     *
     * @return true if this neuron has a sensory coupling attached
     */
    public boolean isInput() {
        return false;
      //  return (sensoryCoupling != null);
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
       for (Synapse synapse : fanIn) {
            synapse.getParentNetwork().deleteSynapse(synapse);
        }
    }

    /**
     * Delete fan out.
     */
    public void deleteFanOut() {
        for (Synapse synapse : fanOut) {
            synapse.getParentNetwork().deleteSynapse(synapse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Neuron [" + getId() + "] " 
            + "  Activation = " + this.getActivation()
            + "  Location = (" + this.x + "," + this.y + ")";
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
        return "(" + id + ") Activation: " + Utils.round(this.getActivation(), MAX_DIGITS);
    }

    /**
     * @return the targetValue
     */
    public double getTargetValue() {
        return targetValue;
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
        if (this.updatePriority != 0 && this.getParentNetwork() != null) {
            this.getParentNetwork().getRootNetwork().setPriorityUpdate(updatePriority);
        }
    }

    /**
     * @return the clamped
     */
    public boolean isClamped() {
        return clamped;
    }

    /**
     * Toggles whether this neuron is clamped.
     * 
     * @param clamped Whether this neuron is to be clamped.
     */
    public void setClamped(final boolean clamped) {
        this.clamped = clamped;
    }
    
    /**
     * {@inheritDoc}
     */
    public final List<? extends ProducingAttribute<?>> getProducingAttributes() {
        return producingAttributes();
    }

    /**
     * {@inheritDoc}
     */
    public final List<? extends ConsumingAttribute<?>> getConsumingAttributes() {
        return consumingAttributes();
    }

    /**
     * Implements the Activation attribute.
     * 
     * @author Matt Watson
     */
    private class ActivationAttribute extends AbstractAttribute 
            implements ProducingAttribute<Double>, ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "Activation";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return getParent().getActivation();
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            getParent().setInputValue(value == null ? 0 : value);
        }
        
        /**
         * {@inheritDoc}
         */
        public Neuron getParent() {
            return Neuron.this;
        }
    }
    
    /**
     * Implements the Upper bound attribute.
     * 
     * @author Matt Watson
     */
    private class UpperBoundAttribute extends AbstractAttribute implements ProducingAttribute<Double>,
            ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "UpperBound";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return upperBound;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            upperBound = value;
        }
        
        /**
         * {@inheritDoc}
         */
        public Neuron getParent() {
            return Neuron.this;
        }

    }

    /**
     * Implements the Lower bound attribute.
     * 
     * @author Matt Watson
     */
    private class LowerBoundAttribute extends AbstractAttribute implements ProducingAttribute<Double>,
            ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getKey() {
            return "LowerBound";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return lowerBound;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            lowerBound = value;
        }
        
        /**
         * {@inheritDoc}
         */
        public Neuron getParent() {
            return Neuron.this;
        }
    }

    /**
     * Implements the Target Value attribute.
     */
    private class TargetValueAttribute extends AbstractAttribute implements ProducingAttribute<Double>,
            ConsumingAttribute<Double> {
        
        /**
         * {@inheritDoc}
         */
        public String getAttributeDescription() {
            return "TargetValue";
        }
        
        /**
         * {@inheritDoc}
         */
        public Double getValue() {
            return targetValue;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setValue(final Double value) {
            targetValue = value;
        }
        
        /**
         * {@inheritDoc}
         */
        public Neuron getParent() {
            return Neuron.this;
        }

        public String getKey() {
            return "TargetValue";
        }
    }

    /**
     * @return the defaultConsumingAttribute
     */
    public ConsumingAttribute<?> getDefaultConsumingAttribute() {
        return defaultConsumingAttribute;
    }

    /**
     * @param defaultConsumingAttribute the defaultConsumingAttribute to set
     */
    public void setDefaultConsumingAttribute(
            final ConsumingAttribute<?> defaultConsumingAttribute) {
        this.defaultConsumingAttribute = defaultConsumingAttribute;
    }

    /**
     * @return the defaultProducingAttribute
     */
    public ProducingAttribute<?> getDefaultProducingAttribute() {
        return defaultProducingAttribute;
    }

    /**
     * @param defaultProducingAttribute the defaultProducingAttribute to set
     */
    public void setDefaultProducingAttribute(
            final ProducingAttribute<?> defaultProducingAttribute) {
        this.defaultProducingAttribute = defaultProducingAttribute;
    }

    /**
     * Returns the id of the neuron.
     * 
     * @return the id of the neuron.
     */
    public String getDescription() {
        return getId();
    }

    /**
     * Returns the parent component.
     * 
     * @return the parent component.
     */
    public NetworkComponent getParentComponent() {
        return parent.getRootNetwork().getParent();
    }
}
