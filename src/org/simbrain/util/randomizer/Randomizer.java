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

import org.simbrain.util.math.ProbDistributions.NormalDistribution;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.EditableObject;

/**
 * <b>Randomizer</b> produces numbers drawn from a probability distribution
 * according to a set of user-specified parameters.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class Randomizer implements EditableObject {

    /**
     * The default prob. distribution for this randomizer.
     */
    public static final UniformDistribution DEFAULT_DISTRIBUTION = new UniformDistribution();

    /**
     * The probability distribution associated with this randomizer.
     */
    protected ProbabilityDistribution pdf = DEFAULT_DISTRIBUTION;

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
    public Randomizer(ProbabilityDistribution pdf) {
        this.pdf = pdf;
    }

    /**
     * Copy constructor.
     *
     * @param dup the <code>RandomSource</code> to duplicate.
     */
    public Randomizer(final Randomizer dup) {
        setPdf(dup.getPdf().deepCopy());
    }

    /**
     * Returns a random number.
     *
     * @return the next random number
     */
    public double getRandom() {
        return pdf.nextRand();
    }


    /**
     * @return the pdf
     */
    public ProbabilityDistribution getPdf() {
        return pdf;
    }

    /**
     * @param pdf the pdf to set
     */
    public void setPdf(final ProbabilityDistribution pdf) {
        this.pdf = pdf;
    }

    /**
     * Test main.
     */
    public static void main(String[] args) {
        Randomizer rand = new Randomizer(new NormalDistribution());
//        rand.setUpperBound(5);
//        rand.setLowerBound(-5);
//        for (int i = 0; i < 10; i++) {
//            System.out.println(rand.getRandom());
//        }
//        System.out.println("-------");
//        rand.setPdf(ProbDistribution.NORMAL);
//        rand.setParam1(0);
//        rand.setParam2(100);
//        rand.setClipping(true);
        for (int i = 0; i < 10; i++) {
            System.out.println(rand.getRandom());
        }
    }

    /**
     * For all but uniform, lower bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     *
     * @param lb the lowerBound to set
     */
    public void setLowerBound(final double lb) {
        this.pdf.setLowerbound(lb);
    }

    /**
     * For all but uniform, upper bound is only used in conjunction with
     * clipping, to truncate the distribution. So if clipping is false this
     * value is not used.
     *
     * @param ub the upperBound to set
     */
    public void setUpperBound(final double ub) {
        this.pdf.setUpperBound(ub);
    }

    /**
     * @param clipping The useBounds to set.
     */
    public void setClipping(final boolean clipping) {
        this.pdf.setClipping(clipping);
    }
}
