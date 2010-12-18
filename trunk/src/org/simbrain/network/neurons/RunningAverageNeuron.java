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

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.NeuronUpdateRule;

/**
 * <b>RunningAverageNeuron</b> keeps a running average of current and past activity.
 */
public class RunningAverageNeuron implements NeuronUpdateRule {

    /** Rate constant variable. */
    private double rateConstant = .5;

    /** Last activation. */
    private double val = 0;

    /**
     * @{inheritDoc}
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * @{inheritDoc}
     */
    public String getName() {
        return "Running Average";
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }
 
//   /**
//     * Returns a duplicate ClampedNeuron (used, e.g., in copy/paste).
//     * @return Duplicated neuron
//     */
//    public RunningAverageNeuron duplicate() {
//        RunningAverageNeuron cn = new RunningAverageNeuron();
//        cn = (RunningAverageNeuron) super.duplicate(cn);
//        cn.setRateConstant(getRateConstant());
//
//        return cn;
//    }

    /**
     * Update neuron.
     */
    public void update(Neuron neuron) {
        // "val" on right is activation at last time step
        val = rateConstant * neuron.getWeightedInputs() + (1 - rateConstant) * val;
        neuron.setBuffer(val);
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
