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

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.events.NeuronEvents2;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule;
import org.simbrain.network.util.BiasedScalarData;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.network.util.SpikingScalarData;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.workspace.couplings.CouplingManagerKt;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

import static org.simbrain.util.GeomKt.plus;
import static org.simbrain.util.GeomKt.point;

/**
 * <b>Neuron</b> represents a node in the neural network. Most of the "logic" of
 * the neural network occurs here, in the update function. Subclasses must
 * override update and duplicate (for copy / paste) and cloning generally.
 *
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public class Neuron extends LocatableModel implements EditableObject, AttributeContainer {

    /**
     * The default neuron update rule. Neurons which are constructed without a
     * specified update rule will default to the rule specified here: Linear
     * with default parameters.
     */
    public static final NeuronUpdateRule DEFAULT_UPDATE_RULE = new LinearRule();

    /**
     * The update method of this neuron, which corresponds to what kind of
     * neuron it is.
     */
    @UserParameter(label = "Update Rule", isObjectType = true, useSetter = true,
            conditionalVisibilityMethod = "notInNeuronGroup", order = 100)
    private NeuronUpdateRule updateRule = DEFAULT_UPDATE_RULE;

    /**
     * Activation value of the neuron. The main state variable.
     */
    @UserParameter(label = "Activation", description = "Neuron activation. If you want a value greater" +
            " than upper bound or less than lower bound you must set those first, and close this dialog.",
            increment = .5, probDist = "Normal", useSetter = true, order = 1)
    private double activation;

    /**
     * The default increment of a neuron using this rule.
     */
    public static final double DEFAULT_INCREMENT = 0.1;

    /**
     * Amount to increment/decrement activation when manually adjusted.
     */
    @UserParameter(
            label = "Increment",
            description = "Amount that a neuron is incremented / decremented when it is manually adjusted.",
            increment = .5,
            order = 6)
    protected double increment = DEFAULT_INCREMENT;

    /**
     * Whether or not this neuron has spiked. Specifically if the result of
     * integration of a spiking neuron update rule at time t, produced an action
     * potential at time t+1. True on t+1 in that case. Always false for
     * non-spiking neuron update rules.
     */
    private boolean spike;

    /**
     * Value of any external inputs to neuron. See description at
     * {@link #addInputValue(double)}
     */
    private double inputValue;

    /**
     * Reference to network this neuron is part of.
     */
    private final Network parent;

    /**
     * Pre-allocates the number of bins in this neuron's fanIn/Out for
     * efficiency.
     */
    public static final int PRE_ALLOCATED_NUM_SYNAPSES = (int) Math.ceil(500 / 0.75);

    /**
     * Fan-out in the form of a map from target neurons to synapses.
     */
    private transient Map<Neuron, Synapse> fanOut = new HashMap<>(PRE_ALLOCATED_NUM_SYNAPSES);

    /**
     * List of synapses attaching to this neuron.
     */
    private transient ArrayList<Synapse> fanIn = new ArrayList<>(PRE_ALLOCATED_NUM_SYNAPSES);

    /**
     * Central x-coordinate of this neuron in 2-space.
     */
    private double x = 0;

    /**
     * Central y-coordinate of this neuron in 2-space.
     */
    private double y = 0;

    /**
     * z-coordinate of this neuron in 3-space. Currently no GUI implementation,
     * but fully useable for scripting. Like polarity this will get a full
     * implementation in the next development cycle... probably by 4.0.
     */
    private double z;

    /**
     * If true then do not update this neuron.
     */
    @UserParameter(
            label = "Clamped",
            description = "In general, a clamped neuron will not change over time; it is \"clamped\" "
                    + "to its current value.",
            order = 3)
    private boolean clamped;

    /**
     * The polarity of this neuron (excitatory, inhibitory, or none, which is
     * null). Used in synapse randomization, and in adding synapses.
     */
    @UserParameter(label = "Polarity", order = 10)
    private Polarity polarity = Polarity.BOTH;

    /**
     * Memory of last activation.
     */
    private double lastActivation;

    /**
     * Parent {@link NeuronGroup}, if any (null if none).  Does not apply to {@link org.simbrain.network.groups.NeuronCollection},
     * which is not a subclass of group.
     */
    private NeuronGroup parentGroup;

    /**
     * Sequence in which the update function should be called for this neuron.
     * By default, this is set to 0 for all the neurons. If you want a subset of
     * neurons to fire before other neurons, assign it a smaller priority value.
     */
    @UserParameter(label = "Update Priority", useSetter = true, description = "What order neurons should be updated" +
            "in, starting with lower values. <br> Only used with priority-based network update",
            order = 20)
    private int updatePriority;

    /**
     * An auxiliary value associated with a neuron. Getting and setting these
     * values can be useful in scripts.
     */
    private double auxValue;

    /**
     * Support for property change events.
     */
    private transient NeuronEvents2 events = new NeuronEvents2();

    /**
     * Local data holder for neuron update rule.
     */
    @UserParameter(label = "State variables", useSetter = true, isEmbeddedObject = true, order = 100, refreshSource = "updateRule.createScalarData")
    private ScalarDataHolder dataHolder = updateRule.createScalarData();

    /**
     * Construct a specific type of neuron.
     *
     * @param parent     The parent network. Be careful not to set this to root network
     *                   if the root network is not the parent.
     * @param updateRule the update method
     */
    public Neuron(final Network parent, final NeuronUpdateRule updateRule) {
        this.parent = parent;
        setUpdateRule(updateRule);
    }

    /**
     * Construct a neuron with all default values in the specified network.
     * Sometimes used as the basis for a template neuron which will be edited
     * and then copied. Also used in scripts.
     *
     * @param parent The parent network of this neuron.
     */
    public Neuron(final Network parent) {
        this(parent, DEFAULT_UPDATE_RULE.deepCopy());
    }

    /**
     * Copy constructor.
     *
     * @param parent The parent network. Be careful not to set this to root network
     *               if the root network is not the parent.
     * @param n      Neuron
     */
    public Neuron(final Network parent, final Neuron n) {
        this.parent = parent;
        setUpdateRule(n.getUpdateRule().deepCopy());
        setDataHolder(n.getDataHolder().copy());
        setClamped(n.isClamped());
        setIncrement(n.getIncrement());
        forceSetActivation(n.getActivation());
        x = n.x;
        y = n.y;
        setUpdatePriority(n.getUpdatePriority());
        setLabel(n.getLabel());
    }

    public ScalarDataHolder getDataHolder() {
        return dataHolder;
    }

    public void setDataHolder(ScalarDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    /**
     * Provides a deep copy of this neuron.
     *
     * @return a deep copy of this neuron.
     */
    public Neuron deepCopy() {
        return new Neuron(parent, this);
    }

    @Override
    public void postOpenInit() {
        events = new NeuronEvents2();
        fanOut = new HashMap<>();
        fanIn = new ArrayList<>();
        if (polarity == null) {
            polarity = Polarity.BOTH;
        }
    }

    /**
     * Returns the time type of this neuron's update rule.
     *
     * @return the time type.
     */
    public TimeType getTimeType() {
        return updateRule.getTimeType();
    }

    /**
     * Returns the current update rule.
     *
     * @return the neuronUpdateRule
     */
    public NeuronUpdateRule getUpdateRule() {
        return updateRule;
    }

    /**
     * Returns the current update rule's description (name).
     *
     * @return the neuronUpdateRule's description
     */
    public String getUpdateRuleDescription() {
        return updateRule.getName();
    }

    /**
     * Sets the update rule using a String description. The provided description
     * must match the class name. E.g. "BinaryNeuron" for "BinaryNeuron.java".
     *
     * @param name the "simple name" of the class associated with the neuron rule
     *             to set.
     */
    public void setUpdateRule(String name) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + name).newInstance();
            setUpdateRule(newRule);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The provided neuron rule name, \"" + name + "\", does not correspond to a known neuron type." + "\n Could not find " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a new update rule. Essentially like changing the type of the network.
     *
     * @param updateRule the neuronUpdateRule to set
     */
    public void setUpdateRule(final NeuronUpdateRule updateRule) {

        NeuronUpdateRule oldRule = this.updateRule;
        this.updateRule = updateRule;
        dataHolder = updateRule.createScalarData();

        if (getNetwork() != null) {
            getNetwork().updateTimeType();
            events.getUpdateRuleChanged().fireAndForget(oldRule, updateRule);
        }
    }

    /**
     * Change the current update rule but perform no other initialization.
     */
    public void changeUpdateRule(final NeuronUpdateRule updateRule, final ScalarDataHolder data) {
        this.updateRule = updateRule;
        this.dataHolder = data;
    }

    public void clip() {
        if (updateRule instanceof BoundedUpdateRule) {
            activation = ((BoundedUpdateRule) updateRule).clip(activation);
        }
    }

    @Override
    public void updateInputs() {
        fanIn.forEach(Synapse::updateOutput);
        addInputValue(getWeightedInputs());
    }

    @Override
    public void update() {
        if (isSpike()) {
            setSpike(false);
        }
        if (isClamped()) {
            return;
        }
        updateRule.apply(this, dataHolder);
        inputValue = 0.0;
    }

    /**
     * Sets the activation of the neuron if it is not clamped. To unequivocally
     * set the activation use {@link #forceSetActivation(double)
     * forceSetActivation(double)}. Under normal circumstances model classes
     * will use this method.
     *
     * @param act Activation
     */
    @Consumable(defaultVisibility = false)
    public void setActivation(final double act) {
        lastActivation = getActivation();
        if (isClamped()) {
            return;
        } else {
            activation = act;
            clip();
        }
        events.getActivationChanged().fireAndForget(lastActivation, act);
    }

    /**
     * Sets the activation of the neuron regardless of the state of the neuron.
     * Overrides clamping and any intrinsic dynamics of the neuron, and forces
     * the neuron's activation to take a specific value. Used primarily by the
     * GUI (e.g. when externally setting the values of clamped input neurons).
     *
     * @param act the new activation value
     */
    @Consumable(customPriorityMethod = "forceSetActivationCouplingPriority")
    public void forceSetActivation(final double act) {
        lastActivation = getActivation();
        activation = act;
        events.getActivationChanged().fireAndForget(lastActivation, act);
    }

    @Producible()
    public double getActivation() {
        return activation;
    }

    /**
     * @return an unmodifiable version of the fanIn list.
     */
    public List<Synapse> getFanIn() {
        return Collections.unmodifiableList(fanIn);
    }

    /**
     * @return an unmodifiable version of the fanOut map.
     */
    public Map<Neuron, Synapse> getFanOut() {
        return Collections.unmodifiableMap(fanOut);
    }

    /**
     * @return the fan out map. Unsafe because the fan out map and the returned map are the same and thus modifications
     * to one will affect the other. Here for performance reasons.
     */
    public Map<Neuron, Synapse> getFanOutUnsafe() {
        return fanOut;
    }

    /**
     * @return the fan in list. Unsafe because the fan in list and the returned list are the same and thus modifications
     * to one will affect the other. Here for performance reasons.
     */
    public List<Synapse> getFanInUnsafe() {
        return fanIn;
    }

    /**
     * Adds an efferent (outgoing) synapse to this neuron, i.e. adds a synapse to
     * {@link #fanOut}. Used when constructing synapses. Should not be called directly
     *
     * Does <b>NOT</b> add this synapse to the network or any
     * intermediate bodies. If the connection is a duplicate connection the
     * original synapse connecting this neuron to a target neuron will be
     * removed and replaced by <i>Synapse s</i>.
     *
     * @param synapse the synapse for which this neuron is a source to add.
     */
    public void addToFanOut(final Synapse synapse) {
        if (fanOut != null) {
            fanOut.put(synapse.getTarget(), synapse);
        }
    }

    /**
     * Remove an efferent (outgoing) weight from this neuron. Used by synapse but should not generally be called
     * directly.
     */
    public void removeFromFanOut(final Synapse synapse) {
        if (fanOut != null) {
            fanOut.remove(synapse.getTarget());
        }
    }

    /**
     * Adds an afferent (incoming) synapse to this neuron, i.e. adds a synapse to
     * {@link #fanIn}. Used when constructing synapses. Should not be called directly
     *
     * Does <b>NOT</b> add this synapse to the network or any intermediate bodies.
     */
    public void addToFanIn(final Synapse source) {
        if (fanIn != null) {
            fanIn.add(source);
        }
    }

    /**
     * Remove an afferent (incoming) weight from this neuron. Used by synapse but should not generally be called
     * directly.
     */
    public void removeFromFanIn(final Synapse synapse) {
        if (fanIn != null) {
            fanIn.remove(synapse);
        }
    }

    /**
     * Sums the weighted inputs to this node, by summing the ouptut from incoming synapses,
     * which can either be connectionist (weight times source activation) or the output of a spike responder.
     */
    public double getWeightedInputs() {
        double wtdSum = 0;
        for (Synapse synapse : fanIn) {
            wtdSum += synapse.getPsr();
        }
        return wtdSum;
    }

    /**
     * Returns the sum of post-synaptic responses of all incoming neurons connected to this one by negative weights.
     * This automatically includes neurons whose polarity is excitatory, since they only produce positive outgoing
     * weights.
     */
    public double getExcitatoryInputs() {
         return fanIn.stream()
                    .filter(s -> s.getStrength() > 0.0)
                    .map(Synapse::getPsr)
                    .reduce(Double::sum).orElse(0.0);
    }

    /**
     * Returns the sum of post-synaptic responses of all incoming neurons connected to this one by negative weights.
     * This automatically includes neurons whose polarity is inhibitory, since they only produce negative outgoing
     * weights.
     */
    public double getInhibitoryInputs() {
        return fanIn.stream()
                    .filter(s -> s.getStrength() < 0.0)
                    .map(Synapse::getPsr)
                    .reduce(Double::sum).orElse(0.0);
    }

    /**
     * Returns "external input" to neuron, separate from any input from connected neurons.
     */
    public double getInput() {
        return inputValue;
    }

    @Override
    public void randomize() {
        forceSetActivation(this.getUpdateRule().getRandomValue());
    }

    /**
     * Sends relevant information about the network to standard output.
     */
    public void debug() {
        System.out.println("neuron " + getId());
        System.out.println("fan in");

        for (int i = 0; i < fanIn.size(); i++) {
            Synapse tempRef = fanIn.get(i);
            System.out.println("fanIn [" + i + "]:" + tempRef);
        }

        System.out.println("fan out");

        for (int i = 0; i < fanOut.size(); i++) {
            Synapse tempRef = fanOut.get(i);
            System.out.println("fanOut [" + i + "]:" + tempRef);
        }
    }

    /**
     * Returns the root network this neuron is embedded in.
     *
     * @return root network.
     */
    public Network getNetwork() {
        return parent;
    }

    /**
     * Add to the input value of the neuron. When external components (like input tables) send activation to the
     * network they should use this. Called in couplings (by reflection) to allow multiple values to be added each
     * time step to a neuron. Inputs are cleared each time step.
     */
    @Consumable(description = "Add activation", customPriorityMethod = "addInputValueCouplingPriority")
    public void addInputValue(double toAdd) {
        inputValue += toAdd;
    }

    /**
     * The name of the update rule of this neuron; it's "type". Used via
     * reflection for consistency checking in the gui. (Open multiple neurons
     * and if they are of the different types the dialog is different).
     *
     * @return the name of the class of this network.
     */
    public String getType() {
        return updateRule.getClass().getSimpleName();
    }

    /**
     * Returns the sum of the strengths of the weights attaching to this neuron.
     *
     * @return the sum of the incoming weights to this neuron.
     */
    public double getSummedIncomingWeights() {
        double ret = 0;

        for (int i = 0; i < fanIn.size(); i++) {
            Synapse tempRef = fanIn.get(i);
            ret += tempRef.getStrength();
        }

        return ret;
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

        for (Synapse synapse : fanIn) {
            ret += synapse.getSource().getActivation();
        }

        return ret;
    }

    /**
     * True if the synapse is connected to this neuron, false otherwise.
     *
     * @param s the synapse to check.
     * @return true if synapse is connected, false otherwise.
     */
    public boolean isConnected(final Synapse s) {
        return (fanIn.contains(s) || fanOut.get(s.getTarget()) != null);
    }

    /**
     * Delete connected synapses and remove them from the network and any other
     * structures.
     */
    public void deleteConnectedSynapses() {
        deleteFanIn();
        deleteFanOut();
    }

    /**
     * Used for deletion to avoid a ConcurrentModificationException as well as
     * conform to the other processes involved in removing synapses from a
     * network.
     *
     * @return an element by element shallow copy of the synapses in this
     * neuron's fanIn map.
     */
    private List<Synapse> getFanInList() {
        // Pre-allocating for speed
        List<Synapse> syns = new ArrayList<Synapse>((int) (fanIn.size() / 0.75));
        for (Synapse s : fanIn) {
            syns.add(s);
        }
        return syns;
    }

    /**
     * Used for deletion to avoid a ConcurrentModificationException as well as
     * conform to the other processes inovlved in removing synapses from a
     * network.
     *
     * @return an element by element shallow copy of the synapses in this
     * neuron's fanOut map.
     */
    private List<Synapse> getFanOutList() {
        // Pre-allocating for speed
        List<Synapse> syns = new ArrayList<Synapse>((int) (fanOut.size() / 0.75));
        for (Synapse s : fanOut.values()) {
            syns.add(s);
        }
        return syns;
    }

    /**
     * Removes all synapses from fanOut and from the network or any intermediate
     * structures.
     */
    private void deleteFanOut() {
        List<Synapse> fanOutList = getFanOutList();
        fanOut.clear();
        for (Synapse s : fanOutList) {
            s.delete();
        }
    }

    /**
     * Removes all synapses from fanIn and from the network or any intermediate
     * structures.
     */
    private void deleteFanIn() {
        List<Synapse> fanInList = getFanInList();
        fanIn.clear();
        for (Synapse synapse : fanInList) {
            synapse.delete();
        }
    }

    @Override
    public String toString() {
        return getId() + ": " + getType() + " Activation = " + SimbrainMath.roundDouble(this.getActivation(), 3);
    }

    @Override
    public void clear() {
        inputValue = 0.0;
        setActivation(0.0);
        updateRule.clear(this);
    }

    public void clearInput() {
        inputValue = 0.0;
    }

    @Override
    public void increment() {
        updateRule.contextualIncrement(this);
    }

    @Override
    public void decrement() {
        updateRule.contextualDecrement(this);
    }

    @Override
    public void toggleClamping() {
        setClamped(!clamped);
    }

    /**
     * Forward to update rule's tool tip method, which returns string for tool
     * tip or short description.
     *
     * @return tool tip text
     */
    public String getToolTipText() {
        return updateRule.getToolTipText(this);
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
        // Update the root network's priority tree map
        if (this.getNetwork() != null) {
            // Resort the neuron in the priority sorted list
            getNetwork().resortPriorities();
        }
    }

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
        getEvents().getClampChanged().fireAndForget();
    }

    /**
     * If this neuron has a bias field, randomize it within the specified
     * bounds.
     *
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBias(double lower, double upper) {
        if (dataHolder instanceof BiasedScalarData) {
            ((BiasedScalarData) dataHolder).setBias((upper - lower) * Math.random() + lower);
        }
    }

    /**
     * Randomize all synapses that attach to this neuron.
     */
    public void randomizeFanIn() {
        for (Synapse synapse : getFanIn()) {
            synapse.randomize();
        }

    }

    /**
     * Randomize all synapses that attach to this neuron.
     */
    public void randomizeFanOut() {
        for (Synapse synapse : getFanOut().values()) {
            synapse.randomize();
        }
    }

    /**
     * A method that returns a list of all the neuron update rules associated
     * with a list of neurons.
     *
     * @param neuronList The list of neurons whose update rules we want to query.
     * @return Returns a list of neuron update rules associated with a group of
     * neurons
     */
    public static List<NeuronUpdateRule> getRuleList(List<Neuron> neuronList) {
        return neuronList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());
    }

    // Search for old commented out uses
    public NeuronGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(NeuronGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    /**
     * Convenience method to set upper bound on the neuron's update rule, if it
     * is a bounded update rule.
     *
     * @param upperBound upper bound to set.
     */
    public void setUpperBound(final double upperBound) {
        if (updateRule instanceof BoundedUpdateRule) {
            ((BoundedUpdateRule) updateRule).setUpperBound(upperBound);
        }
    }

    /**
     * Convenience method to set lower bound on the neuron's update rule, if it
     * is a bounded update rule.
     *
     * @param lowerBound lower bound to set.
     */
    public void setLowerBound(final double lowerBound) {
        if (updateRule instanceof BoundedUpdateRule) {
            ((BoundedUpdateRule) updateRule).setLowerBound(lowerBound);
        }
    }

    /**
     * Return the upper bound for the the underlying rule, if it is bounded.
     * Else it simply returns a "graphical" upper bound. Used to color neuron
     * activations.
     *
     * @return the upper bound, if applicable, and 1 otherwise.
     */
    public double getUpperBound() {
        if (updateRule instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) updateRule).getUpperBound();
        } else {
            return updateRule.getGraphicalUpperBound();
        }
    }

    /**
     * Return the lower bound for the the underlying rule, if it is bounded.
     * Else it simply returns the "graphical" lower bound. Used to color neuron
     * activations.
     *
     * @return the upper bound, if applicable, and -1 otherwise.
     */
    public double getLowerBound() {
        if (updateRule instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) updateRule).getLowerBound();
        } else {
            return updateRule.getGraphicalLowerBound();
        }
    }

    /**
     * Used by reflection by {@link #updateRule} to determine if the update rule should be editable or not.
     */
    public boolean notInNeuronGroup() {
        return !(this.getParentGroup() instanceof NeuronGroup);
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    public double getAuxValue() {
        return auxValue;
    }

    public void setAuxValue(double auxValue) {
        this.auxValue = auxValue;
    }

    /**
     * If the neuron is polarized, it will be excitatory or inhibitory.
     *
     * @return whether this neuron is polarized.
     */
    public boolean isPolarized() {
        return polarity != null && polarity != Polarity.BOTH;
    }

    /**
     * Polarity of this neuron (excitatory, inhibitory, or none = null).
     *
     * @return the current polarity
     */
    public Polarity getPolarity() {
        return polarity;
    }

    /**
     * Note that setting polarity updates fan-out synapses, e.g. inbhitory nodes ensures all fan-out
     * synapses are negative. See {@link Polarity}
     */
    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
        fanOut.values().forEach(s -> s.setStrength(polarity.value(s.getStrength())));
        events.getColorChanged().fireAndForget();
    }

    // TODO: Move these methods to SpikingScalarData?

    public boolean isSpike() {
        return spike;
    }

    public void setSpike(boolean spike) {
        this.spike = spike;
        if (dataHolder instanceof SpikingScalarData) {
            ((SpikingScalarData) dataHolder).setHasSpiked(spike, parent.getTime());
        }
        events.getSpiked().fireAndForget(spike);
    }

    public Double getLastSpikeTime() {
        return ((SpikingScalarData) dataHolder).getLastSpikeTime();
    }

    public double getLastActivation() {
        return lastActivation;
    }

    @NotNull
    @Override
    public Point2D getLocation() {
        return new Point2D.Double(x, y);
    }

    @Override
    public void setLocation(@NotNull Point2D position) {
        setLocation(position, true);
    }

    public void setLocation(Point2D position, boolean fireEvent) {
        x = position.getX();
        y = position.getY();
        if (fireEvent) {
            events.getLocationChanged().fireAndForget();
        }
    }
    public void setLocation(final double x, final double y, boolean fireEvent) {
        setLocation(point(x, y), fireEvent);
    }
    public double[] getPosition3D() {
        return new double[]{x, y, z};
    }

    /**
     * Convenience method for setting the xyz coordinates from
     * an array with (at least) 3 values. Elements beyond position
     * 2 will be ignored.
     *
     * @param xyz - array of coordinate values {x, y, z}
     */
    public void setPosition3D(double[] xyz) {
        setPosition3D(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Convenience method for setting location in 3D rather than just 2D
     * space.
     */
    public void setPosition3D(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(final double x, boolean fireEvent) {
        this.x = x;
        if (fireEvent) {
            events.getLocationChanged().fireAndForget();
        }
    }

    public void setX(final double x) {
        setX(x, true);
    }

    public void setY(final double y, boolean fireEvent) {
        this.y = y;
        if (fireEvent) {
            events.getLocationChanged().fireAndForget();
        }
    }

    public void setY(final double y) {
        setY(y, true);
    }

    public void setZ(final double z) {
        this.z = z;
        events.getLocationChanged().fireAndForget();
    }

    /**
     * Translate the neuron by a specified amount.
     *
     * @param delta_x x amount to translate neuron
     * @param delta_y y amount to translate neuron
     */
    public void offset(final double delta_x, final double delta_y) {
        offset(delta_x, delta_y, true);
    }

    public void offset(final double delta_x, final double delta_y, boolean fireEvent) {
        Point2D delta = point(delta_x, delta_y);
        setLocation(plus(getLocation(), delta), fireEvent);
    }

    @Override
    public String getName() {
        return getId();
    }

    public NeuronEvents2 getEvents() {
        return events;
    }

    @Override
    public void delete() {
        getNetwork().updatePriorityList();
        deleteConnectedSynapses();
        events.getDeleted().fireAndBlock(this);
    }

    /**
     * When the neuron is not clamped, couplings should use add inputs.  Called by reflection using
     * {@link Consumable#customPriorityMethod()}
     */
    public int addInputValueCouplingPriority() {
        if (isClamped()) {
            return CouplingManagerKt.LOW_PRIORITY;
        } else {
            return CouplingManagerKt.HIGH_PRIORITY;
        }
    }


    /**
     * When the neuron is clamped, couplings should use force set activation.  Called by reflection using
     * {@link Consumable#customPriorityMethod()}
     */
    public int forceSetActivationCouplingPriority() {
        if (isClamped()) {
            return CouplingManagerKt.HIGH_PRIORITY;
        } else {
            return CouplingManagerKt.LOW_PRIORITY;
        }
    }

}
