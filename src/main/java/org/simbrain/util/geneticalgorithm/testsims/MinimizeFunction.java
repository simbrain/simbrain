package org.simbrain.util.geneticalgorithm.testsims;

import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.numerical.DoubleAgent;
import org.simbrain.util.geneticalgorithm.numerical.DoubleGenome;

/**
 * From Jenetics manual, section 5.2.
 */
public class MinimizeFunction {


    /**
     * The population to be evolved.
     */
    private Population<DoubleGenome, DoubleAgent> population;

    /**
     * Default population size at each generation.
     */
    public int populationSize = 200;

    /**
     * The maximum number of generations.
     */
    public int maxIteration = 100;

    /**
     * Create a number matching simulation.
     */
    public MinimizeFunction() {
    }

    /**
     * Initialize the simulation. Must call before {@link #run()}.
     * See {@link #main(String[])} for an example.
     */
    public void init() {

        // Create a new population
        population = new Population<>(this.populationSize, System.nanoTime());

        // Create a double genome prototype, basically a list of doubles
        DoubleGenome dg = new DoubleGenome(1);
        dg.setMin(0);
        dg.setMax(2*Math.PI);

        // Create an initial agent prototype
        DoubleAgent doubleAgent = new DoubleAgent(dg, MinimizeFunction::eval);

        // Populate the pool using the prototype
        population.populate(doubleAgent);
    }

    /**
     * Fitness is based on getting the lowest possible value for the function cos (0.5 + sin(x)) * cos(x)
     * Expected result is 3.389.
     *
     * @param agent The agent to be evaluate
     * @return a fitness score
     */
    public static Double eval(DoubleAgent agent) {
        double x = agent.getPhenotype().get(0);
        double valueToMinize = Math.cos(.5 + Math.sin(x)) * Math.cos(x);
        // We are minimizing the function, so smaller numbers are more "fit"
        return -valueToMinize;
    }

    /**
     * Run the simulation. Must call init first.
     */
    public void run() {

        for (int i = 0; i < maxIteration; i++) {

            double bestFitness = population.computeNewFitness();

            System.out.printf("[%d] Fitness %.2f | ", i, bestFitness);
            System.out.println("Phenotype: " + population.getAgentList().get(0).getPhenotype());

            population.replenish();
        }
    }

    /**
     * Main method.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        MinimizeFunction mf = new MinimizeFunction();
        mf.init();
        mf.run();
    }

}
