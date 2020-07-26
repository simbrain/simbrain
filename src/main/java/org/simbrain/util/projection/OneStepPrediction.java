package org.simbrain.util.projection;

import java.util.*;

/**
 * Build a model which can be used to predict the next state 
 * <br>
 * The basic data-structure is a hash map which associates data points with sets of target datapoints, which are
 * {@link DataPointColored}) with have an activation field that can be used to track how many times the point has
 * been visited.
 * <br>
 * Essentially builds a Markov model using a dictionary of visitation  counts, e.g. (in Python notation)
 * {1: {1: 3, 2: 5, 3: 2} , 2: {1: 3}, 3: {1:2, 2:4,3:1}}
 * So in this example the system went three times from state 1->1, five times from 1 -> 2, etc.
 * <br>
 * A next-state probability distribution can then be estimated by normalizing the fan-out from a given source state,
 * e.g.from state 1 the probabilities of going to the next states are 3/10, 5/10, and 2/10.
 * <br>
 * Thanks to Matthew Lloyd on the high level design (implementation errors are my own!)
 *
 * @author Jeff Yoshimi
 */
public class OneStepPrediction {

    /**
     * Associates a source point with a set of target points that store activations, used to
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
