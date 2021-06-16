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

import org.simbrain.network.NetworkModel;
import org.simbrain.network.events.SynapseEvents;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.NonResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.DataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.util.*;

/**
 * <b>Synapse</b> objects represent "connections" between neurons, which learn
 * (grow or weaken) based on various factors, including the activation level of connected neurons.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public class Synapse extends NetworkModel implements EditableObject, AttributeContainer {

    /**
     * A default update rule for the synapse.
     */
    private static final SynapseUpdateRule DEFAULT_LEARNING_RULE = new StaticSynapseRule();

    /**
     * A default spike responder.
     */
    public static final SpikeResponder DEFAULT_SPIKE_RESPONDER = new NonResponder();

    /**
     * Default upper bound.
     */
    private static double DEFAULT_UPPER_BOUND = 100;

    /**
     * Default lower bound.
     */
    private static double DEFAULT_LOWER_BOUND = -100;

    /**
     * Strength of synapse.
     */
    @UserParameter(label = "Strength", useSetter = true, description = "Weight Strength. If you want a value greater" +
            "than upper bound or less than lower bound you must set those first, and close this dialog.",
            probDist = "Normal", probParam1 = .1, probParam2 = .5,
            order = 1)
    private double strength = 1;

    @Override
    public String getName() {
        return getId();
    }

    /**
     * Parent network. Can't just use getSouce().getParent() because synapses and their parents can occur at different
     * levels of the network hierarchy.
     */
    private Network parentNetwork;

    /**
     * Neuron activation will come from.
     */
    private Neuron source;

    /**
     * Neuron to which the synapse is attached.
     */
    private Neuron target;

    /**
     * The update method of this synapse, which corresponds to what kind of synapse it is.
     */
    @UserParameter(label = "Learning Rule", useSetter = true,
            isObjectType = true, order = 100)
    private SynapseUpdateRule learningRule = DEFAULT_LEARNING_RULE;

    /**
     * Only used if source neuron is a spiking neuron.
     */
    @UserParameter(label = "Spike Responder", isObjectType = true,
            showDetails = false, order = 200)
    private SpikeResponder spikeResponder = DEFAULT_SPIKE_RESPONDER;
    // TODO: Conditionally enable based on type of source neuron?

    /**
     * The maximum number of digits to display in the tool tip.
     */
    private static final int MAX_DIGITS = 2;

    /**
     * Post synaptic response. The totality of the output of this synapse; the total contribution of this synapse to the
     * post-synaptic or target neuron. This is computed using a {@link SpikeResponder} in the case of a spiking
     * pre-synaptic neuron. In the case of a non-spiking node this is the product of the source activation and the
     * weight of a synapse, i.e. one term in a classical weighted input.
     */
    private double psr;

    /**
     * Amount to increment the neuron.
     */
    @UserParameter(label = "Increment", description = "Strength Increment", minimumValue = 0, order = 2)
    private double increment = 1;

    /**
     * Upper limit of synapse.
     */
    @UserParameter(label = "Upper bound", description = "Upper bound", minimumValue = 0, order = 3)
    private double upperBound = DEFAULT_UPPER_BOUND;

    /**
     * Lower limit of synapse.
     */
    @UserParameter(label = "Lower bound", description = "Lower bound", maximumValue = 0, order = 4)
    private double lowerBound = DEFAULT_LOWER_BOUND;

    /**
     * Time to delay sending activation to target neuron.
     */
    @UserParameter(label = "Delay", description = "delay", minimumValue = 0, order = 5)
    private int delay;

    /**
     * Parent group, if any (null if none).
     */
    private SynapseGroup parentGroup;

    /**
     * Boolean flag, indicating whether this type of synapse participates in the computation of weighted input. Set to a
     * default value of true.
     */
    @UserParameter(label = "Enabled", description = "Synapse is enabled. If disabled, it won't pass activation through", order = 6)
    private boolean enabled = true;

    /**
     * Boolean flag, indicating whether or not this synapse's strength can be changed by any means other than direct
     * user intervention.
     */
    @UserParameter(label = "Frozen", description = "Synapse is frozen (no learning) or not", order = 6)
    private boolean frozen;

    /**
     * Manages synaptic delay
     */
    private double[] delayManager;

    /**
     * Points to the location in the delay manager that corresponds to the current time.
     */
    private int dlyPtr = 0;

    /**
     * The value {@link #dlyPtr} points to in the delay manager.
     */
    private double dlyVal = 0;

    /**
     * This special tag denotes that the synapse is a template to other synapses. That is, it exists solely to store
     * parameter values for a large group of synapses. Normally synapses must have a source and target neuron. Template
     * synapses are the only case where having a null source and target is acceptable. This tag exists to prevent
     * NullPointerExceptions since some methods in synapse consult the source or target neuron before allowing certain
     * changes.
     */
    private final boolean isTemplate;

    /**
     * Support for property change events.
     */
    private transient SynapseEvents events = new SynapseEvents(this);

    /** Initialize properties */
    static {
        Properties properties = Utils.getSimbrainProperties();
        if (properties.containsKey("weightUpperBound")) {
            DEFAULT_UPPER_BOUND = Double.parseDouble(properties.getProperty("weightUpperBound"));

        }
        if (properties.containsKey("weightLowerBound")) {
            DEFAULT_LOWER_BOUND = Double.parseDouble(properties.getProperty("weightLowerBound"));
        }

    }

    /**
     * Construct a synapse using a source and target neuron, defaulting to ClampedSynapse and assuming the parent of the
     * source neuron is the parent of this synapse.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public Synapse(Neuron source, Neuron target) {
        setSourceAndTarget(source, target);
        initSpikeResponder();
        if (source != null) {
            parentNetwork = source.getNetwork();
        }
        isTemplate = source == null;
    }

    /**
     * Construct a synapse with a specified initial strength.
     *
     * @param source          source neuron
     * @param target          target neuron
     * @param initialStrength initial strength for synapse
     */
    public Synapse(Neuron source, Neuron target, double initialStrength) {
        this(source, target);
        this.forceSetStrength(initialStrength);
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified learning rule and parent network
     *
     * @param newParent    new parent network for this synapse.
     * @param source       source neuron
     * @param target       target neuron
     * @param learningRule update rule for this synapse
     */
    public Synapse(Network newParent, Neuron source, Neuron target, SynapseUpdateRule learningRule) {
        setSourceAndTarget(source, target);
        initSpikeResponder();
        setLearningRule(learningRule);
        parentNetwork = newParent;
        isTemplate = source == null;
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified learning rule. Assumes the parent network
     * is the same as the parent network of the provided source neuron.
     *
     * @param source       source neuron
     * @param target       target neuron
     * @param learningRule update rule for this synapse
     */
    public Synapse(Neuron source, Neuron target, SynapseUpdateRule learningRule) {
        this(source.getNetwork(), source, target, learningRule);
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified learning rule. Assumes the parent network
     * is the same as the parent network of the provided source neuron.
     *
     * @param newParent       new parent network for this synapse. Used when copying and pasting to new network.
     * @param source          source neuron
     * @param target          target neuron
     * @param learningRule    update rule for this synapse
     * @param templateSynapse synapse with parameters to copy
     */
    public Synapse(Network newParent, Neuron source, Neuron target, SynapseUpdateRule learningRule, Synapse templateSynapse) {
        this(templateSynapse); // invoke the copy constructor
        setSourceAndTarget(source, target);
        initSpikeResponder();
        setLearningRule(learningRule);
        parentNetwork = newParent;
    }

    /**
     * Construct a synapse using a source and target neuron, and a specified learning rule.
     *
     * @param source       source neuron
     * @param target       target neuron
     * @param learningRule update rule for this synapse
     * @param parent       parent network for this synapse.
     */
    public Synapse(Neuron source, Neuron target, SynapseUpdateRule learningRule, Network parent) {
        setSourceAndTarget(source, target);
        setLearningRule(learningRule);
        initSpikeResponder();
        parentNetwork = parent;
        isTemplate = source == null;
    }

    /**
     * Copy a synapse with a specified new parent.
     *
     * @param newParent new parent network
     * @param synapse   synapse to copy
     */
    public Synapse(final Network newParent, Synapse synapse) {
        this(synapse);
        parentNetwork = newParent;
    }

    /**
     * Copy constructor.
     *
     * @param s Synapse to used as a template for constructing a new synapse.
     */
    public Synapse(final Synapse s) {
        setLearningRule(s.getLearningRule().deepCopy());
        forceSetStrength(s.getStrength());
        setUpperBound(s.getUpperBound());
        setLowerBound(s.getLowerBound());
        setIncrement(s.getIncrement());
        setSpikeResponder(s.getSpikeResponder());
        setEnabled(s.isEnabled());
        setDelay(s.getDelay());
        this.frozen = s.frozen;
        s.initSpikeResponder();
        isTemplate = s.isTemplate;
    }

    /**
     * Makes a deep copy of a template synapse (one with no source or target).
     *
     * @param s the synapse to copy
     * @return a new synapse with all the same variable valueas as the original
     */
    public static Synapse copyTemplateSynapse(Synapse s) {
        if (s.getSource() != null || s.getTarget() != null) {
            throw new IllegalArgumentException("Synapse is not template" + " synapse.");
        }
        return new Synapse(s);
    }

    /**
     * Set a default spike responder if the spike responder has not been initialized.
     *
     * When a source neuron is spiking, the synapse should have a spike responder,
     * otherwise not.
     */
    public void initSpikeResponder() {
        if (source != null) {
            if (source.getUpdateRule() instanceof SpikingNeuronUpdateRule) {
                // If target neuron is already spiking, don't change the spike responder
                if (spikeResponder instanceof NonResponder) {
                    spikeResponder = DEFAULT_SPIKE_RESPONDER.deepCopy();
                }
            } else {
                spikeResponder = new NonResponder();
            }
        }
    }

    /**
     * Update this synapse using its current learning rule.
     */
    public void update() {
        if (isFrozen()) {
            return;
        }
        // Update synapse strengths for non-static synapses
        // TODO: creating new data holder each update!
        if (!(learningRule instanceof StaticSynapseRule)) {
            setStrength(learningRule.apply(source.getActivation(), target.getActivation(), strength,
                    new DataHolder.EmptyDataHolder()));
        }
        // Udpate psr for synapses with a spike responder
        if(!(spikeResponder instanceof NonResponder)) {
            setPsr(spikeResponder.apply(strength, psr, source.isSpike()));
        }
    }

    // TODO: Review these functions, consolidate, and add tests.
    /**
     * For spiking source neurons, returns the spike-responder's value times the synapse strength. For non-spiking
     * neurons, returns the pre-synaptic activation times the synapse strength.
     *
     * @return the post-synaptic response as determined by a spike responder.
     */
    public double calcPSR() {
        if (!enabled) {
            return 0;
        } else {
            spikeResponder.update(this);
            if (delay == 0) {
                return psr;
            } else {
                dlyVal = dequeu();
                enqueu(psr);
                return dlyVal;
            }
        }
    }

    /**
     * For non-spiking neurons returns the weighted sum, i.e. the activation of the pre-synaptic (source) neuron
     * multiplied by the strength of this synapse.
     *
     * @return the post synaptic response calculated as a simple weighted sum
     */
    public double calcWeightedSum() {
        if (!enabled) {
            return 0;
        } else {
            if (spikeResponder instanceof  NonResponder) {
                psr = source.getActivation() * strength;
            }
            if (delay != 0) {
                dlyVal = dequeu();
                enqueu(psr);
                return dlyVal;
            } else {
                return psr;
            }
        }
    }

    /**
     * The name of the learning rule of the synapse; it's "type". Used via reflection for consistency checking in the
     * gui. (Open multiple synapses and if they are of the different types the dialog is different).
     *
     * @return the name of the class of this network.
     */
    public String getType() {
        return learningRule.getClass().getSimpleName();
    }

    /**
     * @return Source neuron to which the synapse is attached.
     */
    public Neuron getSource() {
        return source;
    }

    /**
     * Sets the source and target neurons simultaneously, used only in constructors. Synapses without source or target
     * neurons are ill formed and generally speaking not allowed. The only exception to this are template synapses used
     * for instance in SynapseGroup which exist to store synapse variables for other synapses or exist as a template.
     *
     * @param newSource the source neuron to the synapse
     * @param newTarget the target neuron to the synapse
     */
    private void setSourceAndTarget(final Neuron newSource, final Neuron newTarget) {

        if (this.source != null) {
            this.source.removeEfferent(this);
        }
        if (this.target != null) {
            this.target.removeAfferent(this);
        }
        if (newSource != null && newTarget != null) {
            this.source = newSource;
            this.target = newTarget;
            newSource.addEfferent(this);
            newTarget.addAfferent(this);
        }
    }

    /**
     * @return Target neuron to which the synapse is attached.
     */
    public Neuron getTarget() {
        return target;
    }

    /**
     * @return Strength of synapse.
     */
    @Producible(defaultVisibility = false)
    public final double getStrength() {
        return strength;
    }

    /**
     * Sets the strength of the synapse.
     *
     * @param wt new strength value
     */
    @Consumable(defaultVisibility = false)
    public void setStrength(final double wt) {
        if (isTemplate) {
            forceSetStrength(wt);
            return;
        }
        if (!isFrozen()) {
            forceSetStrength(clip(source.getPolarity().clip(wt)));
        }
    }

    /**
     * @param wt the value to set the strength of the synapse to
     */
    public void forceSetStrength(final double wt) {
        strength = wt;
        events.fireStrengthUpdate();
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
        events.fireStrengthUpdate(); // to force a graphics update
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
        events.fireStrengthUpdate(); // to force a graphics update
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
    public void increment() {
        if (strength < upperBound) {
            forceSetStrength(strength + increment);
        }
    }

    /**
     * Decrement this weight by increment.
     */
    public void decrement() {
        if (strength > lowerBound) {
            forceSetStrength(strength - increment);
            strength -= increment;
        }
    }

    /**
     * Increase the absolute value of this weight by increment amount.
     */
    public void reinforce() {
        if (strength > 0) {
            increment();
        } else if (strength < 0) {
            decrement();
        } else if (strength == 0) {
            forceSetStrength(0);
        }
    }

    /**
     * Decrease the absolute value of this weight by increment amount.
     */
    public void weaken() {
        if (strength > 0) {
            decrement();
        } else if (strength < 0) {
            increment();
        } else if (strength == 0) {
            forceSetStrength(0);
        }
    }

    /**
     * Randomizes this synapse and sets the symmetric analogue to the same value. A bit of a hack, since it it is used
     * on a collection a bunch of redundancy could happen.
     */
    public void randomizeSymmetric() {
        randomize();
        Synapse symmetric = getSymmetricSynapse();
        if (symmetric != null) {
            symmetric.setStrength(strength);
        }
    }

    /**
     * Returns string for tool tip or short description.
     *
     * @return tool tip text
     */
    public String getToolTipText() {
        return "Strength: " + Utils.round(this.getStrength(), MAX_DIGITS);
    }

    /**
     * Returns symmetric synapse if there is one, null otherwise.
     *
     * @return the symmetric synapse, if any.
     */
    public Synapse getSymmetricSynapse() {
        return getTarget().getFanOut().get(getSource());
    }

    @Override
    public void randomize() {
        double newStrength = (getUpperBound() - getLowerBound()) * Math.random() + getLowerBound();
        forceSetStrength(newStrength);
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
     * Utility function for use in learning rules. If value is above or below the bounds of this synapse set it to those
     * bounds.
     *
     * @param value Value to be checked
     * @return Evaluated value
     */
    public double clip(final double value) {
        double val = value;
        if (val > upperBound) {
            val = upperBound;
        } else {
            if (val < lowerBound) {
                val = lowerBound;
            }
        }
        return val;
    }

    public SpikeResponder getSpikeResponder() {
        return spikeResponder;
    }

    public void setSpikeResponder(final SpikeResponder sr) {

        // Note that a copy of the spike responder is set. Impacts of this not known yet
        this.spikeResponder = sr.deepCopy();
    }

    /**
     * Delay manager.
     *
     * @param dly Amount of delay
     */
    public void setDelay(final int dly) {
        if (dly < 0 && source != null) {
            return;
        }
        delay = dly;

        if (delay <= 0) {
            delayManager = null;

            return;
        }

        delayManager = new double[delay];

        for (int i = 0; i < delay; i++) {
            delayManager[i] = 0;
        }
        dlyPtr = 0;
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
        if (dlyPtr == delay) {
            dlyPtr = 0;
        }
        return delayManager[dlyPtr++];
    }

    /**
     * Enqueeu.
     *
     * @param val Value to enqueu
     */
    private void enqueu(final double val) {
        if (dlyPtr == 0) {
            delayManager[delay - 1] = val;
        } else {
            delayManager[dlyPtr - 1] = val;
        }
    }

    @Override
    public String toString() {
        return getId()
                + ": Strength = " + SimbrainMath.roundDouble(getStrength(), 3)
                + " Connects "
                + (getSource() == null ? "[null]" : getSource().getId())
                + " to "
                + (getTarget() == null ? "[null]" : getTarget().getId());
    }

    /**
     * @return Returns the parent.
     */
    public Network getParentNetwork() {
        return parentNetwork;
    }

    /**
     * A better name than setSendWeightedInput. Forwarding to setSendWeightedInput for now. Possibly change name for
     * 3.0. have not done so yet so as note to break a bunch of simulations.
     *
     * @param enabled true if enabled, false otherwise.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether this synapse is enabled or not.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Convenience method for getting a reference to the parent root network.
     *
     * @return reference to root network.
     */
    public Network getNetwork() {
        if (isTemplate) {
            return null;
        } else {
            return this.getSource().getNetwork();
        }
    }

    /**
     * @return the learningRule
     */
    public SynapseUpdateRule getLearningRule() {
        return learningRule;
    }

    /**
     * Change this synapse's learning rule.
     *
     * @param newLearningRule the learningRule to set
     */
    public void setLearningRule(SynapseUpdateRule newLearningRule) {
        SynapseUpdateRule oldRule = learningRule;
        this.learningRule = newLearningRule.deepCopy();
        initSpikeResponder();
        // TODO: Needed for calls to SynapseGroup.postUnmashallingInit, which calls
        // SynapseGroup.setAndComformToTemplate. Template synapses don't seem to have
        // change support initialized.
        if (events == null) {
            events = new SynapseEvents(this);
        }
        events.fireLearningRuleUpdate(oldRule, learningRule);
    }

    /**
     * Returns a "template" synapse.
     *
     * @return the template synapse.
     */
    public static Synapse getTemplateSynapse() {
        Synapse s = new Synapse(null, null, new StaticSynapseRule(), (Network) null);
        s.setSpikeResponder(new NonResponder());
        return s;
    }

    /**
     * Returns a template synapse with a specified learning rule.
     *
     * @param rule the learning rule.
     * @return the template synapse
     */
    public static Synapse getTemplateSynapse(SynapseUpdateRule rule) {
        Synapse synapse = getTemplateSynapse();
        synapse.setLearningRule(rule);
        return synapse;
    }

    /**
     * A method which takes in a collection of synapses and returns a list of their update rules in the order in which
     * they appear in the original collection, if that collection supports a consistent order.
     *
     * @param synapseCollection The collection of synapses whose update rules we want to query.
     * @return Returns a list of synapse update rules associated with the group of synapses
     */
    public static List<SynapseUpdateRule> getRuleList(Collection<Synapse> synapseCollection) {
        ArrayList<SynapseUpdateRule> ruleList = new ArrayList<SynapseUpdateRule>(synapseCollection.size());
        for (Synapse s : synapseCollection) {
            ruleList.add(s.getLearningRule());

        }
        return ruleList;
    }

    /**
     * A template synapse is a synapse that has no proper references, but is used for setting properties. When the user
     * is ready to instantiate it, they call this method to give the proper references.
     *
     * @param source source neuron
     * @param target target neuron
     * @param parent parent network
     * @return a new synapse with these references and the base synapse's properties
     */
    public Synapse instantiateTemplateSynapse(Neuron source, Neuron target, Network parent) {
        this.source = source;
        this.target = target;
        this.parentNetwork = parent;
        return new Synapse(this);
    }

    public SynapseGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(SynapseGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    /**
     * Decay this synapse by the indicated percentage. E.g. .5 cuts the strength in half.
     *
     * @param decayPercent decay percent
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
     * @param frozen sets whether or not this synapses strength will be frozen
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        // Trying to fire an event from here causes problems relating to
        // template synapses
        if (getNetwork() != null && !isTemplate) {
            getEvents().fireClampChanged();
        }
    }

    /**
     * @return the post-synaptic response
     */
    public double getPsr() {
        return psr;
    }

    public void setPsr(double psr) {
        this.psr = psr;
    }

    @Override
    public void postUnmarshallingInit() {
        events = new SynapseEvents(this);
        if (getTarget() != null) {
            if (getTarget().getFanIn() != null) {
                getTarget().addAfferent(this);
            } else {
                System.out.println("Warning:" + getId() + " has null fanIn");
                // In the past I had to manually remove these synapses
                // when this condition occurred. However it should not occur.
                // But in case it does, I'm leaving this in. Also note that this
                // code used to be at the network level.
                // removeSynapse(synapse);
            }
        }
        if (getSource() != null) {
            if (getSource().getFanOut() != null) {
                getSource().addEfferent(this);
            } else {
                System.out.println("Warning:" + getId() + " has null fanOut");
                // removeSynapse(synapse);
            }
        }

    }

    //  TODO: Without any indication in the GUI this might be unclear to users.
    /**
     * "Clear" the synapse in the sense of setting post synaptic result to 0 and removing all queued activations from
     * the delay manager. Do NOT set strength to 0, which his a more radical move, that should not be achieved with
     * the same GUI actions as the high level "clear".
     */
    @Override
    public void clear() {
        setPsr(0);
        if (delayManager != null) {
            Arrays.fill(delayManager, 0);
        }
    }

    @Override
    public void toggleClamping() {
        setFrozen(!isFrozen());
    }

    public SynapseEvents getEvents() {
        return events;
    }

    /**
     * Returns the length in pixels of the "axon" this synapse is at the end of.
     */
    public Double getLength() {
        return SimbrainMath.distance(source.getLocation(), target.getLocation());
    }

    @Override
    public void delete() {
        // Remove references to this synapse from parent neurons
        if (getSource() != null) {
            getSource().removeEfferent(this);
        }
        if (getTarget() != null) {
            getTarget().removeAfferent(this);
        }

        getEvents().fireDeleted();
    }

    public void hardClear() {
        clear();
        setStrength(0);
    }
}
