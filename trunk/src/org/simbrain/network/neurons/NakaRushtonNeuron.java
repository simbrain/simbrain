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
package org.simbrain.network.neurons;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.util.RandomSource;


/**
 * <b>NakaRushtonNeuron</b> is a firing-rate based neuron which is intended to model spike rates of real
 * neurons.  It is used extensively in Hugh Wilson's Spikes, Decisions, and Action.
 */
public class NakaRushtonNeuron extends Neuron {

    /** Steepness. */
    private double steepness = 2;

    /** Semi saturation constant. */
    private double semiSaturationConstant = 120;

    /** Time constant of spike rate adaptation. */
    private double adaptationTimeConstant = 1;

    /** Parameter of spike rate adaptation. */
    private double adaptationParameter = .7;

    /** Whether to use spike rate adaptation or not. */
    private boolean useAdaptation = false;

    /** Time constant. */
    private double timeConstant = .1;

    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();

    /** Add noise to neuron. */
    private boolean addNoise = false;

    /** Local variable. */
    private double s = 0;

    /** Local variable. */
    private double a = 0;

    /**
     * Default constructor.
     */
    public NakaRushtonNeuron() {
        postUnmarshallingInit();
    }

    /**
     * @return Time type.
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.CONTINUOUS;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to be created
     */
    public NakaRushtonNeuron(final Neuron n) {
        super(n);
        postUnmarshallingInit();
    }

    /**
     * Initializes values for Naka Rushton neuron type.
     */
    public void postUnmarshallingInit() {
        lowerBound = 0;
        upperBound = 100;
    }

    /**
     * @return duplicate NakaRushtonNeuron (used, e.g., in copy/paste).
     */
    public NakaRushtonNeuron duplicate() {
        NakaRushtonNeuron rn = new NakaRushtonNeuron();
        rn = (NakaRushtonNeuron) super.duplicate(rn);
        rn.setSteepness(getSteepness());
        rn.setSemiSaturationConstant(getSemiSaturationConstant());
        rn.setAddNoise(getAddNoise());
        rn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
        rn.setUseAdaptation(getUseAdaptation());
        rn.setAdaptationParameter(getAdaptationParameter());
        rn.setAdaptationTimeConstant(getAdaptationTimeConstant());
        return rn;
    }

    /**
     * See Spikes (Hugh Wilson), pp. 20-21
     */
    public void update() {
        double p = getWeightedInputs();
        double val = getActivation();

        // Update adaptation term; see Spike, p. 81
        if (useAdaptation) {
            a += (this.getParentNetwork().getTimeStep() / adaptationTimeConstant) * (adaptationParameter * val - a);
        } else {
            a = 0;
        }

        if (p > 0) {
            s = (upperBound * Math.pow(p, steepness)) / (Math.pow(semiSaturationConstant + a, steepness)
                                + Math.pow(p, steepness));
        } else {
            s = 0;
        }


        if (addNoise) {
            val += (this.getParentNetwork().getTimeStep() * (((1 / timeConstant) * (-val + s))
            + noiseGenerator.getRandom()));
        } else {
            val += (this.getParentNetwork().getTimeStep() * ((1 / timeConstant) * (-val + s)));
        }

        setBuffer(val);
    }

    /**
     * @return Returns the semiSaturationConstant.
     */
    public double getSemiSaturationConstant() {
        return semiSaturationConstant;
    }

    /**
     * @param semiSaturationConstant The semiSaturationConstant to set.
     */
    public void setSemiSaturationConstant(final double semiSaturationConstant) {
        this.semiSaturationConstant = semiSaturationConstant;
    }

    /**
     * @return Returns the steepness.
     */
    public double getSteepness() {
        return steepness;
    }

    /**
     * @param steepness The steepness to set.
     */
    public void setSteepness(final double steepness) {
        this.steepness = steepness;
    }

    /**
     * @return Returns the timeConstant.
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param timeConstant The timeConstant to set.
     */
    public void setTimeConstant(final double timeConstant) {
        this.timeConstant = timeConstant;
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Naka-Rushton";
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
    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
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
    public void setNoiseGenerator(final RandomSource noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }

    /**
     * @return the boolean value.
     */
    public boolean getUseAdaptation() {
        return useAdaptation;
    }

    /**
     * Sets the boolean use adaptation value.
     *
     * @param useAdaptation Value to set use adaptation to
     */
    public void setUseAdaptation(final boolean useAdaptation) {
        this.useAdaptation = useAdaptation;
    }

    /**
     * @return the adaptation time constant.
     */
    public double getAdaptationTimeConstant() {
        return adaptationTimeConstant;
    }

    /**
     * Sets the adaptation time constant.
     *
     * @param adaptationTimeConstant Value to set adaptation time constant
     */
    public void setAdaptationTimeConstant(final double adaptationTimeConstant) {
        this.adaptationTimeConstant = adaptationTimeConstant;
    }

    /** @see Neuron */
    public void clear() {
        activation = 0;
        a = 0;
        s = 0;
    }

    /** @see Neuron */
    public String getToolTipText() {
        if (useAdaptation) {
            return "" + this.getActivation() + " A = " + a;
        } else {
            return super.getToolTipText();
        }
    }

    /**
     * Return the adaptation parameter.
     *
     * @return the adaptation parameter
     */
    public double getAdaptationParameter() {
        return adaptationParameter;
    }

    /**
     * Sets the adaptation parameter.
     *
     * @param adaptationParameter value to set
     */
    public void setAdaptationParameter(final double adaptationParameter) {
        this.adaptationParameter = adaptationParameter;
    }
}
