package org.simbrain.custom_sims.simulations.neat.util;

/**
 * Some math utility for NEAT use.
 *
 * TODO: Utils like this may already exist; if not move these to Simbrain util classes.
 *
 * @author LeoYulinLi
 *
 */
public class Math {

    /**
     * Check that the specified pair of value is a valid bound.
     * @param floor Lower bound
     * @param ceiling Upper bound
     */
    public static void assertBound(double floor, double ceiling) {
        if (floor > ceiling) {
            throw new IllegalArgumentException("Floor cannot be greater than ceiling.");
        }
    }

    /**
     * Returns a clipped {@code n}.
     * @param n The number to clipped
     * @param floor Lower bound
     * @param ceiling Upper bound
     * @return {@code n} if in range, otherwise floor or ceiling.
     */
    public static double clipping(double n, double floor, double ceiling) {
        assertBound(floor, ceiling);
        if (n > ceiling) {
            return ceiling;
        } else if (n < floor) {
            return floor;
        } else {
            return n;
        }
    }

    /**
     * Returns a clipped {@code n}.
     * @param n The number to clipped
     * @param floor Lower bound
     * @param ceiling Upper bound
     * @return {@code n} if in range, otherwise floor or ceiling.
     */
    public static int clipping(int n, int floor, int ceiling) {
        assertBound(floor, ceiling);
        if (n > ceiling) {
            return ceiling;
        } else if (n < floor) {
            return floor;
        } else {
            return n;
        }
    }

    /**
     * Returns an {@code n} clipped between 0 and 1. Useful for probability.
     * Equivalent to clipping(n, 0.0, 1.0).
     * @param n The number to clipped
     * @return {@code n} if in range, otherwise floor or ceiling.
     */
    public static double probClipping(double n) {
        return clipping(n, 0.0, 1.0);
    }
}
