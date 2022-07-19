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
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;

/**
 * <b>IzhikevichNeuron</b>. Default values correspond to "tonic spiking". TODO:
 * Store a bunch of useful parameters, and add a combo box to switch between the
 * different types. Students could just look it up, but this would be
 * faster/cooler. Just a thought.
 */
public class IzhikevichRule extends SpikingNeuronUpdateRule implements NoisyUpdateRule {

    /**
     * Recovery.
     */
    private double recovery;

    /**
     * A.
     */
    @UserParameter(
            label = "A",
            description = "Parameter for recovery variable.",
            increment = .01,
            order = 1,
            probDist = "Uniform", probParam1 = .01, probParam2 = .12)
    private double a = .02;

    /**
     * B.
     */
    @UserParameter(
            label = "B",
            description = "Parameter for recovery variable.",
            increment = .01,
            order = 2, useSetter = true,
            probDist = "Uniform", probParam1 = .15, probParam2 = .3)
    private double b = .2;

    /**
     * C.
     */
    @UserParameter(
            label = "C",
            description = "The value for v which occurs after a spike.",
            increment = .01,
            order = 3,
            probDist = "Uniform", probParam1 = -70, probParam2 = -45)
    private double c = -65;

    /**
     * D.
     */
    @UserParameter(
            label = "D",
            description = "A constant value added to u after spikes.",
            increment = .01,
            order = 4,
            probDist = "Uniform", probParam1 = 0.02, probParam2 = 10)
    private double d = 8;

    /**
     * Constant background current.
     */
    @UserParameter(
            label = "I bkgd",
            description = "Constant background current.",
            increment = .1,
            order = 5)
    private double iBg = 14;

    /**
     * Threshold value to signal a spike.
     */
    private double threshold = 30;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = new UniformRealDistribution();

    /**
     * Add noise to the neuron.
     */
    private boolean addNoise;

    /**
     * An optional absolute refractory period. In many simulations this
     * promotes network stability.
     */
    private double refractoryPeriod = 0.0; //ms

    //TODO
    private double timeStep;
    private double val;

    @Override
    public IzhikevichRule deepCopy() {
        IzhikevichRule in = new IzhikevichRule();
        in.setA(getA());
        in.setB(getB());
        in.setC(getC());
        in.setD(getD());
        in.setiBg(getiBg());
        in.setAddNoise(getAddNoise());
        in.noiseGenerator = noiseGenerator.deepCopy();
        return in;
    }

    @Override
    public void apply(Neuron neuron, ScalarDataHolder data) {
        timeStep = neuron.getNetwork().getTimeStep();
        final double activation = neuron.getActivation();
        double inputs = 0;
        inputs = neuron.getInput();
        if (addNoise) {
            inputs += noiseGenerator.sampleDouble();
        }
        inputs += iBg;
        recovery += (timeStep * (a * ((b * activation) - recovery)));

        val = activation + (timeStep * (((.04 * (activation * activation)) + (5 * activation) + 140) - recovery + inputs));

        if (val >= threshold) {
            val = c;
            recovery += d;
            neuron.setSpike(true);
        } else {
            neuron.setSpike(false);
        }

        neuron.setActivation(val);
    }

    @Override
    public double getRandomValue() {
        // Equal chance of spiking or not spiking, taking on any value between
        // the resting potential and the threshold if not.
        return 2 * (threshold - c) * Math.random() + c;
    }

    public double getA() {
        return a;
    }

    public void setA(final double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(final double b) {
        this.b = b;
    }

    public double getC() {
        return c;
    }

    public void setC(final double c) {
        this.c = c;
    }

    public double getD() {
        return d;
    }

    public void setD(final double d) {
        this.d = d;
    }

    public double getiBg() {
        return iBg;
    }

    public void setiBg(double iBg) {
        this.iBg = iBg;
    }

    public boolean getAddNoise() {
        return addNoise;
    }

    public void setAddNoise(final boolean addNoise) {
        this.addNoise = addNoise;
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
    public String getName() {
        return "Izhikevich";
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getRefractoryPeriod() {
        return refractoryPeriod;
    }

    public void setRefractoryPeriod(double refractoryPeriod) {
        this.refractoryPeriod = refractoryPeriod;
    }

    public double getGraphicalUpperBound() {
        return threshold;
    }

    public double getGraphicalLowerBound() {
        return c;
    }

}
