package org.simbrain.util.geneticalgorithm.testsims;

import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.geneticalgorithm.numerical.DoubleAgent;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;
import org.simbrain.util.geneticalgorithm.Population;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple simulation demonstrating on the NEAT algorithm. This simulation tries to evolve an agent that can
 * produce a list of numbers that matches the numbers in the {@link #TARGET} list.
 */
public class NumberMatching {

    /**
     * The maximum amount a double gene is allowed to alter within a generation
     */
    public static final double MAX_MUTATION = 0.1;

    /**
     * The population to be evolve
     */
    private Population<DoubleGenome, DoubleAgent> population;

    /**
     * The desired agent phenotype
     */
    private static final List<Double> TARGET = List.of(1.0, 1.0, 1.0);

    /**
     * A randomizer for this simulation
     */
    public SimbrainRandomizer randomizer;

    /**
     * Default population size at each generation.
     */
    public int populationSize = 2000;

    /**
     * The maximum number of generation. Simulation will terminate after this many iterations regardless of the result.
     *  the maximum of iteration/generation to run before forcing the simulation to stop.
     */
    public int maxIteration = 10_000;

    /**
     * Simulation stops if the error is above this.  Error is SSE between {@link #TARGET} output and what
     * {@link #eval(DoubleAgent)} produces.
     */
    public double maxErrorThreshold = -.005;

    /**
     * Create a number matching simulation with a specific seed, population size, and the max generations limit.
     * @param seed a seed for the randomizer. If the seed is the same, the result of the simulation will also be the
     *             same.
     */
    public NumberMatching(long seed) {
        this.randomizer = new SimbrainRandomizer(seed);
    }

    /**
     * Create a number matching simulation with the default parameters. Seed: current time, population size: 200, max
     * iteration: 1000.
     */
    public NumberMatching() {
        this(System.nanoTime());
    }

    /**
     * The evaluation function / fitness function.
     *
     * The fitness score is calculated based on the SSE of the agent generated numbers and the {@link #TARGET} numbers.
     *
     * @param agent The agent to be evaluate
     * @return a fitness score
     */
    public static Double eval(DoubleAgent agent) {
        double sse = 0;
        for (int i = 0; i < TARGET.size(); i++) {
            double error = agent.getAgent().get(i) - TARGET.get(i);
            sse += error * error;
        }
        return -sse;
    }

    /**
     * Initialize the simulation. Must call before {@link #run()}.
     * See {@link #main(String[])} for an example.
     */
    public void init() {

        // create a new population according to the configured size
        population = new Population<>(this.populationSize);

        // create a genome that will be used in the agent prototype.
        DoubleGenome doubleGenomePrototype = new DoubleGenome();
        // the prototype will share the same randomizer of this simulation
        doubleGenomePrototype.setRandomizer(randomizer);

        // crate an agent prototype / template to be copied upon the initial populating process
        DoubleAgent prototype = new DoubleAgent(doubleGenomePrototype, NumberMatching::eval);

        // populate the pool using the prototype
        population.populate(prototype);
    }

    /**
     * Core of the simulation. Must call {@link #init()} before executing.
     *  See {@link #main(String[])} for an example.
     */
    public void run() {

        // run the simulation for at most maxIteration times
        for (int i = 0; i < maxIteration; i++) {

            // compute the fitness score, eliminate the bottom half, and get the top score
            // TODO: break into individual steps.
            double bestFitness = population.computeNewFitness();

            System.out.printf("[%d]Fitness %.2f | ", i, bestFitness);
            System.out.println("Phenotype: " + population.getAgentList().get(0).getAgent()
                    .stream().map(a -> String.format("%.2f", a)).collect(Collectors.joining(", ")));

            // if the SSE is less than maxErrorThreshold, the simulation should stop.
            if (bestFitness > maxErrorThreshold) {
                break;
            }

            // replenish the pool
            population.replenish();
        }
    }

    /**
     * Main method.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        NumberMatching numberMatchingTask = new NumberMatching();
        numberMatchingTask.init();
        numberMatchingTask.run();
    }

}
