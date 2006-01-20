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


/**
 * <b>SigmoidalNeuron</b>.
 */
public class SigmoidalNeuron extends Neuron {
    /** Function list. */
    private static String[] functionList = {"Tanh", "Arctan" };
    /** Implementation index. */
    private int implementationIndex = 1;
    /** Tanh. */
    public static int TANH = 0;
    /** Arctan. */
    public static int ARCTAN = 1;
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
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public SigmoidalNeuron() {
    }

    /**
     * @return Time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.Network.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to be made of type.
     */
    public SigmoidalNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Update neuron.
     */
    public void update() {
        double val = this.weightedInputs() + bias;

        if (implementationIndex == TANH) {
            double A = (2 * slope) / (upperBound - lowerBound);
            val = (((upperBound - lowerBound) / 2) * tanh(A * val)) + ((upperBound  + lowerBound) / 2);
        } else if (implementationIndex == ARCTAN) {
            double A = (Math.PI * slope) / (upperBound - lowerBound);
            val = ((upperBound - lowerBound) / Math.PI) * Math.atan(A * val) + ((upperBound  + lowerBound) / 2);
        }

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = clip(val);
        }

        setBuffer(val);
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
     * @return duplicate SigmoidalNeuron (used, e.g., in copy/paste).
     */
    public Neuron duplicate() {
        SigmoidalNeuron sn = new SigmoidalNeuron();
        sn = (SigmoidalNeuron) super.duplicate(sn);
        sn.setBias(getBias());
        sn.setClipping(getClipping());
        sn.setImplementationIndex(getImplementationIndex());
        sn.setSlope(getSlope());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);

        return sn;
    }

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
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Sigmoidal";
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
