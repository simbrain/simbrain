package org.simbrain.custom_sims.simulations.test;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.util.geneticalgorithms.Agent;
import org.simbrain.util.geneticalgorithms.Population;

import static org.simbrain.network.gui.NetworkDialogsKt.showNetwork;

/**
 * Evolve networks to produce varous characteristic activities using
 * fitness functions in {@link NetworkFitnessFunctions}.
 */
public class TestNetworkEvolution  {

    /**
     * Population of xor networks to evolve
     */
    private Population<SimpleNetGenome, Network> population;

    /**
     * Default population size at each generation.
     */
    private int populationSize = 500;

    /**
     * The maximum number of generation.
     */
    private int maxIterations = 100;

    /**
     * If fitness rises above this threshold before maxiterations is reached, simulation terminates.
     * Must obviously change this depending on fitness function.
     */
    private double fitnessThreshold = -.01;

    /**
     * Evaluate a network agent
     */
    public static Double eval(Agent<SimpleNetGenome, Network> agent) {
        //return NetworkFitnessFunctions.getAverageValue(agent.getPhenotype(), 1);
        return -NetworkFitnessFunctions.howCloseToValue(agent.getPhenotype(), 7.2);
        //return -NetworkFitnessFunctions.getTotalActivation(agent.getPhenotype(), 1);
        //return NetworkFitnessFunctions.getTotalActivation(agent.getPhenotype(), 1);
        //return Double.valueOf(NetworkFitnessFunctions.getNumActive(agent.getPhenotype(), 1, .5 ));
        //return NetworkFitnessFunctions.percentActive(agent.getPhenotype(), 1, .5);
    }

    /**
     * Construct sim
     */
    public TestNetworkEvolution() {
    }

    /**
     * Run the simulation
     */
    public void run() {

        // Evolve the network
        population = new Population<>(this.populationSize);
        Agent<SimpleNetGenome, Network> agent = new Agent<>(new SimpleNetGenome(), TestNetworkEvolution::eval);
        population.populate(agent);

        for (int i = 0; i < maxIterations; i++) {
            double bestFitness = population.computeNewFitness();
            System.out.println(i + ", fitness = " + bestFitness);
            //System.out.println(population.getFittestAgent().getGenome().getIntChromosome());
            if (bestFitness > fitnessThreshold) {
                Network winner =  population.getFittestAgent().getPhenotype();
                NetworkComponent nc = new NetworkComponent("Winner", winner);
                showNetwork(nc);
                return;
            }
            population.replenish();
        }
        Network winner =  population.getFittestAgent().getPhenotype();
        NetworkComponent nc = new NetworkComponent("Winner", winner);
        showNetwork(nc);

    }

    /**
     * Main testing method
     */
    public static void main(String[] args) {
        TestNetworkEvolution test = new TestNetworkEvolution();
        test.run();
    }

}
