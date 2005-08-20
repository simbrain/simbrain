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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.util.RandomSource;


public class LinearNeuron extends Neuron {
	
	private double slope = 1;
	private double bias = 0;
	private RandomSource noise = new RandomSource();
	private boolean addNoise = false;
	private boolean clipping = false;
	
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public LinearNeuron() {
	
	    
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public LinearNeuron(Neuron n) {
		super(n);
	}
		
	/**
	 * Returns a duplicate BinaryNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
		LinearNeuron ln = new LinearNeuron();
		return ln;
	}
	
	public void update() {		
		double wtdInput = this.weightedInputs();
		double val = slope * (wtdInput + bias);
		
		if(addNoise == true) {
			val += noise.getRandom();
		}
		if (clipping == true) {
			val = clip(val);
		}

		setBuffer(val);
	}
	
    /**
     * @return Returns the bias.
     */
    public double getBias() {
        return bias;
    }
    /**
     * @param bias The bias to set.
     */
    public void setBias(double bias) {
        this.bias = bias;
    }
    /**
     * @return Returns the slope.
     */
    public double getSlope() {
        return slope;
    }
    /**
     * @param slope The slope to set.
     */
    public void setSlope(double slope) {
        this.slope = slope;
    }
    
	public static String getName() {return "Linear";}
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
    public boolean getAddNoise() {
        return addNoise;
    }
    /**
     * @param addNoise The addNoise to set.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }
	/**
	 * @return Returns the clipping.
	 */
	public boolean getClipping() {
		return clipping;
	}
	/**
	 * @param clipping The clipping to set.
	 */
	public void setClipping(boolean clipping) {
		this.clipping = clipping;
	}
}
