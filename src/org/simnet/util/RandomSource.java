/*
 * Created on Aug 4, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simnet.util;

import java.util.Random;

/**
 * @author jyoshimi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
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
    private boolean useBounds = false;
    private Random randomGenerator = new Random();

	public double getRandom() {
		if(getDistributionIndex() == UNIFORM) {
			return (upperBound -  lowerBound) * Math.random() + lowerBound;
		} else {
			double val = randomGenerator.nextGaussian();
			val = val * standardDeviation + mean;
			if (useBounds == true) {
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
     * @return Returns the useBounds.
     */
    public boolean getUseBounds() {
        return useBounds;
    }
    /**
     * @param useBounds The useBounds to set.
     */
    public void setUseBounds(boolean useBounds) {
    
    		
        this.useBounds = useBounds;
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
