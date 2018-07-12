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

//  This will take the place of probdistribution.  Possibly change its name later
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

    public abstract double nextRand();

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

    interface Buildable<T> {
        T build();
    }

    public static abstract class ProbabilityDistributionBuilder<
            B extends ProbabilityDistributionBuilder,
            T extends ProbabilityDistribution
            > implements Buildable<T> {

        /**
         * Uniform access to the product being build. Only used in this abstract class
         * where the product cannot be instantiate yet.
         * @return the product being build
         */
        protected abstract T product();

        public B ofUpperBound(double upperBound) {
            product().setUpperBound(upperBound);
            return (B) this;
        }

        public B ofLowerBound(double lowerBound) {
            product().setLowerBound(lowerBound);
            return (B) this;
        }

        public B ofClipping(boolean clipping) {
            product().setClipping(clipping);
            return (B) this;
        }

        public B ofPolarity(Polarity polarity) {
            product().setPolarity(polarity);
            return (B) this;
        }
    }
}
