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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.RootNetwork.TimeType;

/**
 * <b>LogisticNeuron</b> updates using the logistic equation, which is chaotic
 * for the default growth rate. Does not use inputs from other neurons.
 */
public class LogisticNeuron extends NeuronUpdateRule {

    /** Growth rate. */
    private double growthRate = 3.9;

    /**
     * @{inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
    }

    /**
     * @{inheritDoc}
     */
    public LogisticNeuron deepCopy() {
        LogisticNeuron ln = new LogisticNeuron();
        ln.setGrowthRate(getGrowthRate());

        return ln;
    }

    /**
     * @{inheritDoc}
     */
    public void update(Neuron neuron) {

        // TODO: Note that the inputs have to be within the neuron's bounds for
        // behavior to be reasonable.

        double x = neuron.getActivation();

        double y = (x - neuron.getLowerBound())
                / (neuron.getUpperBound() - neuron.getLowerBound());
        y = growthRate * y * (1 - y);
        x = ((neuron.getUpperBound() - neuron.getLowerBound()) * y)
                + neuron.getLowerBound();

        neuron.setBuffer(neuron.clip(x));
    }

    /**
     * @return Returns the firingProbability.
     */
    public double getGrowthRate() {
        return growthRate;
    }

    /**
     * @param growthRate The growthRate to set.
     */
    public void setGrowthRate(final double growthRate) {
        this.growthRate = growthRate;
    }

    @Override
    public String getDescription() {
        return "Logistic";
    }
}
