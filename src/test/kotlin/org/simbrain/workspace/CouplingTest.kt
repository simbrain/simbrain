package org.simbrain.workspace

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.simbrain.iterateAndRun
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.diff

class CouplingTest {

    val workspace = Workspace()

    val networkComponent = NetworkComponent("net1", Network())

    init {
        workspace.addWorkspaceComponent(networkComponent)
    }

    @Test
    fun `check if all producers are created on a neuron`() {
        val neuron = Neuron(networkComponent.network)
        networkComponent.network.addLooseNeuron(neuron)
        networkComponent.couplingManager.run {
            val expected = setOf("getLabel", "getActivation")
            val actual = neuron.producers.map { it.method.name }.toSet()
            val diff = expected diff actual
            assertTrue("$diff", diff.isIdentical())
        }
    }

    @Test
    fun `check if all consumers are created on a neuron`() {
        val neuron = Neuron(networkComponent.network)
        networkComponent.network.addLooseNeuron(neuron)
        networkComponent.couplingManager.run {
            val expected = setOf("setActivation", "forceSetActivation", "setInputValue", "addInputValue", "setLabel")
            val actual = neuron.consumers.map { it.method.name }.toSet()
            val diff = expected diff actual

            assertTrue("$diff", diff.isIdentical())
        }
    }

    @Test
    fun `check neuron getActivation coupling with neuron forceSetActivation`() = runBlocking {
        val neuron1 = Neuron(networkComponent.network)
        val neuron2 = Neuron(networkComponent.network)
        networkComponent.network.apply {
            addLooseNeuron(neuron1)
            addLooseNeuron(neuron2)
        }
        networkComponent.couplingManager.run {
            neuron1.producerByName("getActivation") couple neuron2.consumerByName("forceSetActivation")
        }
        neuron1.activation = 1.0
        neuron2.isClamped = true
        workspace.iterateAndRun { assertEquals(1.0, neuron2.activation, 0.01) }
    }
}