package org.simbrain.util.geneticalgorithms;

import java.util.List;

/**
 * A list of genes that can be crossed with genes from another chromosome of the same type.
 * See {@link org.simbrain.util.geneticalgorithms.numerical.DoubleChromosome} and
 * {@link org.simbrain.util.neat.ConnectionChromosome}.
 *
 * @param <T> the type of the genes in this chromosome
 */
public abstract class Chromosome<T, C extends Chromosome> {

    /**
     * Cross one chromosome over with another. See https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm)
     * Details of crossover will depend on the specific implementaiotn.
     *
     * @param other the other chromosome to cross with this one.
     * @return the new chromosome after crossing the two over
     */
    public abstract C crossOver(C other);

    /**
     * Default chromosome level mutation is just to mutate every gene in the chromosome.
     */
    public void mutate() {
        getGenes().forEach(Gene::mutate);
    };

    // TODO: This could be made more generic and converted to a collection (it was initially).  Currently
    // it's a list to make things easy in NumericGAUtils.singlePointCrossover.  If the converstion to lists
    // in the NEAT chromosome classes is an issue make that change.

    /**
     * The collection of genes will be implemented in different ways depending on the subclcass.
     */
    public abstract List<? extends Gene<T>> getGenes();

    @Override
    public String toString() {
        return getGenes().toString();
    }

    /**
     * Get a reference to a specific gene
     * @param index index of gene to retrieve
     * @return the gene with that index
     */
    public Gene<T> getGene(int index) {
        return getGenes().get(index);
    }
}
