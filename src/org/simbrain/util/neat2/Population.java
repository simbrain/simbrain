package org.simbrain.util.neat2;

import org.simbrain.util.neat.NEATRandomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A agentList has multiple agents
 *   Each agent has a genome, as well as a fitness function
 *     The genome has chromosomes (which enforce compatability of gene in cross-over)
 *     The chromosomes have genes
 *
 * Environments are currently handled at this level.
 *
 * @param <G> The genome the agent has. TODO: Find a way to get rid of this. Only here to satisfy the type check
 *           when, for example, crossover
 * @param <A> The type of agent in this population
 */
public class Population<G extends Genome, A extends Agent<G, A>> {

    private int size;

    private List<A> agentList;

    private NEATRandomizer randomizer;

    public Population(int size) {
        this.size = size;
        this.randomizer = new NEATRandomizer(System.nanoTime());
    }

    public void populate(A prototype) {
        agentList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            agentList.add(prototype.copy());
        }
    }

    public void replenish() {
        int remainingPopulation = agentList.size();
        int reproduceSize = size - remainingPopulation;
        for (int i = 0; i < reproduceSize; i++) {
            A agent1 = agentList.get(randomizer.nextInt(remainingPopulation));
            A agent2 = agentList.get(randomizer.nextInt(remainingPopulation));
            A newAgent = agent1.crossover(agent2);
            newAgent.mutate();
            agentList.add(newAgent);
        }
    }

    public Double computeNewFitness() {
        agentList.forEach(Agent::computeFitness);
        Collections.reverse(agentList);
        agentList = agentList.stream().limit(agentList.size() / 2).collect(Collectors.toList());
        return agentList.get(0).getCurrentFitness();
    }

}
