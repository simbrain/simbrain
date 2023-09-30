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

import org.simbrain.util.UserParameter;
import org.simbrain.util.decayfunctions.DecayFunction;
import org.simbrain.util.decayfunctions.LinearDecayFunction;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * <b>Stimulus</b> represent a distal stimulus in the form of a vector. It can
 * have noise added and has built in functions to compute decay.
 * <p>
 * Standardly these objects represent "distal stimuli" relative to an organism.
 */
public class SmellSource implements EditableObject {

    /**
     * Vector of base stimulus values associated to object.
     */
    @UserParameter(
            label = "Stimulus vector",
            description = "Values associated with this smell",
            order = 1)
    private double[] stimulusVector;

    /**
     * Method for calculating decay of stimulus as a function of distance from
     * object.
     */
    @UserParameter(
            label = "Decay function",
            description = "Way of decaying the stimulus",
            showDetails = false,
            order= 5
    )
    private DecayFunction decayFunction = new LinearDecayFunction(70);

    /**
     * If true, add noise to object's stimulus vector.
     */
    @UserParameter(
            label = "Add noise",
            description = "If true, add noise to object's stimulus vector.",
            order = 10
    )
    private boolean addNoise = false;

    /**
     * Noise generator for this decay function if [DecayFunction.addNoise] is true.
     */
    @UserParameter(
            label = "Randomizer",
            showDetails = false,
            conditionalEnablingMethod = "useRandomWinner",
            order = 15)
    private ProbabilityDistribution randomizer  = new UniformRealDistribution();

    public SmellSource(final double[] distalstim) {
        this.stimulusVector = distalstim;
    }

    public SmellSource(final int numDimensions) {
        this.stimulusVector = new double[numDimensions];
        for (int i = 0; i < numDimensions; i++) {
            stimulusVector[i] = Math.random();
        }
    }

    public static SmellSource createScalarSource(final double val) {
        return new SmellSource(new double[] {val});
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
        var scalingFactor = decayFunction.getScalingFactor(distance);
        return Arrays.stream(stimulusVector)
                .map(s -> s * scalingFactor + getNoise())
                .toArray();
    }

    private Double getNoise() {
        if (addNoise) {
            return randomizer.sampleDouble();
        } else {
            return 0.0;
        }
    }

    /**
     * Randomize values.
     */
    public void randomize() {
        for (int i = 0; i < getStimulusDimension(); i++) {
            stimulusVector[i] = randomizer.sampleDouble();
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

    public void setDispersion(final double d) {
        decayFunction.setDispersion(d);
    }

    public double getDispersion() {
        return decayFunction.getDispersion();
    }

    public DecayFunction getDecayFunction() {
        return decayFunction;
    }

    public void setDecayFunction(DecayFunction decayFunction) {
        this.decayFunction = decayFunction;
    }

    public double[] getStimulusVector() {
        return stimulusVector;
    }

    /**
     * Called by reflection via {@link UserParameter#conditionalEnablingMethod()}
     */
    public Function<Map<String, Object>, Boolean> useRandomWinner() {
        return (map) -> (Boolean) map.get("Add noise");
    }

}
