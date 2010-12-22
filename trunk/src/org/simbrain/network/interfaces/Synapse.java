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
import org.simbrain.network.synapses.HebbianSynapse;
import org.simbrain.network.synapses.HebbianCPCASynapse;
import org.simbrain.network.synapses.HebbianThresholdSynapse;
import org.simbrain.network.synapses.OjaSynapse;
import org.simbrain.network.synapses.RandomSynapse;
import org.simbrain.network.synapses.STDPSynapse;
import org.simbrain.network.synapses.ShortTermPlasticitySynapse;
import org.simbrain.network.synapses.SubtractiveNormalizationSynapse;
import org.simbrain.network.synapses.spikeresponders.JumpAndDecay;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.Utils;

/**
 * <b>Synapse</b> objects represent "connections" between neurons, which learn
 * (grow or weaken) based on various factors, including the activation level of
 * connected neurons.
 */
public class Synapse {

    /**
     * Parent network. Can't just use getSouce().getParent() because synapses
     * and their parents can occur at different levels of the network hierarchy.
     */
    private Network parentNetwork;

    /** Neuron activation will come from. */
    private Neuron source;

    /** Neuron to which the synapse is attached. */
    private Neuron target;

    /**
     * The update method of this synapse, which corresponds to what kind of
     * synapse it is.
     */
    private SynapseUpdateRule learningRule;

    /** Only used of source neuron is a spiking neuron. */
    protected SpikeResponder spikeResponder;

    /** Synapse id. */
    protected String id;

    /** The maximum number of digits to display in the tool tip. */
    private static final int MAX_DIGITS = 2;

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
     * Boolean flag, indicating whether this type of synapse participates in the
     * computation of weighted input Set to a default value of true.
     */
    private boolean sendWeightedInput = true;

    /** Manages synaptic delay */
    private LinkedList<Double> delayManager;

    /** List of Neuron update rules; used in Gui Combo boxes. */
    private static final ClassDescriptionPair[] RULE_LIST = {
            new ClassDescriptionPair(ClampedSynapse.class,
                    new ClampedSynapse().getDescription()),
            new ClassDescriptionPair(HebbianSynapse.class,
                    new HebbianSynapse().getDescription()),
            new ClassDescriptionPair(HebbianCPCASynapse.class,
                    new HebbianCPCASynapse().getDescription()),
            new ClassDescriptionPair(HebbianThresholdSynapse.class,
                    new HebbianThresholdSynapse().getDescription()),
            new ClassDescriptionPair(OjaSynapse.class,
                    new OjaSynapse().getDescription()),
            new ClassDescriptionPair(RandomSynapse.class,
                    new RandomSynapse().getDescription()),
            new ClassDescriptionPair(ShortTermPlasticitySynapse.class,
                    new ShortTermPlasticitySynapse().getDescription()),
            new ClassDescriptionPair(STDPSynapse.class,
                    new STDPSynapse().getDescription()),
            new ClassDescriptionPair(SubtractiveNormalizationSynapse.class,
                    new SubtractiveNormalizationSynapse().getDescription()) };

    /**
     * Construct a synapse using a source and target neuron, defaulting to
     * ClampedSynapse and assuming the parent of the source neuron is the parent
     * of this synapse.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public Synapse(Neuron source, Neuron target) {
        this(source, target, new ClampedSynapse());
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified
     * learning rule. Assumes the parent network is the same as the parent
     * network of the provided source neuron.
     *
     * @param source source neuron
     * @param target target neuron
     * @param learningRule update rule for this synapse
     */
    public Synapse(Neuron source, Neuron target, SynapseUpdateRule learningRule) {
        setSource(source);
        setTarget(target);
        setLearningRule(learningRule);
        if (source != null) {
            parentNetwork = source.getParentNetwork();
        }
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified
     * learning rule.
     *
     * @param source source neuron
     * @param target target neuron
     * @param learningRule update rule for this synapse
     * @param parent parent network for this synapse.
     */
    public Synapse(Neuron source, Neuron target,
            SynapseUpdateRule learningRule, Network parent) {
        setSource(source);
        setTarget(target);
        setLearningRule(learningRule);
        parentNetwork = parent;
    }

    /**
     * Copy constructor.
     *
     * @param s Synapse to be created from another
     */
    public Synapse(final Synapse s) {
        this(s.source, s.target, s.getLearningRule().deepCopy());
        setStrength(s.getStrength());
        setUpperBound(s.getUpperBound());
        setLowerBound(s.getLowerBound());
        setIncrement(s.getIncrement());
        setSpikeResponder(s.getSpikeResponder());
        setSendWeightedInput(s.isSendWeightedInput());
    }

    /**
     * Set a default spike responder if the source neuron is a spiking neuron,
     * else set the spikeResponder to null.
     */
    public void initSpikeResponder() {
        if (source != null) {
            if (source.getUpdateRule() instanceof SpikingNeuronUpdateRule) {
                setSpikeResponder(new JumpAndDecay());
            } else {
                setSpikeResponder(null);
            }
        }
    }

    /**
     * Update this synapse using its current learning rule.
     */
    public void update() {
        learningRule.update(this);
    }

    /**
     * Create duplicate weights. Used in copy/paste.
     *
     * @param s weight to duplicate
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
     * For spiking source neurons, returns the spike-responder's value times the
     * synapse strength. For non-spiking neurons, returns the pre-synaptic
     * activation times the synapse strength.
     *
     * @return Value
     */
    public double getValue() {
        double val;

        if (source.getUpdateRule() instanceof SpikingNeuronUpdateRule) {
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
     * The name of the learning rule of the synapse; it's "type". Used via
     * reflection for consistency checking in the gui. (Open multiple synapses
     * and if they are of the different types the dialog is different).
     *
     * @return the name of the class of this network.
     */
    public String getType() {
        return learningRule.getClass().getSimpleName();
    }

    /**
     * @return Strength of synapse.
     */
    public final double getStrength() {
        return strength;
    }

    /**
     * @return Source neuron to which the synapse is attached.
     */
    public Neuron getSource() {
        return source;
    }

    /**
     * New source neuron to attach the synapse.
     *
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
     *
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
     *
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
     *
     * @param d bound
     */
    public void setUpperBound(final double d) {
        upperBound = d;
    }

    /**
     * @return Lower synapse bound.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Sets the lower synapse bound.
     *
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
     *
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
        getRootNetwork().fireSynapseChanged(this);
    }

    /**
     * Decrement this weight by increment.
     */
    public void decrementWeight() {
        if (strength > lowerBound) {
            strength -= increment;
        }
        getRootNetwork().fireSynapseChanged(this);
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
        getRootNetwork().fireSynapseChanged(this);
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
        getRootNetwork().fireSynapseChanged(this);
    }

    /**
     * Randomizes this synapse and sets the symmetric analogue to the same
     * value. A bit of a hack, since it it is used on a collection a bunch of
     * redundancy could happen.
     */
    public void randomizeSymmetric() {
        randomize();
        Synapse symmetric = getSymmetricSynapse();
        if (symmetric != null) {
            symmetric.setStrength(strength);
        }
        getRootNetwork().fireSynapseChanged(this);
    }

    /**
     * Returns string for tool tip or short description.
     *
     * @return tool tip text
     */
    public String getToolTipText() {
        return "(" + id + ") Strength: "
                + Utils.round(this.getStrength(), MAX_DIGITS);
    }

    /**
     * Returns symmetric synapse if there is one, null otherwise.
     *
     * @return the symmetric synapse, if any.
     */
    public Synapse getSymmetricSynapse() {
        List<Synapse> targetsOut = this.getTarget().getFanOut();
        int index = targetsOut.indexOf(this.getSource());

        return (index < 0) ? null : targetsOut.get(index);
    }

    /**
     * Randomize this weight to a value between its upper and lower bounds.
     */
    public void randomize() {
        strength = getRandomValue();
        getRootNetwork().fireSynapseChanged(this);
    }

    /**
     * Returns a random value between the upper and lower bounds of this
     * synapse.
     *
     * @return the random value.
     */
    public double getRandomValue() {
        return (upperBound - lowerBound) * Math.random() + lowerBound;
    }

    /**
     * If weight value is above or below its bounds set it to those bounds.
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
     * Utility function for use in learning rules. If value is above or below
     * the bounds of this synapse set it to those bounds.
     *
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

    /**
     * Delay manager.
     *
     * @param dly Amount of delay
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
     *
     * @param val Value to enqueu
     */
    private void enqueu(final double val) {
        delayManager.add(new Double(val));
    }

    @Override
    public String toString() {
        String ret = new String();
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

    /**
     * Convenience method for getting a reference to the parent root network.
     *
     * @return reference to root network.
     */
    public RootNetwork getRootNetwork() {
        return this.getSource().getParentNetwork().getRootNetwork();
    }

    /**
     * @return the learningRule
     */
    public SynapseUpdateRule getLearningRule() {
        return learningRule;
    }

    /**
     * Sets the update rule using a String description. The provided description
     * must match the class name. E.g. "BinaryNeuron" for "BinaryNeuron.java".
     *
     * @param name the "simple name" of the class associated with the neuron
     *            rule to set.
     */
    public void setLearningRule(String name) {
        try {
            SynapseUpdateRule newRule = (SynapseUpdateRule) Class.forName(
                    "org.simbrain.network.synapses." + name).newInstance();
            setLearningRule(newRule);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "The provided learning rule name, \"" + name
                            + "\", does not correspond to a known neuron type."
                            + "\n Could not find " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Change this synapse's learning rule.
     *
     * @param newLearningRule the learningRule to set
     */
    public void setLearningRule(SynapseUpdateRule newLearningRule) {
        SynapseUpdateRule oldRule = learningRule;
        this.learningRule = newLearningRule;
        initSpikeResponder();
        if (parentNetwork != null) {
            getRootNetwork().fireSynapseTypeChanged(oldRule, learningRule);
            // getRootNetwork().rootNetwork.updateTimeType();
            // Currently synapses don't have a time type
        }
    }

    /**
     * @return the ruleList
     */
    public static ClassDescriptionPair[] getRuleList() {
        return RULE_LIST;
    }

    /**
     * Returns a "template" synapse.
     *
     * @return the template synapse.
     * @see instantiateTemplateSynapse
     */
    public static Synapse getTemplateSynapse() {
        return new Synapse(null, null, new ClampedSynapse(), null);
    }

    /**
     * Returns a template synapse with a specified learning rule.
     *
     * @param rule the learning rule.
     * @return the template synapse
     * @see instantiateTemplateSynapse
     */
    public static Synapse getTemplateSynapse(SynapseUpdateRule rule) {
        Synapse synapse = getTemplateSynapse();
        synapse.setLearningRule(rule);
        return synapse;
    }

    /**
     * Returns a template synapse with a (string) specified learning rule.
     *
     * @param rule the learning rule.
     * @return the template synapse
     * @see instantiateTemplateSynapse
     */
    public static Synapse getTemplateSynapse(String rule) {
        Synapse synapse = getTemplateSynapse();
        synapse.setLearningRule(rule);
        return synapse;
    }

    /**
     * A template synapse is a synapse that has no proper references, but is
     * used for setting properties. When the user is ready to instantiate it,
     * they call this method to give the proper references.
     *
     * @param source source neuron
     * @param target target neuron
     * @param parent parent network
     * @return a new synapse with these references and the base synapse's
     *         properties
     */
    public Synapse instantiateTemplateSynapse(Neuron source, Neuron target,
            Network parent) {
        this.source = source;
        this.target = target;
        this.parentNetwork = parent;
        return new Synapse(this);
    }

}
