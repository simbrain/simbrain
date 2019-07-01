package org.simbrain.util.geneticalgorithm.testsims;

import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;
import org.simbrain.util.geneticalgorithm.numerical.IntegerGenome;

import java.util.List;

/**
 * Jenetics example 5.1. Tends to converge rapidly.
 */
public class BitCounting {

    /**
     * The population to be evolved.
     */
    private Population<IntegerGenome, List<Integer>> population;

    /**
     * Default population size at each generation.
     */
    private int populationSize = 100;

    /**
     * The maximum number of generation. Simulation will terminate after this many iterations regardless of the result.
     * the maximum of iteration/generation to run before forcing the simulation to stop.
     */
    private int maxIterations = 10;

    /**
     * Number of bits
     */
    private int bitsPerChromosome = 10;

    /**
     * Number of ones that should be produced.
     */
    private int targetOneCount = 5;

    /**
     * Create the simulation.
     */
    public BitCounting() {
    }

    /**
     * Initialize the simulation.
     */
    public void init() {

        population = new Population<>(this.populationSize);
        IntegerGenome ig = new IntegerGenome(bitsPerChromosome);
        ig.setMin(0);
        ig.setMax(1);

        Agent agent = new Agent<>(ig, b ->
                Double.valueOf(-Math.abs(targetOneCount -
                        b.getPhenotype().stream().mapToInt(Integer::intValue).sum())));

        population.populate(agent);
    }


    /**
     * Run the simulation.
     */
    public void run() {

        for (int i = 0; i < maxIterations; i++) {

            double error = population.computeNewFitness();

            System.out.printf("[%d] Error %.2f | ", i, error);
            System.out.println("Phenotype: " + population.getFittestAgent().getPhenotype());

            if (error == 0) {
                break;
            }

            population.replenish();
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        BitCounting numberMatchingTask = new BitCounting();
        numberMatchingTask.init();
        numberMatchingTask.run();
    }

}
