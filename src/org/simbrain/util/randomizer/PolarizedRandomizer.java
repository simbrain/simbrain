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

import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;

/**
 * 
 * A tentative solution to some of the issues involving randomizers....
 * Creaties a randomizer that returns either positive or negative values
 * exclusively depending on its polarity. It is meant to be the case that no
 * parameters should ever be negative since they represent absolute values and
 * the sign of the output is wholly determined by the polarity.
 * 
 * @author Zach Tosi
 *
 */
public class PolarizedRandomizer extends Randomizer {
    
    public static final Polarity DEFAULT_POLARITY = Polarity.EXCITATORY;
    
    public Polarity polarity;
    
    /**
     * Default constructor.
     */
    public PolarizedRandomizer() {
        super();
        polarity = DEFAULT_POLARITY;
    }

    /**
     * 
     * @param polarity
     */
    public PolarizedRandomizer(Polarity polarity) {
        super();
        this.polarity = polarity;
    }
    
    /**
     * Copy constructor.
     *
     * @param dup the <code>RandomSource</code> to duplicate.
     * @return the duplicated <code>RandomSource</code> object.
     */
    public PolarizedRandomizer(final PolarizedRandomizer dup) {
        setPdf(dup.getPdf());
        setParams(dup.getParam1(), dup.getParam2());
        setUpperBound(dup.getUpperBound());
        setLowerBound(dup.getLowerBound());
        setClipping(getClipping());
        setPolarity(dup.getPolarity());
    }

    /**
     * Checks to make sure that there are no negative parameters.
     * @return
     */
    public String checkParamsNonNegative() {
        String ret = "";
        if (param1 < 0) {
            ret = ret.concat(pdf.getParam1Name() + " is negative. \n");
        }
        if (param2 < 0) {
            ret = ret.concat(pdf.getParam2Name() + " is negative. \n");
        }
        if (upperBound < 0) {
            ret = ret.concat("Upper boundary is negative. \n");
        }
        if (lowerBound < 0) {
            ret = ret.concat("Lower boundary is negative. \n");
        }
        return ret;
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
            return polarity.value(pdf.nextRand(param1, param2));
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
     * @return the param1
     */
    public double getParam1() {
        return param1;
    }

    /**
     * @return the param2
     */
    public double getParam2() {
        return param2;
    }

    /**
     * @return the polarity
     */
    public Polarity getPolarity() {
        return Polarity.valueOf(Polarity.class, polarity.toString());
    }

    /**
     * @param polarity the polarity to set
     */
    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
    }

}