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
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.spikeresponders.Step;
import org.simnet.util.RandomSource;
import org.simnet.util.SMath;


public class IntegrateAndFireNeuron extends Neuron implements SpikingNeuron {
    
	private boolean hasSpiked = false;
	
    private double resistance = 1;
    private double time_constant = 1;
    private double threshold = 2;
    private double resetPotential = .1;
    private double restingPotential = .5;
    private double timeStep = .1;
	private RandomSource noiseGenerator = new RandomSource();
    private boolean addNoise = false;
    private boolean clipping = false;
    
	/**
	 * Default constructor needed for external calls which create neurons then 
	 * set their parameters
	 */
	public IntegrateAndFireNeuron() {
	}
	
	/**
	 *  This constructor is used when creating a neuron of one type from another neuron of another type
	 *  Only values common to different types of neuron are copied
	 */
	public IntegrateAndFireNeuron(Neuron n) {
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
		
	
		double val = getActivation()  + timeStep/time_constant * (restingPotential - getActivation() + resistance * weightedInputs());  	
		
		if (val > threshold) {
			hasSpiked = true;
			val = resetPotential;	
		} else {
			hasSpiked = false;
		}
		
		if(addNoise == true) {
			val += noiseGenerator.getRandom();
		}
		if (clipping == true) {
			val = clip(val);
		}
		
		setBuffer(val);
	}

    /**
     * @return Returns the firingProbability.
     */
    public double getTimeStep() {
        return timeStep;
    }
    /**
     * @param firingProbability The firingProbability to set.
     */
    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }
    /**
     * @return Returns the lowerValue.
     */
    public double getRestingPotential() {
        return restingPotential;
    }
    /**
     * @param lowerValue The lowerValue to set.
     */
    public void setRestingPotential(double restingPotential) {
        this.restingPotential = restingPotential;
    }
    /**
     * @return Returns the upperValue.
     */
    public double getResistance() {
        return resistance;
    }
    /**
     * @param upperValue The upperValue to set.
     */
    public void setResistance(double resistance) {
        this.resistance = resistance;
    }
    
	public static String getName() {return "Integrate and fire";}
    /**
     * @return Returns the lowerValue.
     */
    public boolean isAddNoise() {
        return addNoise;
    }
    /**
     * @param lowerValue The lowerValue to set.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

	/**
	 * @param noise The noise to set.
	 */
	public void setAddNoise(RandomSource noise) {
		this.noiseGenerator = noise;
	}
	/**
	 * @return Returns the clipping.
	 */
	public boolean isClipping() {
		return clipping;
	}
	/**
	 * @param clipping The clipping to set.
	 */
	public void setClipping(boolean clipping) {
		this.clipping = clipping;
	}
	/**
	 * @return Returns the noiseGenerator.
	 */
	public RandomSource getNoiseGenerator() {
		return noiseGenerator;
	}
	/**
	 * @param noiseGenerator The noiseGenerator to set.
	 */
	public void setNoiseGenerator(RandomSource noiseGenerator) {
		this.noiseGenerator = noiseGenerator;
	}
	/**
	 * @return Returns the resetPotential.
	 */
	public double getResetPotential() {
		return resetPotential;
	}
	/**
	 * @param resetPotential The resetPotential to set.
	 */
	public void setResetPotential(double resetPotential) {
		this.resetPotential = resetPotential;
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
	 * @return Returns the time_constant.
	 */
	public double getTime_constant() {
		return time_constant;
	}
	/**
	 * @param time_constant The time_constant to set.
	 */
	public void setTime_constant(double time_constant) {
		this.time_constant = time_constant;
	}
	/**
	 * @return Returns the hasSpiked.
	 */
	public boolean hasSpiked() {
		return hasSpiked;
	}
}

