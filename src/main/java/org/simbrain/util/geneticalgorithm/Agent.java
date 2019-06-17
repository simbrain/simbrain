package org.simbrain.util.geneticalgorithm;

import java.util.function.Function;

/**
 * A genome together with its phenotypic expression, and a fitness function
 * Can be used to determine how well an expression of a
 * genome performs.
 *
 * @param <G> The genotype for this agent
 * @param <P> The phenotype for this agent
 */
public class Agent<G extends Genome<G,P>, P> implements Comparable<Agent> {

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
    private Function<Agent<G,P>, Double> fitnessFunction;

    /**
     * The fitness score of this agent. null if this agent has not been evaluate yet.
     */
    private Double fitness = null;

    public Agent(G genome, Function<Agent<G,P>, Double> fitnessFunction) {
        this.genome = genome;
        this.fitnessFunction = fitnessFunction;
        phenotype = genome.express(); // So that phenotypic properties are available in simulations.
    }

    public Agent<G,P> crossover(Agent<G,P> other) {
        return new Agent<>(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
    }

    public void mutate() {
        getGenome().mutate();
        phenotype = genome.express();
    };

    /**
     * Evaluate the fitness score of this agent. Higher is better.
     */
    public void computeFitness() {
        phenotype = getGenome().express();
        computeFitness(this);
    };

    protected void computeFitness(Agent<G,P> agent) {
        fitness = fitnessFunction.apply(agent);
    }

    public Function<Agent<G,P>, Double> getFitnessFunction() {
        return fitnessFunction;
    }

    public G getGenome() {
        return genome;
    }

    public Agent<G,P> copy() {
        return new Agent<>(getGenome().copy(), getFitnessFunction());
    };

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
}
