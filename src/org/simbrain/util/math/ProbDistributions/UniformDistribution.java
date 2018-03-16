package org.simbrain.util.math.ProbDistributions;

import java.util.concurrent.ThreadLocalRandom;

import org.simbrain.util.UserParameter;

import org.simbrain.util.math.ProbabilityDistribution;
import umontreal.iro.lecuyer.probdist.UniformDist;

public class UniformDistribution extends ProbabilityDistribution {

    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Floor",
            description = "An artificial minimum value set by the user.",
            defaultValue = "0", order = 1)
    private double floor = 0;

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Ceiling",
            description = "An artificial minimum value set by the user.",
            defaultValue = "1", order = 2)
    private double ceil = 1;

    public double nextRand() {
        return ThreadLocalRandom.current().nextDouble(this.floor, this.ceil);
    }

    public int nextRandInt() {
        return (int) nextRand();
    }

    public UniformDist getBestFit(double[] observations, int numObs) {
        return UniformDist.getInstanceFromMLE(observations, numObs);
    }


    public double[] getBestFitParams(double[] observations, int numObs) {
        return UniformDist.getMLE(observations, numObs);
    }

    public String getName() {
        return "Uniform";
    }

    @Override
    public UniformDistribution deepCopy() {
        UniformDistribution cpy = new UniformDistribution();
        cpy.ceil = this.ceil;
        cpy.floor = this.floor;
        return cpy;
    }

    @Override
    public void setClipping(boolean clipping) {
    }

    @Override
    public void setUpperBound(double ceiling) {
        this.ceil = ceiling;
    }

    @Override
    public void setLowerbound(double floor) {
        this.floor = floor;
    }

    @Override
    public void setParam1(double p1) {
    }

    @Override
    public void setParam2(double p2) {
    }
}
