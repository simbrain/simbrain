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


public class SinusoidalNeuron extends Neuron {
    
    private double phase = 1;
    private double amplitude = 0;
  
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public SinusoidalNeuron() {
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public SinusoidalNeuron(Neuron n) {
		super(n);
	}
		
	/**
	 * Returns a duplicate BinaryNeuron (used, e.g., in copy/paste)
	 */
	public Neuron duplicate() {
//		BinaryNeuron bn = new BinaryNeuron();
//		bn = (BinaryNeuron)super.duplicate(bn);
//		bn.setThreshold(getThreshold());
		return null;
	}
	
	public void update() {
//		double wtdInput = this.weightedInputs();
//		if(wtdInput > threshold) {
//			setBuffer(upperBound);
//		} else setBuffer(lowerBound);
	}


    /**
     * @return Returns the lowerValue.
     */
    public double getAmplitude() {
        return amplitude;
    }
    /**
     * @param lowerValue The lowerValue to set.
     */
    public void setAmplitude(double amplitude) {
        this.amplitude = amplitude;
    }
    /**
     * @return Returns the upperValue.
     */
    public double getPhase() {
        return phase;
    }
    /**
     * @param upperValue The upperValue to set.
     */
    public void setPhase(double phase) {
        this.phase = phase;
    }
    
	public static String getName() {return "Sinusoidal";}
}
