package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Genome;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains a single chromosome with a list of double genes.
 */
public class DoubleGenome extends Genome<DoubleGenome,List<Double>> {

    /**
     * All genes contained in a single chromosome, since all double genes are compatbile.
     */
    private DoubleChromosome chromosome = new DoubleChromosome();

    /**
     * Create a new double genome with a default chromsome containing
     * 3 double genes.
     */
    public DoubleGenome() {
        this(3);
    }

    /**
     * Create a double genome whose genes are initialized to some value.
     *
     * @param size number of genes
     * @param value initial values for genes
     */
    public DoubleGenome(int size, double value) {
        chromosome = new DoubleChromosome(size, value);
    }

    /**
     * Create a double genome with a specified number of genes.
     *
     * @param chromosomeSize number of genes on the chromosome.
     */
    public DoubleGenome(int chromosomeSize) {
        chromosome = new DoubleChromosome(chromosomeSize);
    }

    @Override
    public DoubleGenome crossOver(DoubleGenome other) {
        DoubleGenome ret = new DoubleGenome(chromosome.getGenes().size());
        ret.chromosome = this.chromosome.crossOver(other.chromosome);
        return ret;
    }

    @Override
    public void mutate() {
        chromosome.mutate();
    }

    @Override
    public DoubleGenome copy() {
        DoubleGenome ret = new DoubleGenome(chromosome.getGenes().size());
        ret.chromosome = this.chromosome.copy();
        return ret;
    }

    @Override
    public List<Double> express() {
        return chromosome.getGenes().stream().map(DoubleGene::getPrototype).collect(Collectors.toList());
    }

    public void setMin(double min) {
        chromosome.getGenes().forEach(g -> g.setMinimum(min));
    }

    public void setMax(double max) {
        chromosome.getGenes().forEach(g -> g.setMaximum(max));
    }

    public void setStepSize(double stepSize) {
        chromosome.getGenes().forEach(g -> g.setStepSize(stepSize));
    }

    public DoubleChromosome getChromosome() {
        return chromosome;
    }

}
