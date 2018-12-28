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

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.util.UserParameter;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;

import java.util.Random;

/**
 * <b>ThreeValuedNeuron</b> is a natural extension of a binary neuron, which
 * takes one of three values depending on the inputs to the neuron in relation
 * to two thresholds.
 */
public class ThreeValueRule extends NeuronUpdateRule {

    /**
     * Bias field.
     */
    @UserParameter(
            label = "Bias",
            description = "A fixed amount of input to the node.",
//            minimumValue = -10, maximumValue = 10,
            defaultValue = "0", order = 1)
    private double bias = 0;

    /**
     * Lower threshold field.
     */
    @UserParameter(
            label = "Lower Threshold",
            description = "If the weighted input plus the bias is less than this value, the activation "
                    + "is set to the Lower Value.",
            defaultValue = "0", order = 2)
    private double lowerThreshold = 0;

    /**
     * Upper threshold field.
     */
    @UserParameter(
            label = "Upper Threshold",
            description = "If the weighted input plus the bias is greater than this value, the activation "
                    + "is set to the Upper Value.",
            defaultValue = "1", order = 3)
    private double upperThreshold = 1;

    /**
     * Lower value field.
     */
    @UserParameter(
            label = "Lower Value",
            description = "The activation is set to this value if the weighted input plus the bias exceeds "
                    + "the Lower Threshold.",
            defaultValue = "-1", order = 4)
    private double lowerValue = -1;

    /**
     * Middle value field.
     */
    @UserParameter(
            label = "Middle Value",
            description = "If the weighted input plus the bias does not exceed the Upper or Lower Thresholds, "
                    + "then the activation is set to this value.",
            defaultValue = "0", order = 5)
    private double middleValue = 0;

    /**
     * Upper value field.
     */
    @UserParameter(
            label = "Upper Value",
            description = "If the weighted input plus the bias is greater than the Upper Threshold, "
                    + "then the activation is set to this value.",
            defaultValue = "1", order = 6)
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
    public ThreeValueRule deepCopy() {
        ThreeValueRule tv = new ThreeValueRule();
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
        double wtdInput = neuron.getInput();

        if (wtdInput < lowerThreshold) {
            neuron.setBuffer(lowerValue);
        } else if (wtdInput > upperThreshold) {
            neuron.setBuffer(upperValue);
        } else {
            neuron.setBuffer(middleValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRandomValue() {
        Random rand = new Random();
        int d = rand.nextInt(3);
        if (d == 0) {
            return lowerValue;
        } else if (d == 1) {
            return middleValue;
        } else {
            return upperValue;
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
    public String getName() {
        return "Three Value";
    }

    @Override
    public double getGraphicalLowerBound() {
        return lowerValue - 1;
    }

    @Override
    public double getGraphicalUpperBound() {
        return upperValue + 1;
    }

}
