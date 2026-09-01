/**
 * Covers the data-holder-backed leaky integration of [ContinuousSigmoidalRule], the squash-time output
 * bias, and serialization of the integrated state.
 */
package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.util.EmptyScalarData
import kotlin.math.exp

class ContinuousSigmoidalRuleTest {

    private fun fittedStyleRule() = ContinuousSigmoidalRule().apply {
        timeConstant = 1.0
        leakConstant = 1.0
        slope = 0.25
        outputBias = 2.02
    }

    @Test
    fun `euler integration with output bias matches the closed form recurrence`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = Neuron(fittedStyleRule())
        neuron.bias = 0.7
        net.addNetworkModelsAsync(neuron)

        var expectedNetActivation = 0.0
        repeat(200) {
            net.update()
            expectedNetActivation = expectedNetActivation * (1 - 0.1) + 0.1 * 0.7
            assertEquals(expectedNetActivation, (neuron.dataHolder as ContinuousSigmoidalData).netActivation, 1e-12)
            assertEquals(1.0 / (1.0 + exp(-(expectedNetActivation + 2.02))), neuron.activation, 1e-12)
        }
    }

    @Test
    fun `output bias shifts the squashing immediately rather than being integrated`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = Neuron(fittedStyleRule())
        net.addNetworkModelsAsync(neuron)

        net.update()

        assertEquals(0.0, (neuron.dataHolder as ContinuousSigmoidalData).netActivation, 0.0)
        assertEquals(1.0 / (1.0 + exp(-2.02)), neuron.activation, 1e-12)
    }

    @Test
    fun `membrane potential exposes the integrated net activation`() {
        val net = Network()
        net.timeStep = 0.1
        val rule = fittedStyleRule()
        val neuron = Neuron(rule)
        neuron.bias = 1.3
        net.addNetworkModelsAsync(neuron)

        repeat(50) { net.update() }

        assertEquals(
            (neuron.dataHolder as ContinuousSigmoidalData).netActivation,
            rule.membranePotential(neuron),
            0.0
        )
    }

    @Test
    fun `net activation survives an xstream round trip`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = Neuron(fittedStyleRule())
        neuron.bias = 0.5
        net.addNetworkModelsAsync(neuron)
        repeat(37) { net.update() }
        val savedNetActivation = (neuron.dataHolder as ContinuousSigmoidalData).netActivation

        val restored = getNetworkXStream().fromXML(getNetworkXStream().toXML(net)) as Network
        val restoredNeuron = restored.freeNeurons.single()

        val restoredRule = restoredNeuron.updateRule as ContinuousSigmoidalRule
        assertEquals(2.02, restoredRule.outputBias, 0.0)
        assertEquals(savedNetActivation, (restoredNeuron.dataHolder as ContinuousSigmoidalData).netActivation, 0.0)
    }

    @Test
    fun `a legacy save with a mismatched data holder is repaired on load`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = Neuron(fittedStyleRule())
        neuron.bias = 0.5
        net.addNetworkModelsAsync(neuron)
        neuron.dataHolder = EmptyScalarData

        val restored = getNetworkXStream().fromXML(getNetworkXStream().toXML(net)) as Network
        val restoredNeuron = restored.freeNeurons.single()

        assertTrue(restoredNeuron.dataHolder is ContinuousSigmoidalData, "the holder must be repaired to the rule's type")
        restored.update()
        assertTrue((restoredNeuron.dataHolder as ContinuousSigmoidalData).netActivation != 0.0)
    }

    @Test
    fun `clearing the neuron resets the integrated state`() {
        val net = Network()
        net.timeStep = 0.1
        val neuron = Neuron(fittedStyleRule())
        neuron.bias = 0.9
        net.addNetworkModelsAsync(neuron)
        repeat(20) { net.update() }

        neuron.clear()

        assertEquals(0.0, (neuron.dataHolder as ContinuousSigmoidalData).netActivation, 0.0)
    }

    @Test
    fun `copy carries the output bias and constants`() {
        val copy = fittedStyleRule().copy()

        assertEquals(1.0, copy.timeConstant, 0.0)
        assertEquals(1.0, copy.leakConstant, 0.0)
        assertEquals(0.25, copy.slope, 0.0)
        assertEquals(2.02, copy.outputBias, 0.0)
    }
}
