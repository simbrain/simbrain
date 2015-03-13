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
package org.simbrain.network.neuron_update_rules.activity_generators;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>SinusoidalNeuron</b> produces a sine wave; inputs are ignored.
 *
 * TODO: Input generator
 */
public class SinusoidalRule extends NeuronUpdateRule implements
        BoundedUpdateRule, ActivityGenerator {

    /** Phase. */
    private double phase = 1;

    /** Frequency. */
    private double frequency = .1;

    /** The upper boundary of the activation. */
    private double ceiling = 1.0;

    /** The lower boundary of the activation. */
    private double floor = -1.0;

    /** Noise dialog. */
    private Randomizer noiseGenerator = new Randomizer();

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
        neuron.setGenerator(true);
    }

    /**
     * {@inheritDoc}
     */
    public SinusoidalRule deepCopy() {
        SinusoidalRule sn = new SinusoidalRule();
        sn.setPhase(getPhase());
        sn.setFrequency(getFrequency());
        sn.setAddNoise(getAddNoise());
        sn.noiseGenerator = new Randomizer(noiseGenerator);
        return sn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {

        double upperBound = getUpperBound();
        double lowerBound = getLowerBound();
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
    public Randomizer getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noise The noise to set.
     */
    public void setNoiseGenerator(final Randomizer noise) {
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

    @Override
    public void setUpperBound(double ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public void setLowerBound(double floor) {
        this.floor = floor;
    }

    @Override
    public double getRandomValue() {
        double rand = (2 * Math.PI) * Math.random();
        double range = getUpperBound() - getLowerBound();
        return ((range / 2) * Math.sin(frequency * rand + phase))
                + ((getUpperBound() + getLowerBound()) / 2);
    }

    @Override
    public double getUpperBound() {
        return ceiling;
    }

    @Override
    public double getLowerBound() {
        return floor;
    }
}
