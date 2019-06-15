package org.simbrain.util.neat2;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.neat.ConnectionGene;

import java.awt.geom.Point2D;

/**
 * A gene representing a node of a given type: input, hidden, or output.
 */
public class NodeGene extends Gene<Neuron> {

    /**
     * Types of Node.
     */
    public enum NodeType { input, hidden, output }

    /**
     * The type of this node
     */
    private NodeType type;

    /**
     * Prototype neuron used when the gene is "expressed".
     */
    private Neuron prototype;

    /**
     * Create a node gene.  Defaults to hidden.
     */
    public NodeGene() {
        this(NodeType.hidden);
    }

    /**
     * Create a node gene of a specified type
     *
     * @param type the type for this gene
     */
    public NodeGene(NodeType type) {
        this.type = type;
        this.prototype = new Neuron(null);
        init();
    }

    /**
     * Initialize the node gene.
     */
    private void init() {
        this.prototype = new Neuron(null);
        //if (type == NodeType.hidden) {
        //    SigmoidalRule rule = new SigmoidalRule();
        //    rule.setUpperBound(1);
        //    rule.setLowerBound(-1);
        //    prototype.setUpdateRule(rule);
        //}
    }

    @Override
    public Neuron getPrototype() {
        return prototype;
    }

    @Override
    public void mutate() {
        // No implementation
    }

    @Override
    public NodeGene copy() {
        NodeGene ret = new NodeGene();
        ret.type = this.type;
        ret.prototype = this.prototype.deepCopy();
        return ret;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
        if (type != NodeType.hidden) {
            this.prototype.setUpdateRule(new LinearRule());
        }
    }
}
