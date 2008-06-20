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
import org.simbrain.network.util.RandomSource;


/**
 * <b>RandomNeuron</b>.
 */
public class RandomNeuron extends Neuron {
    /** Noise dialog. */
    private RandomSource randomizer = new RandomSource();

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public RandomNeuron() {
        randomizer.setUpperBound(this.getUpperBound());
        randomizer.setLowerBound(this.getLowerBound());
    }

    /**
     * @return Time type.
     */
    public int getTimeType() {
        return org.simbrain.network.interfaces.RootNetwork.DISCRETE;
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param n Neuron to be made of type
     */
    public RandomNeuron(final Neuron n) {
        super(n);
        randomizer.setUpperBound(this.getUpperBound());
        randomizer.setLowerBound(this.getLowerBound());
    }

    /**
     * @return duplicate RandomNeuron (used, e.g., in copy/paste).
     */
    public RandomNeuron duplicate() {
        RandomNeuron rn = new RandomNeuron();
        rn = (RandomNeuron) super.duplicate(rn);
        rn.randomizer = randomizer.duplicate(randomizer);

        return rn;
    }

    /**
     * Update neuron.
     */
    public void update() {
        randomizer.setUpperBound(this.getUpperBound());
        randomizer.setLowerBound(this.getLowerBound());
        setBuffer(randomizer.getRandom());
    }

    /**
     * @return Name of neuron type.
     */
    public static String getName() {
        return "Random";
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
        this.setUpperBound(randomizer.getUpperBound());
        this.setLowerBound(randomizer.getLowerBound());
    }
}
