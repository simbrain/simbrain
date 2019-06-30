package org.simbrain.util.geneticalgorithm;

/**
 * Subclasses contain a set of chromosomes, each of which contains a set of genes.
 *
 * @param <P> The phenotype (e.g. Network) encoded by this genome
 */
public abstract class Genome<G extends Genome, P> {

    /**
     * Override
     * @param other the other genome to cross with this one
     * @return the new genome crossing this and another
     */
    public abstract G crossOver(G other);

    /**
     * Mutate the genes in this genome.
     * Must sometimes be overridden, when mutations must be aware of information beyond the chromosome level,
     * e.g. an ability to make new connections.
     */
    public abstract void mutate();

    /**
     * Copy the genome.
     *
     * @return the new copy
     */
    public abstract G copy();

    /**
     * Express the genotype as a phenotype. This should be called before the fitness function
     * is evaluated.
     */
    public abstract P express();

}
