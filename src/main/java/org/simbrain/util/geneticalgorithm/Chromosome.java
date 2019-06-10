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

    /**
     * Cross one chromosome over with another. See https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm)
     * Details of crossover will depend on the specific implementaiotn.
     *
     * @param other the other chromosome to cross with this one.
     * @return the new chromosome after crossing the two over
     */
    public abstract C crossOver(C other);
    // Todo: Possibly create default implementations of standard forms of crossover like single point, two point,
    // k-point, etc.

    /**
     * Default chromosome level mutation is just to mutate every gene in the chromosome.
     */
    public void mutate() {
        getGenes().forEach(Gene::mutate);
    };

    /**
     * The collection of genes will be implemented in different ways depending on the sublcass.
     */
    public abstract Collection<? extends Gene<T>> getGenes();

    public void setRandomizer(SimbrainRandomizer randomizer) {
        this.randomizer = randomizer;
    }

    public SimbrainRandomizer getRandomizer() {
        return randomizer;
    }
}
