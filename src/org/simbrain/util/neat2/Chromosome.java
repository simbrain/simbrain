package org.simbrain.util.neat2;

import java.util.List;

// Genes in this list can be crossed with genes from another chromosome of the same type
// Note that <T>
public abstract class Chromosome<T> {

    private List<Gene<T>> genes;

    public abstract Chromosome<T> crossOver(Chromosome<T> other);

}
