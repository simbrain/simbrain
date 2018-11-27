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

import org.simbrain.util.Utils;
import org.simbrain.util.math.DecayFunction;
import org.simbrain.util.math.DecayFunctions.LinearDecayFunction;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

/**
 * <b>Stimulus</b> represent a distal stimulus in the form of a vector. It can
 * have noise added and has built in functions to compute decay.
 * <p>
 * Standardly these objects represent "distal stimuli" relative to an organism.
 */
public class SmellSource {

    // TODO: Consider using lists instead of arrays to make it easier to
    // dynamically resize
    //TOOD: Possibly give this a location.  Then can make a method getStimulus(sensorLocation)
    // but currently no use cases where a smell location is used.

    /**
     * Vector of base stimulus values associated to object.
     */
    private double[] stimulusVector;

    /**
     * The vector returned. Base stimulus vector + noise, if any.
     */
    private double[] returnVector;

    /**
     * Method for calculating decay of stimulus as a function of distance from
     * object.
     */
    private DecayFunction decayFunction = LinearDecayFunction.create();

    /**
     * Construct smell source from specified parameters.
     *
     * @param distalstim stimulus vector
     * @param decay      decay function
     * @param dispersion level of dispersion
     * Decay Functions.
     */
    public SmellSource(double[] distalstim, DecayFunction decay, double dispersion) {
        this(distalstim, decay);
        decayFunction.setDispersion(dispersion);
    }

    /**
     * Construct smell source from specified parameters.
     *
     * @param distalstim stimulus vector
     * @param decay      decay function
     */
    public SmellSource(final double[] distalstim, final DecayFunction decay) {
        this(distalstim);
        this.decayFunction = decay;
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
     * @param numDimensions number of dimensions of the stimulus vector.
     */
    public SmellSource(final int numDimensions) {
        UniformDistribution randomizer = UniformDistribution.create();
        this.stimulusVector = new double[numDimensions];
        for (int i = 0; i < numDimensions; i++) {
            stimulusVector[i] = randomizer.nextRand();
        }
    }

    /**
     * Default constructor.
     */
    public SmellSource() {
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
        double[] ret;
        if (returnVector == null) {
            returnVector = stimulusVector;
        }
        ret = decayFunction.apply(distance, returnVector);

        return ret;
    }

    /**
     * Update the source.
     */
    public void update() {
        returnVector = stimulusVector;
    }

    /**
     * Randomize values.
     */
    public void randomize() {
        UniformDistribution randomizer =
                UniformDistribution.builder()
                .lowerBound(0)
                .upperBound(10)
                .build();

        for (int i = 0; i < getStimulusDimension(); i++) {
            stimulusVector[i] = randomizer.nextRand();
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
        return decayFunction.getAddNoise();
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
     * Sets the add noise.
     *
     * @param addNoise the add noise
     */
    public void setAddNoise(final boolean addNoise) {
        decayFunction.setAddNoise(addNoise);
    }

    /**
     * Sets the dispersion.
     *
     * @param d Dispersion
     */
    public void setDispersion(final double d) {
        decayFunction.setDispersion(d);
    }

    /**
     * Return the dispersion.
     *
     * @return the dispersion
     */
    public double getDispersion() {
        return decayFunction.getDispersion();
    }

    /**
     * @return Returns the peak.
     */
    public double getPeak() {
        return decayFunction.getPeakDistance();
    }

    /**
     * @param peak The peak to set.
     */
    public void setPeak(final double peak) {
        decayFunction.setPeakDistance(peak);
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

    // /**
    //  * @return the imageBox
    //  */
    // public ComboBoxWrapper getTheDecayFunction() {
    //     return new ComboBoxWrapper() {
    //         public Object getCurrentObject() {
    //             return getDecayFunction();
    //         }
    //
    //         public Object[] getObjects() {
    //             return DecayFunction.values();
    //         }
    //     };
    // }

    /**
     * @param decayFunctionBox
     */
    public void setTheDecayFunction(ComboBoxWrapper decayFunctionBox) {
        setDecayFunction((DecayFunction) decayFunctionBox.getCurrentObject());
    }

}
