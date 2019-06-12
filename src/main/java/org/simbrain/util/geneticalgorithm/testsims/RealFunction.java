package org.simbrain.util.geneticalgorithm.testsims;

import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.numerical.DoubleAgent;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;
import org.simbrain.util.math.SimbrainRandomizer;
import java.util.function.Function;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * A test case for the NEAT algorithm. This simulation tries to evolve an agent that maximizes or minimizes fitness for
 * a specified function. Bits and pieces of code were copied from NumberMatching and DoubleAgent.
 */
public class RealFunction {

    /**
     * The fitness function
     */
    private Function<DoubleAgent, Double> fitnessFunction;

    /**
     * An example fitness function to use for this test. The function is based on example 5.2 from the Jenetics manual.
     * @param agent the agent to check fitness of
     * @return the fitness of the agent
     */
    private static Double exampleFunction(DoubleAgent agent) {
        double sum = 0;

        for (Double num : agent.getAgent()) {
            sum += cos(0.5 + sin(num)) * cos(num);
        }

        return -sum;
    }

    /**
     * The population to be evolved.
     */
    private Population<DoubleGenome, DoubleAgent> population;

    /**
     * Default population size at each generation.
     */
    private int populationSize = 2000;

    /**
     * The maximum of iterations/generations to go through before stopping the simulaton
     */
    private int numIterations = 10_000;

    /**
     * Creates a fitness function simulation
     * @param fitnessFunction the function to use to determine fitness
     */
    private RealFunction(Function<DoubleAgent, Double> fitnessFunction) {
        this.fitnessFunction = fitnessFunction;
    }

    /**
     * Initialize the simulation. Must call before {@link #run()}.
     * See {@link #main(String[])} for an example.
     */
    public void init() {

        // Create a new population
        population = new Population<>(this.populationSize, System.nanoTime());

        // Create a double genome prototype, basically a list of doubles
        DoubleGenome doubleGenomePrototype = new DoubleGenome();

        // The prototype will share the same randomizer as this simulation
        doubleGenomePrototype.setRandomizer(new SimbrainRandomizer(System.nanoTime()));

        // Create an initial agent prototype
        DoubleAgent doubleAgent = new DoubleAgent(doubleGenomePrototype, fitnessFunction);

        // Populate the pool using the prototype
        population.populate(doubleAgent);
    }

    /**
     * Core of the simulation. Must call {@link #init()} before executing.
     *  See {@link #main(String[])} for an example.
     */
    public void run() {

        for (int i = 0; i < numIterations; i++) {

            double bestFitness = population.computeNewFitness();

            System.out.printf("[%d] Fitness %.2f | ", i, bestFitness);
            System.out.println("Phenotype: " + population.getAgentList().get(0).getAgent());

            population.replenish();
        }
    }

    /**
     * Main method.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        RealFunction realFunctionTask = new RealFunction(RealFunction::exampleFunction);
        realFunctionTask.init();
        realFunctionTask.run();
    }
}