/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util;

import java.awt.Point;


/**
 * <b>SimbrainMath</b> is the math functions used in simbrain.
 */
public class SimbrainMath {
    /**
     * Calculates the Euclidean distance between two points.  Used in World.
     *
     * @param src source point
     * @param tar target point
     *
     * @return distance between source and target
     */
    public static int distance(final Point src, final Point tar) {
        int x1 = src.x;
        int x2 = tar.x;
        int y1 = src.y;
        int y2 = tar.y;

        return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    /**
     * Calculates the Euclidean distance between two points.  Used in World.
     *
     * @param src source point
     * @param tar target point
     *
     * @return distance between source and target
     */
    public static double distance(final double[] src, final double[] tar) {
        double x1 = src[0];
        double x2 = tar[0];
        double y1 = src[1];
        double y2 = tar[1];

        return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    /**
     * Set an array of doubles to zero.  TODO: Replace usage in World with library call
     *
     * @param size size of array
     *
     * @return new array of zero'd values
     */
    public static double[] zeroVector(final int size) {
        double[] ret = new double[size];

        for (int i = 0; i < size; i++) {
            ret[i] = 0;
        }

        return ret;
    }

    /**
     * Finds the vector multiple.
     * @param theVec Vecotr
     * @param mult Multiple
     * @return Multiple of the vector and the multiple
     *TODO: Replace occurence in world with library call
     */
    public static double[] multVector(final double[] theVec, final double mult) {
        double[] ret = new double[theVec.length];

        for (int i = 0; i < theVec.length; i++) {
            ret[i] = theVec[i] * mult;
        }

        return ret;
    }

    /**
     * Return the greater of two integers.
     *
     * @param one first int
     * @param two second int
     *
     * @return greater of one and two
     */
    public static int max(final int one, final int two) {
        if (one > two) {
            return one;
        }

        return two;
    }

    /**
     * Finds the larger of two numbers.
     * @param one First number
     * @param two Second number
     * @return the larger number
     */
    public static double[] max(final double[] one, final double[] two) {
        if (one.length > two.length) {
            return one;
        }

        return two;
    }

    /**
     * Add these vectors. If one is larger than the other return a vector with zeros in the difference.
     * @param base Base number
     * @param add Number to be added to base number
     * @return added vectors
     */
    public static double[] addVector(final double[] base, final double[] add) {
        double[] ret = new double[max(base.length, add.length)];

        if (add.length == base.length) {
            for (int i = 0; i < base.length; i++) {
                ret[i] = base[i] + add[i];
            }
        } else {
            // if the vectors are not the same length, add zeros in the extra slots
            double[] temp = max(base, add);
            int max = max(base.length, add.length);
            int min = max - Math.abs(base.length - add.length);

            for (int i = 0; i < min; i++) {
                ret[i] = base[i] + add[i];
            }

            for (int i = min; i < max; i++) {
                ret[i] = temp[i];
            }
        }

        return ret;
    }

    /**
     * Helper function for computing mean values of an array of doubles.
     *
     * @param vals an array of values
     *
     * @return the mean values of the array
     */
    public static double getAverage(final double[] vals) {
        double sum = 0;

        for (int i = 0; i < vals.length; i++) {
            sum += vals[i];
        }

        return sum / vals.length;
    }

    /**
     * Prints out the vector list.
     * @param da Vector list
     */
    public static void printVector(final double[] da) {
        for (int i = 0; i < da.length; i++) {
            System.out.print(da[i] + " ");
        }

        System.out.println("");
    }

    /**
     * Calculates the inverse of the error function.  Originally written by S.C. Pohlig, adapted by J.N. Sanders
     * @param p Parameter to find inverse of the error
     * @return inverse of the error
     */
    public static double inverf(final double p) { // 0 <= p <= 1
        /* Originally written by S.C. Pohlig, adapted by J.N. Sanders
         *
         * This function returns an approximation to the inverse of the standard
         * normal probability distribution.  The approximation error is less than
         * 4.5e-4.  The approximation formula is from M. Abramowitz and I. A. Stegun,
         * Handbook of Mathematical Functions, eqn. 26.2.23, Dover Publications, Inc.
         *
         * The C language error function returns erf(x) = (2/sqrt(pi)) * Integral(0,x)
         *   of exp(-t*t)dt, which gives erf(infinity) = 1. In essence, this gives the
         *   area under the curve between -x and +x, normalized to 1.
         * However, this function (inverf), solves for the inverse of (1/sqrt(pi))
         *   * Integral(-infinity, x) of exp(-t*t)dt.
         * As a result, the symmetric inverse is:
         *      x = inverf(erf(x) / 2. + .5)
         *
         *  Given the integral of a unit variance gaussian, from -infinity to x,
         *   normalized such that the integral to +infinity is 1, multiply this result
         *   by sqrt(2) to obtain x.
         */
        double c0 = 2.515517;
        double c1 = 0.802853;
        double c2 = 0.010328;
        double d1 = 1.432788;
        double d2 = 0.189269;
        double d3 = 0.001308;
        double maxSigma = 7;

        double t1;
        double t2;
        double q;
        double x;

        if (p >= 1.) {
            return (maxSigma);
        } else if (p <= 0.) {
            return (-maxSigma);
        } else if (p == 0.5) {
            return (0.0);
        }

        if (p < 0.5) {
            q = p;
        } else {
            q = 1.0 - p;
        }

        t2 = -2.0 * Math.log(q);
        t1 = Math.sqrt(t2);

        x = t1 - ((c0 + (c1 * t1) + (c2 * t2)) / (1.0 + (d1 * t1) + (d2 * t2) + (d3 * t1 * t2)));
        x = x / Math.sqrt(2.);

        /**
         * jns
         */
        if (p < 0.5) {
            return (-x);
        } else {
            return (x);
        }
    }

    /**
     * Finds the largest value in a vector array.
     * @param theVec Vector array
     * @return largest value in array
     */
    public static double getMaximum(final double[] theVec) {
        double max = Double.MIN_VALUE;

        for (int i = 0; i < theVec.length; i++) {
            if (theVec[i] > max) {
                max = theVec[i];
            }
        }

        return max;
    }

    /**
     * Add noise to a vector.
     *
     * @param vector vector to which noise should be added
     * @param noiselevel Noise level
     */
    public static void addNoise(final double[] vector, final double noiselevel) {
        double randUniform;
        double sigma = noiselevel * SimbrainMath.getMaximum(vector);
        double sqrt2 = Math.sqrt(2);

        for (int i = 0; i < vector.length; i++) {
            randUniform = Math.random();
            vector[i] += (sigma * sqrt2 * SimbrainMath.inverf(randUniform));
        }
    }

    /**
     * Add noise to a vector and return the result.
     *
     * @param vector vector to which noise should be added
     * @param noiselevel Noise level
     * @return resuling vector
     */
    public static double[] getNoisyVector(final double[] vector, final double noiselevel) {
        double randUniform;
        double sigma = noiselevel * SimbrainMath.getMaximum(vector);
        double sqrt2 = Math.sqrt(2);
        double[] returnVector = new double[vector.length];

        for (int i = 0; i < vector.length; i++) {
            randUniform = Math.random();
            returnVector[i] = vector[i] + (sigma * sqrt2 * SimbrainMath.inverf(randUniform));
        }
        return returnVector;
    }

    /**
     * Create a random vector with i components.
     *
     * @param i number of components in this vector.
     * @return the random vector.
     */
    public static double[] randomVector(int length) {
        double[] returnVector = new double[length];
        for (int i = 0; i < returnVector.length; i++) {
            returnVector[i] = Math.random();
        }
        return returnVector;
    }

    /**
     * Returns a vector of random values between min and max.
     *
     * @param length number of components in the vector
     * @param min minimum value for random values
     * @param max maximum value for random values
     * @return the random vector
     */
    public static double[] randomVector(final int length, final double min, final double max) {
    	double[] returnVector = new double[length];
        for (int i = 0; i < returnVector.length; i++) {
            returnVector[i] = min + Math.random() * Math.abs(max - min);
        }
        return returnVector;
    }

    /**
     * Round a double value to a specified number of places.
     *
     * (From user dforbu on the Sun Java Programming forum).
     *
     * @param d double value to round
     * @param places placed to round do
     * @return rounded value
     */
    public static final double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10,
            (double) places);
    }

    /**
     * Returns the Euclidean norm of the supplied vector.
     *
     * @param vector vector to check
     * @return the norm
     */
    public static double getVectorNorm(double[] vector) {
        double ret = 0;
        for (int i = 0; i < vector.length; i++) {
            ret += Math.pow(vector[i], 2);
        }
        return Math.sqrt(ret);
    }
}
