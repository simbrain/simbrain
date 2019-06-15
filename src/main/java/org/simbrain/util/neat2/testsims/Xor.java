package org.simbrain.util.neat2.testsims;

import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.neat2.NetworkGenome;

public class Xor {

    /**
     * Default population size at each generation.
     */
    public int populationSize = 1000;

    /**
     * The maximum number of generation.
     */
    public int maxIterations = 1000;

    /**
     * If fitness rises above this threshold before maxiterations is reached, simulation terminates.
     */
    double fitnessThreshold = -.01;

    /**
     * Population of xor networks to evolve
     */
    private Population<NetworkGenome, Network> population;

    /**
     * Training data
     */
    public static TrainingSet trainingSet =
            new TrainingSet(
                    new double[][]{{0,0},{0,1},{1,0},{1,1}},
                    new double[][]{{0},{1},{1},{0}});

    public static final double NEW_CONNECTION_MUTATION_PROBABILITY = 0.05;
    public static final double NEW_NODE_MUTATION_PROBABILITY = 0.05;
    public static final double MAX_CONNECTION_STRENGTH = 10;
    public static final double MIN_CONNECTION_STRENGTH = -10;
    public static final double MAX_CONNECTION_MUTATION = 1;


    /**
     * Evaluate xor function
     */
    public static Double eval(Agent<NetworkGenome, Network> agent) {

        double sse = 0.0;
        NeuronGroup ig = (NeuronGroup) agent.getPhenotype().getGroupByLabel("inputs");
        NeuronGroup og  = (NeuronGroup) agent.getPhenotype().getGroupByLabel("outputs");
        for(int row = 0; row < trainingSet.getSize(); row++ ) {
            double[] inputs = trainingSet.getInput(row);
            ig.forceSetActivations(inputs);
            // Add to error over a few iterations to penalize for instability
            for (int i = 0; i < 3; i++) {
                agent.getPhenotype().update();
                double[] targets = trainingSet.getTarget(row);
                for (int n = 0; n < targets.length; n++) {
                    double error =  og.getNeuron(n).getActivation() - targets[n];
                    sse += (error * error);
                }
            }

        }
        return -sse;
    }

    /**
     * Initialize the population of networks.
     */
    public void init() {
        population = new Population<>(this.populationSize, System.nanoTime());
        Agent prototype = new Agent<>(new NetworkGenome(), Xor::eval);
        population.populate(prototype);
    }

    /**
     * Run the simulation.
     */
    public void run() {
        for (int i = 0; i < maxIterations; i++) {

            double bestFitness = population.computeNewFitness();
            System.out.println(i + ", fitness = " + bestFitness);
            if (bestFitness > fitnessThreshold) {
                break;
            }
            population.replenish();
        }

        Network winner =  population.getFittestAgent().getPhenotype();
        //System.out.println(winner);

        // Display the winning network
        NetworkPanel.showNetwork(winner);

    }

    /**
     * Main testing method
     */
    public static void main(String[] args) {
        Xor test = new Xor();
        test.init();
        test.run();
    }
}
