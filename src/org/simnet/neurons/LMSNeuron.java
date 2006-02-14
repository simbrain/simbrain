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


/**
 * <b>ClampedNeuron</b>.
 */
public class LMSNeuron extends Neuron {

    /** Learning rate. */
    private double learningRate = 0;
    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public LMSNeuron() {
    }

    /**
     * TODO: Not really true...
     * @return time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.Network.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make the type
     */
    public LMSNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate ClampedNeuron (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        LMSNeuron cn = new LMSNeuron();
        cn = (LMSNeuron) super.duplicate(cn);

        return cn;
    }

    /**
     * Update neuron.
     */
    public void update() {
        setBuffer(activation);
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "LMS";
    }

    /**
     * @return Learning rate.
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * Sets the learning rate.
     * @param learningRate Learning rate to set
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }
}
