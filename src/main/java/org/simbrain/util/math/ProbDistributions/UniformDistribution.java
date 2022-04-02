package org.simbrain.util.math.ProbDistributions;

import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbabilityDistribution;

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


}