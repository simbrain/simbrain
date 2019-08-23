package org.simbrain.util.geneticalgorithm;

import org.simbrain.util.math.SimbrainRandomizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The top level for genetic algorithms in Simbrain. A set of agents, each of which expresses a genotype and produces a
 * fitness value at each generation.  Contains classes to initialize a population and run an evolutionary simulation.
 * <p>
 * Environments are currently handled at this level.
 *
 * @param <G> The type of genes the population evolves, "genotypes"
 */
public class Population<G extends Genome<G, P>, P> {

    /**
     * Number of agents in this population at a given generation.
     */
    private int size;

    /**
     * The agents in this population.
     */
    private List<Agent<G, P>> agentList;

    /**
     * Tracks generation number.
     */
    private int generation = 0;

    /**
     * Amount to eliminate at each generation.
     */
    private double eliminationRatio = .5;

    /**
     * Initialize population to a specific size.
     *
     * @param size number of agents used in each generation
     */
    public Population(int size) {
        this.size = size;
    }

    /**
     * Initialize the population with a set of agents, using a prototype agent.
     *
     * @param prototype the prototype agent, which spawns all agents in the population.
     */
    public void populate(Agent<G, P> prototype) {
        agentList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Agent<G, P> agent = prototype.copy();
            agent.setId(String.format("G%s|A%s", generation, i));
            agentList.add(agent);
        }
    }

    /**
     * Computes fitness of every agent in the population
     *
     * @return the fitness of the most fit agent.
     */
    public Double computeNewFitness() {
        //System.out.println("----------------");
        //System.out.println("Before fitness computation:" + agentList);
        agentList.parallelStream().forEach(Agent::computeFitness);
        //System.out.println("After fitness computation:" + agentList);
        eliminateLeastFit(); // Must happen in this order
        //System.out.println("After elimination:" + agentList);
        //System.out.println("Most fit: " + agentList.get(0).getFitness());
        return agentList.get(0).getFitness();
    }

    /**
     * Eliminate the least fit agents from the population.
     */
    private void eliminateLeastFit() {

        // Agent@compareTo compares them on fitness
        Collections.sort(agentList);
        //System.out.println("Sorted: " + agentList);
        Collections.reverse(agentList);
        //System.out.println("Reversed: " + agentList);
        agentList = agentList.stream()
                .filter(Agent::isAlive)
                .limit((long) (agentList.size() * (1-eliminationRatio))).collect(Collectors.toList());
    }

    /**
     * Replenish the part of the population that was eliminated in {@link #eliminateLeastFit()}) by crossing over the
     * genes of the remaining part of the population.
     */
    public void replenish() {
        generation++;
        int remainingPopulation = agentList.size();
        int reproduceSize = size - remainingPopulation;
        for (int i = 0; i < reproduceSize; i++) {
            int index1 = SimbrainRandomizer.rand.nextInt(remainingPopulation);
            int index2 = SimbrainRandomizer.rand.nextInt(remainingPopulation);
            while (index2 == index1) {
                index2 = SimbrainRandomizer.rand.nextInt(remainingPopulation);
            }
            Agent<G, P> agent1 = agentList.get(index1);
            Agent<G, P> agent2 = agentList.get(index2);
            Agent<G, P> newAgent = agent1.crossover(agent2);// TODO: Test this
            newAgent.mutate();
            newAgent.setId(String.format("G%s|A%s", generation, i));
            agentList.add(newAgent);
        }
        //System.out.println("Mutated:" + agentList);
    }

    public List<Agent<G, P>> getAgentList() {
        return agentList;
    }

    public Agent<G, P> getFittestAgent() {
        return agentList.get(0);
    }

    @Override
    public String toString() {
        return getAgentList().toString();
    }

    public double getEliminationRatio() {
        return eliminationRatio;
    }

    public void setEliminationRatio(double eliminationRatio) {
        this.eliminationRatio = eliminationRatio;
    }

    public int getGeneration() {
        return generation;
    }
}
