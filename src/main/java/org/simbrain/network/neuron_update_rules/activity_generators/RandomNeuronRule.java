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
import org.simbrain.network.updaterules.interfaces.ClippedUpdateRule;
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

/**
 * <b>RandomNeuron</b> produces random activations within specified parameters.
 */
public class RandomNeuronRule extends NeuronUpdateRule implements ActivityGenerator, ClippedUpdateRule, NoisyUpdateRule {

    /**
     * Noise source.
     */
    private ProbabilityDistribution randomizer = new UniformRealDistribution();

    private double ceiling = 1.0;

    private double floor = -1.0;

    /**
     * Bounded update rule is automatically clippable.  It is not needed here since sigmoids automatically respect
     * upper and lower bounds but can still be turned on to constrain contextual increment and decrement.
     */
    private boolean isClipped = false;

    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    public RandomNeuronRule() {
        super();
    }

    public RandomNeuronRule(RandomNeuronRule rn, Neuron n) {
        super();
        setNoiseGenerator(rn.randomizer.deepCopy());
    }

    @Override
    public RandomNeuronRule deepCopy() {
        RandomNeuronRule rn = new RandomNeuronRule();
        rn.randomizer = randomizer.deepCopy();
        return rn;
    }

    @Override
    public void apply(Neuron neuron, ScalarDataHolder data) {
        neuron.setActivation(randomizer.sampleDouble());
    }

    @Override
    public String getName() {
        return "Random";
    }

    @Override
    public double getRandomValue() {
        return randomizer.sampleDouble();
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
    public void setUpperBound(double ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public void setLowerBound(double floor) {
        this.floor = floor;
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return randomizer;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.randomizer = noise;
    }

    @Override
    public boolean getAddNoise() {
        return true;
    }

    @Override
    public void setAddNoise(boolean noise) {
    }

    @Override
    public boolean isClipped() {
        return isClipped;
    }

    @Override
    public void setClipped(boolean clipped) {
        isClipped = clipped;
    }
}
