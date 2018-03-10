package org.simbrain.util.math.ProbDistributions;

import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbabilityDistribution;

import umontreal.iro.lecuyer.probdist.Distribution;
import umontreal.iro.lecuyer.probdist.GammaDist;
import umontreal.iro.lecuyer.randvar.GammaGen;

public class GammaDistribution extends ProbabilityDistribution {

    @UserParameter(
            label = "Shape (k)",
            description = "Shape (k).",
            defaultValue = "2.0", order = 1)
    private double shape = 2.0;


    @UserParameter(
            label = "Scale (\u03B8)",
            description = "Scale (\u03B8).",
            defaultValue = "1.0", order = 2)
    private double scale = 1.0;


    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Floor",
            description = "An artificial minimum value set by the user.",
            defaultValue = "0.0", order = 3)
    private double floor = 0.0;

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    @UserParameter(
            label = "Ceiling",
            description = "An artificial minimum value set by the user.",
            defaultValue = "" + Double.POSITIVE_INFINITY, order = 4)
    private double ceil = Double.POSITIVE_INFINITY;

    @UserParameter(
            label = "Clipping",
            description = "When clipping is enabled, the randomizer will reject outside the floor and ceiling values.",
            defaultValue = "false", order = 5)
    private boolean clipping = false;

    @Override
    public double nextRand() {
        return clipping(
                GammaGen.nextDouble(DEFAULT_RANDOM_STREAM, shape, scale),
                floor,
                ceil
                );
    }

    @Override
    public int nextRandInt() {
        return (int) nextRand();
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
    
    public Distribution getBestFit(double[] observations, int numObs) {
        return GammaDist.getInstanceFromMLE(observations, numObs);
    }

    public double[] getBestFitParams(double[] observations, int numObs) {
        return GammaDist.getMLE(observations, numObs);
    }

}
