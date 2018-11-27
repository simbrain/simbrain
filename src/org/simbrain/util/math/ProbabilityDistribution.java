package org.simbrain.util.math;

import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.*;
import org.simbrain.util.propertyeditor2.CopyableObject;
import org.simbrain.util.propertyeditor2.EditableObject;

import umontreal.iro.lecuyer.rng.LFSR113;
import umontreal.iro.lecuyer.rng.RandomStream;

import java.util.Arrays;
import java.util.List;

//TODO: Allow seed to be set

/**
 * Base class for all ProbabilityDistribution.
 */
public abstract class ProbabilityDistribution implements CopyableObject {

    /**
     * Distributions for drop-down list used by
     * {@link org.simbrain.util.propertyeditor2.ObjectTypeEditor}
     * to set a type of probability distribution.
     */
    public static List<Class> DIST_LIST = Arrays.asList(ExponentialDistribution.class,
        GammaDistribution.class, LogNormalDistribution.class,
        NormalDistribution.class, ParetoDistribution.class, UniformDistribution.class);

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class> getTypes() {
        return DIST_LIST;
    }

    public static final RandomStream DEFAULT_RANDOM_STREAM = new LFSR113();

    /**
     * Get a random double number from a probability distribution
     * @return a random number
     */
    public abstract double nextRand();

    /**
     * Get a random integer number from a probability distribution
     * @return a random number
     */
    public abstract int nextRandInt();

    public abstract ProbabilityDistribution deepCopy();

    public abstract String getName();

    public abstract void setClipping(boolean clipping);

    public abstract void setUpperBound(double ceiling);

    public abstract void setLowerBound(double floor);

    public abstract void setPolarity(Polarity polarity);

    public abstract Polarity getPolarity();

    @Override
    public EditableObject copy() {
        return deepCopy();
    }

    /**
     * Static utility method to get a bounded value.
     * @param value the value to be clipped
     * @param lowerBound lower bound
     * @param upperBound upper bound
     * @return the vlipped value
     */
    protected static double clipping(double value, double lowerBound, double upperBound) {
        double result = value;

        if (result > upperBound) {
            result = upperBound;
        } else if (result < lowerBound) {
            result = lowerBound;
        }

        return result;
    }

    //TODO
    public double getRandom() {
        return getPolarity().value(nextRand());
    }


    /**
     * Base builder for all {@link ProbabilityDistribution} instances.
     * Create custom instances of the specific implementation of {@link ProbabilityDistribution} by using
     * the setter methods on this class.
     * After zero or more of these,
     * use the build() method to create a specific {@link ProbabilityDistribution} instance.
     * These can all be chained. For example, create a custom excitatory {@link LogNormalDistribution}
     * of which has a location of 2.5:
     * <code>
     *     ProbabilityDistribution randomizer =
     *      UniformDistribution.builder()
     *          .polarity(Polarity.EXCITATORY)
     *          .location(2.5)
     *          .build();
     * </code>
     *
     * If no special set-up is needed, just use LogNormalDistribution.builder().build()
     * or the short-cut equivalent LogNormalDistribution.create().
     * @param <B> The type of the builder to return when building
     * @param <T> The type of the final product to return when finish building.
     */
    public abstract static class ProbabilityDistributionBuilder<
            B extends ProbabilityDistributionBuilder,
            T extends ProbabilityDistribution
            > {

        /**
         * Uniform access to the product being build. Only used in this abstract class
         * where the product cannot be instantiate yet.
         * @return the product being build
         */
        protected abstract T product();

        /**
         * Sets the upper bound of the probability distribution when clipped.
         * @param upperBound the highest value of the interval for clipping
         * @return the Builder instance (for use in chained initialization)
         */
        public B upperBound(double upperBound) {
            product().setUpperBound(upperBound);
            return (B) this;
        }

        /**
         * Sets the lower bound of the probability distribution when clipped.
         * @param lowerBound the lowest value of the interval for clipping
         * @return the Builder instance (for use in chained initialization)
         */
        public B lowerBound(double lowerBound) {
            product().setLowerBound(lowerBound);
            return (B) this;
        }

        /**
         * Sets if the random number generated should be clip or not.
         * @param clipping if the random number should be clip
         * @return the Builder instance (for use in chained initialization)
         */
        public B clipping(boolean clipping) {
            product().setClipping(clipping);
            return (B) this;
        }

        /**
         * Sets if the random number generated should be only positive, negative, or both.
         * @param polarity the polarity
         * @return the Builder instance (for use in chained initialization)
         */
        public B polarity(Polarity polarity) {
            product().setPolarity(polarity);
            return (B) this;
        }

        /**
         * Builds a instance of specific {@link ProbabilityDistribution} of given states.
         * @return the final product
         */
        public abstract T build();
    }

    /**
     * Utility class that encapsulates a probability distribution so that it
     * can be used in an {@link org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor}
     * so that it's easy to create a property editor to edit a probability distribution.
     */
    public static class Randomizer implements EditableObject {

        @UserParameter(label = "Randomizer", isObjectType = true)
        private ProbabilityDistribution probabilityDistribution = UniformDistribution.create();

        public ProbabilityDistribution getProbabilityDistribution() {
            return probabilityDistribution;
        }

        public void setProbabilityDistribution(ProbabilityDistribution probabilityDistribution) {
            this.probabilityDistribution = probabilityDistribution;
        }

        /**
         * Returns a random number from the underlying probabiliyt distribution,
         * whose properties are set by the property editor.
         *
         * @return the random number
         */
        public double getRandom() {
            return probabilityDistribution.nextRand();
        }
    }
}
