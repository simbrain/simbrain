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
package org.simbrain.world.odorworld;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.Utils;

/**
 * 
 * <b>Stimulus</b>
 */
public class Stimulus {
    
	private double peak = 0;
    
	public static final String STEP = "Step";
	public static final String LINEAR = "Linear";
	public static final String GAUSSIAN = "Gaussian";
	public static final String QUADRATIC = "Quadratic";
	
	/** vector of stimulus values associated to object */
	private double[] stimulusVector;

	
	/** Method for calcluating decay of stimulus as a function of distance from object */
	private String decayFunction = LINEAR;

	/** If outside of this radius the object has no affect on the network */
	private double stimulusDispersion = 70;

	/** If true, add noise to object's stimulus vector */
	private boolean addNoise = false;

	/** A value between 0 and 1 which describes how much noise is added */
	private double noiseLevel = .3;

	public static String[] decayFunctions = {Stimulus.STEP, Stimulus.LINEAR, Stimulus.GAUSSIAN, Stimulus.QUADRATIC};

	/**
	 * Construct a world entity (food or creature)
	 * 
	 * @param the_type kind of entity (mouse, flower, etc)
	 * @param x x location of new entity
	 * @param y y location of new entity
	 * @param vec "smell signature" associated with this entity. 
	 */
	public Stimulus(double[] distal_stim, String decay, double disp, boolean add_noise, double noise_level) {
		stimulusVector = distal_stim;
		decayFunction = decay;
		stimulusDispersion = disp;
		addNoise = add_noise;
		noiseLevel = noise_level;
	}
	
	public Stimulus(){
	    
	}
	

	public void randomize() {

		java.util.Random theRandNum = new java.util.Random();
		for (int i = 0; i < getStimulusDimension(); i++) {
			stimulusVector[i] = (theRandNum.nextInt(10));
		}

	}
	
	/////////////////////////
	// Getters and Setters //
	/////////////////////////
	public int getStimulusDimension() {
		if (stimulusVector == null) {
			return 0;
		}
		
		return stimulusVector.length;
	}
	
	public void setStimulusVector(double[] newStim) {
		stimulusVector = newStim;
	}

	public double[] getStimulusVector() {
		return stimulusVector;
	}
	
	public boolean isAddNoise() {
		return addNoise;
	}
	
	public double[] getStimulus() {
		return stimulusVector;
	}

	public String getStimulusS() {
		return Utils.getVectorString(stimulusVector, ",");
	}
	
	public void setStimulusS(String vectorString) {
		stimulusVector =  Utils.getVectorString(vectorString, ",");
	}

	public double getNoiseLevel() {
		return noiseLevel;
	}

	public void setAddNoise(boolean b) {
		addNoise = b;
	}

	public boolean getAddNoise() {
		return addNoise;
	}

	public void setDecayFunction(String decay) {
		decayFunction = decay;
	}

	public void setNoiseLevel(double d) {
		noiseLevel = d;
	}

	public void setDispersion(double d) {
		stimulusDispersion = d;
	}

	public double getDispersion() {
		return stimulusDispersion;
	}
	
	public String getDecayFunction() {
		return decayFunction;
	}
	
	/**
	 * Calculate what impact the object will have on the creature's receptors (input nodes)
	 * based on its distance from this object and its features (whether it is a "noisy object",
	 * and how the stimulus decays.  That is, calculate the proximal stimulus this distal
	 * stimulus gives rise to.
	 * 
	 * @param distance distance of creature from object
	 * @return proximal stimulus to creature caused by this object
	 */
	public double[] getStimulus(double distance) {

		double[] ret = SimbrainMath.zeroVector(getStimulusDimension());
		
		if (distance < stimulusDispersion) {
		
			if (decayFunction.equals(STEP)) {
				if (distance > peak) {
					ret = (double[]) (stimulusVector.clone());					
				}
			} else if (decayFunction.equals(LINEAR)) {
				if (distance < peak) {
					double scaling_factor = (stimulusDispersion - 2 * peak + distance)/(stimulusDispersion - peak);
					if (scaling_factor < 0) scaling_factor = 0;
					ret = SimbrainMath.multVector(stimulusVector, scaling_factor);					
				} else {
					double scaling_factor = (stimulusDispersion - distance)/(stimulusDispersion - peak);
					ret = SimbrainMath.multVector(stimulusVector, scaling_factor);		
				}
			} else if (decayFunction.equals(GAUSSIAN)) {
				distance -= peak;
				double sigma = .5 * (stimulusDispersion - peak);
				double scaling_factor =
					Math.exp(- (distance * distance) / (2 * sigma * sigma));
				ret = SimbrainMath.multVector(stimulusVector, scaling_factor);
			} else if (decayFunction.equals(QUADRATIC)) {
				double scaling_factor = 1 - Math.pow((distance - peak)/(stimulusDispersion - peak), 2);
				if (scaling_factor < 0) scaling_factor = 0;
				ret = SimbrainMath.multVector(stimulusVector, scaling_factor);
			}

			//Add noise to object vector
			if (addNoise == true) {
				SimbrainMath.addNoise(ret, noiseLevel);
			}
		}
		return ret;
	}

	/**
	 * Helper function for combo boxes
	 */	
	public int getDecayFunctionIndex(String df) {
		for (int i = 0; i < decayFunctions.length; i++) {
			if (df.equals(decayFunctions[i])) {
				return i;
			}
		}
		return 0;
	}
	
	public static String[] getDecayFunctions() {
		return decayFunctions;
	}
    /**
     * @return Returns the peak.
     */
    public double getPeak() {
        return peak;
    }
    /**
     * @param peak The peak to set.
     */
    public void setPeak(double peak) {
        this.peak = peak;
    }
}
