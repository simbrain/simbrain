package org.simbrain.util.geneticalgorithm.numerical;


import org.simbrain.util.geneticalgorithm.Agent;

import java.util.List;
import java.util.function.Function;

/**
 * An agent whose properties take the form of a list of doubles.
 */
public class DoubleAgent extends Agent<DoubleGenome, DoubleAgent> {

    /**
     * The observable properties of this phenotype, that are used to compute fitnes.
     */
    private List<Double> phenotype;

    /**
     * Construct a double phenotype from a genome and fitness function.
     *
     * @param genome the genome
     * @param fitnessFunction the fitness function
     */
    public DoubleAgent(DoubleGenome genome, Function<DoubleAgent, Double> fitnessFunction) {
        super(genome, fitnessFunction);
        phenotype = genome.build();
    }

    @Override
    public DoubleAgent crossover(DoubleAgent other) {
        return new DoubleAgent(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
    }

    @Override
    public void mutate() {
        getGenome().mutate();
    }

    @Override
    public void computeFitness() {
        computeFitness(this);
    }

    @Override
    public DoubleAgent copy() {
        return new DoubleAgent(getGenome().copy(), getFitnessFunction());
    }

    public List<Double> getPhenotype() {
        return phenotype;
    }
}
