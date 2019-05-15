package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkBasedAgent extends Agent<NetworkGenome, NetworkBasedAgent> {

    /**
     * A network to be run in the simulation
     */
    private Network agent;

    /**
     * The fitness score of this agent. null if this agent has not been evaluate yet.
     */
    private Double fitness = null;

    public NetworkBasedAgent(NetworkGenome genotype, Function<NetworkBasedAgent, Double> fitnessFunction) {
        super(genotype, fitnessFunction);
        agent = getGenome().build();
    }

    @Override
    public NetworkBasedAgent crossover(NetworkBasedAgent other) {
        return new NetworkBasedAgent(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
    }

    public Network getAgent() {
        return agent;
    }

    @Override
    public void computeFitness() {
        fitness = getFitnessFunction().apply(this);
    }

    @Override
    public Double getCurrentFitness() {
        return fitness;
    }

    @Override
    public NetworkBasedAgent copy() {
        return new NetworkBasedAgent(getGenome().copy(), getFitnessFunction());
    }
}
