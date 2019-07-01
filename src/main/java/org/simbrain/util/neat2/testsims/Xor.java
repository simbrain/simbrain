package org.simbrain.util.neat2.testsims;

import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.neat2.NetworkGenome;

/**
 * The classic NEAT application.
 */
public class Xor {

    /**
     * Default population size at each generation.
     */
    private int populationSize = 2000;

    /**
     * The maximum number of generation.
     */
    private int maxIterations = 1000;

    /**
     * If fitness rises above this threshold before maxiterations is reached, simulation terminates.
     */
    private double fitnessThreshold = -.01;

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
        population = new Population<>(this.populationSize);
        NetworkGenome.Configuration configuration = new NetworkGenome.Configuration();
        configuration.setNumInputs(2);
        configuration.setNumOutputs(1);
        configuration.setAllowSelfConnection(false);
        configuration.setMaxNodes(20);

        Agent<NetworkGenome, Network> prototype = new Agent<>(new NetworkGenome(configuration), Xor::eval);
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
                Network winner =  population.getFittestAgent().getPhenotype();
                NetworkPanel.showNetwork(winner);
                break;
            }
            population.replenish();
        }


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
