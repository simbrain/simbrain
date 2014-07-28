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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.NeuronUpdateRule.InputType;
import org.simbrain.network.groups.Group;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.util.SimbrainConstants.Polarity;

/**
 * <b>Neuron</b> represents a node in the neural network. Most of the "logic" of
 * the neural network occurs here, in the update function. Subclasses must
 * override update and duplicate (for copy / paste) and cloning generally.
 */
public class Neuron {

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
    private NeuronUpdateRule updateRule;

    /** A unique id for this neuron. */
    private String id;

    /** An optional String description associated with this neuron. */
    private String label = "";

    /** Activation value of the neuron. The main state variable. */
    private double activation;

    /** Temporary activation value. */
    private double buffer;

    /**
     * Value of any external inputs to neuron. See description at
     * {@link #setInputValue(double)}
     */
    private double inputValue;

    /** Reference to network this neuron is part of. */
    private final Network parent;

    /** List of synapses this neuron attaches to. */
    private Map<Neuron, Synapse> fanOut = new HashMap<Neuron, Synapse>();

    /** List of synapses attaching to this neuron. */
    private ArrayList<Synapse> fanIn = new ArrayList<Synapse>();

    /** A marker for whether or not the update rule is an input generator. */
    private boolean generator;

    /** x-coordinate of this neuron in 2-space. */
    private double x;

    /** y-coordinate of this neuron in 2-space. */
    private double y;

    /**
     * z-coordinate of this neuron in 3-space. Currently no GUI implementation,
     * but fully useable for scripting. Like polarity this will get a full
     * implementation in the next development cycle... probably by 4.0.
     */
    private double z;

    /** If true then do not update this neuron. */
    private boolean clamped;

    /**
     * The polarity of this neuron (excitatory, inhibitory, or none, which is
     * null).
     */
    private Polarity polarity = null;

    /** Target value. */
    private double targetValue;

    /** Parent group, if any (null if none). */
    private Group parentGroup;

    /**
     * Sequence in which the update function should be called for this neuron.
     * By default, this is set to 0 for all the neurons. If you want a subset of
     * neurons to fire before other neurons, assign it a smaller priority value.
     */
    private int updatePriority;

    /**
     * An auxiliary value associated with a neuron. Getting and setting these
     * values can be useful in scripts.
     */
    private double auxValue;

    /**
     * Construct a neuron with all default values in the specified network.
     * Usually this is used as the basis for a template neuron which will be
     * edited and then copied.
     *
     * @param parent
     *            The parent network of this neuron.
     */
    public Neuron(final Network parent) {
        this.parent = parent;
        setUpdateRule(DEFAULT_UPDATE_RULE);
    }

    /**
     * Construct a specific type of neuron from a string description.
     *
     * @param parent
     *            The parent network. Be careful not to set this to root network
     *            if the root network is not the parent.
     * @param updateRule
     *            the update method
     */
    public Neuron(final Network parent, final String updateRule) {
        this.parent = parent;
        setUpdateRule(updateRule);
    }

    /**
     * Construct a specific type of neuron.
     *
     * @param parent
     *            The parent network. Be careful not to set this to root network
     *            if the root network is not the parent.
     * @param updateRule
     *            the update method
     */
    public Neuron(final Network parent, final NeuronUpdateRule updateRule) {
        this.parent = parent;
        setUpdateRule(updateRule);
    }

    /**
     * Copy constructor.
     *
     * @param parent
     *            The parent network. Be careful not to set this to root network
     *            if the root network is not the parent.
     * @param n
     *            Neuron
     */
    public Neuron(final Network parent, final Neuron n) {
        this.parent = parent;
        setClamped(n.isClamped());
        setUpdateRule(n.getUpdateRule().deepCopy());
        forceSetActivation(n.getActivation());
        setInputValue(n.getInputValue());
        setX(n.getX());
        setY(n.getY());
        setUpdatePriority(n.getUpdatePriority());
        setLabel(n.getLabel());
    }

    /**
     * Provides a deep copy of this neuron.
     *
     * @return a deep copy of this neuron.
     */
    public Neuron deepCopy() {
        return new Neuron(parent, this);
    }

    /**
     * Perform any initialization required when creating a neuron, but after the
     * parent network has been added.
     */
    public void postUnmarshallingInit() {
        // TODO: Add checks?
        fanOut = new HashMap<Neuron, Synapse>();
        fanIn = new ArrayList<Synapse>();
        // Todo: Backwards compatibility for before r3061. Maybe be removable
        // if all existing workspaces are replaced.
        if (updateRule instanceof SpikingNeuronUpdateRule) {
            updateRule.inputType = InputType.SYNAPTIC;
        } else {
            updateRule.inputType = InputType.WEIGHTED;
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
        return updateRule.getDescription();
    }

    /**
     * Sets the update rule using a String description. The provided description
     * must match the class name. E.g. "BinaryNeuron" for "BinaryNeuron.java".
     *
     * @param name
     *            the "simple name" of the class associated with the neuron rule
     *            to set.
     */
    public void setUpdateRule(String name) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName(
                "org.simbrain.network.neuron_update_rules." + name)
                .newInstance();
            setUpdateRule(newRule);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                "The provided neuron rule name, \"" + name
                    + "\", does not correspond to a known neuron type."
                    + "\n Could not find " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a new update rule. Essentially like changing the type of the network.
     *
     * @param updateRule
     *            the neuronUpdateRule to set
     */
    public void setUpdateRule(final NeuronUpdateRule updateRule) {
        NeuronUpdateRule oldRule = updateRule;
        this.updateRule = updateRule;
        for (Synapse s : getFanOut().values()) {
            s.initSpikeResponder();
        }
        if (getNetwork() != null) {
            getNetwork().updateTimeType();
            getNetwork().fireNeuronTypeChanged(oldRule, updateRule);
        }
        setGenerator(updateRule instanceof ActivityGenerator);
    }

    /**
     * Updates neuron.
     */
    public void update() {
        if (isClamped()) {
            return;
        }
        updateRule.update(this);
    }

    /**
     * Sets the activation of the neuron if it is not clamped. To unequivocally
     * set the activation use {@link #forceSetActivation(double)
     * forceSetActivation(double)}. Under normal circumstances model classes
     * will use this method.
     *
     * @param act
     *            Activation
     */
    public void setActivation(final double act) {
        if (isClamped()) {
            return;
        } else {
            activation = act;
        }
    }

    /**
     * Sets the activation of the neuron regardless of the state of the neuron.
     * Overrides clamping and any intrinsic dynamics of the neuron, and forces
     * the neuron's activation to take a specific value. Used primarily by the
     * GUI (e.g. when externally setting the values of clamped input neurons).
     *
     * @param act
     *            the new activation value
     */
    public void forceSetActivation(final double act) {
        activation = act;
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
     *
     * @param theName
     *            Neuron id
     */
    public void setId(final String theName) {
        id = theName;
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
     * Adds an efferent synapse to this neuron, i.e. adds a synapse to
     * {@link #fanOut}. Does <b>NOT</b> add this synapse to the network or any
     * intermediate bodies. If the connection is a duplicate connection the
     * original synapse connecting this neuron to a target neuron will be
     * removed and replaced by <i>Synapse s</i>.
     *
     * @param s
     */
    public void addEfferent(final Synapse synapse) {
        if (fanOut != null) {
            if (fanOut.containsKey(synapse.getTarget())) {
                getNetwork().removeSynapse(fanOut.get(synapse.getTarget()));
            }
            fanOut.put(synapse.getTarget(), synapse);
        }
    }

    /**
     * Remove this neuron from target neuron via a weight.
     *
     * @param synapse
     *            the connection between this neuron and a target neuron
     */
    public void removeEfferent(final Synapse synapse) {
        if (fanOut != null) {
            fanOut.remove(synapse.getTarget());
        }
    }

    /**
     * Adds an afferent synapse to this neuron, i.e. adds a synapse to
     * {@link #fanIn}. Does <b>NOT</b> add this synapse to the network or any
     * intermediate bodies.
     *
     * @param s
     */
    public void addAfferent(final Synapse source) {
        if (fanIn != null) {
            fanIn.add(source);
        }
    }

    /**
     * Remove this neuron from source neuron via a weight.
     *
     * @param synapse
     *            the connection between this neuron and a source neuron
     */
    public void removeAfferent(final Synapse synapse) {
        if (fanIn != null) {
            fanIn.remove(synapse);
        }
    }

    /**
     * Sums the weighted signals that are sent to this node. This sums all the
     * weighted inputs to a neuron in a connectionist sense. No spike responders
     * are called and thus this is <b>not</b> appropriate for most biological
     *  models.
     *
     * @return weighted input to this node
     */
    public double getWeightedInputs() {
        double wtdSum = inputValue;
        for (Synapse w : fanIn) {
            wtdSum += w.getWeightedSum();
        }
        return wtdSum;
    }

    /**
     * Sums the weighted <b>synaptic</b> inputs to a given neuron based on that
     * synapse's spike responder. This is usually only appropriate for
     * biological model neurons.
     * @return
     */
    public double getSynapticInput() {
        double wtdSum = inputValue;
        for (Synapse s : fanIn) {
            wtdSum += s.getValue();
        }
        return wtdSum;
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomize() {
        forceSetActivation(this.getUpdateRule().getRandomValue());
        getNetwork().fireNeuronChanged(this);
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomizeBuffer() {
        setBuffer(getUpdateRule().getRandomValue());
    }

    /**
     * Sends relevant information about the network to standard output.
     */
    public void debug() {
        System.out.println("neuron " + id);
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
     * Temporary buffer which can be used for algorithms which should not depend
     * on the order in which neurons are updated.
     *
     * @param d
     *            temporary value
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
     * Set the input value of the neuron. This is used in
     * {@link #getWeightedInputs()} as an "external input" to the neuron. When
     * external components (like input tables) send activation to the network
     * they should use this.
     *
     * @param inputValue
     *            The inputValue to set.
     */
    public void setInputValue(final double inputValue) {
        this.inputValue = inputValue;
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
     * Returns the number of neurons attaching to this one which have activity
     * above a specified threshold.
     *
     * @param threshold
     *            value above which neurons are considered "active."
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
            ret += fanIn.get(i).getSource().getActivation();
        }

        return ret;
    }

    /**
     * True if the synapse is connected to this neuron, false otherwise.
     *
     * @param s
     *            the synapse to check.
     * @return true if synapse is connected, false otherwise.
     */
    public boolean isConnected(final Synapse s) {
        return (fanIn.contains(s) || fanOut.get(s.getTarget()) != null);
    }

    /**
     * @return Returns the x coordinate.
     */
    public double getX() {
        return x;
    }

    /**
     * @return Returns the y coordinate.
     */
    public double getY() {
        return y;
    }

    /**
     * @return Returns the z coordinate.
     */
    public double getZ() {
        return z;
    }

    /**
     * @param x
     *            The x coordinate to set.
     */
    public void setX(final double x) {
        this.x = x;
        if (this.getNetwork() != null) {
            if (this.getNetwork() != null) {
                this.getNetwork().fireNeuronMoved(this);
            }
        }
    }

    /**
     * @param y
     *            The y coordinate to set.
     */
    public void setY(final double y) {
        this.y = y;
        if (this.getNetwork() != null) {
            this.getNetwork().fireNeuronMoved(this);
        }
    }

    /**
     *
     * @param z
     *            The z coordinate to set.
     */
    public void setZ(final double z) {
        this.z = z;
        if (this.getNetwork() != null) {
            this.getNetwork().fireNeuronMoved(this);
        }
    }

    /**
     * Set x, y position of a neuron.
     *
     * @param x
     *            x coordinate for neuron
     * @param y
     *            y coordinate for neuron
     */
    public void setLocation(final double x, final double y) {
        setX(x);
        setY(y);
    }

    /**
     * Translate the neuron by a specified amount.
     *
     * @param delta_x
     *            x amount to translate neuron
     * @param delta_y
     *            y amount to translate neuron
     */
    public void offset(final double delta_x, final double delta_y) {
        setX(getX() + delta_x);
        setY(getY() + delta_y);
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
     * conform to the other processes inovlved in removing synapses from a
     * network.
     *
     * @return an element by element shallow copy of the synapses in this
     *         neuron's fanIn map.
     */
    public List<Synapse> getFanInList() {
        // Pre-allocating for speed
        List<Synapse> syns =
            new ArrayList<Synapse>((int) (fanIn.size() / 0.75));
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
     *         neuron's fanOut map.
     */
    public List<Synapse> getFanOutList() {
        // Pre-allocating for speed
        List<Synapse> syns = new ArrayList<Synapse>(
            (int) (fanOut.size() / 0.75));
        for (Synapse s : fanOut.values()) {
            syns.add(s);
        }
        return syns;
    }

    /**
     * Removes all synapses from fanOut and from the network or any intermediate
     * structures.
     */
    public void deleteFanOut() {
        List<Synapse> fanOutList = getFanOutList();
        fanOut.clear();
        for (Synapse s : fanOutList) {
            parent.removeSynapse(s);
        }
    }

    /**
     * Removes all synapses from fanIn and from the network or any intermediate
     * structures.
     */
    public void deleteFanIn() {
        List<Synapse> fanInList = getFanInList();
        fanIn.clear();
        for (Synapse synapse : fanInList) {
            parent.removeSynapse(synapse);
        }
    }

    @Override
    public String toString() {
        return "Neuron [" + getId() + "] " + getType() + "  Activation = "
            + this.getActivation() + "  Location = (" + this.x + ","
            + this.y + ")";
    }

    /**
     * Forward to updaterule's clearing method. By default set activation to 0.
     */
    public void clear() {
        updateRule.clear(this);
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
     * @return the targetValue
     */
    public double getTargetValue() {
        return targetValue;
    }

    /**
     * Set target value.
     *
     * @param targetValue
     *            value to set.
     */
    public void setTargetValue(final double targetValue) {
        this.targetValue = targetValue;
    }

    /**
     * @return updatePriority for the neuron
     */
    public int getUpdatePriority() {
        return updatePriority;
    }

    /**
     * @param updatePriority
     *            to set.
     */
    public void setUpdatePriority(final int updatePriority) {
        this.updatePriority = updatePriority;
        // Update the root network's priority tree map
        if (this.getNetwork() != null) {
            // Resort the neuron in the priority sorted list
            getNetwork().resortPriorities();
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
     * @param clamped
     *            Whether this neuron is to be clamped.
     */
    public void setClamped(final boolean clamped) {
        this.clamped = clamped;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(final String label) {
        this.label = label;
        this.getNetwork().fireNeuronLabelChanged(this);
    }

    /**
     * Returns position as a 2-d point.
     *
     * @return point representation of neuron position.
     */
    public Point2D getPosition() {
        return new Point((int) this.getX(), (int) this.getY());
    }

    /**
     * Set position of neuron using a point object.
     *
     * @param position
     *            point location of neuron
     */
    public void setPosition(Point2D position) {
        this.setX(position.getX());
        this.setY(position.getY());
    }

    /**
     * If this neuron has a bias field, randomize it within the specified
     * bounds.
     *
     * @param lower
     *            lower bound for randomization.
     * @param upper
     *            upper bound for randomization.
     * */
    public void randomizeBias(double lower, double upper) {
        if (this.getUpdateRule() instanceof BiasedUpdateRule) {
            ((BiasedUpdateRule) this.getUpdateRule()).setBias((upper - lower)
                * Math.random() + lower);
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
     * @param neuronList
     *            The list of neurons whose update rules we want to query.
     * @return Returns a list of neuron update rules associated with a group of
     *         neurons
     */
    public static List<NeuronUpdateRule> getRuleList(List<Neuron> neuronList) {
        List<NeuronUpdateRule> ruleSet = new ArrayList<NeuronUpdateRule>();

        for (Neuron n : neuronList) {
            ruleSet.add(n.getUpdateRule());
        }
        return ruleSet;
    }

    /**
     * TODO: Possibly make this be a NeuronGroup. See design notes.
     *
     * @return the parentGroup
     */
    public Group getParentGroup() {
        return parentGroup;
    }

    /**
     * @param parentGroup
     *            the parentGroup to set
     */
    public void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }

    /**
     * @return Whether or not this is an input generator.
     */
    public boolean isGenerator() {
        return generator;
    }

    /**
     * Mark this neuron as an input generator. Automatically sets the
     * {@link #fanIn fan-in list} to null if true.
     *
     * @param generator
     *            Whether or not this is being set as an input generator.
     */
    public void setGenerator(boolean generator) {
        this.generator = generator;
        if (generator) {
            fanIn = null;
        }
    }

    /**
     * Convenience method to set upper bound on the neuron's update rule, if it
     * is a bounded update rule.
     *
     * @param upperBound
     *            upper bound to set.
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
     * @param lowerBound
     *            lower bound to set.
     */
    public void setLowerBound(final double lowerBound) {
        if (updateRule instanceof BoundedUpdateRule) {
            ((BoundedUpdateRule) updateRule).setLowerBound(lowerBound);
        }
    }

    /**
     * Return the upper bound for the the underlying rule, if it is bounded.
     * Else it simply returns 1. Used to color neuron activations.
     *
     * @return the upper bound, if applicable, and 1 otherwise.
     */
    public double getUpperBound() {
        if (updateRule instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) updateRule).getUpperBound();
        } else {
            return 1;
        }
    }

    /**
     * Return the lower bound for the the underlying rule, if it is bounded.
     * Else it simply returns -1. Used to color neuron activations.
     *
     * @return the upper bound, if applicable, and -1 otherwise.
     */
    public double getLowerBound() {
        if (updateRule instanceof BoundedUpdateRule) {
            return ((BoundedUpdateRule) updateRule).getLowerBound();
        } else {
            return -1;
        }
    }

    /**
     * Convenience method to set increment on the neuron's update rule.
     *
     * @param increment
     *            increment to set
     */
    public void setIncrement(final double increment) {
        updateRule.setIncrement(increment);
    }

    /**
     * @return the auxValue
     */
    public double getAuxValue() {
        return auxValue;
    }

    /**
     * @param auxValue
     *            the auxValue to set
     */
    public void setAuxValue(double auxValue) {
        this.auxValue = auxValue;
    }

    /**
     * If the neuron is polarized, it will be excitatory or inhibitory.
     *
     * @return whether this neuron is polarized.
     */
    public boolean isPolarized() {
        return polarity != null;
    }

    /**
     * Polarity of this neuron (excitatory, inhibitory, or none = null).
     *
     * @return the current polarity
     */
    public Polarity getPolarity() {
        return polarity;
    }

}
