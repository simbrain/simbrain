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
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.EmptyScalarData;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.network.util.SpikingMatrixData;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import smile.math.matrix.Matrix;

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
    private ProbabilityDistribution noiseGenerator = new UniformDistribution();

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
    public void apply(NeuronArray array, MatrixDataHolder data) {
        // TODO: Implement using matrix operations
        double[] vals = new double[array.size()];
        for (int i = 0; i < vals.length; i++) {
            if (spikingThresholdRule(array.getInputs().col(0)[i])) {
                ((SpikingMatrixData) data).getSpikes()[0] = true;
                // dataspk.lastSpikeTimes[0] = getTime; // TODO
                vals[i] = 1;
            } else {
                ((SpikingMatrixData) data).getSpikes()[0] = false;
                vals[i] = 0;
            }
        }
        array.setActivations(new Matrix(vals));
    }

    @Override
    public void apply(Neuron neuron, ScalarDataHolder data) {
        if (spikingThresholdRule(neuron.getInput())) {
            neuron.setSpike(true);
            setHasSpiked(true, neuron);
            neuron.setActivation(1);
        } else {
            neuron.setSpike(false);
            setHasSpiked(false, neuron);
            neuron.setActivation(0); // Make this a separate variable?
        }
    }

    public boolean spikingThresholdRule(double in) {
        final double input = in + (addNoise ? noiseGenerator.nextDouble() : 0);
        if (input >= threshold) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public MatrixDataHolder createMatrixData(int size) {
        return new SpikingMatrixData(size);
    }

    @Override
    public ScalarDataHolder createScalarData() {
        return new EmptyScalarData();
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
