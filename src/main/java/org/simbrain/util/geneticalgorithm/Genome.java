package org.simbrain.util.geneticalgorithm;

import org.simbrain.util.math.SimbrainRandomizer;

/**
 * Subclasses contain a set of chromosomes, each of which contains a set of genes.
 *
 * @param <P> The phenotype (e.g. Network) encoded by this genome
 */
public abstract class Genome<G extends Genome, P> {

    /**
     * The randomizer for this genome.
     */
    private SimbrainRandomizer randomizer = new SimbrainRandomizer(0);

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

    public abstract G copy();

    /**
     * Express the genotype as a phenotype. This should be called before the fitness function
     * is evaluated.
     */
    public abstract P express();

    public SimbrainRandomizer getRandomizer() {
        return randomizer;
    }

    public void setRandomizer(SimbrainRandomizer randomizer) {
        this.randomizer = randomizer;
    }

    /**
     * Make a new randomizer based on the state of the parent's randomizer.
     *
     * @param randomizer the randomizer of the parent
     */
    public void inheritRandomizer(SimbrainRandomizer randomizer) {
        this.randomizer = new SimbrainRandomizer(randomizer.nextLong());
    }

}
