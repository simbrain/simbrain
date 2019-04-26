package org.simbrain.util.neat2;

import java.util.function.Supplier;

public abstract class Phenotype<T, G extends Genotype<T, G>> implements Comparable<Phenotype> {

    private G genotype;

    private Supplier<Double> fitnessFunction;

    public Phenotype(G genotype, Supplier<Double> fitnessFunction) {
        this.genotype = genotype;
        this.fitnessFunction = fitnessFunction;
    }

    public abstract void assemble();

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

    public Supplier<Double> getFitnessFunction() {
        return fitnessFunction;
    }

    public G getGenotype() {
        return genotype;
    }

    public abstract Phenotype<T, G> copy();

    @Override
    public int compareTo(Phenotype o) {
        return getCurrentFitness().compareTo(o.getCurrentFitness());
    }
}
