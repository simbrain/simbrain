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
import org.simnet.neurons.rules.Tanh;

public class BinaryNeuron extends Neuron{
	
	private double threshold = .5;
	private double upperValue = 1;
	private double lowerValue = 0;
	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public BinaryNeuron() {
	
		this.setUpperBound(upperValue);
		this.setLowerBound(lowerValue);
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public BinaryNeuron(Neuron n) {
		super(n);
	}
		
	public Neuron duplicate() {
		BinaryNeuron bn = new BinaryNeuron();
		return super.duplicate(bn);
	}
	
	public void update() {
		double wtdInput = this.weightedInputs();
		if(wtdInput > threshold) {
			setBuffer(upperValue);
		} else setBuffer(lowerValue);
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
		setLowerBound(lowerValue);
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
		setUpperBound(upperValue);
	}
	public static String getName() {return "Binary";}

}