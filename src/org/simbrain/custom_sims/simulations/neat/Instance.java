package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.Network;
import static java.util.Objects.requireNonNull;

/**
 * Instance holds the Genome and the Network generated from that genome.
 * In the future, it may also hold multiple network instances and world instances.
 * @author LeoYulinLi
 *
 */
public class Instance implements Comparable<Instance> {
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
    public Instance(Genome genome) {
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
    public int compareTo(Instance o) {
        return this.fitness.compareTo(o.fitness);
    }
}
