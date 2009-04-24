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

import java.util.LinkedList;
import java.util.List;

import org.simbrain.network.synapses.ClampedSynapse;
import org.simbrain.network.synapses.Hebbian;
import org.simbrain.network.synapses.HebbianCPCA;
import org.simbrain.network.synapses.HebbianThresholdSynapse;
import org.simbrain.network.synapses.OjaSynapse;
import org.simbrain.network.synapses.RandomSynapse;
import org.simbrain.network.synapses.ShortTermPlasticitySynapse;
import org.simbrain.network.synapses.SignalSynapse;
import org.simbrain.network.synapses.SimpleSynapse;
import org.simbrain.network.synapses.SubtractiveNormalizationSynapse;
import org.simbrain.network.synapses.TDSynapse;
import org.simbrain.network.synapses.TraceSynapse;
import org.simbrain.network.synapses.spikeresponders.JumpAndDecay;


/**
 * <b>Synapse</b> objects represent "connections" between neurons, which learn (grow or  weaken) based on various
 * factors, including the activation level of connected neurons.
 */
public abstract class Synapse {

    /** Neuron activation will come from. */
    private Neuron source;

    /** Neuron to which the synapse is attached. */
    private Neuron target;

    /**  Only used of source neuron is a spiking neuron. */
    protected SpikeResponder spikeResponder = null;

    /** Synapse id. */
    protected String id = null;

    /**
     * Parent network.  Cant' just use getSouce().getParent() because synapses
     * and their parents can occur at any level of the netork hierarcy.
     */
    private Network parentNetwork;

    /** Number of parameters. */
    public static final int NUM_PARAMETERS = 8;

    /** Strength of synapse. */
    protected double strength = 1;

    /** Amount to increment the neuron. */
    protected double increment = 1;

    /** Upper limit of synapse. */
    protected double upperBound = 10;

    /** Lower limit of synapse. */
    protected double lowerBound = -10;

    /** Time to delay sending activation to target neuron. */
    private int delay = 0;

    /**
     *  Boolean flag, indicating whether this type of synapse
     *  participates in the computation of weighted input
     *  Set to a default value of true.
     */
    private boolean sendWeightedInput = true;

    /** Manages delays of synapses. */
    private LinkedList<Double> delayManager = null;

    /** List of synapse types for combo box. */
    private static String[] typeList = {
            ClampedSynapse.getName(),
            Hebbian.getName(), HebbianCPCA.getName(),
            HebbianThresholdSynapse.getName(), OjaSynapse.getName(),
            RandomSynapse.getName(), ShortTermPlasticitySynapse.getName(),
            SignalSynapse.getName(), SimpleSynapse.getName(), SubtractiveNormalizationSynapse.getName(),
            TDSynapse.getName(), TraceSynapse.getName()

    };
    
    public Synapse(Neuron source, Neuron target) {
        setSource(source);
        setTarget(target);
    }
    
    /**
     * This constructor is used when creating a synapse of one type from another
     * synapse of another type Only values common to different types of synapse
     * are copied.
     * 
     * Copies the source and target of the passed in Synapse
     * 
     * @param s Synapse to be created from another
     */
    public Synapse(final Synapse s) {
        this(s.source, s.target);
        setStrength(s.getStrength());
        setUpperBound(s.getUpperBound());
        setLowerBound(s.getLowerBound());
        setIncrement(s.getIncrement());
        setSpikeResponder(s.getSpikeResponder());
        setSendWeightedInput(s.isSendWeightedInput());
    }

    /**
     * Set a default spike responder if the source neuron is a  spiking neuron, else set the spikeResponder to null.
     */
    public void initSpikeResponder() {
        if (source instanceof SpikingNeuron) {
            setSpikeResponder(new JumpAndDecay());
        } else {
            setSpikeResponder(null);
        }
    }

    /**
     * Create duplicate weights. Used in copy/paste.
     *
     * @param s weight to duplicate
     *
     * @return duplicate weight
     */
    public Synapse duplicate(final Synapse s) {
        s.setStrength(this.getStrength());
        s.setIncrement(this.getIncrement());
        s.setUpperBound(this.getUpperBound());
        s.setLowerBound(this.getLowerBound());
        s.setSpikeResponder(this.getSpikeResponder());
        s.setSendWeightedInput(this.isSendWeightedInput());
        return s;
    }

    /**
     * Update synapse.
     */
    public abstract void update();

    /**
     * @return Duplicate synapse.
     */
    public abstract Synapse duplicate();

    /**
     * For spiking source neurons, returns the spike-responder's value times the synapse strength.
     * For non-spiking neurons, returns the pre-synaptic activation times the synapse strength.
     *
     * @return Value
     */
    public double getValue() {
        double val;

        if (source instanceof SpikingNeuron) {
            spikeResponder.update();
            val = strength * spikeResponder.getValue();
        } else {
            val = source.getActivation() * strength;
        }

        if (delayManager == null) {
            return val;
        } else {
            enqueu(val);

            return dequeu();
        }
    }

    /**
     * @return the name of the class of this synapse
     */
    public String getType() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * @return Strength of synapse.
     */
    public double getStrength() {
        return strength;
    }

    /** @see GaugeSource */
    public double getGaugeValue() {
        return getStrength();
    }

    /**
     * Cleans up this Synapse that has been deleted.
     */
    void delete() {
        if (source != null) source.removeTarget(this);
        if (target != null) target.removeSource(this);

        if (parentNetwork != null) parentNetwork.getSynapseList().remove(this);
        else System.out.println("parentNetwork is null");
    }

    /**
     * @return Source neuron to which the synapse is attached.
     */
    public Neuron getSource() {
        return source;
    }

    /**
     * New source neuron to attach the synapse.
     * @param n Neuron to attach synapse
     */
    public void setSource(final Neuron n) {
        if (this.source != null) {
        	this.source.removeTarget(this);
        }

        if (n != null) {
            this.source = n;
            n.addTarget(this);            
        }
    }

    /**
     * @return Target neuron to which the synapse is attached.
     */
    public Neuron getTarget() {
        return target;
    }

    /**
     * New target neuron to attach the synapse.
     * @param n Neuron to attach synapse
     */
    public void setTarget(final Neuron n) {
    	if (this.target != null) {
        	this.target.removeSource(this);
        }

    	if (n != null) {
            this.target = n;
            n.addSource(this);    	    
    	}
    }

    /**
     * Sets the strength of the synapse.
     * @param wt Strength value
     */
    public void setStrength(final double wt) {
        strength = wt;
    }

    /**
     * @return Upper synapse bound.
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Sets the upper synapse bound.
     * @param d bound
     */
    public void setUpperBound(final double d) {
        upperBound = d;
    }

    /**
     * @return Lower synapse boundy.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower synapse bound.
     * @param d bound
     */
    public void setLowerBound(final double d) {
        lowerBound = d;
    }


    /**
     * @return Amount to increment neuron.
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * Sets the amount to increment neuron.
     * @param d Increment amount
     */
    public void setIncrement(final double d) {
        increment = d;
    }

    /**
     * Increment this weight by increment.
     */
    public void incrementWeight() {
        if (strength < upperBound) {
            strength += increment;
        }
        this.getParentNetwork().getRootNetwork().fireSynapseChanged(null, this);
    }

    /**
     * Decrement this weight by increment.
     */
    public void decrementWeight() {
        if (strength > lowerBound) {
            strength -= increment;
        }
    }

    /**
     * Increase the absolute value of this weight by increment amount.
     */
    public void reinforce() {
        if (strength > 0) {
            incrementWeight();
        } else if (strength < 0) {
            decrementWeight();
        } else if (strength == 0) {
            strength = 0;
        }
    }

    /**
     * Decrease the absolute value of this weight by increment amount.
     */
    public void weaken() {
        if (strength > 0) {
            decrementWeight();
        } else if (strength < 0) {
            incrementWeight();
        } else if (strength == 0) {
            strength = 0;
        }
    }


    /**
     * Randomizes this synapse and sets the symmetric analogue to the same value.
     * A bit of a hack, since it it is used on a collection a bunch of redundancy could
     * happen.
     */
    public void randomizeSymmetric() {
        randomize();
        Synapse symmetric = getSymmetricSynapse();
        if (symmetric != null) {
            symmetric.setStrength(strength);
        }
    }

    /**
     * Returns symmetric synapse if there is one, null otherwise.
     * @return the symmetric synapse, if any.
     */
    public Synapse getSymmetricSynapse() {
    	List<Synapse> targetsOut = this.getTarget().getFanOut();
    	int index = targetsOut.indexOf(this.getSource());
    	
    	return (index < 0) ? null : targetsOut.get(index);
    	
//        for (Synapse synapse : this.getTarget().getFanOut()) {
//            if (synapse.getTarget() == this.getSource()) {
//                return synapse;
//            }
//        }
//        return null;
    }

    /**
     * Randomize this weight to a value between its upper and lower bounds.
     */
    public void randomize() {
        strength = getRandomValue();
        this.getSource().getParentNetwork().getRootNetwork().fireSynapseChanged(null, this);
    }

    /**
     * Returns a random value between the upper and lower bounds of this synapse.
     * @return the random value.
     */
    public double getRandomValue() {
        return (upperBound - lowerBound) * Math.random() + lowerBound;
    }

    /**
     * If weight  value is above or below its bounds set it to those bounds.
     */
    public void checkBounds() {
        if (strength > upperBound) {
            strength = upperBound;
        }

        if (strength < lowerBound) {
            strength = lowerBound;
        }
    }

    /**
     * If value is above or below its bounds set it to those bounds.
     * @param value Value to be checked
     * @return Evaluated value
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
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Helper function for combo boxes.  Associates strings with indices.
     * @param type Synapse type
     * @return Index
     */
    public static int getSynapseTypeIndex(final String type) {
        for (int i = 0; i < typeList.length; i++) {
            if (type.equals(typeList[i])) {
                return i;
            }
        }

        return 0;
    }

    /**
     * @return Returns the typeList.
     */
    public static String[] getTypeList() {
        return typeList;
    }

    /**
     * @return Returns the spikeResponder.
     */
    public SpikeResponder getSpikeResponder() {
        return spikeResponder;
    }

    /**
     * @param sr The spikeResponder to set.
     */
    public void setSpikeResponder(final SpikeResponder sr) {
        this.spikeResponder = sr;

        if (sr == null) {
            return;
        }

        spikeResponder.setParent(this);
    }

    ////////////////////
    //  Delay manager //
    ////////////////////
    /**
     * Delay manager.
     * @param dly Amound of delay
     */
    public void setDelay(final int dly) {
        delay = dly;

        if (delay == 0) {
            delayManager = null;

            return;
        }

        delayManager = new LinkedList<Double>();
        delayManager.clear();

        for (int i = 0; i < delay; i++) {
            delayManager.add(new Double(0));
        }
    }

    /**
     * @return Current amount of delay.
     */
    public int getDelay() {
        return delay;
    }

    /**
     * @return the deque.
     */
    private double dequeu() {
        return delayManager.removeFirst().doubleValue();
    }

    /**
     * Enqueeu.
     * @param val Value to enqueu
     */
    private void enqueu(final double val) {
        delayManager.add(new Double(val));
    }

    /**
     * @see Object
     */
    public String toString() {
        String ret =  new String();
        ret += ("Synapse [" + getId() + "]: " + getStrength());
        ret += ("  Connects neuron " + getSource().getId() + " to neuron "
                           + getTarget().getId() + "\n");
        return ret;
    }

    /**
     * @return Returns the parent.
     */
    public Network getParentNetwork() {
        return parentNetwork;
    }

    /**
     * @param parentNetwork the parentNetwork to set
     */
    public void setParentNetwork(Network parentNetwork) {
        this.parentNetwork = parentNetwork;
    }

    /**
    * @return sendWeightedInput for the synapse
    */
    public boolean isSendWeightedInput() {
        return sendWeightedInput;
    }

    /**
     * @param sendWeightedInput to set.
     */
    public void setSendWeightedInput(boolean sendWeightedInput) {
        this.sendWeightedInput = sendWeightedInput;
    }
}
