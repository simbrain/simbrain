package org.simbrain.util.geneticalgorithm;

import org.simbrain.util.math.SimbrainRandomizer;

import java.util.Collection;

/**
 * A list of genes that can be crossed with genes from another chromosome of the same type.
 * See {@link org.simbrain.util.geneticalgorithm.numerical.DoubleChromosome} and
 * {@link org.simbrain.util.neat2.ConnectionChromosome}.
 *
 * @param <T> the type of the genes in this chromosome
 */
public abstract class Chromosome<T, C extends Chromosome> {

    private SimbrainRandomizer randomizer;

    public abstract C crossOver(C other);

    public abstract void mutate();

    public abstract Collection<? extends Gene<T>> getGenes();

    public void setRandomizer(SimbrainRandomizer randomizer) {
        this.randomizer = randomizer;
    }

    /**
     * Randomizer used to set crossover points in mutations.
     */
    public SimbrainRandomizer getRandomizer() {
        return randomizer;
    }
}
