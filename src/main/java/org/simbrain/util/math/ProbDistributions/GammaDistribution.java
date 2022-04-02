package org.simbrain.util.math.ProbDistributions;

import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbabilityDistribution;
import umontreal.ssj.randvar.GammaGen;

public class GammaDistribution extends ProbabilityDistribution {

    @UserParameter(
            label = "Shape (k)",
            description = "Shape (k).",
            order = 1)
    private double shape = 2.0;

    @UserParameter(
            label = "Scale (\u03B8)",
            description = "Scale (\u03B8).",
            order = 2)
    private double scale = 1.0;

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

    /**
     * Public constructor for reflection-based creation. You are encourage to use
     * the builder pattern provided for ProbabilityDistributions.
     */
    public GammaDistribution() {
    }

    @Override
    public double nextDouble() {
        return GammaGen.nextDouble(DEFAULT_RANDOM_STREAM, shape, scale);
    }

    @Override
    public GammaDistribution deepCopy() {
        GammaDistribution cpy = new GammaDistribution();
        cpy.shape = this.shape;
        cpy.scale = this.scale;
        cpy.ceil = this.ceil;
        cpy.floor = this.floor;
        cpy.clipping = this.clipping;
        return cpy;
    }

    @Override
    public String getName() {
        return "Gamma";
    }

    public double getShape() {
        return shape;
    }

    public void setShape(double shape) {
        this.shape = shape;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

}