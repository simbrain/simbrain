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
import org.simbrain.network.interfaces.RootNetwork.TimeType;

/**
 * <b>ThreeValuedNeuron</b> is a natural extension of a binary neuron, which
 * takes one of three values depending on the inputs to the neuron in relation
 * to two thresholds.
 */
public class ThreeValueNeuron extends NeuronUpdateRule {

    /** Bias field. */
    private double bias = 0;

    /** Lower threshold field. */
    private double lowerThreshold = 0;

    /** Upper threshold field. */
    private double upperThreshold = 1;

    /** Lower value field. */
    private double lowerValue = -1;

    /** Middle value field. */
    private double middleValue = 0;

    /** Upper value field. */
    private double upperValue = 1;

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
        upperValue = neuron.getUpperBound();
        lowerValue = neuron.getLowerBound();
        middleValue = upperValue / lowerValue;
        lowerThreshold = (lowerValue + middleValue) / 2;
        upperThreshold = (middleValue + upperValue) / 2;
    }

    /**
     * {@inheritDoc}
     */
    public ThreeValueNeuron deepCopy() {
        ThreeValueNeuron tv = new ThreeValueNeuron();
        tv.setBias(getBias());
        tv.setLowerThreshold(getLowerThreshold());
        tv.setUpperThreshold(getUpperThreshold());
        tv.setLowerValue(getLowerValue());
        tv.setMiddleValue(getMiddleValue());
        tv.setUpperValue(getUpperValue());

        return tv;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {
        double wtdInput = neuron.getWeightedInputs() + bias;

        if (wtdInput < lowerThreshold) {
            neuron.setBuffer(lowerValue);
        } else if (wtdInput > upperThreshold) {
            neuron.setBuffer(upperValue);
        } else {
            neuron.setBuffer(middleValue);
        }
    }

    /**
     * @return the bias.
     */
    public double getBias() {
        return bias;
    }

    /**
     * @param bias The bias to set.
     */
    public void setBias(final double bias) {
        this.bias = bias;
    }

    /**
     * @return the lower threshold.
     */
    public double getLowerThreshold() {
        return lowerThreshold;
    }

    /**
     * @param lowerThreshold The lower threshold to set.
     */
    public void setLowerThreshold(final double lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }

    /**
     * @return the lower value.
     */
    public double getLowerValue() {
        return lowerValue;
    }

    /**
     * @param lowerValue The lower value to set.
     */
    public void setLowerValue(final double lowerValue) {
        this.lowerValue = lowerValue;
    }

    /**
     * @return the middle value.
     */
    public double getMiddleValue() {
        return middleValue;
    }

    /**
     * @param middleValue The middle value to set.
     */
    public void setMiddleValue(final double middleValue) {
        this.middleValue = middleValue;
    }

    /**
     * @return the upper threshold.
     */
    public double getUpperThreshold() {
        return upperThreshold;
    }

    /**
     * @param upperThreshold The upper threshold to set.
     */
    public void setUpperThreshold(final double upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    /**
     * @return the upper value.
     */
    public double getUpperValue() {
        return upperValue;
    }

    /**
     * @param upperValue The upper value to set.
     */
    public void setUpperValue(final double upperValue) {
        this.upperValue = upperValue;
    }

    @Override
    public String getDescription() {
        return "Three valued";
    }
}

