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
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.util.RandomSource;

/**
 * <b>RandomNeuron</b> produces random activations within specified parameters.
 */
public class RandomNeuronRule extends NeuronUpdateRule {

    /** Noise source. */
    private RandomSource randomizer = new RandomSource();

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
        // No implementation
        randomizer.setUpperBound(neuron.getUpperBound());
        randomizer.setLowerBound(neuron.getLowerBound());
    }

    /**
     * {@inheritDoc}
     */
    public RandomNeuronRule deepCopy() {
        RandomNeuronRule rn = new RandomNeuronRule();
        rn.randomizer = new RandomSource(randomizer);
        return rn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {
        randomizer.setUpperBound(neuron.getUpperBound());
        randomizer.setLowerBound(neuron.getLowerBound());
        neuron.setBuffer(randomizer.getRandom());
    }

    /**
     * @return Returns the randomizer.
     */
    public RandomSource getRandomizer() {
        return randomizer;
    }

    /**
     * @param randomizer The randomizer to set.
     */
    public void setRandomizer(final RandomSource randomizer) {
        this.randomizer = randomizer;
    }

    @Override
    public String getDescription() {
        return "Random";
    }
}
