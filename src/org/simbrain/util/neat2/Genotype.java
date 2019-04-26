package org.simbrain.util.neat2;

public abstract class Genotype<T, G extends Genotype<T, G>> {

    public abstract Genotype<T, G> crossOver(G other);

    public abstract Genotype<T, G> copy();
}
