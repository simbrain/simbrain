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


public class SinusoidalNeuron extends Neuron {
    
    private double phase = 1;
    private double frequency = .1;
    private double bias = 0;
	private boolean clipping = false;
	private RandomSource noiseGenerator = new RandomSource();
	private boolean addNoise = false;

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
        SinusoidalNeuron sn = new SinusoidalNeuron();
        sn = (SinusoidalNeuron)super.duplicate(sn);
        sn.setPhase(getPhase());
        sn.setBias(getBias());
        sn.setFrequency(getFrequency());
        sn.setClipping(getClipping());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
        return sn;
	}
	
	public void update() {

		double range = upperBound - lowerBound;
		double val = range * Math.sin(this.getParentNetwork().getTime() + phase) + range/2 ;

		if(addNoise == true) {
			val += noiseGenerator.getRandom();
		}
		if (clipping == true) {
			val = clip(val);
		}
		
		setBuffer(val);

	}


	public RandomSource getNoiseGenerator() {
		return noiseGenerator;
	}
	/**
	 * @param noise The noise to set.
	 */
	public void setNoiseGenerator(RandomSource noise) {
		this.noiseGenerator = noise;
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
	/**
	 * @return Returns the frequency.
	 */
	public double getFrequency() {
		return frequency;
	}
	/**
	 * @param frequency The frequency to set.
	 */
	public void setFrequency(double frequency) {
		this.frequency = frequency;
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
}
