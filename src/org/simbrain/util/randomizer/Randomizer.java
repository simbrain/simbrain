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
 * <b>Randomizer</b> produces numbers drawn from a probability distribution
 * according to a set of user-specified parameters.
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class Randomizer {

    /** The default prob. distribution for this randomizer. */
    public static final ProbDistribution DEFAULT_DISTRIBUTION = ProbDistribution.UNIFORM;

    /** The probability distribution associated with this randomizer. */
    protected ProbDistribution pdf = DEFAULT_DISTRIBUTION;

    /**
     * First parameter of the selected probability distribution. E.g. mean of
     * the normal. See <a href =
     * "http://www.simbrain.net/Documentation/docs/Pages/Utils/Randomizers/Randomizers.html">.
     * Also see {@link org.simbrain.util.math.ProbDistribution#getParam1Name()}.
     */
    protected double param1 = 0;

    /**
     * Second parameter of the selected probability distribution. E.g. stdev of
     * the normal. See <a href =
     * "http://www.simbrain.net/Documentation/docs/Pages/Utils/Randomizers/Randomizers.html">.
     * Also see {@link org.simbrain.util.math.ProbDistribution#getParam1Name()}.
     */
    protected double param2 = 1;

    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
    protected double upperBound = 1;

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     */
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

    /**
     * Construct a randomizer with a specific probability density function.
     *
     * @param pdf the probability density function.
     */
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
    public void setPdf(final ProbDistribution pdf) {
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
     * Set both parameters.
     */
    protected void setParams(double param1, double param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    /**
     * Set the randomizer by specifying parameter names and parameters.
     */
    public void setParamsByName(String p1Name, double param1, String p2Name,
            double param2) {
        // Not currently used and will probably be changed in a refactor.
        if (!p1Name.equals(pdf.getParam1Name())
                || !p2Name.equals(pdf.getParam2Name())) {
            throw new IllegalArgumentException(
                    "Parameter name/Distribution" + " mismatch.");
        }
        setParams(param1, param2);
    }

    /**
     * Set the parameter using its string name.
     * 
     * @param p1Name string name, must match exactly.
     * @param param1 the parameter value.
     */
    public void setParam1ByName(String p1Name, double param1) {
        if (!p1Name.equals(pdf.getParam1Name())) {
            throw new IllegalArgumentException(
                    "Parameter name/Distribution" + " mismatch.");
        }
        this.param1 = param1;
    }

    /**
     * Set the parameter using its string name.
     * 
     * @param p2Name string name, must match exactly.
     * @param param2 the parameter value.
     */
    public void setParam2ByName(String p2Name, double param2) {
        if (p2Name == null)
            return; // TODO think about this, it's a hack
        if (!p2Name.equals(pdf.getParam2Name())) {
            throw new IllegalArgumentException(
                    "Parameter name/Distribution" + " mismatch.");
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
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     *
     * @param ub the upperBound to set
     */
    public void setUpperBound(final double ub) {
        this.upperBound = ub;
        if (pdf == ProbDistribution.UNIFORM) {
            param2 = upperBound;
        }
    }

    /**
     * @return lower bound
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     *
     * @param lb the lowerBound to set
     */
    public void setLowerBound(final double lb) {
        this.lowerBound = lb;
        if (pdf == ProbDistribution.UNIFORM) {
            param1 = lb;
        }
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

    /**
     * Test main.
     */
    public static void main(String[] args) {
        Randomizer rand = new Randomizer();
        rand.setUpperBound(5);
        rand.setLowerBound(-5);
        for (int i = 0; i < 10; i++) {
            System.out.println(rand.getRandom());
        }
        System.out.println("-------");
        rand.setPdf(ProbDistribution.NORMAL);
        rand.setParam1(0);
        rand.setParam2(100);
        rand.setClipping(true);
        for (int i = 0; i < 10; i++) {
            System.out.println(rand.getRandom());
        }
    }

}
