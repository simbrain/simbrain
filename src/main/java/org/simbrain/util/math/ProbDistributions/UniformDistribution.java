package org.simbrain.util.math.ProbDistributions;

import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbabilityDistribution;
import umontreal.ssj.probdist.UniformDist;

import java.util.concurrent.ThreadLocalRandom;

public class UniformDistribution extends ProbabilityDistribution {

    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Floor",
            description = "An artificial minimum value set by the user.",
            order = 1)
    private double floor = 0;

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Ceiling",
            description = "An artificial minimum value set by the user.",
            order = 2)
    private double ceil = 1;

    private Polarity polarity = Polarity.BOTH;

    /**
     * Public constructor for reflection-based creation. You are encourage to use
     * the builder pattern provided for ProbabilityDistributions.
     */
    public UniformDistribution() {
    }

    /**
     * Create a uniform dist with specified floor and ceiling
     */
    public UniformDistribution(double floor, double ceil) {
        this.floor = floor;
        this.ceil = ceil;
    }

    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble(this.floor, this.ceil);
    }

    public int nextInt() {
        return (int) Math.round(nextDouble());
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

    public String toString() { return "Uniform"; }

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
    public void setLowerBound(double floor) {
        this.floor = floor;
    }

    public static UniformDistributionBuilder builder() {
        return new UniformDistributionBuilder();
    }

    public static UniformDistribution create() {
        return new UniformDistribution();
    }

    public static class UniformDistributionBuilder
        extends ProbabilityDistributionBuilder<
            UniformDistributionBuilder,
            UniformDistribution> {

        private UniformDistribution product = new UniformDistribution();

        @Override
        public UniformDistribution build() {
            return product;
        }

        @Override
        protected UniformDistribution product() {
            return product;
        }

        public UniformDistributionBuilder floor(double floor) {
            product.floor = floor;
            return this;
        }

        public UniformDistributionBuilder ceil(double ceil) {
            product.ceil = ceil;
            return this;
        }

    }

}