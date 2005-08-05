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

import java.util.Random;

import org.simbrain.util.SimbrainMath;
import org.simnet.interfaces.Neuron;
import org.simnet.util.RandomSource;

public class RandomNeuron extends Neuron {

	private RandomSource randomizer = new RandomSource();
    
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public RandomNeuron() {
		randomizer.setUpperBound(this.getUpperBound());
		randomizer.setLowerBound(this.getLowerBound());
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public RandomNeuron(Neuron n) {
		super(n);
		randomizer.setUpperBound(this.getUpperBound());
		randomizer.setLowerBound(this.getLowerBound());
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
		randomizer.setUpperBound(this.getUpperBound());
		randomizer.setLowerBound(this.getLowerBound());
		setBuffer(randomizer.getNumber());
	}
	
    /**
     * @return Returns the mean.
     */
    public double getMean() {
        return randomizer.getMean();
    }
    /**
     * @param mean The mean to set.
     */
    public void setMean(double mean) {
        randomizer.setMean(mean);
    }
    /**
     * @return Returns the standardDeviation.
     */
    public double getStandardDeviation() {
        return randomizer.getStandardDeviation();
    }
    /**
     * @param standardDeviation The standardDeviation to set.
     */
    public void setStandardDeviation(double standardDeviation) {
        randomizer.setStandardDeviation(standardDeviation);
    }
    /**
     * @return Returns the useBounds.
     */
    public boolean isUseBounds() {
    		return randomizer.isUseBounds();
    }
    /**
     * @param useBounds The useBounds to set.
     */
    public void setUseBounds(boolean useBounds) {
    		randomizer.setUseBounds(useBounds);
    }
    /**
     * @return Returns the distributionIndex.
     */
    public int getDistributionIndex() {
        return randomizer.getDistributionIndex();
    }
    /**
     * @param distributionIndex The distributionIndex to set.
     */
    public void setDistributionIndex(int distributionIndex) {
    		randomizer.setDistributionIndex(distributionIndex);
    }
    

	public static String getName() {return "Random";}

}
