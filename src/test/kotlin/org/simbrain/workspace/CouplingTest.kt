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

    private val couplingManager
        get() = workspace.couplingManager

    /**
     * Create a network and "also" add it to the workspace.
     */
    private val network = Network().also { workspace.addWorkspaceComponent(NetworkComponent("net1", it)) }

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

    @Test
    fun `test many to one`() {
        val neuron1 = Neuron(network)
        val neuron2 = Neuron(network)
        network.addLooseNeuron(neuron1)
        network.addLooseNeuron(neuron2)
        neuron1.forceSetActivation(.5)
        neuron2.forceSetActivation(-.2)

        // Add a second network with a neuron
        val network2 = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net2", network2))
        val neuron3 = Neuron(network2)
        network2.addLooseNeuron(neuron3)

        // Now couple them
        couplingManager.createCoupling(neuron1.getProducer("getActivation"),
                neuron3.getConsumer("addInputValue"));
        couplingManager.createCoupling(neuron2.getProducer("getActivation"),
                neuron3.getConsumer("addInputValue"));

        // We expect neuron 3 to have  value of 1 after update
        workspace.simpleIterate()
        assertEquals(.3, neuron3.activation, 0.0001)
    }

    @Test
    fun `test one to many`() {
        val neuron1 = Neuron(network)
        network.addLooseNeuron(neuron1)
        neuron1.forceSetActivation(.5)

        // Add a second network with a neuron
        val network2 = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net2", network2))
        val neuron2 = Neuron(network2)
        val neuron3 = Neuron(network2)
        network2.addLooseNeuron(neuron2)
        network2.addLooseNeuron(neuron3)

        // Now couple them
        couplingManager.createCoupling(neuron1.getProducer("getActivation"),
                neuron2.getConsumer("setInputValue"));
        couplingManager.createCoupling(neuron1.getProducer("getActivation"),
                neuron3.getConsumer("setInputValue"));

        // We expect neurons 2 and 3 to have  value of .5 after update
        workspace.simpleIterate()
        assertEquals(.5, neuron2.activation, 0.0001)
        assertEquals(.5, neuron3.activation, 0.0001)
    }
}
