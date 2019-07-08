package org.simbrain.util.math;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.simbrain.util.neat.NeatUtils.assertBound;

/**
 * Extends java randomizer with convenience functions to, e.g. get numbers in a range.
 * If called using the static instance, threadsafe random numbers are returned. If created
 * using the constructor,a deterministic sequence of random numbers from an initial seed
 * are returned.
 *
 * @author LeoYulinLi
 * @author Jeff Yoshimi
 *
 */
public class SimbrainRandomizer extends Random {

    //TODO: Move to Simbrain Math or somewhere else generic
    //TODO: If seed is used, use using superclass, else threadlocal

    private static final long serialVersionUID = -3217013262668966634L;

    /**
     * If true, use the superclass randomizer for determinate results.
     */
    private boolean useSeed = true;

    /**
     * Threadsafe non-seeded randomizer for general use.
     */
    public static SimbrainRandomizer rand = new SimbrainRandomizer(System.nanoTime());

    static {
        rand.useSeed = false;
    }

    /**
     * Creates a new random number generator using a single long seed.
     *
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
        if (useSeed) {
            return (nextDouble() * range) + floor;
        } else {
            return (ThreadLocalRandom.current().nextDouble() * range) + floor;
        }
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
        if (useSeed) {
            int range = ceiling - floor;
            return (nextInt(range) + floor);
        } else  {
            return ThreadLocalRandom.current().nextInt(floor, ceiling+ 1);
        }
    }

    public double mutateNumber(double source, double interval) {
        return source + nextDouble(-interval, +interval);
    }

    public double mutateNumberWithProbability(double source, double interval, double probability) {
        if (nextDouble(0, 1) > probability) {
            return mutateNumber(source, interval);
        }
        return source;
    }

}
