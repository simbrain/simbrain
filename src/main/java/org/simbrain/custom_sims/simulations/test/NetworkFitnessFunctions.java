package org.simbrain.custom_sims.simulations.test;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;

/**
 * Static fitness functions to use with genetic algorithms for network. E.g. evolve a network that produces an average
 * of 50% activity.
 */
public class NetworkFitnessFunctions {

    //TODO: Unit tests

    /**
     * Returns the percentage of nodes active in a network over a specified number of iterations.
     *
     * @param numIterations how many iterations to use in assessing average activation
     * @param threshold     threshold activation above which a node is considered active
     * @return a percentage
     */
    public static double percentActive(Network network, int numIterations, double threshold) {

        int numNeurons = network.getFlatNeuronList().size();
        int numActive = getNumActive(network, numIterations, threshold);

        double percentActive = (double) numActive / (double) numNeurons
                / numIterations;
        //System.out.println(percentActive);
        return percentActive;
    }

    /**
     * Evolve a network whose average activity is as close as possible to a specified value.
     *
     * @param network reference to network
     * @param value   target average activity
     * @return how close the network's average value is to the target value.
     */
    public static double howCloseToValue(Network network, double value) {
        return Math.abs(getAverageValue(network, 1) - value);
    }

    /**
     * Average activity of a network over a time window (specified by numIterations). Return only values greater than
     * 0.
     * <p>
     * Takes average over several iterations, to avoid solutions that oscillate.
     *
     * @param network reference to network
     * @return average value over specified window.
     */
    public static double getAverageValue(Network network, int numIterations) {
        double total = getTotalActivation(network, numIterations);
        double averageVal = total / network.getFlatNeuronList().size()
                / numIterations;
        //System.out.println("average value:" + averageVal);
        return Math.max(0, averageVal);
    }


    /**
     * Returns number of active nodes, relative to a threshold activation above which a node is considered active
     */
    public static int getNumActive(Network network, int numIterations, double threshold) {
        int numActive = 0;

        for (int iterations = 0; iterations < numIterations; iterations++) {
            for (Neuron neuron : network.getFlatNeuronList()) {
                if (neuron.getActivation() > threshold) {
                    numActive++;
                }
            }
            network.update();
        }

        return numActive;
    }


    /**
     * Returns the total summed activation in the network over specified number of iterations
     */
    public static double getTotalActivation(Network network, int numIterations) {
        double total = 0;

        for (int iterations = 0; iterations < numIterations; iterations++) {
            for (Neuron neuron : network.getFlatNeuronList()) {
                total += neuron.getActivation();
            }
            network.update();
        }
        return total;
    }
}
