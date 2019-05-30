package org.simbrain.util.neat2;

import org.simbrain.util.Pair;
import org.simbrain.util.math.SimbrainRandomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DoubleChromosome extends Chromosome<Double, DoubleChromosome> {
    
    private List<DoubleGene> genes;

    public DoubleChromosome(int size) {
        genes = Stream.generate(DoubleGene::new).limit(size).collect(Collectors.toList());
    }

    public DoubleChromosome() {
        this(3);
    }

    @Override
    public DoubleChromosome crossOver(DoubleChromosome other) {

        DoubleChromosome ret = new DoubleChromosome();
        ret.genes = new ArrayList<>();

        // single-point crossover
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
