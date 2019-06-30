package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Chromosome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A list of double genes.
 */
public class DoubleChromosome extends Chromosome<Double, DoubleChromosome> {

    /**
     * The list of genes.
     */
    private List<DoubleGene> genes;

    /**
     * Default implementation with three double genes.
     */
    public DoubleChromosome() {
        this(3);
    }

    /**
     * Create a new double chromosome with a specified number of {@link DoubleGene}s.
     *
     * @param size number of genes
     */
    public DoubleChromosome(int size) {
        genes = Stream.generate(DoubleGene::new).limit(size).collect(Collectors.toList());
    }

    /**
     * Create an double chromosome initialized to some value.
     *
     * @param size number of genes
     * @param value initial values for genes
     */
    public DoubleChromosome(int size, Double value) {
        genes = Stream.generate(() -> new DoubleGene(value)).limit(size).collect(Collectors.toList());
    }

    @Override
    public DoubleChromosome crossOver(DoubleChromosome other) {
        DoubleChromosome ret = new DoubleChromosome(0);
        NumericalGeneticAlgUtils.
                singlePointCrossover(this, other, ret);
        return ret;
    }

    public DoubleChromosome copy() {
        DoubleChromosome ret = new DoubleChromosome();
        ret.genes = genes.stream().map(DoubleGene::copy).collect(Collectors.toList());
        return ret;
    }

    @Override
    public List<DoubleGene> getGenes() {
        return genes;
    }

}
