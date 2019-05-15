package org.simbrain.util.neat2;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains a set of chromosomes, each of which contains a set of genes.
 *
 * @param <T> The Type of object (e.g. Network) encoded by this genome
 */
public abstract class Genome<T, G extends Genome<T, G>> {

    /**
     * A list of Chromosomes, indexed by a string description of their type
     */
    // private Map<String, Chromosome<?>> chromosomes = new HashMap<>();

    public abstract G crossOver(G other);

    public abstract G copy();

    /**
     * Create an object using this genome. In a sense, create a phenotype.
     */
    public abstract T build();

    // public Map<String, Chromosome<?>> getChromosomes() {
    //     return chromosomes;
    // }
}
