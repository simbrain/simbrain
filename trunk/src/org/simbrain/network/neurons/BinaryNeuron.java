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
 * <b>BinaryNeuron</b> takes one of two values.
 */
public class BinaryNeuron implements NeuronUpdateRule {

    /** Threshold for binary neurons. */
    private double threshold = .5;

    /** Bias for binary neurons. */
    private double bias = 0;

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
        return "Binary";
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }

//    /**
//     * @return a duplicate BinaryNeuron (used, e.g., in copy/paste).
//     */
//    public BinaryNeuron duplicate() {
//        BinaryNeuron bn = new BinaryNeuron();
//        bn = (BinaryNeuron) super.duplicate(bn);
//        bn.setThreshold(getThreshold());
//
//        return bn;
//    }

    /**
     * @{inheritDoc}
     */
    public void update(Neuron neuron) {
        double wtdInput = neuron.getWeightedInputs() + bias;

        if (wtdInput > threshold) {
            neuron.setBuffer(neuron.getUpperBound());
        } else {
            neuron.setBuffer(neuron.getLowerBound());
        }
    }

    /**
     * @return Returns the threshold.
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold The threshold to set.
     */
    public void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return the bias of the neuron.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param bias sets the bias of the neuron.
     */
    public void setBias(final double bias) {
        this.bias = bias;
    }
}
