package org.simbrain.util.geneticalgorithm;

import org.simbrain.util.math.SimbrainRandomizer;

import java.util.Collection;

/**
 * A list of genes that can be crossed with genes from another chromosome of the same type.
 * Examples include Neuron Chromosomes, Double Chromosomes, and Synapse Chromosomes.
 *
 * @param <T> the type of the genes in this chromosome
 */
public abstract class Chromosome<T, C extends Chromosome<T, C>> {

    private SimbrainRandomizer randomizer;

    public abstract C crossOver(C other);

    public abstract void mutate();

    public abstract Collection<? extends Gene<T>> getGenes();

    public void setRandomizer(SimbrainRandomizer randomizer) {
        this.randomizer = randomizer;
    }

    public SimbrainRandomizer getRandomizer() {
        return randomizer;
    }
}
