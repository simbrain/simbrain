package org.simbrain.util.neat;

import static java.util.Objects.requireNonNull;

import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.util.Utils;

/**
 * ConnectionGene holds the instruction of how to create synapses in a network.
 * It also holds some other genetic information for niching and crossover.
 * @author LeoYulinLi
 *
 */
public class ConnectionGene {

    /**
     * Innovation Number.
     */
    private int innovationNumber;

    // TODO: Alternative to integer indices.  Discuss
    public NodeGene sourceGene;
    public NodeGene targetGene;

    // directly to source and target genes?
    /**
     * The index of the source node in the node gene list maintained in {@link Genome}
     */
    private int sourceNode;

    /**
     * The index of the target node in the node gene list maintained in {@link Genome}
     */
    private int targetNode;

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
     *
     * @param sourceNode The index of the source node on the node gene list
     * @param targetNode The index of the target node on the node gene list
     * @param weightStrength The weight strength the synapse will have
     * @param updateRule The learning rule the synapse will have
     */
    public ConnectionGene(int sourceNode, int targetNode, double weightStrength, SynapseUpdateRule updateRule) {
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.weightStrength = weightStrength;
        this.updateRule = updateRule;
        this.enabled = true;
    }

    /**
     * Construct a ConnectionGene with static learning rule. The connection is always enabled.
     * @param sourceNode The index of the source node on the node gene list
     * @param targetNode The index of the target node on the node gene list
     * @param weightStrength The weight strength the synapse will have
     */
    public ConnectionGene(int sourceNode, int targetNode, double weightStrength) {
        this(sourceNode, targetNode, weightStrength, new StaticSynapseRule());
    }

    /**
     * Copy constructor.
     *
     * @param cpy The connection gene to be copied
     */
    public ConnectionGene(ConnectionGene cpy) {
        this(cpy.sourceNode, cpy.targetNode, cpy.weightStrength, cpy.updateRule.deepCopy());
        this.enabled = cpy.enabled;
        this.innovationNumber = cpy.innovationNumber;
    }

    public int getInnovationNumber() {
        return innovationNumber;
    }

    public void setInnovationNumber(int innovationNumber) {
        this.innovationNumber = innovationNumber;
    }

    public int getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(int sourceNode) {
        this.sourceNode = sourceNode;
    }

    public int getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(int targetNode) {
        this.targetNode = targetNode;
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

    @Override
    public String toString() {
        return "Innov " + innovationNumber + ": Input " + sourceNode + " -" + (enabled ? "-" : "×") + "-> Output " + targetNode
            + " (" + Utils.round(weightStrength, 2) + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + sourceNode;
        result = prime * result + targetNode;
        result = prime * result + ((updateRule == null) ? 0 : updateRule.getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConnectionGene)) {
            return false;
        }
        ConnectionGene other = (ConnectionGene) obj;
        if (sourceNode != other.sourceNode) {
            return false;
        }
        if (targetNode != other.targetNode) {
            return false;
        }
        if (updateRule == null) {
            if (other.updateRule != null) {
                return false;
            }
        } else if (!updateRule.getName().equals(other.updateRule.getName())) {
            return false;
        }
        return true;
    }
}
