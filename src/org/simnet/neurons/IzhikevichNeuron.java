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
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.util.RandomSource;


/**
 * <b>IzhikevichNeuron</b>.
 */
public class IzhikevichNeuron extends Neuron implements SpikingNeuron {
    /** Has spiked. */
    private boolean hasSpiked = false;
    /** Recovery. */
    private double recovery = 0;
    /** A. */
    private double a = .2;
    /** B. */
    private double b = 2;
    /** C. */
    private double c = -56;
    /** D. */
    private double d = -16;
    /** Noise dialog. */
    private RandomSource noiseGenerator = new RandomSource();
    /** Add noise to the neuron. */
    private boolean addNoise = false;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public IzhikevichNeuron() {
    }

    /**
     * @return Time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.Network.CONTINUOUS;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make of the type
     */
    public IzhikevichNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return duplicate IzhikevichNeuron (used, e.g., in copy/paste).
     */
    public Neuron duplicate() {
        IzhikevichNeuron in = new IzhikevichNeuron();
        in = (IzhikevichNeuron) super.duplicate(in);
        in.setA(getA());
        in.setB(getB());
        in.setC(getC());
        in.setD(getD());
        in.setAddNoise(getAddNoise());
        in.noiseGenerator = noiseGenerator.duplicate(noiseGenerator);

        return in;
    }

    /**
     * Updates the neuron.
     */
    public void update() {
        double timeStep = this.getParentNetwork().getTimeStep();
        double inputs = weightedInputs();

        if (addNoise) {
            inputs += noiseGenerator.getRandom();
        }

        recovery += (timeStep * (a * ((b * activation) - recovery)));

        double val = activation
                + (timeStep * (((.04 * (activation * activation))
                        + (5 * activation) + 140)
                        - recovery + inputs));

        if (val > 30) {
            val = c;
            recovery += d;
            hasSpiked = true;
        } else {
            hasSpiked = false;
        }

        setBuffer(val);
    }

    /**
     * @return Has the neuron spiked.
     */
    public boolean hasSpiked() {
        return hasSpiked;
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
     * @return Name of the neuron type.
     */
    public static String getName() {
        return "Izhikevich";
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
}
