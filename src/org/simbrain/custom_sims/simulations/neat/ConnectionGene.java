package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;

/**
 * ConnectionGene holds the instruction of how to create synapses in a network.
 * It also holds some other genetic information for niching and crossover.
 * @author LeoYulinLi
 *
 */
public class ConnectionGene {

    /**
     * Innovation Number
     * Currently not implemented
     */
    private int innovationNumber;

    /**
     * The index of the source node on the node gene list
     */
    private int inNode;

    /**
     * The index of the target node on the node gene list
     */
    private int outNode;

    /**
     * The weight strength the synapse will have
     */
    private double weightStrength;

    /**
     * The learning rule the synapse will have
     * Currently not implemented
     */
    private SynapseUpdateRule updateRule;

    /**
     * If false, the synapse will be calculated.
     */
    private boolean enabled;

    /**
     * Construct a ConnectionGene. The connection is always enabled.
     * @param inNode The index of the source node on the node gene list
     * @param outNode The index of the target node on the node gene list
     * @param weightStrength The weight strength the synapse will have
     * @param updateRule The learning rule the synapse will have
     */
    public ConnectionGene(int inNode, int outNode, double weightStrength, SynapseUpdateRule updateRule) {
        this.inNode = inNode;
        this.outNode = outNode;
        this.weightStrength = weightStrength;
        this.updateRule = updateRule;
        this.enabled = true;
    }

    /**
     * Construct a ConnectionGene with static learning rule. The connection is always enabled.
     * @param inNode The index of the source node on the node gene list
     * @param outNode The index of the target node on the node gene list
     * @param weightStrength The weight strength the synapse will have
     */
    public ConnectionGene(int inNode, int outNode, double weightStrength) {
        this(inNode, outNode, weightStrength, new StaticSynapseRule());
    }

    /**
     * Copy constructor.
     * @param cpy The connection gene to be copied
     */
    public ConnectionGene(ConnectionGene cpy) {
        this.inNode = cpy.inNode;
        this.outNode = cpy.outNode;
        this.weightStrength = cpy.weightStrength;
        this.updateRule = cpy.updateRule.deepCopy();
        this.enabled = cpy.enabled;
    }

    public int getInnovationNumber() {
        return innovationNumber;
    }

    public void setInnovationNumber(int innovationNumber) {
        this.innovationNumber = innovationNumber;
    }

    public int getInNode() {
        return inNode;
    }

    public void setInNode(int inNode) {
        this.inNode = inNode;
    }

    public int getOutNode() {
        return outNode;
    }

    public void setOutNode(int outNode) {
        this.outNode = outNode;
    }

    public double getWeightStrength() {
        return weightStrength;
    }

    public void setWeightStrength(double weightStrength) {
        this.weightStrength = weightStrength;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public SynapseUpdateRule getUpdateRule() {
        return updateRule;
    }

    public void setUpdateRule(SynapseUpdateRule updateRule) {
        this.updateRule = updateRule;
    }
}
