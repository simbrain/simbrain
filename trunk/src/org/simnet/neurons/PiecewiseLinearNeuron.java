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

public class PiecewiseLinearNeuron extends Neuron {

    private double slope = 1;
    private double bias = 0;
    
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public PiecewiseLinearNeuron() {
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public PiecewiseLinearNeuron(Neuron n) {
		super(n);
	}
	
	/**
	 * Returns a duplicate BinaryNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
		PiecewiseLinearNeuron pn = new PiecewiseLinearNeuron();
//		bn = (BinaryNeuron)super.duplicate(bn);
//		bn.setThreshold(getThreshold());
		return pn;
	}
	
	public void update() {
		
		double wtdInput = this.weightedInputs();
		setBuffer(clip(slope * (wtdInput + bias)));
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
	public static String getName() {return "Piecewise Linear";}
}
