package org.simbrain.util.geneticalgorithm;

import java.util.function.Function;

/**
 * An agent based on a genome.  The main thing added at this level
 * is a fitness function used to determine how well an expression of the
 * genome (e.g. an Agent based on a neural network) does in an environment.
 *
 * @param <G> The genome, e.g. NeuralNetwork, this agent is based on.
 */
public abstract class Agent<G extends Genome, A extends Agent> implements Comparable<Agent> {

    /**
     * The agent's genome.
     */
    private G genome;

    /**
     * The agent's fitness function, typically specified
     */
    private Function<A, Double> fitnessFunction;

    /**
     * The fitness score of this agent. null if this agent has not been evaluate yet.
     */
    private Double fitness = null;

    public Agent(G genome, Function<A, Double> fitnessFunction) {
        this.genome = genome;
        this.fitnessFunction = fitnessFunction;
    }

    public abstract A crossover(A other);

    public abstract void mutate();

    /**
     * Evaluate the fitness score of this agent. Higher is better.
     */
    public abstract void computeFitness();

    protected void computeFitness(A agent) {
        fitness = fitnessFunction.apply(agent);
    }

    // TODO: Re-implement using current fitness score

    public Function<A, Double> getFitnessFunction() {
        return fitnessFunction;
    }

    public G getGenome() {
        return genome;
    }

    public abstract A copy();

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
}
