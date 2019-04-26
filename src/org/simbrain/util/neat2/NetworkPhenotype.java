package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;

import java.util.ArrayList;
import java.util.function.Supplier;

public class NetworkPhenotype extends Phenotype<Network, NetworkGenotype> {

    /**
     * A network to be run in the simulation
     */
    private Network agent;

    /**
     * The fitness score of this agent. null if this agent has not been evaluate yet.
     */
    private Double fitness = null;

    public NetworkPhenotype(NetworkGenotype genotype, Supplier<Double> fitnessFunction) {
        super(genotype, fitnessFunction);
    }

    @Override
    public void assemble() {
        agent = getGenotype().assemble();
    }

    @Override
    public void computeFitness() {
         fitness = getFitnessFunction().get();
        ArrayList[] g = new ArrayList[100];
    }

    @Override
    public Double getCurrentFitness() {
        return fitness;
    }

    @Override
    public Phenotype<Network, NetworkGenotype> copy() {
        return new NetworkPhenotype(getGenotype(), getFitnessFunction());
    }
}
