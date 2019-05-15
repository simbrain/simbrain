package org.simbrain.util.neat2;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An agent based on a genome.  The main thing added at this level
 * is a fitness function used to determine how well an expression of the
 * genome (e.g. an Agent based on a neural network) does in an environment.
 * Any environment is currently maintained at the Population level.
 *
 * @param <G> The genome, e.g. NeuralNetwork, this agent is based on.
 */
public abstract class Agent<G extends Genome, A extends Agent<G, A>> implements Comparable<Agent> {

    private G genome;

    private Function<A, Double> fitnessFunction;

    public Agent(G genome, Function<A, Double> fitnessFunction) {
        this.genome = genome;
        this.fitnessFunction = fitnessFunction;
    }

    public abstract A crossover(A other);

    /**
     * Evaluate the fitness score of this agent.
     */
    public abstract void computeFitness();

    /**
     * Get the computed fitness score of this agent. Null if the fitness score has not been computed yet.
     *
     * @return the computed fitness score.
     */
    public abstract Double getCurrentFitness();
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
        return getCurrentFitness().compareTo(o.getCurrentFitness());
    }
}
