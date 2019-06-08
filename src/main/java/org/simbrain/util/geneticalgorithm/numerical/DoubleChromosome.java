package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Chromosome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A list of double genes.
 *
 * Currently uses single point crossover. See https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm)#Single-point_crossover
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


    @Override
    public DoubleChromosome crossOver(DoubleChromosome other) {

        DoubleChromosome ret = new DoubleChromosome();
        ret.genes = new ArrayList<>();

        // Single-point crossover.
        int point = getRandomizer().nextInt(Integer.min(other.genes.size(), this.genes.size()));

        DoubleChromosome firstChromosome;
        DoubleChromosome secondChromosome;

        if (getRandomizer().nextBoolean()) {
            firstChromosome = this;
            secondChromosome = other;
        } else {
            firstChromosome = other;
            secondChromosome = this;
        }

        int i = 0;
        for (; i < point; i++) {
            ret.genes.add(firstChromosome.genes.get(i));
        }
        for (; i < secondChromosome.genes.size(); i++) {
            ret.genes.add(secondChromosome.genes.get(i));
        }
        return ret;
    }

    public DoubleChromosome copy() {
        DoubleChromosome ret = new DoubleChromosome();
        ret.genes = genes.stream().map(DoubleGene::copy).collect(Collectors.toList());
        return ret;
    }

    @Override
    public void mutate() {
        genes.forEach(DoubleGene::mutate);
    }

    @Override
    public Collection<DoubleGene> getGenes() {
        return genes;
    }
}
