package org.simbrain.util.neat2;

import java.util.Map;

/**
 *
 * @param <T> The Type of object (e.g. Network) encoded by this genome
 * @param <G> The Genome... TODO: Yulin think about this and the generic types here
 */
public abstract class Genome<T, G extends Genome<T, G>> {


    private Map<String, Chromosome<T>> chromosomes;

    public abstract Genome<T, G> crossOver(G other);

    public abstract Genome<T, G> copy();

    /**
     * Create an object using this genome.
     * In a sense, create a phenotype.
     */
    public abstract T build();

}
