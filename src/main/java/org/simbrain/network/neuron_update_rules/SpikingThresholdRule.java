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

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.DataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

import java.util.Random;

/**
 * A simple spiking neuron that fires when weighted inputs exceed a threshold.
 * TODO: Has no documentation.
 */
public class SpikingThresholdRule extends SpikingNeuronUpdateRule implements NoisyUpdateRule {

    /**
     * Threshold.
     */
    @UserParameter(
            label = "Threshold",
            description = "Input value above which the neuron spikes.",
            increment = .1,
            order = 1)
    private double threshold = .5;

    /**
     * The noise generating randomizer.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Whether or not to add noise to the inputs .
     */
    private boolean addNoise;

    @Override
    public SpikingThresholdRule deepCopy() {
        SpikingThresholdRule neuron = new SpikingThresholdRule();
        neuron.setThreshold(getThreshold());
        return neuron;
    }

    @Override
    public DataHolder createDataHolder(int size) {
        return new DataHolder.SpikingDataHolder(size);
    }

    @Override
    public double[] apply(double[] inputs, double[] activations, DataHolder dataHolder) {
        var dataspk = (DataHolder.SpikingDataHolder)dataHolder;
        double[] vals = new double[inputs.length];
        for (int i = 0; i < inputs.length ; i++) {
            final double input = inputs[i] + (addNoise ? noiseGenerator.getRandom() : 0);
            if (input >= threshold) {
                dataspk.spikes[i] = true;
                // setHasSpiked(true, neuron); // todo
                vals[i] = 1;
            } else {
                dataspk.spikes[i] = false;
                // setHasSpiked(false, neuron);
                vals[i] = 0;
            }
        }
        return vals;
    }

    @Override
    public void update(Neuron neuron) {
        final double input = neuron.getInput() + (addNoise ? noiseGenerator.getRandom() : 0);
        if (input >= threshold) {
            neuron.setSpike(true);
            setHasSpiked(true, neuron);
            neuron.setActivation(1);
        } else {
            neuron.setSpike(false);
            setHasSpiked(false, neuron);
            neuron.setActivation(0); // Make this a separate variable?
        }
    }

    @Override
    public double getRandomValue() {
        Random rand = new Random();
        return rand.nextBoolean() ? 1 : 0;
    }

    /**
     * @return the threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public String getName() {
        return "Spiking Threshold";
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    @Override
    public boolean getAddNoise() {
        return addNoise;
    }

    @Override
    public void setAddNoise(boolean noise) {
        this.addNoise = noise;
    }
}
