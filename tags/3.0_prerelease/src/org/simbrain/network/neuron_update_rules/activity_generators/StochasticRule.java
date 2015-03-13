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
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;

/**
 * <b>StochasticNeuron</b> is a simple type of random neuron which takes the
 * value of the upper bound if a random variable is above a specified firing
 * probability, and the lower bound otherwise. Ignores inputs.
 */
public class StochasticRule extends SpikingNeuronUpdateRule implements
         ActivityGenerator {

    /** The default firing probability for the Neuron. */
    private static final double DEFAULT_FIRING_PROBABILITY = .5;

    /** Probability the neuron will fire. */
    private double firingProbability = DEFAULT_FIRING_PROBABILITY;

    /** Lower value field. */
    private double lowerValue = -1;

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
        neuron.setGenerator(true);
    }

    /**
     * {@inheritDoc}
     */
    public StochasticRule deepCopy() {
        StochasticRule sn = new StochasticRule();
        sn.setFiringProbability(getFiringProbability());
        return sn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {
        double rand = Math.random();
        if (rand > firingProbability) {
            neuron.setBuffer(upperValue);
        } else {
            neuron.setBuffer(lowerValue);
        }
    }

    /**
     * @return Returns the firingProbability.
     */
    public double getFiringProbability() {
        return firingProbability;
    }

    /**
     * @param firingProbability The firingProbability to set.
     */
    public void setFiringProbability(final double firingProbability) {
        this.firingProbability = firingProbability;
    }

    @Override
    public String getDescription() {
        return "Stochastic";
    }

    /**
     * @return the lowerValue
     */
    public double getLowerValue() {
        return lowerValue;
    }

    /**
     * @param lowerValue the lowerValue to set
     */
    public void setLowerValue(double lowerValue) {
        this.lowerValue = lowerValue;
    }

    /**
     * @return the upperValue
     */
    public double getUpperValue() {
        return upperValue;
    }

    /**
     * @param upperValue the upperValue to set
     */
    public void setUpperValue(double upperValue) {
        this.upperValue = upperValue;
    }

}
