package org.simbrain.util.geneticalgorithm.testsims;

import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.numerical.IntegerGenome;
import org.simbrain.util.geneticalgorithm.odorworld.OdorWorldEntityGenome;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.util.List;

public class SensorTesting {

    /**
     * The population to be evolved.
     */
    private Population<OdorWorldEntityGenome, OdorWorldEntity> population;

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
     * Number of ones that should be produced.
     */
    private int targetOneCount = 5;

    /**
     * Create the simulation.
     */
    public SensorTesting() {
    }

    /**
     * Initialize the simulation.
     */
    public void init() {

        population = new Population<>(this.populationSize);

        OdorWorldEntityGenome genome = new OdorWorldEntityGenome();
        genome.getConfig().setRadiusMin(20);


        Agent agent = new Agent<>(genome, b ->
                (double) -Math.abs(targetOneCount -
                        b.getPhenotype().getSensors().size()));

        population.populate(agent);
    }


    /**
     * Run the simulation.
     */
    public void run() {

        for (int i = 0; i < maxIterations; i++) {

            double error = population.computeNewFitness();

            System.out.printf("[%d] Error %.2f | ", i, error);
            System.out.println("Phenotype: " + population.getFittestAgent().getPhenotype().getSensors().size());

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
        SensorTesting numberMatchingTask = new SensorTesting();
        numberMatchingTask.init();
        numberMatchingTask.run();
    }

}
