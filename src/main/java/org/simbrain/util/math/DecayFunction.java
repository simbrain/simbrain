package org.simbrain.util.math;

import org.simbrain.util.UserParameter;
import org.simbrain.util.math.DecayFunctions.GaussianDecayFunction;
import org.simbrain.util.math.DecayFunctions.LinearDecayFunction;
import org.simbrain.util.math.DecayFunctions.QuadraticDecayFunction;
import org.simbrain.util.math.DecayFunctions.StepDecayFunction;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DecayFunction implements CopyableObject {

    /**
     * Decay functions for drop-down list used by
     * {@link org.simbrain.util.propertyeditor.ObjectTypeEditor}
     * to set a type of probability distribution.
     */
    public static List<Class> DECAY_FUNCTIONS_LIST = Arrays.asList(
            StepDecayFunction.class,
            LinearDecayFunction.class,
            GaussianDecayFunction.class,
            QuadraticDecayFunction.class
    );

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class> getTypes() {
        return DECAY_FUNCTIONS_LIST;
    }

    /**
     * If outside of this radius the object has no affect on the network.
     */
    @UserParameter(
            label = "Dispersion",
            description = "If outside of this radius the object has no affect on the network.",
            order = 1)
    private double dispersion = 70;

    /**
     * Peak value.
     */
    @UserParameter(
            label = "Peak Distance",
            description = "Peak value",
            order = 2)
    private double peakDistance = 0;

    /**
     * Noise generator for this decay function if {@link DecayFunction#addNoise} is true.
     */
    @UserParameter(label = "Randomizer", isObjectType = true, order = 1000, tab = "Noise")
    private ProbabilityDistribution randomizer = new UniformRealDistribution();

    /**
     * If true, add noise to object's stimulus vector.
     */
    @UserParameter(
            label = "Add noise",
            description = "If true, add noise to object's stimulus vector.",
            order = 99, tab = "Noise")
    private boolean addNoise = false;

    public double getDispersion() {
        return dispersion;
    }

    public void setDispersion(double dispersion) {
        this.dispersion = dispersion;
    }

    public double getPeakDistance() {
        return peakDistance;
    }

    public void setPeakDistance(double peak) {
        this.peakDistance = peak;
    }

    public ProbabilityDistribution getRandomizer() {
        return randomizer;
    }

    public void setRandomizer(ProbabilityDistribution randomizer) {
        this.randomizer = randomizer;
    }

    public boolean getAddNoise() {
        return addNoise;
    }

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * Get the decay amount for the given distance.
     *
     * @param distance the distance to compute the decay scaling factor
     * @return the decay scaling factor
     */
    public abstract double getScalingFactor(double distance);

    /**
     * Apply the scaling factor on a double array based on the given distance.
     *
     * @param distance the distance to compute the decay scaling factor
     * @param vector the vector to be multiplied with the decay scaling factor
     * @return the multiplied vector
     */
    public double[] apply(double distance, double[] vector) {
        double[] ret = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            ret[i] = vector[i] * getScalingFactor(distance) + (addNoise ? randomizer.sampleDouble() : 0);
        }
        return ret;
    }

    /**
     * Apply the scaling factor on a Double List based on the given distance.
     *
     * @param distance the distance to compute the decay scaling factor
     * @param vector the vector to be multiplied with the decay scaling factor
     * @return the multiplied vector
     */
    public List<Double> apply(double distance, List<Double> vector) {
        List<Double> ret = new ArrayList<>();
        for (Double e : vector) {
            ret.add(e * getScalingFactor(distance) + (addNoise ? randomizer.sampleDouble() : 0));
        }
        return ret;
    }

    public abstract static class DecayFunctionBuilder<
            B extends DecayFunctionBuilder,
            T extends DecayFunction
            > {

        /**
         * Uniform access to the product being build. Only used in this abstract class
         * where the product cannot be instantiate yet.
         * @return the product being build
         */
        protected abstract T product();

        /**
         * Builds a instance of specific {@link DecayFunction} of given states.
         * @return the final product
         */
        public abstract T build();

        public B dispersion(double dispersion) {
            product().setDispersion(dispersion);
            return (B) this;
        }

        public B peakDistance(double peak) {
            product().setPeakDistance(peak);
            return (B) this;
        }

        public B randomizer(ProbabilityDistribution randomizer) {
            product().setRandomizer(randomizer);
            return (B) this;
        }

        public B addNoise(boolean addNoise) {
            product().setAddNoise(addNoise);
            return (B) this;
        }
    }
}