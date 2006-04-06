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
public class ExponentialDecayNeuron extends Neuron {

    /** Time constant. */
    private double timeConstant = .1;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public ExponentialDecayNeuron() {
    }

    /**
     * TODO: Not really true...
     * @return time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.Network.CONTINUOUS;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make the type
     */
    public ExponentialDecayNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate ClampedNeuron (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        ExponentialDecayNeuron ed = new ExponentialDecayNeuron();
        ed = (ExponentialDecayNeuron) super.duplicate(ed);
        ed.setTimeConstant(getTimeConstant());

        return ed;
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
        return "Exponential decay";
    }

    /**
     * @return time constant value.
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * Sets the time constant value.
     *
     * @param timeConstant value to set time constant to
     */
    public void setTimeConstant(final double timeConstant) {
        this.timeConstant = timeConstant;
    }
}
