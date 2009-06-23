/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.util.environment;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.Utils;

/**
 * <b>Stimulus</b> represent a distal stimulus in the form of a vector.  It can have noise added and 
 * has built in functions to compute decay. 
 * 
 * Standardly these objects represent "distal stimuli" relative to an organism.
 *  
 */
public class SmellSource {

	/** Vector of stimulus values associated to object. */
    private double[] stimulusVector;

    /** Location of the distal stimulus. */
    private double[] location;

    /** Decay Functions. */
    public enum DecayFunction {

    	STEP("Step"), LINEAR("Linear"), GAUSSIAN("Gaussian"), QUADRATIC(
                "Quadratic");

        /** Name of decay function. */
        private String name;

        /**
         * Constructor.
         * 
         * @param name name.
         */
        DecayFunction(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    };

    /** Method for calculating decay of stimulus as a function of distance from object. */
    private DecayFunction decayFunction = DecayFunction.LINEAR;

    /** If outside of this radius the object has no affect on the network. */
    private double stimulusDispersion = 70;

    /** Peak value. */
    private double peak = 0;

    /** If true, add noise to object's stimulus vector. */
    private boolean addNoise = false;

    /** Initial noise. */
    private final double initNoise = .3;

    /** A value between 0 and 1 which describes how much noise is added. */
    private double noiseLevel = initNoise;

    /**
     * Instance of stimulus initializing values to be set.
     *
     * @param distalstim Distal stimulus
     * @param decay Decay rate
     * @param disp Dispersion
     * @param addNoise Add noise
     * @param noiseLevel Level of noise
     */
    public SmellSource(final double[] distalstim, final DecayFunction decay,
            final double disp, final boolean addNoise, final double noiseLevel) {
    	this.stimulusVector = distalstim;
        this.decayFunction = decay;
        this.stimulusDispersion = disp;
        this.addNoise = addNoise;
        this.noiseLevel = noiseLevel;
    }
    
    /**
     * Instance of stimulus initializing values to be set.
     *
     * @param distalstim Distal stimulus
     */
    public SmellSource(final double[] distalstim, final DecayFunction decay, final double[] location) {
        this.stimulusVector = distalstim;
        this.decayFunction = decay;
        this.location = location;
    }

    /**
     * Default constructor.
     */
    public SmellSource() {
    }

    /**
     * Randomize values.
     */
    public void randomize() {
        java.util.Random theRandNum = new java.util.Random();
        final int ten = 10;

        for (int i = 0; i < getStimulusDimension(); i++) {
            stimulusVector[i] = (theRandNum.nextInt(ten));
        }
    }

    /**
     * Return the number of dimensions in the stimulus vector.
     *
     * @return the dimension of the stimulus
     */
    public int getStimulusDimension() {
        if (stimulusVector == null) {
            return 0;
        }

        return stimulusVector.length;
    }

    /**
     * Sets the stimulus vector.
     *
     * @param newStim New stimulus
     */
    public void setStimulusVector(final double[] newStim) {
        stimulusVector = newStim;
    }

    /**
     * Return the stimulus vector.
     *
     * @return the stimulus vector
     */
    public double[] getStimulusVector() {
        return stimulusVector;
    }

    /**
     * Return add noise.
     *
     * @return add noise
     */
    public boolean isAddNoise() {
        return addNoise;
    }

    /**
     * Return the stimulus.
     *
     * @return the stimulus
     */
    public double[] getStimulus() {
        return stimulusVector;
    }

    /**
     * Return the stimulus string.
     *
     * @return the stimulus string
     */
    public String getStimulusString() {
        return Utils.getVectorString(stimulusVector, ",");
    }

    /**
     * Sets the stimulus string.
     *
     * @param vectorString Stimulus string
     */
    public void setStimulusS(final String vectorString) {
        stimulusVector = Utils.getVectorString(vectorString, ",");
    }

    /**
     * Return the noise level.
     *
     * @return the noise level
     */
    public double getNoiseLevel() {
        return noiseLevel;
    }

    /**
     * Sets the add noise.
     *
     * @param b the add noise
     */
    public void setAddNoise(final boolean b) {
        addNoise = b;
    }

    /**
     * Return the add noise.
     *
     * @return the add noise
     */
    public boolean getAddNoise() {
        return addNoise;
    }

    /**
     * Sets the noise level.
     *
     * @param d Noise level
     */
    public void setNoiseLevel(final double d) {
        noiseLevel = d;
    }

    /**
     * Sets the dispersion.
     *
     * @param d Dispersion
     */
    public void setDispersion(final double d) {
        stimulusDispersion = d;
    }

    /**
     * Return the dispersion.
     *
     * @return the dispersion
     */
    public double getDispersion() {
        return stimulusDispersion;
    }

    /**
     * Calculate what impact the object will have on the creature's receptors
     * (input nodes) based on its distance from this object and its features
     * (whether it is a "noisy object", and how the stimulus decays). That is,
     * calculate the proximal stimulus this distal stimulus gives rise to.
     * 
     * @param distance distance of creature from object
     * @return proximal stimulus to creature caused by this object
     */
    public double[] getStimulus(final double distance) {
        double[] ret = SimbrainMath.zeroVector(getStimulusDimension());

        if (distance < stimulusDispersion) {
            if (decayFunction == DecayFunction.STEP) {
                if (distance > peak) {
                    ret = (double[]) (stimulusVector.clone());
                }
            } else if (decayFunction == DecayFunction.LINEAR) {
                if (distance < peak) {
                    double scalingFactor = (stimulusDispersion - (2 * peak) + distance) / (stimulusDispersion - peak);

                    if (scalingFactor < 0) {
                        scalingFactor = 0;
                    }

                    ret = SimbrainMath.multVector(stimulusVector, scalingFactor);
                } else {
                    double scalingFactor = (stimulusDispersion - distance) / (stimulusDispersion - peak);
                    ret = SimbrainMath.multVector(stimulusVector, scalingFactor);
                }
            } else if (decayFunction == DecayFunction.GAUSSIAN) {
                double temp = distance;
                temp -= peak;

                double sigma = .5 * (stimulusDispersion - peak);
                double scalingFactor = Math.exp(-(temp * temp) / (2 * sigma * sigma));
                ret = SimbrainMath.multVector(stimulusVector, scalingFactor);
            } else if (decayFunction == DecayFunction.QUADRATIC) {
                double scalingFactor = 1 - Math.pow((distance - peak) / (stimulusDispersion - peak), 2);

                if (scalingFactor < 0) {
                    scalingFactor = 0;
                }

                ret = SimbrainMath.multVector(stimulusVector, scalingFactor);
            }
        }

        return ret;
    }
    
    public void update() {
        //Add noise to object vector
        if (addNoise) {
            SimbrainMath.addNoise(stimulusVector, noiseLevel);
        }
    }
    
    /**
     * Calculate what impact the object will have on the creature's receptors
     * (input nodes) based on its distance from this object and its features
     * (whether it is a "noisy object", and how the stimulus decays). That is,
     * calculate the proximal stimulus this distal stimulus gives rise to.
     * 
     * @param distance distance of creature from object
     * @return proximal stimulus to creature caused by this object
     */
    public double getStimulus(final int dimension, final double distance) {
        double ret = 0;
        double val = stimulusVector[dimension];
        
        if (distance < stimulusDispersion) {
            if (decayFunction == DecayFunction.STEP) {
                if (distance > peak) {
                    ret = val;
                }
            } else if (decayFunction == DecayFunction.LINEAR) {
                if (distance < peak) {
                    double scalingFactor = (stimulusDispersion - (2 * peak) + distance)
                            / (stimulusDispersion - peak);
                    if (scalingFactor < 0) {
                        scalingFactor = 0;
                    }
                    ret = val * scalingFactor;
                } else {
                    double scalingFactor = (stimulusDispersion - distance)
                            / (stimulusDispersion - peak);
                    ret = val * scalingFactor;
                }
            } else if (decayFunction == DecayFunction.GAUSSIAN) {
                double temp = distance;
                temp -= peak;

                double sigma = .5 * (stimulusDispersion - peak);
                double scalingFactor = Math.exp(-(temp * temp) / (2 * sigma * sigma));
                ret = val * scalingFactor;
            } else if (decayFunction == DecayFunction.QUADRATIC) {
                double scalingFactor = 1 - Math.pow((distance - peak)
                        / (stimulusDispersion - peak), 2);

                if (scalingFactor < 0) {
                    scalingFactor = 0;
                }
                ret = val * scalingFactor;
            }
        }
        return ret;
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
    public void setPeak(final double peak) {
        this.peak = peak;
    }

	/**
	 * @return the decayFunction
	 */
	public DecayFunction getDecayFunction() {
		return decayFunction;
	}

	/**
	 * @param decayFunction the decayFunction to set
	 */
	public void setDecayFunction(DecayFunction decayFunction) {
		this.decayFunction = decayFunction;
	}

	/**
	 * @return the location
	 */
	public double[] getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void getLocation(double[] location) {
		this.location = location;
	}

}
