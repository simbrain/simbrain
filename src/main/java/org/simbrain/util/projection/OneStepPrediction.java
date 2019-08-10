package org.simbrain.util.projection;

import java.util.*;

/**
 * Build a graph model which can be used to predict the next state of an arbitrary system.
 * <br>
 * The basic data-structure is a hash map which associated data points with sets of target
 * {@link DataPointColored}, which have an activation field that can be used to track probabilities.
 * <br>
 * Builds a Markov Model (which assumes the history of the states does not influence the next state, only the current
 * state) with an NxN state transition matrix which needs to be estimated. Then estimate the transition matrix Stores
 * simple dictionaries of state counts rather than the matrix, as in Python {1: {2: 3, 4: 1, 5: 1}, 2: {4: 3}} etc., and
 * “smoothing” can be done by simply adding a small constant to every element in the notional sparse matrix before
 * normalizing it.  (Paraphrased from discussion with Matthew Lloyd)
 */
public class OneStepPrediction {

    /**
     * Associate a source {@link DataPointColored} with a set of DataPointColoreds that store activations, used to
     * compute next-step probabilities
     */
    private final HashMap<DataPointColored, HashSet<DataPointColored>>
            data = new HashMap<>();

    /**
     * Add a source point and the target point that occurred after it. If the source-target pair already exists in the
     * dataset increment the "activation" of that pair by one.
     *
     * @param src the source point
     * @param tar the next point that occurred
     */
    public double addSourceTargetPair(DataPointColored src, DataPointColored tar) {

        HashSet<DataPointColored> targets = data.get(src);

        // Get the probability before accounting for this event
        double prob = tar.getProbability(src);

        // Increment target point
        tar.incrementActivationCount(src);

        if (targets == null) {
            // Add a set of targets for this source
            targets = new HashSet<DataPointColored>();
            targets.add(tar);
            data.put(src, targets);
        } else {
            targets.add(tar);
        }

        return prob;
    }

    /**
     * Get the sum of the activations of the target points associated with the provided source.
     */
    private double getSummedActivations(DataPointColored src) {
        HashSet<DataPointColored> targets = data.get(src);
        if (targets != null) {
            double total = 0;
            for (DataPointColored tar : targets) {
                total += tar.getActivationCount(src);
            }
            return total;
        } else {
            return 0;
        }
    }

    /**
     * Clear the data.
     */
    public void clear() {
        data.clear();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (DataPointColored src : data.keySet()) {
            ret.append("Source: " + src + "\n");
            HashSet<DataPointColored> targets = data.get(src);
            ret.append("Targets: " + "\n");
            for (DataPointColored tar : targets) {
                ret.append("   " + tar + "\n");
            }
            // TODO: Add counts and probabilities?
        }
        return ret.toString();
    }


    /**
     * Number of sources in the dataset (each associated with a list of targets and probabilities.)
     */
    public int getNumPairs() {
        return data.keySet().size();
    }

    /**
     * Get set of targets for a given source point, or null if there is none.
     *
     * @param src the source point
     * @return the set of targets, or null if no targets are yet associated with it
     */
    public HashSet<DataPointColored> getTargets(DataPointColored src) {
        return data.get(src);
    }

    /**
     * Returns the number of targets a source point has.
     */
    public int getNumTargetsForSource(DataPointColored src) {
        HashSet<DataPointColored> targets = data.get(src);
        if (targets == null) {
            return 0;
        } else {
            return targets.size();
        }
    }

}
