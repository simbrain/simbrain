package org.simbrain.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.complement

class CouplingTest {

    private val workspace = Workspace()

    private val network = Network().also { workspace.addWorkspaceComponent(NetworkComponent("net1", it)) }

    private val couplingManager
        get() = workspace.couplingManager

    @Test
    fun `check if all producers are created on a neuron`() {
        val neuron = Neuron(network)
        network.addLooseNeuron(neuron)
        val expected = setOf("getLabel", "getActivation")
        val actual = neuron.producers.map { it.method.name }.toSet()
        val diff = expected complement actual // For error message if test fails
        assertTrue("$diff", diff.isIdentical())
    }

    @Test
    fun `check if all consumers are created on a neuron`() {
        val neuron = Neuron(network)
        network.addLooseNeuron(neuron)
        val expected = setOf("setActivation", "forceSetActivation", "setInputValue", "addInputValue", "setLabel")
        val actual = neuron.consumers.map { it.method.name }.toSet()
        val diff = expected complement actual
        assertTrue("$diff", diff.isIdentical())
    }

    @Test
    fun `check neuron getActivation coupling with neuron forceSetActivation`() {
        val neuron1 = Neuron(network)
        val neuron2 = Neuron(network)
        network.apply {
            addLooseNeuron(neuron1)
            addLooseNeuron(neuron2)
        }
        with(couplingManager) {
            neuron1.producerByName("getActivation") couple neuron2.consumerByName("forceSetActivation")
        }
        neuron1.activation = 1.0
        neuron2.isClamped = true
        workspace.simpleIterate()
        assertEquals(1.0, neuron2.activation, 0.01)
    }
}