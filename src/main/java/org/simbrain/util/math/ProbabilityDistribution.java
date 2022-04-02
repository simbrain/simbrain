package org.simbrain.util.math;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.*;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.propertyeditor.EditableObject;
import umontreal.ssj.rng.LFSR113;
import umontreal.ssj.rng.RandomStream;

import java.util.Arrays;
import java.util.List;

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
        NormalDistribution.class, ParetoDistribution.class, UniformDistribution.class);

    /**
     * Called via reflection using {@link UserParameter#typeListMethod()}.
     */
    public static List<Class> getTypes() {
        return DIST_LIST;
    }

    /**
     * Backing for this distribution.
     */
    protected AbstractRealDistribution dist;

    /**
     * Random generator for pseudo-random sequences on which a seed can be set.
     */
    protected final JDKRandomGenerator randomGenerator = new JDKRandomGenerator();

    public static final RandomStream DEFAULT_RANDOM_STREAM = new LFSR113();

    public abstract double nextDouble();

    public abstract ProbabilityDistribution deepCopy();

    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public ProbabilityDistribution copy() {
        return deepCopy();
    }

    /**
     * Use this to ensure two probability distributions return the same pseudo-random sequence of numbers.
     * See ProbabilityDistributionTest.kt for exmamples
     *
     */
    public void setSeed(int seed) {
        randomGenerator.setSeed(seed);
    }

    /**
     * Utility class that encapsulates a probability distribution so that it can
     * be used in an {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}
     * so that it's easy to create a property editor to edit a probability
     * distribution.
     */
    public static class Randomizer implements EditableObject {

        @UserParameter(label = "Randomizer", isObjectType = true)
        private ProbabilityDistribution probabilityDistribution = new UniformDistribution();

        public Randomizer() {}

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
            return probabilityDistribution.nextDouble();
        }

        @Override
        public String getName() {
            return "Randomizer";
        }
    }
}
