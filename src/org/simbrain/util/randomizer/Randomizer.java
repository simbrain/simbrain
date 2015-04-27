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
package org.simbrain.util.randomizer;

import org.simbrain.util.math.ProbDistribution;

/**
 * @author Zach Tosi
 * @author Jeff Yoshimi
 *
 * <b>Randomizer</b> produces numbers drawn from a probability distribution
 * according to a set of user-specified parameters.
 */
public class Randomizer {

    public static final ProbDistribution DEFAULT_DISTRIBUTION =
            ProbDistribution.UNIFORM;

    protected ProbDistribution pdf = DEFAULT_DISTRIBUTION;

    /**
     * First parameter of the selected probability distribution. To see what
     * this signifies see
     * {@link org.simbrain.util.math.ProbDistribution#getParam1Name()}.
     */
    protected double param1 = 0;

    /**
     * First parameter of the selected probability distribution. To see what
     * this signifies see
     * {@link org.simbrain.util.math.ProbDistribution#getParam2Name()}.
     */
    protected double param2 = 1;

    /** Upper bound of the random distribution. */
    protected double upperBound = 1;

    /** Lower bound of the random distribution. */
    protected double lowerBound = 0;

    /**
     * Whether the Gaussian distribution should be clipped at upper / lower
     * bound values.
     */
    protected boolean clipping = false;

    /**
     * Default constructor.
     */
    public Randomizer() {

    }

    public Randomizer(ProbDistribution pdf) {
        this.pdf = pdf;
    }
    
    /**
     * Copy constructor.
     *
     * @param dup the <code>RandomSource</code> to duplicate.
     */
    public Randomizer(final Randomizer dup) {
        setPdf(dup.getPdf());
        setParams(dup.getParam1(), dup.getParam2());
        setUpperBound(dup.getUpperBound());
        setLowerBound(dup.getLowerBound());
        setClipping(getClipping());
    }

    //    /**
    //     * Creates a "mirrored" copy of this randomizer, wherein the mirror's lower
    //     * bound is set to the additive inverse of the original's upper bound and
    //     * likewise the mirror's upper bound is set to the additive inverse of the
    //     * original's lower bound. The mean of the mirror is the additive inverse of
    //     * the mean of the original. The standard deviation and clipping are
    //     * directly copied.
    //     *
    //     * @return a mirrored copy of this randomizer
    //     */
    //    public Randomizer mirrorCopy() {
    //        Randomizer mirror = new Randomizer();
    //        mirror.setDistributionIndex(this.getDistributionIndex());
    //        mirror.setLowerBound(-this.getUpperBound());
    //        mirror.setUpperBound(-this.getLowerBound());
    //        mirror.setMean(-this.getMean());
    //        mirror.setStandardDeviation(this.getStandardDeviation());
    //        mirror.setClipping(this.getClipping());
    //        return mirror;
    //    }

    /**
     * Returns a random number.
     *
     * @return the next random number
     */
    public double getRandom() {
        if (clipping) {
            return clip(pdf.nextRand(param1, param2));
        } else {
            return pdf.nextRand(param1, param2);
        }
    }

    /**
     * Clip <code>val</code> to upper and lower bounds.
     *
     * @param val the value to clip
     * @return the clipped value
     */
    private double clip(final double val) {
        double ret = val;

        if (ret > upperBound) {
            ret = upperBound;
        } else if (ret < lowerBound) {
            ret = lowerBound;
        }

        return ret;
    }

    /**
     * @return Returns the clipping.
     */
    public boolean getClipping() {
        return clipping;
    }

    /**
     * @param clipping The useBounds to set.
     */
    public void setClipping(final boolean clipping) {
        this.clipping = clipping;
    }

    /**
     * Returns the string name of the distribution.
     *
     * @return the distribution name.
     */
    public String getDistributionName() {
        return pdf.toString();
    }

    /**
     * @return the pdf
     */
    public ProbDistribution getPdf() {
        return pdf;
    }

    /**
     * @param pdf the pdf to set
     */
    public void setPdf(ProbDistribution pdf) {
        this.pdf = pdf;
    }

    /**
     * See the javadoc at {@link #param1}.
     *
     * @return the param1
     */
    public double getParam1() {
        return param1;
    }

    /**
     * See the javadoc at {@link #param2}.
     *
     * @return the param2
     */
    public double getParam2() {
        return param2;
    }

    /**
     *
     * @param param1
     * @param param2
     */
    protected void setParams(double param1, double param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    public void setParamsConsistent(String p1Name, double param1,
            String p2Name, double param2) {
        if(!p1Name.equals(pdf.getParam1Name())
                || !p2Name.equals(pdf.getParam2Name())) {
            throw new IllegalArgumentException("Parameter name/Distribution" +
                    " mismatch.");
        }
        setParams(param1, param2);
    }



    public void setParam1Consistent(String p1Name, double param1) {
        if (!p1Name.equals(pdf.getParam1Name())) {
            throw new IllegalArgumentException("Parameter name/Distribution" +
                    " mismatch.");
        }
        this.param1 = param1;
    }

    public void setParam2Consistent(String p2Name, double param2) {
        if (p2Name == null) return; //TODO think about this, it's a hack
        if (!p2Name.equals(pdf.getParam2Name())) {
            throw new IllegalArgumentException("Parameter name/Distribution" +
                    " mismatch.");
        }
        this.param2 = param2;
    }

    /**
     * @return the upperBound
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * @param upperBound the upperBound to set
     */
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * @return the lowerBound
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * @param lowerBound the lowerBound to set
     */
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @param param1 the param1 to set
     */
    public void setParam1(double param1) {
        this.param1 = param1;
    }

    /**
     * @param param2 the param2 to set
     */
    public void setParam2(double param2) {
        this.param2 = param2;
    }

}
