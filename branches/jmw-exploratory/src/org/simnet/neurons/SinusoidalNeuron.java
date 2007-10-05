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
package org.simnet.neurons;

import org.simnet.interfaces.Neuron;
import org.simnet.util.RandomSource;


/**
 * <b>SinusoidalNeuron</b>.
 */
public class SinusoidalNeuron extends Neuron {
    /** Phase. */
    private double phase = 1;
    /** Frequency. */
    private double frequency = .1;

    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();
    /** Add noise to the neuron. */
    private boolean addNoise = false;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public SinusoidalNeuron() {
    }

    /**
     * TODO: As with clamped, no real time type...
     * @return Time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to be made of type
     */
    public SinusoidalNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return duplicate SinusoidalNeuron (used, e.g., in copy/paste).
     */
    public SinusoidalNeuron duplicate() {
        SinusoidalNeuron sn = new SinusoidalNeuron();
        sn = (SinusoidalNeuron) super.duplicate(sn);
        sn.setPhase(getPhase());
        sn.setFrequency(getFrequency());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);

        return sn;
    }

    /**
     * Updates the neuron.
     */
    public void update() {
        double range = upperBound - lowerBound;
        double val = ((range / 2)  * Math.sin(frequency * getParentNetwork().getRootNetwork().getTime() + phase))
            + ((upperBound + lowerBound) / 2);

        if (addNoise) {
            val += noiseGenerator.getRandom();
        }

        setBuffer(val);
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
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Sinusoidal";
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

}
