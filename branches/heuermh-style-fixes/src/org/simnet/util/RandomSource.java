/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.util;

import java.util.Random;

/**
 * 
 * <b>RandomSource</b>
 */
public class RandomSource {

    private static String[] functionList = {"Uniform", "Gaussian"};
    
    public static int UNIFORM = 0;
    public static int GAUSSIAN = 1;
    
    private int distributionIndex = 0;
    
    private double upperBound = 1;
    private double lowerBound = -1;
    private double mean = .5;
    private double standardDeviation = .5;
    private boolean clipping = false;
    private Random randomGenerator = new Random();

	public double getRandom() {
		if(getDistributionIndex() == UNIFORM) {
			return (upperBound -  lowerBound) * Math.random() + lowerBound;
		} else {
			double val = randomGenerator.nextGaussian();
			val = val * standardDeviation + mean;
			if (clipping == true) {
				val = clip(val);
			} 
			return val;
		}
	}
	
	private double clip(double val) {
		double ret = val;
		if (ret > upperBound) {
			ret = upperBound;
		} else if (ret < lowerBound) {
			ret = lowerBound;
		}
		return ret;
	}
    
    /**
     * Returns a duplicate AdditiveNeuron (used, e.g., in copy/paste)
     */
    public RandomSource duplicate(RandomSource dup) {
        RandomSource rs = new RandomSource();
        rs = dup;
        rs.setLowerBound(getLowerBound());
        rs.setUpperBound(getUpperBound());
        rs.setMean(getMean());
        rs.setStandardDeviation(getStandardDeviation());
        rs.setClipping(getClipping());
        return rs;
    }
    
    /**
     * @return Returns the mean.
     */
    public double getMean() {
        return mean;
    }
    /**
     * @param mean The mean to set.
     */
    public void setMean(double mean) {
        this.mean = mean;
    }
    /**
     * @return Returns the standardDeviation.
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }
    /**
     * @param standardDeviation The standardDeviation to set.
     */
    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
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
    public void setClipping(boolean clipping) {
    
    		
        this.clipping = clipping;
    }
    /**
     * @return Returns the functionList.
     */
    public static String[] getFunctionList() {
        return functionList;
    }
    /**
     * @return Returns the distributionIndex.
     */
    public int getDistributionIndex() {
        return distributionIndex;
    }
    /**
     * @param distributionIndex The distributionIndex to set.
     */
    public void setDistributionIndex(int distributionIndex) {
        this.distributionIndex = distributionIndex;
    }
    
	/**
	 * @return Returns the lowerBound.
	 */
	public double getLowerBound() {
		return lowerBound;
	}
	/**
	 * @param lowerBound The lowerBound to set.
	 */
	public void setLowerBound(double lowerBound) {
		this.lowerBound = lowerBound;
	}
	/**
	 * @return Returns the upperBound.
	 */
	public double getUpperBound() {
		return upperBound;
	}
	/**
	 * @param upperBound The upperBound to set.
	 */
	public void setUpperBound(double upperBound) {
		this.upperBound = upperBound;
	}
}
