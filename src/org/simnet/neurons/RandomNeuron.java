/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;

public class RandomNeuron extends Neuron {

    private static String[] functionList = {"Uniform", "Gaussian"};
    
    private int distributionIndex = 0;
    
    private double upperValue = 1;
    private double lowerValue = 0;
    private double mean = .5;
    private double standardDeviation = .5;
    private boolean useBounds = false;
    
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public RandomNeuron() {
	
		this.setUpperBound(upperValue);
		this.setLowerBound(lowerValue);
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public RandomNeuron(Neuron n) {
		super(n);
	}
	
	/**
	 * Returns a duplicate BinaryNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
//		BinaryNeuron bn = new BinaryNeuron();
//		bn = (BinaryNeuron)super.duplicate(bn);
//		bn.setThreshold(getThreshold());
//		return bn;
	    return null;
	}
	
	public void update() {
//		double wtdInput = this.weightedInputs();
//		if(wtdInput > threshold) {
//			setBuffer(upperValue);
//		} else setBuffer(lowerValue);
	}
    /**
     * @return Returns the lowerValue.
     */
    public double getLowerValue() {
        return lowerValue;
    }
    /**
     * @param lowerValue The lowerValue to set.
     */
    public void setLowerValue(double lowerValue) {
        this.lowerValue = lowerValue;
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
     * @return Returns the upperValue.
     */
    public double getUpperValue() {
        return upperValue;
    }
    /**
     * @param upperValue The upperValue to set.
     */
    public void setUpperValue(double upperValue) {
        this.upperValue = upperValue;
    }
    /**
     * @return Returns the useBounds.
     */
    public boolean isUseBounds() {
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
    
	public static String getName() {return "Random";}
}
