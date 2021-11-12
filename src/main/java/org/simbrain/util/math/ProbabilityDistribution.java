package org.simbrain.util.math;

import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.*;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.propertyeditor.EditableObject;
import umontreal.ssj.rng.LFSR113;
import umontreal.ssj.rng.RandomStream;

import java.util.Arrays;
import java.util.List;

//TODO: Allow seed to be set

/**
 * Base class for all ProbabilityDistribution.
 */
public abstract class ProbabilityDistribution implements CopyableObject {

    /**
     * Distributions for drop-down list used by {@link org.simbrain.util.propertyeditor.ObjectTypeEditor}
     * to set a type of probability distribution.
     */
    public static List<Class> DIST_LIST = Arrays.asList(ExponentialDistribution.class,
        GammaDistribution.class, LogNormalDistribution.class,
        NormalDistribution.class, ParetoDistribution.class, TwoValued.class, UniformDistribution.class);

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class> getTypes() {
        return DIST_LIST;
    }

    public static final RandomStream DEFAULT_RANDOM_STREAM = new LFSR113();

    /**
     * Get a random double number from a probability distribution
     *
     * @return a random number
     */
    public abstract double nextRand();

    /**
     * Get a random integer number from a probability distribution
     *
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
    public ProbabilityDistribution copy() {
        return deepCopy();
    }

    /**
     * Static utility method to get a bounded value. Attempts to re-draw
     * the value from the distribution until the drawn value falls within the
     * specified bounds, so as to maintain the relative probabilities
     * dictated by the PDF in that interval. If too many attempts are made
     * and none fall in the bounds, assigns lower or upper bound to the value
     * depending upon which is closer and returns it. If the value is already
     * within the interval, does nothing, returns the value.
     *
     * @param value      the value to be clipped
     * @param lowerBound lower bound of the interval
     * @param upperBound upper bound of the interval
     * @return the clipped value
     */
    protected static double clipping(ProbabilityDistribution dist, double value,
                                     double lowerBound, double upperBound) {
        if(value >= lowerBound && value <= upperBound) {
            return value;
        }
        int cnt = 0;
        double result = value;
        do {
            //TODO: Parameterize 20, so it's not a magic number
            if (cnt >= 20)
                break;
            result = dist.nextRand();
            cnt++;
        } while(result < lowerBound || result > upperBound);

        if (cnt >= 20) { // Failed to re-draw a value that fell within the bounds
            if (value > upperBound) {
                return upperBound;
            } else if (value < lowerBound) {
                return lowerBound;
            }
        }
        return result;
    }

    /**
     * Return the next sampled value from this probability distribution.
     */
    public double getRandom() {
        return getPolarity().value(nextRand());
    }

    /**
     * Helper class to return a builder for a specified type of distribution.
     *
     * @param distType a string description of the distribution type.
     *                 Options are: "Uniform", "Normal", "LogNormal","Pareto", and "Exponential".
     * @param param1 The first parameter of the distribution.  For normal this is mean, for lognormal
     *               location, for pareto it is slope, for exponential it is lambda.
     * @param param2 The second parameter of the distribution.  For normal this is stdev, for lognormal
     *               scale, for pareto it is min.
     * @return the builder, ready to use
     */
    public static ProbabilityDistributionBuilder getBuilder(String distType, double param1, double param2) {
        switch (distType) {
        case "Uniform":
            return UniformDistribution.builder().floor(param1).ceil(param2);
        case "Normal":
            return  NormalDistribution.builder().mean(param1).standardDeviation(param2);
        case "LogNormal":
            return LogNormalDistribution.builder().location(param1).scale(param2);
        case "Pareto":
            return ParetoDistribution.builder().slope(param1).min(param2);
        case "Exponential":
            return ExponentialDistribution.builder().lambda(param1);
        default:
            return UniformDistribution.builder().floor(param1).ceil(param2);
        }
    }

    /**
     * Returns a specified distribution with parameters 1 & 2 (e.g. mean & std for a
     * normal distribution, shape & scale for gamma and so on), where the random generator
     * using that distribution will only draw values in the range [lowerBound, upperBound].
     * Clipping is set to true automatically. The domains constraints on the PDF will
     * still be respected
     * @param distType a string description of the distribution type.
     *                 Options are: "Uniform", "Normal", "LogNormal","Pareto", and "Exponential".
     * @param param1 The first parameter of the distribution.  For normal this is mean, for lognormal
     *               location, for pareto it is slope, for exponential it is lambda.
     * @param param2 The second parameter of the distribution.  For normal this is stdev, for lognormal
     *               scale, for pareto it is min.
     * @param lowerBound the lower boundary from which random values can be drawn. No values
     *                   will be generated below this value, relative probabilities within the
     *                   interval as dictated by the PDF will be respected.
     * @param upperBound the upper boundary from which random values can be drawn. No values
     *                   will be generated above this value, relative probabilities within the
     *                   interval as dictated by the PDF will be respected.
     * @return A probability distribution builder using the specified PDF, with the specified parameters
     * which only generates values in the given range. Ready to use.
     */
    public static ProbabilityDistributionBuilder getBuilder(String distType, double param1, double param2,
                                                            double lowerBound, double upperBound) {
        return getBuilder(distType, param1, param2)
                .clipping(true).lowerBound(lowerBound).upperBound(upperBound);
    }

    /**
     * @see #getBuilder(String, double, double).
     * Param 1 is set to 0, param 2 to 1.
     */
    public static ProbabilityDistributionBuilder getBuilder(String distType) {
        return getBuilder(distType, 0, 1);
    }

    /**
     * Base builder for all {@link ProbabilityDistribution} instances. Create
     * custom instances of the specific implementation of {@link
     * ProbabilityDistribution} by using the setter methods on this class. After
     * zero or more of these, use the build() method to create a specific {@link
     * ProbabilityDistribution} instance. These can all be chained. For example,
     * create a custom excitatory {@link LogNormalDistribution} of which has a
     * location of 2.5:
     * <code>
     * ProbabilityDistribution randomizer = UniformDistribution.builder()
     * .polarity(Polarity.EXCITATORY) .location(2.5) .build();
     * </code>
     * <p>
     * If no special set-up is needed, just use LogNormalDistribution.builder().build()
     * or the short-cut equivalent LogNormalDistribution.create().
     *
     * @param <B> The type of the builder to return when building
     * @param <T> The type of the final product to return when finish building.
     */
    public abstract static class ProbabilityDistributionBuilder<
        B extends ProbabilityDistributionBuilder,
        T extends ProbabilityDistribution
        > {

        /**
         * Uniform access to the product being build. Only used in this abstract
         * class where the product cannot be instantiate yet.
         *
         * @return the product being build
         */
        protected abstract T product();

        /**
         * Sets the upper bound of the probability distribution when clipped.
         *
         * @param upperBound the highest value of the interval for clipping
         * @return the Builder instance (for use in chained initialization)
         */
        public B upperBound(double upperBound) {
            product().setUpperBound(upperBound);
            return (B) this;
        }

        /**
         * Sets the lower bound of the probability distribution when clipped.
         *
         * @param lowerBound the lowest value of the interval for clipping
         * @return the Builder instance (for use in chained initialization)
         */
        public B lowerBound(double lowerBound) {
            product().setLowerBound(lowerBound);
            return (B) this;
        }

        /**
         * Sets if the random number generated should be clip or not.
         *
         * @param clipping if the random number should be clip
         * @return the Builder instance (for use in chained initialization)
         */
        public B clipping(boolean clipping) {
            product().setClipping(clipping);
            return (B) this;
        }

        /**
         * Sets if the random number generated should be only positive,
         * negative, or both.
         *
         * @param polarity the polarity
         * @return the Builder instance (for use in chained initialization)
         */
        public B polarity(Polarity polarity) {
            product().setPolarity(polarity);
            return (B) this;
        }

        /**
         * Builds a instance of specific {@link ProbabilityDistribution} of
         * given states.
         *
         * @return the final product
         */
        public abstract T build();
    }

    /**
     * Utility class that encapsulates a probability distribution so that it can
     * be used in an {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}
     * so that it's easy to create a property editor to edit a probability
     * distribution.
     */
    public static class Randomizer implements EditableObject {

        @UserParameter(label = "Randomizer", isObjectType = true)
        private ProbabilityDistribution probabilityDistribution = UniformDistribution.create();

        public Randomizer() {
        }

        public Randomizer(ProbabilityDistribution probabilityDistribution) {
            this.probabilityDistribution = probabilityDistribution;
        }

        public ProbabilityDistribution getProbabilityDistribution() {
            return probabilityDistribution;
        }

        public void setProbabilityDistribution(ProbabilityDistribution probabilityDistribution) {
            this.probabilityDistribution = probabilityDistribution;
        }

        /**
         * Returns a random number from the underlying probability distribution,
         * whose properties are set by the property editor.
         *
         * @return the random number
         */
        public double getRandom() {
            return probabilityDistribution.nextRand();
        }

        public double getRandomInt() {
            return probabilityDistribution.nextRandInt();
        }

        @Override
        public String getName() {
            return "Randomizer";
        }
    }
}
