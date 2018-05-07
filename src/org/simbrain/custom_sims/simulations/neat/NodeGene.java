package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;

import static java.util.Objects.requireNonNull;
/**
 * NodeGene holds the instruction of how to create all the neurons in a network.
 * @author LeoYulinLi
 *
 */
public class NodeGene {

    /**
     * Types of Node.
     */
    public enum NodeType { input, hidden, output };

    /**
     * The type of this node
     */
    private NodeType type;

    /**
     * Index for this node; used in {@link ConnectionGene}.
     * TODO: Implement.
     */
    private int nodeIndex;

    //TODO: Discuss. If used could replace updateRule.  Eitehr way be clear
    // that these are prototype objects from which copies should be made
    public Neuron neuron;

    /**
     * The {@link NeuronUpdateRule} the neuron will be using. Defaults to {@link LinearRule}.
     */
    private NeuronUpdateRule updateRule = new LinearRule();

    /**
     * See {@link Neuron#increment}.
     */
    private double increment = Neuron.DEFAULT_INCREMENT;

    /**
     * See {@link Neuron#clamped}.
     */
    private boolean clamped = false;

    /**
     * The group name this node gene is in, if it is in one.
     */
    private String groupName = null;

    /**
     * Construct a node gene by specifying NodeType and NeuronUpdateRule.
     * @param type Type of neuron
     * @param updateRule NeuronUpdateRule to use
     * @param increment See {@link Neuron#increment}.
     * @param clamped See {@link Neuron#clamped}.
     */
    public NodeGene(NodeType type, NeuronUpdateRule updateRule, double increment, boolean clamped) {
        this(type, updateRule);
        this.increment = increment;
        this.clamped = clamped;
    }

    /**
     * Construct a node gene by specifying NodeType and NeuronUpdateRule.
     * @param type Type of neuron
     * @param updateRule NeuronUpdateRule to use
     */
    public NodeGene(NodeType type, NeuronUpdateRule updateRule) {
        setType(type);
        setUpdateRule(updateRule);
    }

    /**
     * Construct a hidden node by specifying NeuronUpdateRule.
     * @param updateRule NeuronUpdateRule to use
     */
    public NodeGene(NeuronUpdateRule updateRule) {
        this(NodeType.hidden, updateRule);
    }

    /**
     * Constructing a specific type of node that uses SigmoidalRule.
     * @param type Type of neuron
     */
    public NodeGene(NodeType type) {
        this(type, new SigmoidalRule());
    }

    /**
     * Construct a hidden nodes that uses SigmoidalRule.
     */
    public NodeGene() {
        this(NodeType.hidden, new SigmoidalRule());
    }

    /**
     * Copy constructor.
     * @param cpy The NodeGene to copy.
     */
    public NodeGene(NodeGene cpy) {
        this.type = cpy.type;
        this.updateRule = cpy.updateRule.deepCopy();
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public NeuronUpdateRule getUpdateRule() {
        return updateRule;
    }

    public void setUpdateRule(NeuronUpdateRule updateRule) {
        this.updateRule = requireNonNull(updateRule);
    }

    public NodeGene deepCopy() {
        return new NodeGene(this);
    }

    @Override
    public String toString() {
        return type + ": " + updateRule.getName();
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    public boolean isClamped() {
        return clamped;
    }

    public void setClamped(boolean clamped) {
        this.clamped = clamped;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
