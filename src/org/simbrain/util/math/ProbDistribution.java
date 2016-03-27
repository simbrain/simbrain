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
package org.simbrain.util.math;

import java.util.concurrent.ThreadLocalRandom;

import umontreal.iro.lecuyer.probdist.Distribution;
import umontreal.iro.lecuyer.probdist.ExponentialDist;
import umontreal.iro.lecuyer.probdist.GammaDist;
import umontreal.iro.lecuyer.probdist.LognormalDist;
import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.probdist.ParetoDist;
import umontreal.iro.lecuyer.probdist.UniformDist;
import umontreal.iro.lecuyer.randvar.ExponentialGen;
import umontreal.iro.lecuyer.randvar.GammaGen;
import umontreal.iro.lecuyer.randvar.LognormalGen;
import umontreal.iro.lecuyer.randvar.ParetoGen;
import umontreal.iro.lecuyer.rng.LFSR113;
import umontreal.iro.lecuyer.rng.RandomStream;

public enum ProbDistribution {

    EXPONENTIAL {

        @Override
        public double nextRand(double lambda, double nullVar) {
            return ExponentialGen.nextDouble(DEFAULT_RANDOM_STREAM, lambda);
        }

        @Override
        public int nextRandInt(int lambda, int nullVar) {
            return (int) nextRand(lambda, nullVar);
        }

        @Override
        public Distribution getBestFit(double[] observations, int numObs) {
            return ExponentialDist.getInstanceFromMLE(observations, numObs);
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return ExponentialDist.getMLE(observations, numObs);
        }

        @Override
        public String toString() {
            return "Exponential";
        }

        @Override
        public String getParam1Name() {
            return "Rate (\u03BB)";
        }

        @Override
        public String getParam2Name() {
            return null;
        }

        @Override
        public double getDefaultParam1() {
            return 1;
        }

        @Override
        public double getDefaultParam2() {
            return Double.NaN;
        }

        @Override
        public double getDefaultUpBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getDefaultLowBound() {
            return 0;
        }

    },
    GAMMA {

        @Override
        public double nextRand(double shape, double scale) {
            return GammaGen.nextDouble(DEFAULT_RANDOM_STREAM, shape, scale);
        }

        @Override
        public int nextRandInt(int shape, int scale) {
            return (int) nextRand(shape, scale);
        }

        @Override
        public Distribution getBestFit(double[] observations, int numObs) {
            return GammaDist.getInstanceFromMLE(observations, numObs);
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return GammaDist.getMLE(observations, numObs);
        }

        @Override
        public String toString() {
            return "Gamma";
        }

        @Override
        public String getParam1Name() {
            return "Shape (k)";
        }

        @Override
        public String getParam2Name() {
            return "Scale (\u03B8)";
        }

        @Override
        public double getDefaultParam1() {
            return 2;
        }

        @Override
        public double getDefaultParam2() {
            return 1;
        }

        @Override
        public double getDefaultUpBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getDefaultLowBound() {
            return 0;
        }

    },
    LOGNORMAL {

        @Override
        public double nextRand(double location, double scale) {
            return LognormalGen.nextDouble(DEFAULT_RANDOM_STREAM, location,
                    scale);
        }

        @Override
        public int nextRandInt(int mean, int std) {
            return (int) nextRand(mean, std);
        }

        @Override
        public Distribution getBestFit(double[] observations, int numObs) {
            return LognormalDist.getInstanceFromMLE(observations, numObs);
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return LognormalDist.getMLE(observations, numObs);
        }

        @Override
        public String toString() {
            return "Log-Normal";
        }

        @Override
        public String getParam1Name() {
            return "Location (\u03BC)";
        }

        @Override
        public String getParam2Name() {
            return "Scale (\u03C3)";
        }

        @Override
        public double getDefaultParam1() {
            return 1;
        }

        @Override
        public double getDefaultParam2() {
            return 0.5;
        }

        @Override
        public double getDefaultUpBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getDefaultLowBound() {
            return 0;
        }

    },
    NORMAL {

        /**
         * @param mean the mean for this normal distribution
         * @param std the standard deviation for this normal distribution
         */
        @Override
        public double nextRand(double mean, double std) {
            return (ThreadLocalRandom.current().nextGaussian() * std) + mean;
        }

        /**
         * @param mean the mean for this normal distribution
         * @param std the standard deviation for this normal distribution
         */
        @Override
        public int nextRandInt(int mean, int std) {
            return (int) nextRand(mean, std);
        }

        @Override
        public Distribution getBestFit(double[] observations, int numObs) {
            return NormalDist.getInstanceFromMLE(observations, numObs);
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return NormalDist.getMLE(observations, numObs);
        }

        @Override
        public String toString() {
            return "Normal";
        }

        @Override
        public String getParam1Name() {
            return "Mean (\u03BC)";
        }

        @Override
        public String getParam2Name() {
            return "Std. Dev. (\u03C3)";
        }

        @Override
        public double getDefaultParam1() {
            return 1;
        }

        @Override
        public double getDefaultParam2() {
            return 0.5;
        }

        @Override
        public double getDefaultUpBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getDefaultLowBound() {
            return Double.NEGATIVE_INFINITY;
        }

    },
    PARETO {

        @Override
        public double nextRand(double slope, double min) {
            return ParetoGen.nextDouble(DEFAULT_RANDOM_STREAM, slope, min);
        }

        @Override
        public int nextRandInt(int slope, int min) {
            return (int) nextRand(slope, min);
        }

        @Override
        public Distribution getBestFit(double[] observations, int numObs) {
            return ParetoDist.getInstanceFromMLE(observations, numObs);
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return ParetoDist.getMLE(observations, numObs);
        }

        @Override
        public String toString() {
            return "Pareto";
        }

        @Override
        public String getParam1Name() {
            return "Slope (\u03B1)";
        }

        @Override
        public String getParam2Name() {
            return "Minimum";
        }

        @Override
        public double getDefaultParam1() {
            return 2.0;
        }

        @Override
        public double getDefaultParam2() {
            return 1.0;
        }

        @Override
        public double getDefaultUpBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getDefaultLowBound() {
            return getDefaultParam2();
        }

    },
    UNIFORM {

        /**
         * @param floor the lowest value of the interval
         * @param ceil the highest value of the interval
         */
        @Override
        public double nextRand(double floor, double ceil) {
            return ThreadLocalRandom.current().nextDouble(floor, ceil);
        }

        /**
         * @param floor the lowest value of the interval
         * @param ceil the highest value of the interval
         */
        @Override
        public int nextRandInt(int floor, int ceil) {
            return (int) nextRand(floor, ceil);
        }

        @Override
        public UniformDist getBestFit(double[] observations, int numObs) {
            return UniformDist.getInstanceFromMLE(observations, numObs);
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return UniformDist.getMLE(observations, numObs);
        }

        @Override
        public String toString() {
            return "Uniform";
        }

        @Override
        public String getParam1Name() {
            return "Floor";
        }

        @Override
        public String getParam2Name() {
            return "Ceiling";
        }

        @Override
        public double getDefaultParam1() {
            return 0;
        }

        @Override
        public double getDefaultParam2() {
            return 1;
        }

        @Override
        public double getDefaultUpBound() {
            return 1;
        }

        @Override
        public double getDefaultLowBound() {
            return 0;
        }

    },
    NULL {

        @Override
        public String toString() {
            return "...";
        }

        @Override
        public double nextRand(double var1, double var2) {
            return 0;
        }

        @Override
        public int nextRandInt(int var1, int var2) {
            return 0;
        }

        @Override
        public Distribution getBestFit(double[] observations, int numObs) {
            return null;
        }

        @Override
        public double[] getBestFitParams(double[] observations, int numObs) {
            return null;
        }

        @Override
        public String getParam1Name() {
            return null;
        }

        @Override
        public String getParam2Name() {
            return null;
        }

        @Override
        public double getDefaultParam1() {
            return 0;
        }

        @Override
        public double getDefaultParam2() {
            return 0;
        }

        @Override
        public double getDefaultUpBound() {
            return 0;
        }

        @Override
        public double getDefaultLowBound() {
            return 0;
        };

    };

    // POISSON { //TODO: Move somewhere else, because of single parameter and
    // // integer values being required... doesn't fit with others.
    //
    // @Override
    // public double nextRand(double lambda, double nullVar) {
    // return nextRandInt((int) lambda, (int)nullVar);
    // }
    //
    // @Override
    // public int nextRandInt(int lambda, int nullVar) {
    // return PoissonGen.nextInt(stream, lambda);
    // }
    //
    // @Override
    // public Distribution getBestFit(double[] observations, int numObs) {
    // int [] obs = new int[observations.length]; //Problematic, move?
    // for (int i = 0, n = observations.length; i < n; i++) {
    // obs[i] = (int) observations[i];
    // }
    // return PoissonDist.getInstanceFromMLE(obs, numObs);
    // }
    //
    // @Override
    // public double[]
    // getBestFitParams(double[] observations, int numObs) {
    // int [] obs = new int[observations.length]; //Problematic, move?
    // for (int i = 0, n = observations.length; i < n; i++) {
    // obs[i] = (int) observations[i];
    // }
    // return PoissonDist.getMLE(obs, numObs);
    // }
    //
    // @Override
    // public String getName() {
    // return "Poisson";
    // }
    // };

    public static final RandomStream DEFAULT_RANDOM_STREAM = new LFSR113();

    public abstract double nextRand(double var1, double var2);

    public abstract int nextRandInt(int var1, int var2);

    public abstract Distribution getBestFit(double[] observations, int numObs);

    public abstract double[] getBestFitParams(double[] observations,
            int numObs);

    @Override
    public abstract String toString();

    public abstract String getParam1Name();

    public abstract String getParam2Name();

    public abstract double getDefaultParam1();

    public abstract double getDefaultParam2();

    public abstract double getDefaultUpBound();

    public abstract double getDefaultLowBound();

    public static String[] getNames() {
        String[] names = new String[ProbDistribution.values().length];
        for (int i = 0; i < ProbDistribution.values().length; i++) {
            names[i] = ProbDistribution.values()[i].toString();
        }
        return names;
    }

    /**
     * The Kullback-Leibler divergence between some distribution and a set of
     * observations. In this case, the returned value represents the amount of
     * information lost when the given distribution is used to approximate the
     * given observations.
     *
     * @param d a probability distribution
     * @param observations a 2D array such that the first row represents x
     *            values and the second represents y values (must sum to 1, so
     *            all observations must be scaled to fit this constraint prior
     *            to being passed to this function)
     * @return the amount of information lost in bits when the distribution d is
     *         used to approximate the distribution implicit in the
     *         observations.
     */
    public static double KL_Divergence(Distribution d,
            double[][] observations) {
        double tot = 0;
        double interval;
        double distProb;
        for (int i = 0, n = observations[0].length - 1; i < n; i++) {
            interval = observations[0][i + 1] - observations[0][i];
            distProb = d.cdf(observations[0][i + 1])
                    - d.cdf(observations[0][i]);
            tot += ((Math.log(interval * observations[1][i]) / Math.log(2))
                    / (Math.log(distProb) / Math.log(2)))
                    * (interval * observations[1][i]);
        }
        return tot;
    }

    /**
     * Normalizes a set of observations so they may be used as a discrete
     * probability density function.
     *
     * @param observations
     * @return
     */
    public static double[][] observationsToProbDist(double[][] observations) {
        double[][] retObs = new double[observations.length][observations[0].length];
        retObs[0] = SimbrainMath.normalizeVec(observations[0]);
        retObs[1] = observations[1];
        return retObs;
    }
}
