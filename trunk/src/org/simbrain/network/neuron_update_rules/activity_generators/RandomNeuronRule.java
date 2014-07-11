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
import org.simbrain.util.randomizer.Randomizer;

/**
 * <b>RandomNeuron</b> produces random activations within specified parameters.
 *
 * TODO: This should be an input generator
 */
public class RandomNeuronRule extends NeuronUpdateRule implements
        BoundedUpdateRule, ActivityGenerator {

    /** Noise source. */
    private Randomizer randomizer = new Randomizer();

    private double ceiling = 1.0;

    private double floor = -1.0;

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    public RandomNeuronRule(Neuron n) {
        super();
        init(n);
        randomizer.setUpperBound(getUpperBound());
        randomizer.setLowerBound(getLowerBound());
    }

    public RandomNeuronRule(RandomNeuronRule rn, Neuron n) {
        super();
        init(n);
        setRandomizer(new Randomizer(rn.randomizer));
    }

    public RandomNeuronRule() {
        super();
    }

    /**
     * {@inheritDoc} <b>Unsafe for activity generators</b>. If copied across a
     * set of neurons, {@link #init(Neuron) init} must be called to ensure
     * rational behavior for an activity generator. The
     * {@link #RandomNeuronRule(RandomNeuronRule, Neuron) copy constructor} is
     * the preferred method of copying because {@link #init(Neuron) init} is
     * called on the neuron parameter automatically.
     */
    public RandomNeuronRule deepCopy() {
        RandomNeuronRule rn = new RandomNeuronRule();
        rn.randomizer = new Randomizer(randomizer);
        return rn;
    }

    /**
     * {@inheritDoc}
     */
    public void update(Neuron neuron) {
        neuron.setBuffer(randomizer.getRandom());
    }

    /**
     * @return Returns the randomizer.
     */
    public Randomizer getRandomizer() {
        return randomizer;
    }

    /**
     * @param randomizer The randomizer to set.
     */
    public void setRandomizer(final Randomizer randomizer) {
        this.randomizer = randomizer;
    }

    @Override
    public String getDescription() {
        return "Random";
    }

    @Override
    public double getRandomValue() {
        return randomizer.getRandom();
    }

    @Override
    public double getUpperBound() {
        return ceiling;
    }

    @Override
    public double getLowerBound() {
        return floor;
    }

    @Override
    public void init(Neuron n) {
        n.setGenerator(true);
    }

    @Override
    public void setUpperBound(double ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public void setLowerBound(double floor) {
        this.floor = floor;
    }
}
