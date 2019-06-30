package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Chromosome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A list of integer genes.
 */
public class IntegerChromosome extends Chromosome<Integer, IntegerChromosome> {

    /**
     * The list of genes.
     */
    private List<IntegerGene> genes;

    /**
     * Default implementation with three double genes.
     */
    public IntegerChromosome() {
        this(3);
    }

    /**
     * Create a new double chromosome with a specified number of {@link DoubleGene}s.
     *
     * @param size number of genes
     */
    public IntegerChromosome(int size) {
        genes = Stream.generate(IntegerGene::new).limit(size).collect(Collectors.toList());
    }

    /**
     * Create an integer chromosome initialized to some value.
     *
     * @param size number of genes
     * @param value initial values for genes
     */
    public IntegerChromosome(int size, int value) {
        genes = Stream.generate(() -> new IntegerGene(value)).limit(size).collect(Collectors.toList());
    }

    @Override
    public IntegerChromosome crossOver(IntegerChromosome other) {
        IntegerChromosome ret = new IntegerChromosome(0);
        NumericalGeneticAlgUtils.
                singlePointCrossover(this, other, ret);
        return ret;
    }

    public IntegerChromosome copy() {
        IntegerChromosome ret = new IntegerChromosome();
        ret.genes = new ArrayList<>(genes);
        return ret;
    }

    @Override
    public List<IntegerGene> getGenes() {
        return genes;
    }
}
