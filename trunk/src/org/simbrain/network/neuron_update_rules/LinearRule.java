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
package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.util.RandomSource;

/**
 * <b>LinearNeuron</b> is a standard linear neuron.
 */
public class LinearRule extends NeuronUpdateRule implements BiasedUpdateRule,
        DifferentiableUpdateRule {

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
     * @{inheritDoc
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * @{inheritDoc
     */
    public void init(Neuron neuron) {
        // No implementation
    }

    /**
     * @{inheritDoc
     */
    public LinearRule deepCopy() {
        LinearRule ln = new LinearRule();
        ln.setBias(getBias());
        ln.setSlope(getSlope());
        ln.setClipping(getClipping());
        ln.setAddNoise(getAddNoise());
        ln.noiseGenerator = new RandomSource(noiseGenerator);
        return ln;
    }

    /**
     * @{inheritDoc
     */
    public void update(Neuron neuron) {
        double wtdInput = neuron.getWeightedInputs();
        double val = slope * wtdInput;

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        if (clipping) {
            val = neuron.clip(val);
        }

        neuron.setBuffer(val);
    }

    /**
     * {@inheritDoc}
     */
    public double getDerivative(double val, Neuron neuron) {
        if (val >= neuron.getUpperBound()) {
            return 0;
        } else if (val <= neuron.getLowerBound()) {
            return 0;
        } else {
            return slope;
        }
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

    @Override
    public String getDescription() {
        return "Linear";
    }

}
