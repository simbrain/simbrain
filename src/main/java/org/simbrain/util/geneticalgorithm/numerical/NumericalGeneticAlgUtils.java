package org.simbrain.util.geneticalgorithm.numerical;

import org.simbrain.util.geneticalgorithm.Chromosome;
import org.simbrain.util.math.SimbrainRandomizer;

import java.util.ArrayList;

//TODO: Move this to GeneticAlgorithmUtils

/**
 * Static methods common across types of numerical genes, chromosomes and genomes.
 */
public class NumericalGeneticAlgUtils {

    // An effort was made to create a generic NumericGene class which so that arbitrary subclasses of java.lang.Number
    // could be used, but int and double are the main use cases and the generic solution posed some problems.
    // So it didn't end up being worth it. Perhaps at some later point.

    /**
     * Cross mom and dad chromosomes, and store results in child.
     *
     * See https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm)#Single-point_crossover
     *
     * @param mom one chromosome to cross
     * @param dad the other to cross.
     * @param child an empty chrosome of the appropriate type, to write into
     */
    public static void singlePointCrossover(Chromosome mom, Chromosome dad, Chromosome child) {

        // Single-point crossover.
        int point =  SimbrainRandomizer.rand.nextInt(Integer.min(mom.getGenes().size(), dad.getGenes().size()));

        Chromosome firstChromosome;
        Chromosome secondChromosome;

        if (SimbrainRandomizer.rand.nextBoolean()) {
            firstChromosome = mom;
            secondChromosome = dad;
        } else {
            firstChromosome = dad;
            secondChromosome = mom;
        }

        int i = 0;
        for (; i < point; i++) {
            child.getGenes().add(firstChromosome.getGenes().get(i));
        }
        for (; i < secondChromosome.getGenes().size(); i++) {
            child.getGenes().add(secondChromosome.getGenes().get(i));
        }
    }
}
