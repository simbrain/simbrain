/**
 * Covers [AfdThermoreceptorRule] against the thermotaxis reference model's AFD convolution, plus its
 * activation/graphical-value split, input isolation, priming, and copy/clear behavior.
 */
package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.neuroscience.ThermotaxisModel
import org.simbrain.custom_sims.simulations.neuroscience.fittedBiases
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addSynapseAsync
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

class AfdThermoreceptorRuleTest {

    private fun afdNeuron() = Neuron(AfdThermoreceptorRule())

    private fun afdState(neuron: Neuron) = (neuron.dataHolder as AfdScalarData).state

    @Test
    fun `state matches the reference model convolution over a temperature sweep`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = afdNeuron()
        net.addNetworkModelsAsync(neuron)
        val model = ThermotaxisModel(states = DoubleArray(5), biases = fittedBiases)

        repeat(3000) { step ->
            val temperature = 17.0 + 1.5 * sin(2.0 * PI * step * 0.1 / 60.0)
            neuron.setTemperatureInput(temperature)
            net.update()
            val expected = model.step(temperature).afdState
            assertEquals(expected, afdState(neuron), 1e-12, "AFD state diverged at step $step")
        }
    }

    @Test
    fun `activation is the squashed state while the graphical value is the raw state`() {
        val net = Network()
        net.timeStep = 0.1
        val rule = AfdThermoreceptorRule()
        val neuron = Neuron(rule)
        net.addNetworkModelsAsync(neuron)

        repeat(500) { step ->
            neuron.setTemperatureInput(16.0 + step * 0.01)
            net.update()
        }

        val state = afdState(neuron)
        assertTrue(abs(state) > 0.1, "warming should move the state off rest")
        assertEquals(1.0 / (1.0 + exp(-(state + 11.57))), neuron.activation, 1e-12)
        assertEquals(state, rule.getGraphicalValue(neuron), 0.0)
        assertEquals(state, rule.membranePotential(neuron), 0.0)
    }

    @Test
    fun `synaptic input does not perturb the state`() {
        val net = Network()
        net.timeStep = 0.1
        val isolated = afdNeuron()
        val bombarded = afdNeuron()
        val driver = Neuron().apply { activation = 1.0; clamped = true }
        net.addNetworkModelsAsync(isolated, bombarded, driver)
        net.addSynapseAsync(driver, bombarded) { strength = 10.0 }

        repeat(500) { step ->
            val temperature = 17.0 + sin(step * 0.05)
            isolated.setTemperatureInput(temperature)
            bombarded.setTemperatureInput(temperature)
            net.update()
        }

        assertEquals(afdState(isolated), afdState(bombarded), 0.0)
    }

    @Test
    fun `an unprimed rule holds its resting output`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = afdNeuron()
        net.addNetworkModelsAsync(neuron)

        repeat(50) { net.update() }

        assertEquals(0.0, afdState(neuron), 0.0)
        assertEquals(1.0 / (1.0 + exp(-11.57)), neuron.activation, 1e-12, "resting output is sigmoid of the bias")
    }

    @Test
    fun `clearing the neuron resets the state and priming`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = afdNeuron()
        net.addNetworkModelsAsync(neuron)
        repeat(200) { step ->
            neuron.setTemperatureInput(16.0 + step * 0.02)
            net.update()
        }

        neuron.clear()

        assertEquals(0.0, afdState(neuron), 0.0)
        net.update()
        assertEquals(0.0, afdState(neuron), 0.0, "an unprimed holder must stay at rest")
    }

    @Test
    fun `copy carries the fitted parameters`() {
        val rule = AfdThermoreceptorRule().apply {
            thresholdTemperature = 15.0
            dissociationConstant = 70.0
            hillCoefficient = 5.0
            outputBias = 10.0
        }

        val copy = rule.copy()

        assertEquals(15.0, copy.thresholdTemperature, 0.0)
        assertEquals(70.0, copy.dissociationConstant, 0.0)
        assertEquals(5.0, copy.hillCoefficient, 0.0)
        assertEquals(10.0, copy.outputBias, 0.0)
    }
}
