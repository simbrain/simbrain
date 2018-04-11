package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.Network;
import static java.util.Objects.requireNonNull;

/**
 * Agent holds the Genome and the Network generated from that genome.
 * Provides a convenient abstraction for describing an agent in an environment
 * and tracking its fitness.
 *
 * May add world references here in which case possibly rename to "embodied agent".
 *
 * May even add mutliple networks, using the same genome, to support evolution of
 * coupled behaviors.
 *
 * @author LeoYulinLi
 *
 */
public class Agent implements Comparable<Agent> {
    /**
     * Reference of the genome this instance uses
     */
    private Genome genome;

    /**
     * The network to be created from the genome
     */
    private Network net;

    /**
     * The fitness to be evaluated from the evaluation method.
     */
    private Double fitness;

    /**
     * Create a instance and build the network from a genome.
     * @param genome A genome to build this instance
     */
    public Agent(Genome genome) {
        setGenome(genome);
        net = genome.buildNetwork();
    }

    public void setGenome(Genome genome) {
        this.genome = requireNonNull(genome);
    }

    public Genome getGenome() {
        return genome;
    }

    public Network getNet() {
        return net;
    }

    public double getFitness() {
        return fitness;
    }

    /**
     * Setting the fitness of this instance and the genome it uses.
     * @param fitness A fitness score
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
        genome.setFitness(fitness);
    }

    @Override
    public int compareTo(Agent o) {
        return this.fitness.compareTo(o.fitness);
    }
}
