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

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.util.RandomSource;

/**
 * <b>SinusoidalNeuron</b> produces a sine wave; inputs are ignored.
 */
public class SinusoidalNeuron extends NeuronUpdateRule {

    /** Phase. */
    private double phase = 1;

    /** Frequency. */
    private double frequency = .1;

    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();

    /** Add noise to the neuron. */
    private boolean addNoise = false;

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * {@inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }

    /**
     * {@inheritDoc}
     */
    public SinusoidalNeuron deepCopy() {
        SinusoidalNeuron sn = new SinusoidalNeuron();
        sn.setPhase(getPhase());
        sn.setFrequency(getFrequency());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = new RandomSource(noiseGenerator);
        return sn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {

        double upperBound = neuron.getUpperBound();
        double lowerBound = neuron.getLowerBound();
        double range = upperBound - lowerBound;
        double val = ((range / 2) * Math.sin(frequency
                * neuron.getNetwork().getTime() + phase))
            + ((upperBound + lowerBound) / 2);

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        neuron.setBuffer(val);
    }

    /**
     * @return Random noise generator dialog.
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
     * @return Returns the upperValue.
     */
    public double getPhase() {
        return phase;
    }

    /**
     * @param phase The phase to set.
     */
    public void setPhase(final double phase) {
        this.phase = phase;
    }

    /**
     * @return Returns the frequency.
     */
    public double getFrequency() {
        return frequency;
    }

    /**
     * @param frequency The frequency to set.
     */
    public void setFrequency(final double frequency) {
        this.frequency = frequency;
    }

    @Override
    public String getDescription() {
        return "Sinusoidal";
    }
}
