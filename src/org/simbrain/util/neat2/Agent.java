package org.simbrain.util.neat2;

import java.util.function.Supplier;


public abstract class Agent<T, G extends Genome<T, G>> implements Comparable<Agent> {

    private G genotype;

    private Supplier<Double> fitnessFunction;

    public Agent(G genotype, Supplier<Double> fitnessFunction) {
        this.genotype = genotype;
        this.fitnessFunction = fitnessFunction;
    }

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

    public Supplier<Double> getFitnessFunction() {
        return fitnessFunction;
    }

    public G getGenotype() {
        return genotype;
    }

    public abstract Agent<T, G> copy();

    @Override
    public int compareTo(Agent o) {
        return getCurrentFitness().compareTo(o.getCurrentFitness());
    }
}
