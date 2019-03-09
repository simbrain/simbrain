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
import org.simbrain.util.UserParameter;

/**
 * <b>StochasticNeuron</b> is a simple type of random neuron which takes the
 * value of the upper bound if a random variable is above a specified firing
 * probability, and the lower bound otherwise. Ignores inputs.
 * <p>
 * TODO: Separate spiking from non-spiking version?
 */
public class StochasticRule extends ActivityGenerator {

    /**
     * The default firing probability for the Neuron.
     */
    private static final double DEFAULT_FIRING_PROBABILITY = .05;

    /**
     * Probability the neuron will fire.
     */
    @UserParameter(
            label = "Firing Probability",
            description = "This parameter determines the probability that the generator will fire, "
                    + "causing it to have an activation equal to its upper bound, given an iteration.",
            order = 1)
    private double firingProbability = DEFAULT_FIRING_PROBABILITY;

    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public StochasticRule deepCopy() {
        StochasticRule sn = new StochasticRule();
        sn.setFiringProbability(getFiringProbability());
        return sn;
    }

    @Override
    public void update(Neuron neuron) {
        double rand = Math.random();
        if (rand > 1 - firingProbability) {
            neuron.setSpkBuffer(true);
            setHasSpiked(true, neuron);
            neuron.setBuffer(1);
        } else {
            neuron.setSpkBuffer(false);
            setHasSpiked(false, neuron);
            neuron.setBuffer(0); // Make this a separate variable?
        }
    }

    public double getFiringProbability() {
        return firingProbability;
    }

    public void setFiringProbability(final double firingProbability) {
        this.firingProbability = firingProbability;
    }

    @Override
    public String getName() {
        return "Stochastic";
    }

}
