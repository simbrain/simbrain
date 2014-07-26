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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>IzhikevichNeuron</b>. Default values correspond to "tonic spiking". TODO:
 * Store a bunch of useful parameters, and add a combo box to switch between the
 * different types. Students could just look it up, but this would be
 * faster/cooler. Just a thought.
 */
public class IzhikevichRule extends SpikingNeuronUpdateRule implements
    NoisyUpdateRule {

    /** Recovery. */
    private double recovery;

    /** A. */
    private double a = .02;

    /** B. */
    private double b = .2;

    /** C. */
    private double c = -65;

    /** D. */
    private double d = 6;

    /** Threshold value to signal a spike. */
    private double threshold = 30;

    /** Noise dialog. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Add noise to the neuron. */
    private boolean addNoise;

    /**
     * {@inheritDoc}
     */
    public IzhikevichRule deepCopy() {
        IzhikevichRule in = new IzhikevichRule();
        in.setA(getA());
        in.setB(getB());
        in.setC(getC());
        in.setD(getD());
        in.setAddNoise(getAddNoise());
        in.noiseGenerator = new Randomizer(noiseGenerator);

        return in;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Neuron neuron) {
        double timeStep = neuron.getNetwork().getTimeStep();
        double inputs = inputType.getInput(neuron);
        double activation = neuron.getActivation();

        if (addNoise) {
            inputs += noiseGenerator.getRandom();
        }

        recovery += (timeStep * (a * ((b * activation) - recovery)));

        double val = activation
            + (timeStep * (((.04 * (activation * activation))
                + (5 * activation) + 140)
                - recovery + inputs));

        if (val >= threshold) {
            val = c;
            recovery += d;
            setHasSpiked(true, neuron);
        } else {
            setHasSpiked(false, neuron);
        }

        neuron.setBuffer(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRandomValue() {
        // Equal chance of spiking or not spiking, taking on any value between
        // the resting potential and the threshold if not.
        return 2 * (threshold - c) * Math.random() + c;
    }

    /**
     * @return Returns the a.
     */
    public double getA() {
        return a;
    }

    /**
     * @param a The a to set.
     */
    public void setA(final double a) {
        this.a = a;
    }

    /**
     * @return Returns the b.
     */
    public double getB() {
        return b;
    }

    /**
     * @param b The b to set.
     */
    public void setB(final double b) {
        this.b = b;
    }

    /**
     * @return Returns the c.
     */
    public double getC() {
        return c;
    }

    /**
     * @param c The c to set.
     */
    public void setC(final double c) {
        this.c = c;
    }

    /**
     * @return Returns the d.
     */
    public double getD() {
        return d;
    }

    /**
     * @param d The d to set.
     */
    public void setD(final double d) {
        this.d = d;
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
    public Randomizer getNoiseGenerator() {
        return noiseGenerator;
    }

    /**
     * @param noiseGenerator The noiseGenerator to set.
     */
    public void setNoiseGenerator(final Randomizer noiseGenerator) {
        this.noiseGenerator = noiseGenerator;
    }

    @Override
    public String getDescription() {
        return "Izhikevich";
    }

}
