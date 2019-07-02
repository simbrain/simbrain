package org.simbrain.util.geneticalgorithm;

import java.util.function.Function;

/**
 * A genome together with its phenotypic expression, and a fitness function. Can be used to determine how well an
 * expression of a genome performs.
 *
 * @param <G> The genotype for this agent
 * @param <P> The phenotype for this agent
 */
public class Agent<G extends Genome<G, P>, P> implements Comparable<Agent> {

    /**
     * The agent's genome.
     */
    private G genome;

    /**
     * The agent's phenotype
     */
    private P phenotype;

    /**
     * The agent's fitness function, typically specified
     */
    private Function<Agent<G, P>, Double> fitnessFunction;

    /**
     * Used for debugging now; tracks generation number and differentiates agents.
     */
    private String id;

    /**
     * The fitness score of this agent. null if this agent has not been evaluate yet.
     */
    private Double fitness = null;

    /**
     * Construct a new agent using a genome and a fitness function
     *
     * @param genome          the genome
     * @param fitnessFunction the fitness functio
     */
    public Agent(G genome, Function<Agent<G, P>, Double> fitnessFunction) {
        this.genome = genome;
        this.fitnessFunction = fitnessFunction;
        phenotype = genome.express(); // So that phenotypic properties are available in simulations.
    }

    /**
     * Cross over the genomes of this agents and  another, and return the new agent that results Let this agent be mom.
     *
     * @param other dad
     * @return child
     */
    public Agent<G, P> crossover(Agent<G, P> other) {
        return new Agent<>(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
    }

    /**
     * Mutates the genome, and expresses the genome as a phenotype.
     */
    public void mutate() {
        getGenome().mutate();
        phenotype = genome.express();
    }

    ;

    /**
     * Evaluate the fitness score of this agent. Higher is better.
     */
    public void computeFitness() {
        //phenotype = getGenome().express();
        fitness = fitnessFunction.apply(this);
    }

    public Function<Agent<G, P>, Double> getFitnessFunction() {
        return fitnessFunction;
    }

    public G getGenome() {
        return genome;
    }

    public Agent<G, P> copy() {
        return new Agent<>(getGenome().copy(), getFitnessFunction());
    }

    ;

    @Override
    public int compareTo(Agent o) {
        return fitness.compareTo(o.fitness);
    }

    public Double getFitness() {
        return fitness;
    }

    public void setFitness(Double fitness) {
        this.fitness = fitness;
    }

    public P getPhenotype() {
        return phenotype;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id + ":" + getPhenotype();
    }

}
