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
package org.simbrain.network.neuron_update_rules.activity_generators;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.network.neuron_update_rules.interfaces.BoundedUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ClippableUpdateRule;
import org.simbrain.util.UserParameter;

/**
 * <b>LogisticNeuron</b> updates using the logistic equation, which is chaotic
 * for the default growth rate. Does not use inputs from other neurons.
 */
public class LogisticRule extends NeuronUpdateRule implements ActivityGenerator, BoundedUpdateRule, ClippableUpdateRule {

    /**
     * Growth rate.
     */
    @UserParameter(
            label = "Growth Rate",
            description = "A number that determines the exact form of the quadratic function. "
                    + "It must be between 0 and 4.",
            minimumValue = 0.0, maximumValue = 4.0,
            order = 1)
    private double growthRate = 3.9;

    private double ceiling = 10.0;

    private double floor = -10.0;

    public LogisticRule() {
        super();
    }

    public LogisticRule(LogisticRule lr, Neuron n) {
        super();
        this.ceiling = lr.getUpperBound();
        this.floor = lr.getLowerBound();
        this.growthRate = lr.getGrowthRate();
    }

    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public LogisticRule deepCopy() {
        LogisticRule ln = new LogisticRule();
        ln.setGrowthRate(getGrowthRate());
        ln.setUpperBound(ceiling);
        ln.setLowerBound(floor);
        return ln;
    }

    @Override
    public void update(Neuron neuron) {

        // TODO: Note that the inputs have to be within the neuron's bounds for
        // behavior to be reasonable.

        double x = neuron.getActivation();

        double y = (x - getLowerBound()) / (getUpperBound() - getLowerBound());
        y = growthRate * y * (1 - y);
        x = ((getUpperBound() - getLowerBound()) * y) + getLowerBound();

        neuron.setBuffer(clip(x));
    }

    public double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(final double growthRate) {
        this.growthRate = growthRate;
    }

    @Override
    public String getName() {
        return "Logistic";
    }

    @Override
    public double clip(double val) {
        if (val < getLowerBound()) {
            return getLowerBound();
        }
        if (val > getUpperBound()) {
            return getUpperBound();
        }
        return val;
    }

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public void setClipped(boolean clipping) {
    }

    @Override
    public void setUpperBound(double ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public void setLowerBound(double floor) {
        this.floor = floor;
    }

    @Override
    public double getUpperBound() {
        return ceiling;
    }

    @Override
    public double getLowerBound() {
        return floor;
    }

}