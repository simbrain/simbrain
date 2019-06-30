package org.simbrain.util.geneticalgorithm.testsims;

import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;
import org.simbrain.util.geneticalgorithm.Population;

import java.util.List;

/**
 * A simple simulation demonstrating on the NEAT algorithm. This simulation tries to evolve an agent that can produce a
 * list of numbers that matches the numbers in the {@link #TARGET} list.
 */
public class NumberMatching {

    /**
     * The desired agent phenotype
     */
    private static final List<Double> TARGET = List.of(9.0, 1.4, -5.8);

    /**
     * The population to be evolved.
     */
    private Population<DoubleGenome, List<Double>> population;

    /**
     * Default population size at each generation.
     */
    public int populationSize = 2000;

    /**
     * The maximum number of generation. Simulation will terminate after this many iterations regardless of the result.
     * the maximum of iteration/generation to run before forcing the simulation to stop.
     */
    public int maxIteration = 1000;

    /**
     * Simulation stops if the error is above this.
     */
    public double maxErrorThreshold = -.05;

    /**
     * Create a number matching simulation.
     */
    public NumberMatching() {
    }

    /**
     * Initialize the simulation. Must call before {@link #run()}. See {@link #main(String[])} for an example.
     */
    public void init() {

        // Create a new population
        population = new Population<>(this.populationSize);

        // Create a double genome prototype, basically a list of doubles
        DoubleGenome doubleGenomePrototype = new DoubleGenome(3);
        //doubleGenomePrototype.setMin(0);
        //doubleGenomePrototype.setMax(1);
        doubleGenomePrototype.setStepSize(.01);

        // Create an initial agent prototype
        Agent doubleAgent = new Agent<>(doubleGenomePrototype, NumberMatching::eval);

        // Populate the pool using the prototype
        population.populate(doubleAgent);
    }


    /**
     * Fitness is based on the SSE of the agent generated numbers and the {@link #TARGET} numbers.
     *
     * @param agent The agent to be evaluate
     * @return a fitness score
     */
    public static Double eval(Agent<DoubleGenome, List<Double>> agent) {
        double sse = 0;
        for (int i = 0; i < TARGET.size(); i++) {
            double error = agent.getPhenotype().get(i) - TARGET.get(i);
            sse += error * error;
        }
        return -sse;
    }

    /**
     * Core of the simulation. Must call {@link #init()} before executing. See {@link #main(String[])} for an example.
     */
    public void run() {

        for (int i = 0; i < maxIteration; i++) {

            double bestFitness = population.computeNewFitness();

            System.out.printf("[%d] Fitness %.2f | ", i, bestFitness);
            System.out.println("Phenotype: " + population.getFittestAgent().getPhenotype());

            // If the SSE is less than maxErrorThreshold, the simulation should stop.
            if (bestFitness > maxErrorThreshold) {
                break;
            }

            population.replenish();
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        NumberMatching numberMatchingTask = new NumberMatching();
        numberMatchingTask.init();
        numberMatchingTask.run();
    }

}
