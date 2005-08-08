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
import org.simnet.util.RandomSource;

public class BinaryNeuron extends Neuron{
	
	private double threshold = .5;
	private RandomSource noise = new RandomSource();
	private boolean addNoise = false;
	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public BinaryNeuron() {
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public BinaryNeuron(Neuron n) {
		super(n);
	}
		
	/**
	 * Returns a duplicate BinaryNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
		BinaryNeuron bn = new BinaryNeuron();
		bn = (BinaryNeuron)super.duplicate(bn);
		bn.setThreshold(getThreshold());
		return bn;
	}
	
	public void update() {
		double wtdInput = this.weightedInputs();
		if(wtdInput > threshold) {
			setBuffer(upperBound);
		} else setBuffer(lowerBound);
	}
	
	/**
	 * @return Returns the threshold.
	 */
	public double getThreshold() {
		return threshold;
	}
	/**
	 * @param threshold The threshold to set.
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	/**
	 * @return Returns the noise.
	 */
	public RandomSource getNoise() {
		return noise;
	}
	/**
	 * @param noise The noise to set.
	 */
	public void setNoise(RandomSource noise) {
		this.noise = noise;
	}
    /**
     * @return Returns the addNoise.
     */
    public boolean isAddNoise() {
        return addNoise;
    }
    /**
     * @param addNoise The addNoise to set.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }
	
	public static String getName() {return "Binary";}

}