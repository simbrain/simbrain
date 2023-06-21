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

import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.updaterules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.network.util.SpikingMatrixData;
import org.simbrain.util.UserParameter;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

import java.util.Random;

/**
 * A simple spiking neuron that fires when weighted inputs exceed a threshold.
 * When spiking activation is 1, else it is 0.
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
    private ProbabilityDistribution noiseGenerator = new UniformRealDistribution();

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
    public void apply(Layer array, MatrixDataHolder data) {
        for (int i = 0; i < array.outputSize(); i++) {
            if (spikingThresholdRule(array.getInputs().get(i, 0))) {
                ((SpikingMatrixData) data).setHasSpiked(i, true, array.getNetwork().getTime());
                array.getOutputs().set(i, 0, 1);
            } else {
                ((SpikingMatrixData) data).setHasSpiked(i, false, array.getNetwork().getTime());
                array.getOutputs().set(i, 0, 0);
            }
        }
    }

    @Override
    public void apply(Neuron neuron, ScalarDataHolder data) {
        if (spikingThresholdRule(neuron.getInput())) {
            neuron.setSpike(true);
            neuron.setActivation(1);
        } else {
            neuron.setSpike(false);
            neuron.setActivation(0); // Make this a separate variable?
        }
    }

    public boolean spikingThresholdRule(double in) {
        final double input = in + (addNoise ? noiseGenerator.sampleDouble() : 0);
        if (input >= threshold) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public double getRandomValue() {
        Random rand = new Random();
        return rand.nextBoolean() ? 1 : 0;
    }

    public double getThreshold() {
        return threshold;
    }

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
