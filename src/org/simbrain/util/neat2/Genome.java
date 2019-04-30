package org.simbrain.util.neat2;

import java.util.Map;

/**
 * Contains a set of chromosomes, each of which contains a set of genes.
 *
 * @param <T> The Type of object (e.g. Network) encoded by this genome
 */
public abstract class Genome<T> {

    /**
     * A list of Chromosomes, indexed by a string description of their type
     */
    private Map<String, Chromosome<?>> chromosomes;

    public abstract Genome<T> crossOver(Genome<T> other);

    public abstract Genome<T> copy();

    /**
     * Create an object using this genome. In a sense, create a phenotype.
     */
    public abstract T build();

}
