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
package org.simbrain.network.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.JumpAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.Utils;

/**
 * <b>Synapse</b> objects represent "connections" between neurons, which learn
 * (grow or weaken) based on various factors, including the activation level of
 * connected neurons.
 */
public class Synapse {

    private static final SynapseUpdateRule DEFAULT_LEARNING_RULE =
        new StaticSynapseRule();

    private static final SpikeResponder DEFAULT_SPIKE_RESPONDER =
        new JumpAndDecay();

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
    private SynapseUpdateRule learningRule = DEFAULT_LEARNING_RULE;

    /** Only used of source neuron is a spiking neuron. */
    private SpikeResponder spikeResponder = DEFAULT_SPIKE_RESPONDER;

    /** Synapse id. */
    private String id = "";

    /** The maximum number of digits to display in the tool tip. */
    private static final int MAX_DIGITS = 2;

    /** Strength of synapse. */
    private double strength = 0;

    /** Post-Synaptic Response */
    private double psr;

    /** Amount to increment the neuron. */
    private double increment = 1;

    /** Upper limit of synapse. */
    private double upperBound = 10;

    /** Lower limit of synapse. */
    private double lowerBound = -10;

    /** Time to delay sending activation to target neuron. */
    private int delay;

    /** Parent group, if any (null if none). */
    private SynapseGroup parentGroup;

    /**
     * Boolean flag, indicating whether this type of synapse participates in the
     * computation of weighted input Set to a default value of true.
     */
    private boolean sendWeightedInput = true;

    /**
     * Boolean flag, indicating whether or not this synapse's strength can be
     * changed by any means other than direct user intervention.
     */
    private boolean frozen;

    /** Manages synaptic delay */
    private LinkedList<Double> delayManager;

    /**
     * Construct a synapse using a source and target neuron, defaulting to
     * ClampedSynapse and assuming the parent of the source neuron is the parent
     * of this synapse.
     *
     * @param source
     *            source neuron
     * @param target
     *            target neuron
     */
    public Synapse(Neuron source, Neuron target) {
        setSourceAndTarget(source, target);
        initSpikeResponder();
        if (source != null) {
            parentNetwork = source.getNetwork();
        }
    }

    /**
     * Construct a synapse with a specified initial strength.
     *
     * @param source
     *            source neuron
     * @param target
     *            target neuron
     * @param initialStrength
     *            initial strength for synapse
     */
    public Synapse(Neuron source, Neuron target, double initialStrength) {
        this(source, target);
        this.setStrength(initialStrength);
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified
     * learning rule. Assumes the parent network is the same as the parent
     * network of the provided source neuron.
     *
     * @param source
     *            source neuron
     * @param target
     *            target neuron
     * @param learningRule
     *            update rule for this synapse
     */
    public Synapse(Neuron source, Neuron target,
        SynapseUpdateRule learningRule) {
        setSourceAndTarget(source, target);
        initSpikeResponder();
        setLearningRule(learningRule);
        if (source != null) {
            parentNetwork = source.getNetwork();
        }
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified
     * learning rule. Assumes the parent network is the same as the parent
     * network of the provided source neuron.
     *
     * @param source
     *            source neuron
     * @param target
     *            target neuron
     * @param learningRule
     *            update rule for this synapse
     * @param templateSynapse
     *            synapse with parameters to copy
     */
    public Synapse(Neuron source, Neuron target,
        SynapseUpdateRule learningRule, Synapse templateSynapse) {
        this(templateSynapse); // invoke the copy constructor
        setSourceAndTarget(source, target);
        initSpikeResponder();
        setLearningRule(learningRule);
        if (source != null) {
            parentNetwork = source.getNetwork();
        }
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified
     * learning rule.
     *
     * @param source
     *            source neuron
     * @param target
     *            target neuron
     * @param learningRule
     *            update rule for this synapse
     * @param parent
     *            parent network for this synapse.
     */
    public Synapse(Neuron source, Neuron target,
        SynapseUpdateRule learningRule, Network parent) {
        setSourceAndTarget(source, target);
        setLearningRule(learningRule);
        initSpikeResponder();
        parentNetwork = parent;
    }

    /**
     * Copy constructor.
     *
     * @param s
     *            Synapse to used as a template for constructing a new synapse.
     */
    private Synapse(final Synapse s) {
        setLearningRule(s.getLearningRule().deepCopy());
        forceSetStrength(s.getStrength());
        setUpperBound(s.getUpperBound());
        setLowerBound(s.getLowerBound());
        setIncrement(s.getIncrement());
        setSpikeResponder(s.getSpikeResponder());
        setSendWeightedInput(s.isSendWeightedInput());
        setDelay(s.getDelay());
        setFrozen(s.isFrozen());
        s.initSpikeResponder();
    }

    /**
     * Makes a deep copy of a template synapse (one with no source or target). 
     * @param s
     * @return
     */
    public static Synapse copyTemplateSynapse(Synapse s) {
        if (s.getSource() != null || s.getTarget() != null) {
            throw new IllegalArgumentException("Synapse is not template"
                + " synapse.");
        }
        return new Synapse(s);
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
        if (!isFrozen()) {
            learningRule.update(this);
        }
    }

    /**
     * For spiking source neurons, returns the spike-responder's value times the
     * synapse strength. For non-spiking neurons, returns the pre-synaptic
     * activation times the synapse strength.
     *
     * @return Value
     */
    public double getValue() {
        if (!sendWeightedInput) {
            return 0;
        } else {
            spikeResponder.update(this);
            if (delay == 0) {
                return psr;
            } else {
                enqueu(psr);
                return dequeu();
            }
        }
    }

    public double getWeightedSum() {
        if (!sendWeightedInput) {
            return 0;
        } else {
            psr = source.getActivation() * strength;
            if (delay != 0) {
                enqueu(psr);
                return dequeu();
            } else {
                return psr;
            }
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
     * Sets the source and target neurons simultaneously.
     *
     * @param source
     * @param target
     */
    private void setSourceAndTarget(final Neuron source, final Neuron target) {
        if (this.source != null) {
            this.source.removeEfferent(this);
        }
        if (this.target != null) {
            this.target.removeAfferent(this);
        }
        if (source != null && target != null) {
            this.source = source;
            this.target = target;
            source.addEfferent(this);
            target.addAfferent(this);
        }
    }

    /**
     * @return Target neuron to which the synapse is attached.
     */
    public Neuron getTarget() {
        return target;
    }

    /**
     * Sets the strength of the synapse.
     *
     * @param wt
     *            Strength value
     */
    public void setStrength(final double wt) {
        if (!isFrozen()) {
            strength = wt;
        }
    }

    /**
     *
     * @param wt
     *            the value to set the strength of the synapse to
     */
    public void forceSetStrength(final double wt) {
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
     * @param d
     *            bound
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
     * @param d
     *            bound
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
     * @param d
     *            Increment amount
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
        // target.weightChanged(this); // Maybe?
        getNetwork().fireSynapseChanged(this);
    }

    /**
     * Decrement this weight by increment.
     */
    public void decrementWeight() {
        if (strength > lowerBound) {
            strength -= increment;
        }
        getNetwork().fireSynapseChanged(this);
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
        getNetwork().fireSynapseChanged(this);
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
        getNetwork().fireSynapseChanged(this);
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
        getNetwork().fireSynapseChanged(this);
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
        return getTarget().getFanOut().get(getSource());
    }

    /**
     * Randomize this weight to a value between its upper and lower bounds.
     */
    public void randomize() {
        strength = (getUpperBound() - getLowerBound()) * Math.random()
            + getLowerBound();
        getNetwork().fireSynapseChanged(this);
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
     * @param value
     *            Value to be checked
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
     * @param id
     *            The id to set.
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
     * @param sr
     *            The spikeResponder to set.
     */
    public void setSpikeResponder(final SpikeResponder sr) {
        this.spikeResponder = sr;
        if (sr == null) {
            return;
        }
    }

    /**
     * Delay manager.
     *
     * @param dly
     *            Amount of delay
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
     * @param val
     *            Value to enqueu
     */
    private void enqueu(final double val) {
        delayManager.add(new Double(val));
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Synapse [" + getId() + "]: " + getStrength());
        ret += ("  Connects neuron " + (getSource() == null ? "[null]"
            : getSource().getId()) + " to neuron "
            + (getTarget() == null ? "[null]" : getTarget().getId())
            + "\n");
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
     * A better name than setSendWeightedInput. Forwarding to
     * setSendWeightedInput for now. Possibly change name for 3.0. have not done
     * so yet so as note to break a bunch of simulations.
     *
     * @param enabled
     *            true if enabled, false otherwise.
     */
    public void setEnabled(final boolean enabled) {
        setSendWeightedInput(enabled);
    }

    /**
     * Whether this synapse is enabled or not.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isEnabled() {
        return this.isSendWeightedInput();
    }

    /**
     * @param sendWeightedInput
     *            to set.
     */
    public void setSendWeightedInput(boolean sendWeightedInput) {
        this.sendWeightedInput = sendWeightedInput;
    }

    /**
     * Convenience method for getting a reference to the parent root network.
     *
     * @return reference to root network.
     */
    public Network getNetwork() {
        return this.getSource().getNetwork();
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
     * @param name
     *            the "simple name" of the class associated with the neuron rule
     *            to set.
     */
    public void setLearningRule(String name) {
        try {
            SynapseUpdateRule newRule = (SynapseUpdateRule) Class.forName(
                "org.simbrain.network.synapse_update_rules." + name)
                .newInstance();
            setLearningRule(newRule);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                "The provided learning rule name, \""
                    + name
                    + "\", does not correspond to a known synapse type."
                    + "\n Could not find " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Change this synapse's learning rule.
     *
     * @param newLearningRule
     *            the learningRule to set
     */
    public void setLearningRule(SynapseUpdateRule newLearningRule) {
        SynapseUpdateRule oldRule = learningRule;
        this.learningRule = newLearningRule;
        initSpikeResponder();
        if (parentNetwork != null) {
            getNetwork().fireSynapseTypeChanged(oldRule, learningRule);
            // getNetwork().Network.updateTimeType();
            // Currently synapses don't have a time type
        }
    }

    /**
     * Returns a "template" synapse.
     *
     * @return the template synapse.
     * @see instantiateTemplateSynapse
     */
    public static Synapse getTemplateSynapse() {
        return new Synapse(null, null, new StaticSynapseRule(), (Network) null);
    }

    /**
     * Returns a template synapse with a specified learning rule.
     *
     * @param rule
     *            the learning rule.
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
     * @param rule
     *            the learning rule.
     * @return the template synapse
     * @see instantiateTemplateSynapse
     */
    public static Synapse getTemplateSynapse(String rule) {
        Synapse synapse = getTemplateSynapse();
        synapse.setLearningRule(rule);
        return synapse;
    }

    /**
     * A method which takes in a collection of synapses and returns a list of
     * their update rules in the order in which they appear in the original
     * collection, if that collection supports a consistent order.
     *
     * @param synapseCollection
     *            The collection of synapses whose update rules we want to
     *            query.
     * @return Returns a list of synapse update rules associated with the group
     *         of synapses
     */
    public static List<SynapseUpdateRule> getRuleList(
        Collection<Synapse> synapseCollection) {
        ArrayList<SynapseUpdateRule> ruleList =
            new ArrayList<SynapseUpdateRule>(
                synapseCollection.size());
        for (Synapse s : synapseCollection) {
            ruleList.add(s.getLearningRule());

        }
        return ruleList;
    }

    /**
     * A template synapse is a synapse that has no proper references, but is
     * used for setting properties. When the user is ready to instantiate it,
     * they call this method to give the proper references.
     *
     * @param source
     *            source neuron
     * @param target
     *            target neuron
     * @param parent
     *            parent network
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

    /**
     * @return the parentGroup
     */
    public SynapseGroup getParentGroup() {
        return parentGroup;
    }

    /**
     * @param parentGroup
     *            the parentGroup to set
     */
    public void setParentGroup(SynapseGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    /**
     * Decay this synapse by the indicated percentage. E.g. .5 cuts the strength
     * in half.
     *
     * @param decayPercent
     *            decay percent
     */
    public void decay(final double decayPercent) {
        double decayAmount = decayPercent * getStrength();
        setStrength(getStrength() - decayAmount);
    }

    /**
     * @return if the synapse's strength is frozen
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * @param frozen
     *            sets whether or not this synapses strength will be frozen
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /**
     * @return the post-synaptic response
     */
    public double getPsr() {
        return psr;
    }

    /**
     * @param psr
     *            set the post-synaptic response
     */
    public void setPsr(double psr) {
        this.psr = psr;
    }

}
