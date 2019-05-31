package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkBasedAgent extends Agent<NetworkGenome, NetworkBasedAgent> {

    /**
     * A network to be run in the simulation
     */
    private Network agent;

    public List<String> activationRecoding = new LinkedList<>();

    public NetworkBasedAgent(NetworkGenome genotype, Function<NetworkBasedAgent, Double> fitnessFunction) {
        super(genotype, fitnessFunction);
        agent = getGenome().build();
    }

    @Override
    public NetworkBasedAgent crossover(NetworkBasedAgent other) {
        return new NetworkBasedAgent(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
    }

    @Override
    public void mutate() {
        getGenome().mutate();
    }

    public Network getAgent() {
        return agent;
    }

    @Override
    public void computeFitness() {
        computeFitness(this);
    }

    @Override
    public NetworkBasedAgent copy() {
        return new NetworkBasedAgent(getGenome().copy(), getFitnessFunction());
    }
}
