package org.simbrain.util.neat;

import java.util.Random;
import static org.simbrain.util.neat.NeatUtils.assertBound;

/**
 * Java Random with extra method for NEAT use.
 * @author LeoYulinLi
 *
 */
public class NEATRandomizer extends Random {

    private static final long serialVersionUID = -3217013262668966634L;

    /**
     * Creates a new random number generator using a single long seed.
     * @param seed the initial seed
     */
    public NEATRandomizer(long seed) {
        super(seed);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed double value between floor and ceil
     * from this random number generator's sequence.
     * @param floor lower bound
     * @param ceiling upper bound
     * @return the next pseudorandom double value between floor and ceil
     */
    public double nextDouble(double floor, double ceiling) {
        assertBound(floor, ceiling);
        double range = ceiling - floor;
        return (nextDouble() * range) + floor;
    }
}
