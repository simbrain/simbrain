package org.simbrain.util.math.ProbDistributions;

import java.util.concurrent.ThreadLocalRandom;

import org.simbrain.util.UserParameter;

import org.simbrain.util.math.ProbabilityDistribution;
import umontreal.iro.lecuyer.probdist.Distribution;
import umontreal.iro.lecuyer.probdist.NormalDist;

public class NormalDistribution extends ProbabilityDistribution {

    @UserParameter(
            label = "Mean (\u03BC)",
            description = "The expected value of the distribution.",
            defaultValue = "1.0", order = 1)
    private double mean = 1.0;


    @UserParameter(
            label = "Std. Dev. (\u03C3)",
            description = "The average squared distance from the mean.",
            defaultValue = "0.5", order = 2)
    private double standardDeviation = 0.5;


    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Floor",
            description = "An artificial minimum value set by the user.",
            defaultValue = "1", order = 3)
    private double floor = 3;

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Ceiling",
            description = "An artificial minimum value set by the user.",
            defaultValue = "0", order = 4)
    private double ceil = 4;

    @UserParameter(
            label = "Add noise",
            description = "When clipping is enabled, the randomizer will reject outside the floor and ceiling values.",
            defaultValue = "false", order = 5)
    private boolean clipping = false;

    public double nextRand() {
        return (ThreadLocalRandom.current().nextGaussian() * standardDeviation) + mean;
    }

    public int nextRandInt() {
        return (int) nextRand();
    }

    public Distribution getBestFit(double[] observations, int numObs) {
        return NormalDist.getInstanceFromMLE(observations, numObs);
    }

    public double[] getBestFitParams(double[] observations, int numObs) {
        return NormalDist.getMLE(observations, numObs);
    }

    public String getName() {
        return "Normal";
    }

    @Override
    public String toString() {
        return "Normal";
    }
}
