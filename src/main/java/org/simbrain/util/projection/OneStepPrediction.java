package org.simbrain.util.projection;

import java.util.*;

/**
 * Build a graph model which can be used to predict the next state of an arbitrary system.
 * <p>
 * Builds a Markov Model (which assumes the history of the states does not influence the next state, only the current
 * state) with an NxN state transition matrix which needs to be estimated. Then estimate the transition matrix Stores
 * simple dictionaries of state counts rather than the matrix, as in Python {1: {2: 3, 4: 1, 5: 1}, 2: {4: 3}} etc., and
 *  “smoothing” can be done by simply adding a small constant to every element in the notional sparse matrix before
 * normalizing it.  (Paraphrased from discussion with Matthew Lloyd)
 */
public class OneStepPrediction {

    /**
     * Associate a source {@link DataPoint} with a set of target datapoints associated with "activations" which are in
     * turn used to compute next-step probabilities
     */
    private final HashMap<DataPoint, HashMap<DataPoint, Double>>
            data = new HashMap<>();

    /**
     * Add a source point and the target point that occurred after it. If the source-target pair already exists in the
     * dataset increment the "activation" of that pair by one.
     *
     * @param src the source point
     * @param tar the next point that occurred
     */
    public double addSourceTargetPair(DataPoint src, DataPoint tar) {

        HashMap<DataPoint, Double> targets = data.get(src);

        if (targets == null) {
            // A new source point
            targets = new HashMap();
            targets.put(tar, 1.0);
            data.put(src, targets);
            return 0.0;
        } else {
            Double d = targets.get(tar);
            // Source point has not occurred with this target before
            if (d == null) {
                targets.put(tar, 1.0);
                return 0.0;
            } else {
                // If the target already exists increment its activation by one
                targets.put(tar, d + 1.0);
                return d / getSummedActivations(src); // return the probability before incrementing
            }
        }

    }

    /**
     * Get the probability that the provided target point will occur after the provided source point.
     *
     * @param src the source point
     * @param tar the target point
     * @return the conditional probability of target given source.
     */
    public Double getProbability(DataPoint src, DataPoint tar) {
        // TODO: This is inefficient.  A separate map from source-target pairs to probabilities should be maintained.
        HashMap<DataPoint, Double> fanOut = data.get(src);
        if (fanOut == null) {
            return 0.0;
        }
        Double value = fanOut.get(tar);
        if (value == null) {
            return 0.0;
        } else {
            return value / getSummedActivations(src);
        }
    }

    /**
     * Get the sum of the activations of the target points associated with the provided source.
     */
    private double getSummedActivations(DataPoint src) {
        HashMap<DataPoint, Double> fanOut = data.get(src);
        if (fanOut != null) {
            return fanOut.values().stream().mapToDouble(Double::doubleValue).sum();
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
        for (DataPoint src : data.keySet()) {
            ret.append("Source: " + src + "\n");
            HashMap<DataPoint, Double> targets = data.get(src);
            ret.append("Targets: " + "\n");
            for (DataPoint tar : targets.keySet()) {
                ret.append("   " + tar + ":" + targets.get(tar) + "\n");
            }
        }
        return ret.toString();
    }

    /**
     * Returns the set of  target points and their probabilities, assocaited
     * with a given source.
     */
    public HashMap<DataPoint, Double> getTargets(DataPoint src) {
        return data.get(src);
    }

    /**
     * Number of sources in the dataset (each associated with a
     * list of targets and probabilities.)
     */
    public int getNumPairs() {
        return data.keySet().size();
    }

    /**
     * Returns the number of targets a source point has.
     */
    public int getNumTargetsForSource(DataPoint src) {
        HashMap<DataPoint, Double> targetMap = data.get(src);
        if (targetMap == null) {
            return 0;
        } else {
            return targetMap.size();
        }
    }

}
