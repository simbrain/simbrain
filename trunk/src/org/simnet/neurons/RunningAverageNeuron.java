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


/**
 * <b>RunningAverageNeuron</b>.
 */
public class RunningAverageNeuron extends Neuron {

    /** Rate constant variable. */
    private double rateConstant = .5;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public RunningAverageNeuron() {
    }

    /**
     * @return time type.
     */
    public int getTimeType() {
        return org.simnet.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to make the type
     */
    public RunningAverageNeuron(final Neuron n) {
        super(n);
    }

    /**
     * Returns a duplicate ClampedNeuron (used, e.g., in copy/paste).
     * @return Duplicated neuron
     */
    public Neuron duplicate() {
        RunningAverageNeuron cn = new RunningAverageNeuron();
        cn = (RunningAverageNeuron) super.duplicate(cn);
        cn.setRateConstant(getRateConstant());

        return cn;
    }

    double val = 0;

    /**
     * Update neuron.
     */
    public void update() {
        // "val" on left is activation at last time step
        val = rateConstant * getWeightedInputs() + (1 - rateConstant) * val;
        this.setBuffer(val);
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Running Average";
    }

    /**
     * @return Rate constant.
     */
    public double getRateConstant() {
        return rateConstant;
    }

    /**
     * @param rateConstant Parameter to be set.
     */
    public void setRateConstant(final double rateConstant) {
        this.rateConstant = rateConstant;
    }
}
