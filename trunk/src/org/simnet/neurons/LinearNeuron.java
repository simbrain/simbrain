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


public class LinearNeuron extends Neuron {
	
	private double slope = 1;
	private double midpoint = 0;
	private double decayRate = 0;
	
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
//		ln = (LinearNeuron)super.duplicate(ln);
//		bn.setThreshold(getThreshold());
		return ln;
	}
	
	public void update() {
//		double wtdInput = this.weightedInputs();
//		if(wtdInput > threshold) {
//			setBuffer(upperValue);
//		} else setBuffer(lowerValue);
	}
	

    /**
     * @return Returns the decayRate.
     */
    public double getDecayRate() {
        return decayRate;
    }
    /**
     * @param decayRate The decayRate to set.
     */
    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
    }
    /**
     * @return Returns the midpoint.
     */
    public double getMidpoint() {
        return midpoint;
    }
    /**
     * @param midpoint The midpoint to set.
     */
    public void setMidpoint(double midpoint) {
        this.midpoint = midpoint;
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
}
