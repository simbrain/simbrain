package org.simbrain.util.geneticalgorithms.numerical;

import org.simbrain.util.geneticalgorithms.Genome;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains a single chromosome with a list of integer genes.
 */
public class IntegerGenome extends Genome<IntegerGenome,List<Integer>> {

    /**
     * All genes contained in a single chromosome, since all integer genes are compatbile.
     */
    private IntegerChromosome chromosome = new IntegerChromosome();

    /**
     * Create a new integer genome with a default chromsome containing
     * 3 integer genes.
     */
    public IntegerGenome() {
        this(3);
    }

    /**
     * Create an integer genome with a specified number of genes.
     *
     * @param chromosomeSize number of genes on the chromosome.
     */
    public IntegerGenome(int chromosomeSize) {
        chromosome = new IntegerChromosome(chromosomeSize);
    }

    /**
     * Create an integer genome initialized to some value.
     *
     * @param size number of genes
     * @param value initial values for genes
     */
    public IntegerGenome(int size, int value) {
        chromosome = new IntegerChromosome(size, value);
    }

    @Override
    public IntegerGenome crossOver(IntegerGenome other) {
        IntegerGenome ret = new IntegerGenome(chromosome.getGenes().size());
        ret.chromosome = this.chromosome.crossOver(other.chromosome);
        return ret;
    }

    @Override
    public void mutate() {
        chromosome.mutate();
    }

    @Override
    public IntegerGenome copy() {
        IntegerGenome ret = new IntegerGenome(chromosome.getGenes().size());
        ret.chromosome = this.chromosome.copy();
        return ret;
    }

    @Override
    public List<Integer> express() {
        return chromosome.getGenes().stream().map(IntegerGene::getPrototype).collect(Collectors.toList());
    }

    public void setMin(int min) {
        chromosome.getGenes().forEach(g -> g.setMinimum(min));
    }

    public void setMax(int max) {
        chromosome.getGenes().forEach(g -> g.setMaximum(max));
    }

    public void setStepSize(int stepSize) {
        chromosome.getGenes().forEach(g -> g.setStepSize(stepSize));
    }

    public IntegerChromosome getChromosome() {
        return chromosome;
    }

}
