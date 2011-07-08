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
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

/**
 * <b>Stimulus</b> represent a distal stimulus in the form of a vector. It can
 * have noise added and has built in functions to compute decay.
 *
 * Standardly these objects represent "distal stimuli" relative to an organism.
 */
public class SmellSource {

    /** Vector of base stimulus values associated to object. */
    private double[] stimulusVector;

    /** The vector returned.  Base stimulus vector + noise, if any. */
    private double[] returnVector;

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
     * Construct smell source from specified parameters.
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
     * Construct smell source from specified parameters.
     *
     * @param distalstim  stimulus vector
     * @param decay decay function
     * @param dispersion level of dispersion
     * @param location location of smell source
     */
    public SmellSource(final double[] distalstim, final DecayFunction decay,
            final double dispersion, final double[] location) {
        this.stimulusVector = distalstim;
        this.decayFunction = decay;
        this.stimulusDispersion = dispersion;
        this.location = location;
    }

    /**
     * Construct smell source from specified parameters.
     *
     * @param distalstim  stimulus vector
     * @param decay decay function
     * @param location location of smell source
     */
    public SmellSource(final double[] distalstim, final DecayFunction decay, final double[] location) {
        this.stimulusVector = distalstim;
        this.decayFunction = decay;
        this.location = location;
    }

    /**
     * Construct a smell source from a specified stimulus vector, using defaults
     * otherwise.
     *
     * @param distalstim the stimulus vector
     */
    public SmellSource(final double[] distalstim) {
        this.stimulusVector = distalstim;
    }

    /**
     * Construct a smell source with a specified number of dimensions, randomly
     * initialized.
     *
     * @param numDimensions
     *            number of dimensions of the stimulus vector.
     */
    public SmellSource(final int numDimensions) {
        this.stimulusVector = SimbrainMath.randomVector(numDimensions);
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
        if (returnVector == null) {
            returnVector = stimulusVector;
        }
        if (distance < stimulusDispersion) {
            if (decayFunction == DecayFunction.STEP) {
                if (distance >= peak) {
                    ret = (double[]) (returnVector.clone());
                }
            } else if (decayFunction == DecayFunction.LINEAR) {
                if (distance < peak) {
                    double scalingFactor = (stimulusDispersion - (2 * peak) + distance) / (stimulusDispersion - peak);

                    if (scalingFactor < 0) {
                        scalingFactor = 0;
                    }

                    ret = SimbrainMath.multVector(returnVector, scalingFactor);
                } else {
                    double scalingFactor = (stimulusDispersion - distance) / (stimulusDispersion - peak);
                    ret = SimbrainMath.multVector(returnVector, scalingFactor);
                }
            } else if (decayFunction == DecayFunction.GAUSSIAN) {
                double temp = distance;
                temp -= peak;
                double sigma = .5 * (stimulusDispersion - peak);
                double scalingFactor = Math.exp(-(temp * temp) / (2 * sigma * sigma));
                ret = SimbrainMath.multVector(returnVector, scalingFactor);
            } else if (decayFunction == DecayFunction.QUADRATIC) {
                double scalingFactor = 1 - Math.pow((distance - peak) / (stimulusDispersion - peak), 2);
                if (scalingFactor < 0) {
                    scalingFactor = 0;
                }
                ret = SimbrainMath.multVector(returnVector, scalingFactor);
            }
        }

        return ret;
    }

    /**
     * Update the source.
     */
    public void update() {
        //Add noise to object vector
        if (addNoise) {
            returnVector = SimbrainMath.getNoisyVector(stimulusVector, noiseLevel);
        } else {
            returnVector = stimulusVector;
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
        double val = returnVector[dimension];

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
     * @param decayFunction
     *            the decayFunction to set
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
     * Return location of this smell source.
     *
     * @param location the location to set
     */
    public void getLocation(final double[] location) {
        this.location = location;
    }

    /**
     * Set the location of this smell source.
     *
     * @param location
     */
    public void setLocation(final double[] location) {
        this.location = location;
    }

    /**
     * @return the imageBox
     */
    public ComboBoxWrapper getTheDecayFunction() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                return getDecayFunction();
            }

            public Object[] getObjects() {
                return DecayFunction.values();
            }
        };
    }

    /**
     * @param imageBox the imageBox to set
     */
    public void setTheDecayFunction(ComboBoxWrapper decayFunctionBox) {
        setDecayFunction((DecayFunction) decayFunctionBox.getCurrentObject());
    }

}
