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
package org.simbrain.network.updaterules

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SpikingNeuronUpdateRule
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils.round
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution

/**
 * Linear **IntegrateAndFireNeuron** implements an integrate and fire neuron.
 * Parameters taken from recordings of rat cortex from: Maass (2002) Real Time
 * Computing Without Stable States: A new framework for neural computations
 * based on perturbations.
 *
 * Graphical upper and lower bounds is currently set to so that the 0 is halfway
 * between its reset potential and firing threshold.
 *
 * @author Zoë Tosi
 *
 */
open class IntegrateAndFireRule : SpikingNeuronUpdateRule(), NoisyUpdateRule {

    @UserParameter(
        label = "Resistance (MΩ)",
        description = "The resistance across the cell's membrane determines how much of an effect "
                + "currents have of the membrane potential.",
        increment = .1,
        order = 4
    )
    var resistance = 1.0

    /**
     * Time constant (ms)
     */
    @UserParameter(
        label = "Time-Constant (ms)",
        description = "How quickly/slowly the neuron responds to external change and returns to its "
                + "resting potential. Roughly: how long to decay to resting state. Smaller time constant > faster" +
                " decay to 0",
        increment = .1,
        order = 6,
        minimumValue = 1.0,
        probDist = "Normal",
        probParam1 = 30.0,
        probParam2 = 2.5
    )
    var timeConstant = 30.0

    /**
     * Threshold (mV)
     */
    @UserParameter(
        label = "Threshold (mV)",
        description = "The value of the membrane potential that if met or exceeded triggers an "
                + "action-potential as well as the onset of the refractory period.",
        increment = .1,
        order = 1
    )
    var threshold = -50.0

    /**
     * Reset potential (mV)
     */
    @UserParameter(
        label = "Reset Potential (mV)",
        description = "The value of the membrane potential to which it is set and held at immediately "
                + "after firing an action potential.",
        order = 2
    )
    var resetPotential = -55.0

    /**
     * Resting potential (mV) Default: 0.0
     */
    @UserParameter(
        label = "Resting potential (mV)",
        description = "In the absence of further perturbation, the voltage will exponentially return "
                + "to this value.",
        increment = .1,
        order = 3
    )
    var restingPotential = -70.0

    /**
     * Background Current (nA) .
     */
    @UserParameter(
        label = "Background Current (nA)",
        description = "A constant background current to the neuron.",
        increment = .1,
        order = 5
    )
    var backgroundCurrent = 17.5

    /**
     * Refractory Period (ms) .
     */
    @UserParameter(
        label = "Refractory Period (ms)",
        description = "The period of time after a spike during which a neuron will not fire and rejects external input",
        increment = .1,
        order = 7
    )
    var refractoryPeriod = 3.0

    /**
     * Noise generator.
     */
    private var noiseGenerator: ProbabilityDistribution = UniformRealDistribution()

    /**
     * Add noise to neuron.
     */
    private var addNoise = false

    override fun deepCopy(): IntegrateAndFireRule {
        val ifn = IntegrateAndFireRule()
        ifn.restingPotential = restingPotential
        ifn.resetPotential = resetPotential
        ifn.threshold = threshold
        ifn.backgroundCurrent = backgroundCurrent
        ifn.timeConstant = timeConstant
        ifn.resistance = resistance
        ifn.setAddNoise(getAddNoise())
        ifn.noiseGenerator = noiseGenerator.deepCopy()
        return ifn
    }

    /*
    * dV/dt = ( -(Vm - Vr) + Rm * (Isyn + Ibg) ) / tau
    * Vm > theta ? Vm <- Vreset ; spike
    *
    * Vm: membrane potential Vr: resting potential* Rm: membrane resistance
    * Isyn: synaptic input current Ibg: background input current tau: time
    * constant Vreset: reset potential theta: threshold
    */
    override fun apply(neuron: Neuron, data: ScalarDataHolder) {

        // Incoming current is 0 during the refractory period, otherwise it's
        // equal to input and background current
        var synCurrent: Double =
            if (neuron.network.time < lastSpikeTime + refractoryPeriod) {
                // println("Refractory")
                0.0
            } else {
                // println("syncurrent = ${neuron.input + backgroundCurrent}")
                neuron.input + backgroundCurrent
            }

        if (addNoise) {
            synCurrent += noiseGenerator.sampleDouble()
        }

        var memPotential = neuron.activation
        val dVm =
            neuron.network.timeStep * (-(memPotential - restingPotential) + resistance * synCurrent) / timeConstant
        memPotential += dVm

        // if(ThreadLocalRandom.current().nextDouble() < randSpkChance*neuron.getNetwork().getTimeStep()) {
        //     ((IntFireScalarData)data).setMembranePotential(threshold + 1);
        // }

        if (memPotential >= threshold && neuron.network.time > lastSpikeTime + refractoryPeriod) {
            neuron.isSpike = true
            // println("Spike!")
            setHasSpiked(true, neuron)
            memPotential = resetPotential
        } else {
            neuron.isSpike = false
            setHasSpiked(false, neuron)
        }

        neuron.activation = memPotential

    }

    override fun getToolTipText(neuron: Neuron): String {
        return "${neuron.id}  Location: ( ${neuron.x.toInt()}, ${neuron.x.toInt()}). " +
                "Activation (Membrane Potential): ${round(neuron.activation, 3)}"
    }

    override fun getRandomValue(): Double {
        // Equal chance of spiking or not spiking, taking on any value between
        // the resting potential and the threshold if not.
        return 2 * (threshold - restingPotential) * Math.random() + restingPotential
    }

    override fun getAddNoise(): Boolean {
        return addNoise
    }

    override fun setAddNoise(addNoise: Boolean) {
        this.addNoise = addNoise
    }

    override fun getNoiseGenerator(): ProbabilityDistribution {
        return noiseGenerator
    }

    override fun setNoiseGenerator(noise: ProbabilityDistribution) {
        noiseGenerator = noise
    }

    override fun getName(): String {
        return "Integrate and Fire"
    }

    // An alternative here would be to have reset potential be the zero point
    // so that colors would track hyper and de-polarization. That could be
    // achieved by resetPotential-(resetPotential-threshold)
    override fun getGraphicalLowerBound(): Double {
        return resetPotential
    }

    override fun getGraphicalUpperBound(): Double {
        return threshold
    }
}