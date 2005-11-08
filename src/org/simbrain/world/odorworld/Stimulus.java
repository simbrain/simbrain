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
 * <b>Stimulus</b>.
 */
public class Stimulus {
    private double peak = 0;
    public static final String STEP = "Step";
    public static final String LINEAR = "Linear";
    public static final String GAUSSIAN = "Gaussian";
    public static final String QUADRATIC = "Quadratic";

    /** vector of stimulus values associated to object. */
    private double[] stimulusVector;

    /** Method for calcluating decay of stimulus as a function of distance from object. */
    private String decayFunction = LINEAR;

    private final double initDis = 70;
    /** If outside of this radius the object has no affect on the network. */
    private double stimulusDispersion = initDis;

    /** If true, add noise to object's stimulus vector. */
    private boolean addNoise = false;

    private final double initNoise = .3;
    /** A value between 0 and 1 which describes how much noise is added. */
    private double noiseLevel = initNoise;
    public static final String[] DECAYFUNCTIONS =
        {Stimulus.STEP, Stimulus.LINEAR, Stimulus.GAUSSIAN, Stimulus.QUADRATIC };

    public Stimulus(final double[] distalstim, final String decay,
            final double disp, final boolean addNoise, final double noiseLevel) {
        stimulusVector = distalstim;
        decayFunction = decay;
        stimulusDispersion = disp;
        this.addNoise = addNoise;
        this.noiseLevel = noiseLevel;
    }

    public Stimulus() {
    }

    public void randomize() {
        java.util.Random theRandNum = new java.util.Random();
        final int ten = 10;

        for (int i = 0; i < getStimulusDimension(); i++) {
            stimulusVector[i] = (theRandNum.nextInt(ten));
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

    public void setStimulusVector(final double[] newStim) {
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

    public void setStimulusS(final String vectorString) {
        stimulusVector = Utils.getVectorString(vectorString, ",");
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public void setAddNoise(final boolean b) {
        addNoise = b;
    }

    public boolean getAddNoise() {
        return addNoise;
    }

    public void setDecayFunction(final String decay) {
        decayFunction = decay;
    }

    public void setNoiseLevel(final double d) {
        noiseLevel = d;
    }

    public void setDispersion(final double d) {
        stimulusDispersion = d;
    }

    public double getDispersion() {
        return stimulusDispersion;
    }

    public String getDecayFunction() {
        return decayFunction;
    }

    /**
     * Calculate what impact the object will have on the creature's receptors (input nodes) based on its distance from
     * this object and its features (whether it is a "noisy object", and how the stimulus decays.  That is, calculate
     * the proximal stimulus this distal stimulus gives rise to.
     *
     * @param distance distance of creature from object
     *
     * @return proximal stimulus to creature caused by this object
     */
    public double[] getStimulus(final double distance) {
        double[] ret = SimbrainMath.zeroVector(getStimulusDimension());

        if (distance < stimulusDispersion) {
            if (decayFunction.equals(STEP)) {
                if (distance > peak) {
                    ret = (double[]) (stimulusVector.clone());
                }
            } else if (decayFunction.equals(LINEAR)) {
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
            } else if (decayFunction.equals(GAUSSIAN)) {
                double temp = distance;
                temp -= peak;

                final double half = .5;

                double sigma = half * (stimulusDispersion - peak);
                double scalingFactor = Math.exp(-(temp * temp) / (2 * sigma * sigma));
                ret = SimbrainMath.multVector(stimulusVector, scalingFactor);
            } else if (decayFunction.equals(QUADRATIC)) {
                double scalingFactor = 1 - Math.pow((distance - peak) / (stimulusDispersion - peak), 2);

                if (scalingFactor < 0) {
                    scalingFactor = 0;
                }

                ret = SimbrainMath.multVector(stimulusVector, scalingFactor);
            }

            //Add noise to object vector
            if (addNoise) {
                SimbrainMath.addNoise(ret, noiseLevel);
            }
        }

        return ret;
    }

    /**
     * Helper function for combo boxes.
     * @param df the decayfunction index to return
     * @return the int associated with df
     */
    public int getDecayFunctionIndex(final String df) {
        for (int i = 0; i < DECAYFUNCTIONS.length; i++) {
            if (df.equals(DECAYFUNCTIONS[i])) {
                return i;
            }
        }

        return 0;
    }

    public static String[] getDecayFunctions() {
        return DECAYFUNCTIONS;
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
}
