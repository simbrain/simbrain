package org.simbrain.util.geneticalgorithm;

import org.simbrain.util.math.SimbrainRandomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The top level for genetic algorithms in Simbrain. A set of agents, each of which expresses
 * a genotype and produces a fitness value at each generation.  Contains classes to initialize
 * a population and run an evolutionary simulation.
 *
 * Environments are currently handled at this level.
 *
 * @param <G> The type of genes the population evolves, "genotypes"
 * @param <A> The type of agent the population evolves, "phenotypes"
 */
public class Population<G extends Genome, A extends Agent> {

    private int size;

    private List<A> agentList;

    private SimbrainRandomizer randomizer;

    public Population(int size) {
        this.size = size;
        this.randomizer = new SimbrainRandomizer(System.nanoTime());
    }

    public void populate(A prototype) {
        agentList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            agentList.add((A) prototype.copy());
        }
    }

    public void replenish() {
        int remainingPopulation = agentList.size();
        int reproduceSize = size - remainingPopulation;
        for (int i = 0; i < reproduceSize; i++) {
            int index1 = randomizer.nextInt(remainingPopulation);
            int index2 = randomizer.nextInt(remainingPopulation);
            while (index2 == index1) {
                index2 = randomizer.nextInt(remainingPopulation);
            }
            A agent1 = agentList.get(index1);
            A agent2 = agentList.get(index2);
            A newAgent = (A) agent1.crossover(agent2);
            newAgent.mutate();
            agentList.add(newAgent);
        }
    }

    public Double computeNewFitness() {
        agentList.forEach(Agent::computeFitness);
        Collections.sort(agentList);
        Collections.reverse(agentList);
        agentList = agentList.stream().limit(agentList.size() / 2).collect(Collectors.toList());
        return agentList.get(0).getFitness();
    }

    public List<A> getAgentList() {
        return agentList;
    }
}
