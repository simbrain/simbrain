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

import java.util.Random;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;

/**
 * <b>BinaryNeuron</b> takes one of two values.
 */
public class BinaryRule extends NeuronUpdateRule {

    private static final double DEFAULT_CEILING = 1.0;

    private static final double DEFAULT_FLOOR = -1.0;

    /** Threshold for binary neurons. */
    private double threshold = .5;

    private double ceiling = DEFAULT_CEILING;

    private double floor = DEFAULT_FLOOR;

    /** Bias for binary neurons. */
    private double bias = 0;

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * {@inheritDoc}
     */
    public BinaryRule deepCopy() {
        BinaryRule bn = new BinaryRule();
        bn.setThreshold(getThreshold());
        bn.setCeiling(getCeiling());
        bn.setFloor(getFloor());
        bn.setIncrement(getIncrement());
        return bn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {
        double wtdInput = neuron.getWeightedInputs() + bias;

        if (wtdInput > threshold) {
            neuron.setBuffer(getCeiling());
        } else {
            neuron.setBuffer(getFloor());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRandomValue() {
        Random rand = new Random();
        return rand.nextBoolean() ? getCeiling() : getFloor();
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

    @Override
    public String getDescription() {
        return "Binary";
    }

    public double getCeiling() {
        return ceiling;
    }

    public void setCeiling(double ceiling) {
        this.ceiling = ceiling;
    }

    public double getFloor() {
        return floor;
    }

    public void setFloor(double floor) {
        this.floor = floor;
    }

}
