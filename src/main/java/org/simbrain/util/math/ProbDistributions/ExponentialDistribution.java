package org.simbrain.util.math.ProbDistributions;

import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbabilityDistribution;
import umontreal.ssj.randvar.ExponentialGen;

public class ExponentialDistribution extends ProbabilityDistribution {

    @UserParameter(
            label = "Rate (\u03BB)",
            description = "The rate of exponential decay; higher rate parameters will produce more small values.",
            order = 1)
    private double lambda = 1.0;

    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Floor",
            description = "An artificial minimum value set by the user.",
            order = 3, useSetter = true)
    private double floor = 0.0;

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Ceiling",
            description = "An artificial minimum value set by the user.",
            order = 4, useSetter = true)
    private double ceil = Double.POSITIVE_INFINITY;

    @UserParameter(
            label = "Clipping",
            description = "When clipping is enabled, the randomizer will reject outside the floor and ceiling values.",
            order = 5)
    private boolean clipping = false;

    private Polarity polarity = Polarity.BOTH;

    /**
     * Public constructor for reflection-based creation. You are encourage to use
     * the builder pattern provided for ProbabilityDistributions.
     */
    public ExponentialDistribution() {
    }

    @Override
    public double nextDouble() {
        return ExponentialGen.nextDouble(DEFAULT_RANDOM_STREAM, lambda);
    }

    @Override
    public ProbabilityDistribution deepCopy() {
        ExponentialDistribution cpy = new ExponentialDistribution();
        cpy.lambda = this.lambda;
        cpy.ceil = this.ceil;
        cpy.floor = this.floor;
        cpy.clipping = this.clipping;
        return cpy;
    }

    @Override
    public String getName() {
        return "Exponential";
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

}