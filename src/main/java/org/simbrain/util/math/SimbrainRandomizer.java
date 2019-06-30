package org.simbrain.util.math;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.simbrain.util.neat.NeatUtils.assertBound;

/**
 * Extends java randomizer with convenience functions, e.g.
 * getting a number in a range.
 *
 * @author LeoYulinLi
 *
 */
public class SimbrainRandomizer extends Random {

    //TODO: Move to Simbrain Math or somewhere else generic
    //TODO: If seed is used, use using superclass, else threadlocal

    private static final long serialVersionUID = -3217013262668966634L;

    /**
     * For general use.
     */
    public static SimbrainRandomizer rand = new SimbrainRandomizer(System.nanoTime());

    /**
     * Creates a new random number generator using a single long seed.
     * @param seed the initial seed
     */
    public SimbrainRandomizer(long seed) {
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
        return (ThreadLocalRandom.current().nextDouble() * range )+ floor;
    }

    /**
     * Returns the next pseudorandom, uniformly distributed integer value between floor and ceil
     * from this random number generator's sequence.
     * @param floor lower bound
     * @param ceiling upper bound
     * @return the next pseudorandom integer value between floor and ceil
     */
    public int nextInteger(int floor, int ceiling) {
        assertBound(floor, ceiling);
        return ThreadLocalRandom.current().nextInt(floor, ceiling+ 1);
    }

}
