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
	
	private double threshold = 0;
	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public BinaryNeuron() {
	
		this.setUpperBound(1);
		this.setLowerBound(-1);
		this.setIncrement(1);
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public BinaryNeuron(Neuron n) {
		super(n);
	}
		
	public Neuron duplicate() {
		return super.duplicate(this);
	}
	
	public void update() {
		double wtdInput = this.weightedInputs();
		if(wtdInput > threshold) {
			setActivation(getUpperBound());
		} else setActivation(getLowerBound());
	}

	public void commitBuffer() {
		
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
	
	public static String getName() {return "Binary";}

}