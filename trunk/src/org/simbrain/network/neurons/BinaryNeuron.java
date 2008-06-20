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
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.ProducingAttribute;


/**
 * <b>BinaryNeuron</b> takes one of two values.  See docs.
 */
public class BinaryNeuron extends Neuron {

    /** Threshold for binary neurons. */
    private double threshold = .5;

    /** Bias for binary neurons. */
    private double bias = 0;

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public BinaryNeuron() {
    }

    /**
     * @return time type of binary neuron.
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to be made the type
     */
    public BinaryNeuron(final Neuron n) {
        super(n);
    }

    /**
     * @return a duplicate BinaryNeuron (used, e.g., in copy/paste).
     */
    public BinaryNeuron duplicate() {
        BinaryNeuron bn = new BinaryNeuron();
        bn = (BinaryNeuron) super.duplicate(bn);
        bn.setThreshold(getThreshold());

        return bn;
    }

    /**
     * Updates the neurons as inputs change.
     */
    public void update() {
        double wtdInput = this.getWeightedInputs() + bias;

        if (wtdInput > threshold) {
            setBuffer(upperBound);
        } else {
            setBuffer(lowerBound);
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
     * @return the name of the neuron.
     */
    public static String getName() {
        return "Binary";
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
