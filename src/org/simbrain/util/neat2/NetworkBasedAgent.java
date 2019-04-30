package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;

import java.util.ArrayList;
import java.util.function.Supplier;

public class NetworkBasedAgent extends Agent<NetworkGenome> {

    /**
     * A network to be run in the simulation
     */
    private Network agent;

    /**
     * The fitness score of this agent. null if this agent has not been evaluate yet.
     */
    private Double fitness = null;

    public NetworkBasedAgent(NetworkGenome genotype, Supplier<Double> fitnessFunction) {
        super(genotype, fitnessFunction);
        agent = getGenome().build();
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
    public Agent<NetworkGenome> copy() {
        return new NetworkBasedAgent(getGenome(), getFitnessFunction());
    }
}
