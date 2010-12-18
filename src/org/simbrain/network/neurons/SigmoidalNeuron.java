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

import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;
import org.simbrain.network.util.RandomSource;

/**
 * <b>SigmoidalNeuron</b> provides variuos implementations of a standard
 * sigmoidal neuron.
 */
public class SigmoidalNeuron implements NeuronUpdateRule, BiasedNeuron {

    /** Function list. */
    private static String[] functionList = {"Sigmoidal", "Arctan", "Barebones" };

    /** Implementation index. */
    private int implementationIndex = SIGM;

    /** Tanh. */
    public static final int TANH = 3;

    /** Tanh. */
    public static final int BARE = 2;

    /** Arctan. */
    public static final int ARCTAN = 1;

    /** Standard Sigmoidal. */
    public static final int SIGM = 0;

    /** Bias. */
    private double bias = 0;

    /** Slope. */
    private double slope = 1;

    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();

    /** Adds noise to neuron. */
    private boolean addNoise = false;

    /** Clipping. */
    private boolean clipping = false;

    /**
     * @{inheritDoc}
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * @{inheritDoc}
     */
    public String getName() {
        return "Sigmoidal";
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }

    /**
     * Update neuron.
     */
    public void update(Neuron neuron) {

        double val = neuron.getWeightedInputs() + bias;
        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();

        // TANH not currently used because identical to SIGM
        if (implementationIndex == TANH) {
            double a = (2 * slope) / (upperBound - lowerBound);
            val = (((upperBound - lowerBound) / 2) * tanh(a * val)) + ((upperBound  + lowerBound) / 2);
        } else if (implementationIndex == ARCTAN) {
            double a = (Math.PI * slope) / (upperBound - lowerBound);
            val = ((upperBound - lowerBound) / Math.PI) * Math.atan(a * val) + ((upperBound  + lowerBound) / 2);
        } else if (implementationIndex == SIGM) {
            double diff = upperBound - lowerBound;
            //val = diff * sigm(4 * slope * val / diff) + lowerBound;
            val = diff * sigm(slope * val / diff) + lowerBound;
        } else if (implementationIndex == BARE) {
            val = sigm(val);
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = neuron.clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * Returns the results of the hyperbolic tangent function.
     *
     * @param input argument
     * @return results of tanh
     */
    private double tanh(final double input) {
        double val = Math.exp(2 * input);
        return ((val - 1) / (val + 1));
    }

    /**
     * Returns the results of the standard sigmoidal function.
     *
     * @param in argument
     * @return results of sigm
     */
    private double sigm(final double in) {
        return  1 / (1 + Math.exp(-in));
    }

//    /**
//     * @return duplicate SigmoidalNeuron (used, e.g., in copy/paste).
//     */
//    public SigmoidalNeuron duplicate() {
//        SigmoidalNeuron sn = new SigmoidalNeuron();
//        sn = (SigmoidalNeuron) super.duplicate(sn);
//        sn.setBias(getBias());
//        sn.setClipping(getClipping());
//        sn.setImplementationIndex(getImplementationIndex());
//        sn.setSlope(getSlope());
//        sn.setAddNoise(getAddNoise());
//        sn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);
//
//        return sn;
//    }

    /**
     * @return Returns the inflectionPoint.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param inflectionY The inflectionY to set.
     */
    public void setBias(final double inflectionY) {
        this.bias = inflectionY;
    }

    /**
     * @return Returns the inflectionPointSlope.
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @param inflectionPointSlope The inflectionPointSlope to set.
     */
    public void setSlope(final double inflectionPointSlope) {
        this.slope = inflectionPointSlope;
    }

    /**
     * @return Returns the functionList.
     */
    public static String[] getFunctionList() {
        return functionList;
    }

    /**
     * @param index The impementatinIndex to set
     */
    public void setImplementationIndex(final int index) {
        this.implementationIndex = index;
    }

    /**
     * @return Returns the implementationIndex
     */
    public int getImplementationIndex() {
        return implementationIndex;
    }

    /**
     * @return Returns the noise.
     */
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise to set.
     */
    public void setNoiseGenerator(final RandomSource noise) {
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
    public void setAddNoise(final boolean addNoise) {
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
    public void setClipping(final boolean clipping) {
        this.clipping = clipping;
    }
}
