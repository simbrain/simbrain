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
import org.simbrain.network.util.RandomSource;


/**
 * <b>RandomNeuron</b> produces random activations within specified parameters.
 */
public class RandomNeuron implements NeuronUpdateRule {

    /** Noise source. */
    private RandomSource randomizer = new RandomSource();

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
        return "Random";
    }

    /**
     * @{inheritDoc}
     */
    public void init(Neuron neuron) {
        // No implementation
        randomizer.setUpperBound(neuron.getUpperBound());
        randomizer.setLowerBound(neuron.getLowerBound());
    }


//    /**
//     * @return duplicate RandomNeuron (used, e.g., in copy/paste).
//     */
//    public RandomNeuron duplicate() {
//        RandomNeuron rn = new RandomNeuron();
//        rn = (RandomNeuron) super.duplicate(rn);
//        rn.randomizer = randomizer.duplicate(randomizer);
//
//        return rn;
//    }

    /**
     * Update neuron.
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
}
