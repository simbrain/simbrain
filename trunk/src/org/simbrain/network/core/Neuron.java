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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.groups.Group;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;
import org.simbrain.network.gui.dialogs.neuron.BinaryRulePanel;
import org.simbrain.network.gui.dialogs.neuron.ClampedNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.DecayRulePanel;
import org.simbrain.network.gui.dialogs.neuron.IACRulePanel;
import org.simbrain.network.gui.dialogs.neuron.IntegrateAndFireRulePanel;
import org.simbrain.network.gui.dialogs.neuron.IzhikevichRulePanel;
import org.simbrain.network.gui.dialogs.neuron.LinearRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NakaRushtonRulePanel;
import org.simbrain.network.gui.dialogs.neuron.PointNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.RandomNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.SigmoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.SinusoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.SpikingThresholdRulePanel;
import org.simbrain.network.gui.dialogs.neuron.StochasticRulePanel;
import org.simbrain.network.gui.dialogs.neuron.ThreeValueRulePanel;
import org.simbrain.network.neuron_update_rules.BiasedUpdateRule;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.network.neuron_update_rules.IACRule;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.network.neuron_update_rules.PointNeuronRule;
import org.simbrain.network.neuron_update_rules.RandomNeuronRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SinusoidalRule;
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;
import org.simbrain.network.neuron_update_rules.StochasticRule;
import org.simbrain.network.neuron_update_rules.ThreeValueRule;
import org.simbrain.network.synapse_update_rules.ClampedSynapseRule;

/**
 * <b>Neuron</b> represents a node in the neural network. Most of the "logic" of
 * the neural network occurs here, in the update function. Subclasses must
 * override update and duplicate (for copy / paste) and cloning generally.
 */
public class Neuron {

    /**
     * The update method of this neuron, which corresponds to what kind of
     * neuron it is.
     */
    private NeuronUpdateRule updateRule;

    /** A unique id for this neuron. */
    private String id = null;

    /** An optional String description associated with this neuron. */
    private String label = "";

    /** Activation value of the neuron. The main state variable. */
    private double activation = 0;

    /** Minimum value this neuron can take. */
    private double lowerBound = -1;

    /** Maximum value this neuron can take. */
    private double upperBound = 1;

    /** Amount by which to increment or decrement neuron. */
    private double increment = .1;

    /** Temporary activation value. */
    private double buffer = 0;

    /** Value of any external inputs to neuron. */
    private double inputValue = 0;

    /** Reference to network this neuron is part of. */
    private Network parent;

    /** List of synapses this neuron attaches to. */
    private ArrayList<Synapse> fanOut = new ArrayList<Synapse>();

    /** List of synapses attaching to this neuron. */
    private ArrayList<Synapse> fanIn = new ArrayList<Synapse>();

    /** x-coordinate of this neuron in 2-space. */
    private double x;

    /** y-coordinate of this neuron in 2-space. */
    private double y;

    /** If true then do not update this neuron. */
    private boolean clamped = false;

    /** Target value. */
    private double targetValue = 0;

    /** Parent group, if any (null if none). */
    private Group parentGroup;

    /**
     * Sequence in which the update function should be called for this neuron.
     * By default, this is set to 0 for all the neurons. If you want a subset of
     * neurons to fire before other neurons, assign it a smaller priority value.
     */
    private int updatePriority = 0;

    /** Associated between names of rules and panels for editing them. */
    public static final LinkedHashMap<String, AbstractNeuronPanel> RULE_MAP = new LinkedHashMap<String, AbstractNeuronPanel>();

    /**
     * Initializes the rule map.
     */
    private void initRuleMap() {
        RULE_MAP.put(new BinaryRule().getDescription(), new BinaryRulePanel(
                parent));
        RULE_MAP.put(new ClampedSynapseRule().getDescription(),
                new ClampedNeuronRulePanel(parent));
        RULE_MAP.put(new DecayRule().getDescription(), new DecayRulePanel(
                parent));
        RULE_MAP.put(new IACRule().getDescription(), new IACRulePanel(parent));
        RULE_MAP.put(new IntegrateAndFireRule().getDescription(),
                new IntegrateAndFireRulePanel(parent));
        RULE_MAP.put(new IzhikevichRule().getDescription(),
                new IzhikevichRulePanel(parent));
        RULE_MAP.put(new LinearRule().getDescription(), new LinearRulePanel(
                parent));
        RULE_MAP.put(new NakaRushtonRule().getDescription(),
                new NakaRushtonRulePanel(parent));
        RULE_MAP.put(new PointNeuronRule().getDescription(),
                new PointNeuronRulePanel(parent));
        RULE_MAP.put(new RandomNeuronRule().getDescription(),
                new RandomNeuronRulePanel(parent));
        RULE_MAP.put(new SigmoidalRule().getDescription(),
                new SigmoidalRulePanel(parent));
        RULE_MAP.put(new SinusoidalRule().getDescription(),
                new SinusoidalRulePanel(parent));
        RULE_MAP.put(new SpikingThresholdRule().getDescription(),
                new SpikingThresholdRulePanel(parent));
        RULE_MAP.put(new StochasticRule().getDescription(),
                new StochasticRulePanel(parent));
        RULE_MAP.put(new ThreeValueRule().getDescription(),
                new ThreeValueRulePanel(parent));
    }

    /**
     * Construct a specific type of neuron from a string description.
     *
     * @param parent The parent network. Be careful not to set this to root
     *            network if the root network is not the parent.
     * @param updateRule the update method
     */
    public Neuron(final Network parent, final String updateRule) {
        this.parent = parent;
        setUpdateRule(updateRule);
        initRuleMap();
    }

    /**
     * Construct a specific type of neuron.
     *
     * @param parent The parent network. Be careful not to set this to root
     *            network if the root network is not the parent.
     * @param updateRule the update method
     */
    public Neuron(final Network parent, final NeuronUpdateRule updateRule) {
        this.parent = parent;
        setUpdateRule(updateRule);
        initRuleMap();
    }

    /**
     * Copy constructor.
     *
     * @param parent The parent network. Be careful not to set this to root
     *            network if the root network is not the parent.
     * @param n Neuron
     */
    public Neuron(final Network parent, final Neuron n) {
        this.parent = parent;
        setUpdateRule(n.getUpdateRule().deepCopy());
        setActivation(n.getActivation());
        setUpperBound(n.getUpperBound());
        setLowerBound(n.getLowerBound());
        setIncrement(n.getIncrement());
        setInputValue(n.getInputValue());
        setX(n.getX());
        setY(n.getY());
        setUpdatePriority(n.getUpdatePriority());
        setLabel(n.getLabel());
        initRuleMap();
    }

    /**
     * Perform any initialization required when creating a neuron, but after the
     * parent network has been added.
     */
    public void postUnmarshallingInit() {
        // TODO: Add checks?
        fanOut = new ArrayList<Synapse>();
        fanIn = new ArrayList<Synapse>();
        initRuleMap();
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
     * Sets the update rule using a String description. The provided description
     * must match the class name. E.g. "BinaryNeuron" for "BinaryNeuron.java".
     *
     * @param name the "simple name" of the class associated with the neuron
     *            rule to set.
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
     * @param updateRule the neuronUpdateRule to set
     */
    public void setUpdateRule(final NeuronUpdateRule updateRule) {
        NeuronUpdateRule oldRule = updateRule;
        this.updateRule = updateRule;
        for (Synapse s : getFanOut()) {
            s.initSpikeResponder();
        }
        if (getNetwork() != null) {
            getNetwork().updateTimeType();
            getNetwork().fireNeuronTypeChanged(oldRule, updateRule);
        }
        updateRule.init(this);
    }

    /**
     * Updates neuron.
     */
    public void update() {
        updateRule.update(this);
    }

    /**
     * Initialize when changing update method.
     */
    public void init() {
        updateRule.init(this);
    }

    /**
     * Sets the activation of the neuron.
     *
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
     *
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
     *
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
     *
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
     *
     * @param d Value to set increment
     */
    public void setIncrement(final double d) {
        increment = d;
    }

    /**
     * @return the fan in array list.
     */
    public List<Synapse> getFanIn() {
        return fanIn;
    }

    /**
     * @return the fan out array list.
     */
    public List<Synapse> getFanOut() {
        return fanOut;
    }

    /**
     * Increment this neuron by increment.
     */
    public void incrementActivation() {
        if (activation < upperBound) {
            activation += increment;
        }
        this.getNetwork().fireNeuronChanged(this);
    }

    /**
     * Decrement this neuron by increment.
     */
    public void decrementActivation() {
        if (activation > lowerBound) {
            activation -= increment;
        }
        this.getNetwork().fireNeuronChanged(this);
    }

    /**
     * Connect this neuron to target neuron via a weight.
     *
     * @param target the connection between this neuron and a target neuron
     */
    void addTarget(final Synapse target) {
        if (fanOut != null) {
            fanOut.add(target);
        }
    }

    /**
     * Remove this neuron from target neuron via a weight.
     *
     * @param target the connection between this neuron and a target neuron
     */
    void removeTarget(final Synapse target) {
        if (fanOut != null) {
            fanOut.remove(target);
        }
    }

    /**
     * Connect this neuron to source neuron via a weight.
     *
     * @param source the connection between this neuron and a source neuron
     */
    void addSource(final Synapse source) {
        if (fanIn != null) {
            fanIn.add(source);
        }
    }

    /**
     * Remove this neuron from source neuron via a weight.
     *
     * @param source the connection between this neuron and a source neuron
     */
    void removeSource(final Synapse source) {
        if (fanIn != null) {
            fanIn.remove(source);
        }
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
                Synapse w = fanIn.get(j);
                if (w.isSendWeightedInput()) {
                    wtdSum += w.getValue();
                }
            }
        }
        if (this.getUpdateRule() instanceof BiasedUpdateRule) {
            wtdSum += ((BiasedUpdateRule) this.getUpdateRule()).getBias();
        }

        return wtdSum;
    }

    /**
     * Randomize this neuron to a value between upperBound and lowerBound.
     */
    public void randomize() {
        setActivation(getRandomValue());
        getNetwork().fireNeuronChanged(this);
    }

    /**
     * Returns a random value between the upper and lower bounds of this neuron.
     *
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
     *
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
            ret += fanIn.get(i).getSource().getActivation();
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
        return (fanIn.contains(s) || fanOut.contains(s));
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
     * @param x The x coordinate to set.
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
     * @param y The y coordinate to set.
     */
    public void setY(final double y) {
        this.y = y;
        if (this.getNetwork() != null) {
            this.getNetwork().fireNeuronMoved(this);
        }
    }

    /**
     * Set position.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setLocation(final double x, final double y) {
        setX(x);
        setY(y);
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
            synapse.getNetwork().removeSynapse(synapse);
        }
    }

    /**
     * Delete fan out.
     */
    public void deleteFanOut() {
        for (Synapse synapse : fanOut) {
            synapse.getNetwork().removeSynapse(synapse);
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
     * @param targetValue value to set.
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
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
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
     * @param position point location of neuron
     */
    public void setPosition(Point2D position) {
        this.setX(position.getX());
        this.setY(position.getY());
    }

    /**
     * If this neuron has a bias field, randomize it within the specified
     * bounds.
     *
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
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
        for (Synapse synapse : getFanOut()) {
            synapse.randomize();
        }
    }

    /**
     * @return the rulelist
     */
    public static String[] getRulelist() {
        return RULE_MAP.keySet().toArray(new String[RULE_MAP.size()]);
    }

    /**
     * A method that returns a compact list of all the neuron update rules
     * associated with a list of neurons. The list is compact in the sense that
     * the size of the list is determined by the number of active update rule
     * objects managing the neurons in the list. For instance, the list being
     * queried may have 1000 neurons, but only 1 neuron update rule since one
     * neuron update rule object can service any number of neurons. In such a
     * case this method would return a singleton list. This makes multiple
     * consistency checks down the line much faster.
     *
     * @see org.simbrain.network.gui.dialogs.neuron.NeuronDialog.java
     * @see org.simbrain.network.gui.NetworkUtils.java
     *
     *      TODO: Written with expanding support for heterogeneous groups of
     *      neurons.
     *
     * @param neuronList The list of neurons whose update rules we want to
     *            query.
     * @return Returns a compact list of neuron update rules associated with a
     *         group of neurons
     */
    public static List<NeuronUpdateRule> getRuleList(List<Neuron> neuronList) {
        HashSet<NeuronUpdateRule> ruleSet = new HashSet<NeuronUpdateRule>();

        for (Neuron n : neuronList) {
            NeuronUpdateRule nur = n.getUpdateRule();
            if (!ruleSet.contains(nur))
                ruleSet.add(nur);
        }

        return Arrays.asList(ruleSet.toArray(new NeuronUpdateRule[ruleSet
                .size()]));

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
     * @param parentGroup the parentGroup to set
     */
    public void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;
    }
}
