package org.simbrain.util.neat2;

import org.simbrain.network.core.Network;
import org.simbrain.util.geneticalgorithm.Agent;

import java.util.*;
import java.util.function.Function;

public class NetworkAgent extends Agent<NetworkGenome, NetworkAgent> {

    /**
     * A network to be run in the simulation
     */
    private Network agent;

    public List<String> activationRecoding = new LinkedList<>();

    public NetworkAgent(NetworkGenome genotype, Function<NetworkAgent, Double> fitnessFunction) {
        super(genotype, fitnessFunction);
        agent = getGenome().build();
    }

    @Override
    public NetworkAgent crossover(NetworkAgent other) {
        return new NetworkAgent(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
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
    public NetworkAgent copy() {
        return new NetworkAgent(getGenome().copy(), getFitnessFunction());
    }
}
