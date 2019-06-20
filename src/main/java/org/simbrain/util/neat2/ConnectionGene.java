package org.simbrain.util.neat2;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat2.testsims.Xor;

/**
 * A description of a connection. Note that connection genes are associated with innovation numbers but these are stored
 * at the {@link NetworkGenome} level.
 */
public class ConnectionGene extends Gene<Synapse> {

    /**
     * The index of the source node in the node gene list maintained in
     */
    private int sourceIndex;

    /**
     * The index of the target node in the node gene list maintained in
     */
    private int targetIndex;

    /**
     * If false, the synapse will be calculated.
     */
    private boolean enabled;

    private SimbrainRandomizer randomizer;

    private Synapse prototype = new Synapse(null, null, 1.0);

    /**
     * Connection genes need to be aware of some features of NetworkGenome configuration.
     */
    private NetworkGenome.Configuration configuration;

    /**
     * Construct a ConnectionGene. The connection is always enabled.
     *
     * @param sourceIndex The index of the source node on the node gene list
     * @param targetIndex The index of the target node on the node gene list
     * @param weightStrength The weight strength the synapse will have
     * @param updateRule The learning rule the synapse will have
     */
    public ConnectionGene(int sourceIndex, int targetIndex, double weightStrength, SynapseUpdateRule updateRule) {
        this.sourceIndex = sourceIndex;
        this.targetIndex = targetIndex;
        this.prototype.setStrength(weightStrength);
        this.prototype.setLearningRule(updateRule);
        this.enabled = true;
    }

    /**
     * Construct a ConnectionGene with static learning rule. The connection is always enabled.
     * @param sourceIndex The index of the source node on the node gene list
     * @param targetIndex The index of the target node on the node gene list
     * @param weightStrength The weight strength the synapse will have
     */
    public ConnectionGene(int sourceIndex, int targetIndex, double weightStrength) {
        this(sourceIndex, targetIndex, weightStrength, new StaticSynapseRule());
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public void setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public void setTargetIndex(int targetIndex) {
        this.targetIndex = targetIndex;
    }

    @Override
    public Synapse getPrototype() {
        return prototype;
    }

    @Override
    public void mutate() {
        double newStrength =
                randomizer.nextDouble(-configuration.getMaxConnectionMutation(), configuration.getMaxConnectionMutation())
                        + prototype.getStrength();
        if (newStrength > prototype.getUpperBound()) {
            newStrength = prototype.getUpperBound();
        } else if (newStrength < prototype.getLowerBound()) {
            newStrength = prototype.getLowerBound();
        }
        prototype.setStrength(newStrength);
    }

    @Override
    public ConnectionGene copy() {
        ConnectionGene ret = new ConnectionGene(sourceIndex, targetIndex, prototype.getStrength(), prototype.getLearningRule());
        ret.randomizer = new SimbrainRandomizer(randomizer.nextLong());
        ret.enabled = enabled;
        return ret;
    }

    public void setRandomizer(SimbrainRandomizer randomizer) {
        this.randomizer = randomizer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectionGene that = (ConnectionGene) o;

        if (sourceIndex != that.sourceIndex) return false;
        return targetIndex == that.targetIndex;
    }

    /**
     * Used in the "innovationNumberMap" in {@link NetworkGenome}.
     */
    @Override
    public int hashCode() {
        int result = sourceIndex;
        result = 31 * result + targetIndex;
        return result;
    }

    public void setConfiguration(NetworkGenome.Configuration configuration) {
        this.configuration = configuration;
    }
}
