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
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * <b>AdditiveNeuron</b> See Haykin (2002), section 14.5. Used with continuous
 * Hopfield networks.
 */
public class AdditiveRule extends NeuronUpdateRule implements NoisyUpdateRule {

    //TODO: May need clipping and bounds.

    /**
     * Lambda.
     */
    private double lambda = 1.4;

    /**
     * Resistance.
     */
    private double resistance = 1;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = new UniformDistribution();

    /**
     * For adding noise to the neuron.
     */
    private boolean addNoise = false;

    @Override
    public TimeType getTimeType() {
        return TimeType.CONTINUOUS;
    }

    @Override
    public AdditiveRule deepCopy() {
        AdditiveRule an = new AdditiveRule();
        an.setLambda(getLambda());
        an.setResistance(getResistance());
        an.setAddNoise(getAddNoise());
        an.noiseGenerator = noiseGenerator.deepCopy();
        return an;
    }

    @Override
    public void apply(Neuron neuron, ScalarDataHolder data) {

        // Update buffer of additive neuron using Euler's method.
        double wtdSum = 0;
        if (neuron.getFanIn().size() > 0) {
            for (int j = 0; j < neuron.getFanIn().size(); j++) {
                Synapse w = neuron.getFanIn().get(j);
                Neuron source = w.getSource();
                wtdSum += (w.getStrength() * g(source.getActivation()));
            }
        }

        double val = neuron.getActivation() + neuron.getNetwork().getTimeStep() * (-neuron.getActivation() / resistance + wtdSum);

        if (addNoise) {
            val += noiseGenerator.nextDouble();
        }

        neuron.setActivation(val);
        neuron.addInputValue(0);
    }

    /**
     * Implements a Hopfield type sigmoidal function.
     *
     * @param x input to function
     * @return output of function
     */
    private double g(final double x) {
        return 2 / Math.PI * Math.atan((Math.PI * lambda * x) / 2);
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(final double lambda) {
        this.lambda = lambda;
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(final double resistance) {
        this.resistance = resistance;
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    public boolean getAddNoise() {
        return addNoise;
    }

    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
    }

    @Override
    public String getName() {
        return "Additive (Continuous Hopfield)";
    }

}
