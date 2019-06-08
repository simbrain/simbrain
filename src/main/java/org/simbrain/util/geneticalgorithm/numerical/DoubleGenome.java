package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Genome;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A list of double genes
 */
public class DoubleGenome extends Genome<List<Double>, DoubleGenome> {

    /**
     * All genes contained in a single chromosome, since all double genes are compatbile.
     */
    private DoubleChromosome chromosome = new DoubleChromosome();

    @Override
    public DoubleGenome crossOver(DoubleGenome other) {
        DoubleGenome ret = new DoubleGenome();
        ret.inheritRandomizer(getRandomizer());
        ret.chromosome = this.chromosome.crossOver(other.chromosome);
        ret.chromosome.setRandomizer(ret.getRandomizer());
        return ret;
    }

    @Override
    public void mutate() {
        chromosome.mutate();
    }

    @Override
    public DoubleGenome copy() {
        DoubleGenome ret = new DoubleGenome();
        ret.inheritRandomizer(getRandomizer());
        ret.chromosome = this.chromosome.copy();
        ret.chromosome.setRandomizer(ret.getRandomizer());
        ret.chromosome.getGenes().forEach(g -> g.setRandomizer(ret.getRandomizer()));
        return ret;
    }

    @Override
    public List<Double> build() {
        return chromosome.getGenes().stream().map(DoubleGene::getPrototype).collect(Collectors.toList());
    }
}
