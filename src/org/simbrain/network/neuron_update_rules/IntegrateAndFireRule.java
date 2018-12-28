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
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * Linear <b>IntegrateAndFireNeuron</b> implements an integrate and fire neuron.
 * Parameters taken from recordings of rat cortex from: Maass (2002) Real Time
 * Computing Without Stable States: A new framework for neural computations
 * based on perturbations.
 *
 * Graphical upper and lower bounds is currently set to so that the 0 is halfway
 * between its reset potential and firing threshold.
 *
 * @author Zoë Tosi
 * <p>
 * TODO: Add custom tooltip
 */
public class IntegrateAndFireRule extends SpikingNeuronUpdateRule implements NoisyUpdateRule {

    /**
     * Resistance (M ohms).
     */
    @UserParameter(
            label = "Resistance (MΩ)",
            description = "The resistance across the cell's membrane determines how much of an effect "
                    + "currents have of the membrane potential.",
            defaultValue = "1", order = 4)
    private double resistance = 1;

    /**
     * Time constant (ms)
     */
    @UserParameter(
            label = "Time-Constant (ms)",
            description = "How quickly/slowly the neuron responds to external change and returns to its "
                    + "resting potential.",
            defaultValue = "30", order = 6)
    private double timeConstant = 30;

    /**
     * Threshold (mV)
     */
    @UserParameter(
            label = "Threshold (mV)",
            description = "The value of the membrane potential that if met or exceeded triggers an "
                    + "action-potential as well as the onset of the refractory period.",
            defaultValue = "15", order = 1)
    private double threshold = 15;

    /**
     * Reset potential (mV)
     */
    @UserParameter(
            label = "Reset Potential (mV)",
            description = "The value of the membrane potential to which it is set and held at immediately "
                    + "after firing an action potential.",
            defaultValue = "13.5", order = 2)
    private double resetPotential = 13.5;

    /**
     * Resting potential (mV) Default: 0.0
     */
    @UserParameter(
            label = "Resting potential (mV)",
            description = "In the absence of further perturbation, the voltage will exponentially return "
                    + "to this value.",
            defaultValue = "0.0", order = 3)
    private double restingPotential = 0.0;

    /**
     * Background Current (nA) .
     */
    @UserParameter(
            label = "Background Current (nA)",
            description = "A constant background current to the neuron.",
            defaultValue = "13.5", order = 5)
    private double backgroundCurrent = 13.5;

    /**
     * Refractory Period (ms) .
     */
    @UserParameter(
        label = "Refractory Period (ms)",
        description = "The period of time after a spike during which a neuron will not fire and rejects external input",
        order = 7)
    private double refractoryPeriod = 3;

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Add noise to neuron.
     */
    private boolean addNoise;

    /**
     * Membrane potential for the neuron.
     */
    private double memPotential = 0;

    @Override
    public IntegrateAndFireRule deepCopy() {
        IntegrateAndFireRule ifn = new IntegrateAndFireRule();
        ifn.setRestingPotential(getRestingPotential());
        ifn.setResetPotential(getResetPotential());
        ifn.setThreshold(getThreshold());
        ifn.setBackgroundCurrent(getBackgroundCurrent());
        ifn.setTimeConstant(getTimeConstant());
        ifn.setResistance(getResistance());
        ifn.setAddNoise(getAddNoise());
        ifn.noiseGenerator = noiseGenerator.deepCopy();
        return ifn;
    }

    @Override
    public void update(Neuron neuron) {

        // Incoming current is 0 during the refractory period, otherwise it's
        // equal to input and background current
        double synCurrent = neuron.getNetwork().getTime() < (getLastSpikeTime() + refractoryPeriod) ? 0 : neuron.getInput() + backgroundCurrent;

        if (addNoise) {
            synCurrent += noiseGenerator.getRandom();
        }

        double timeStep = neuron.getNetwork().getTimeStep();

        memPotential = neuron.getActivation();

        /*
         * Formula:
         *
         * dV/dt = ( -(Vm - Vr) + Rm * (Isyn + Ibg) ) / tau
         *
         * Vm > theta ? Vm <- Vreset ; spike
         *
         * Vm: membrane potential Vr: resting potential* Rm: membrane resistance
         * Isyn: synaptic input current Ibg: background input current tau: time
         * constant Vreset: reset potential theta: threshold
         */
        double dVm = timeStep * (-(memPotential - restingPotential) + resistance * synCurrent) / timeConstant;

        memPotential += dVm;

        if ((memPotential >= threshold) && (neuron.getNetwork().getTime() > (getLastSpikeTime() + refractoryPeriod))) {
            neuron.setSpkBuffer(true);
            setHasSpiked(true, neuron);
            memPotential = resetPotential;
        } else {
            neuron.setSpkBuffer(false);
            setHasSpiked(false, neuron);
        }

        neuron.setBuffer(memPotential);
    }

    @Override
    public double getRandomValue() {
        // Equal chance of spiking or not spiking, taking on any value between
        // the resting potential and the threshold if not.
        return 2 * (threshold - restingPotential) * Math.random() + restingPotential;
    }

    public double getRestingPotential() {
        return restingPotential;
    }

    public void setRestingPotential(final double restingPotential) {
        this.restingPotential = restingPotential;
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(final double resistance) {
        this.resistance = resistance;
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

    public double getResetPotential() {
        return resetPotential;
    }

    public void setResetPotential(final double resetPotential) {
        this.resetPotential = resetPotential;
    }

    public double getBackgroundCurrent() {
        return backgroundCurrent;
    }

    public void setBackgroundCurrent(double backgroundCurrent) {
        this.backgroundCurrent = backgroundCurrent;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    public double getTimeConstant() {
        return timeConstant;
    }

    public void setTimeConstant(final double timeConstant) {
        this.timeConstant = timeConstant;
    }

    @Override
    public String getName() {
        return "Integrate and Fire";
    }

    // An alternative here would be to have reset potential be the zero point
    // so that colors would track hyper and de-polarization. That could be
    // achieved by resetPotential-(resetPotential-threshold)
    @Override
    public double getGraphicalLowerBound() {
        return resetPotential;
    }

    @Override
    public double getGraphicalUpperBound() {
        return threshold;
    }

    public double getRefractoryPeriod() {
        return refractoryPeriod;
    }

    public void setRefractoryPeriod(double refractoryPeriod) {
        this.refractoryPeriod = refractoryPeriod;
    }
}