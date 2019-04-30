package org.simbrain.util.neat2;

import java.util.List;

/**
 * A list of genes that can be crossed with genes from another chromosome of the same type.
 * Examples include Neuron Chromosomes, Double Chromosomes, and Synapse Chromosomes.
 *
 * @param <T> the type of the genes in this chromosome
 */
public abstract class Chromosome<T> {

    private List<Gene<T>> genes;

    public abstract Chromosome<T> crossOver(Chromosome<T> other);

}
