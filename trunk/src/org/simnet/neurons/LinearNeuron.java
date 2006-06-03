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
 * <b>LinearNeuron</b>.
 */
public class LinearNeuron extends Neuron {
    /** Slope. */
    private double slope = 1;
    /** Bias. */
    private double bias = 0;
    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();
    /** Add noise to the neuron. */
    private boolean addNoise = false;
    /** Clipping. */
    private boolean clipping = true;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public LinearNeuron() {
    }

    /** @see Neuron */
    public LinearNeuron(final double x, final double y) {
        super(x, y);
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
     * @param n Neuron to be made of type linear
     */
    public LinearNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return duplicate LinearNeuron (used, e.g., in copy/paste).
     */
    public Neuron duplicate() {
        LinearNeuron ln = new LinearNeuron();
        ln = (LinearNeuron) super.duplicate(ln);
        ln.setBias(getBias());
        ln.setSlope(getSlope());
        ln.setClipping(getClipping());
        ln.setAddNoise(getAddNoise());
        ln.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);

        return ln;
    }

    /**
     * Updates the neuron.
     */
    public void update() {
        double wtdInput = this.getWeightedInputs();
        double val = slope * (wtdInput + bias);

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = clip(val);
        }

        setBuffer(val);
    }

    /**
     * @return Returns the bias.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param bias The bias to set.
     */
    public void setBias(final double bias) {
        this.bias = bias;
    }

    /**
     * @return Returns the slope.
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @param slope The slope to set.
     */
    public void setSlope(final double slope) {
        this.slope = slope;
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Linear";
    }

    /**
     * @return Returns the noise generator.
     */
    public RandomSource getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise generator to set.
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
