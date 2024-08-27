package org.simbrain.workspace


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.complement
import org.simbrain.workspace.couplings.Coupling

class CouplingTest {

    private val workspace = Workspace()

    private val couplingManager
        get() = workspace.couplingManager

    /**
     * Create a network and "also" add it to the workspace.
     */
    private val network = Network().also { workspace.addWorkspaceComponent(NetworkComponent("net1", it)) }

    @Test
    fun `ensure producers from multiple identical gets are equal`() {
        val neuron = Neuron()
        with(couplingManager) {
            val get1 = neuron.getProducer("getActivation")
            val get2 = neuron.getProducer("getActivation")
            assert(get1 == get2)
            assertEquals(1, setOf(get1, get2).size)
        }
    }

    @Test
    fun `ensure consumers from multiple identical gets are equal`() {
        val neuron = Neuron()
        with(couplingManager) {
            val get1 = neuron.getConsumer("setActivation")
            val get2 = neuron.getConsumer("setActivation")
            assert(get1 == get2)
            assertEquals(1, setOf(get1, get2).size)
        }
    }

    @Test
    fun `ensure couplings from multiple identical createCoupling calls are equal`() {
        val neuron = Neuron()
        with(couplingManager) {
            val producer = neuron.getProducer("getActivation")
            val consumer = neuron.getConsumer("addInputValue")
            val coupling1 = createCoupling(producer, consumer)
            val coupling2 = createCoupling(producer, consumer)
            assert(coupling1 == coupling2)
        }
    }

    @Test
    fun `check if createCoupling successfully creates coupling`() {
        val neuron = Neuron()
        with(couplingManager) {
            val producer = neuron.getProducer("getActivation")
            val consumer = neuron.getConsumer("addInputValue")
            createCoupling(producer, consumer)
            assert(couplings.isNotEmpty())
        }
    }

    @Test
    fun `ensure createCoupling does not create duplicate coupling`() {
        val neuron = Neuron()
        with(couplingManager) {
            val producer = neuron.getProducer("getActivation")
            val consumer = neuron.getConsumer("addInputValue")
            createCoupling(producer, consumer)
            createCoupling(producer, consumer)
            assertEquals(1, couplings.size)
        }
    }

    @Test
    fun `check if removeCoupling successfully removes coupling`() {
        val neuron = Neuron()
        with(couplingManager) {
            val producer = neuron.getProducer("getActivation")
            val consumer = neuron.getConsumer("addInputValue")
            val coupling = createCoupling(producer, consumer)
            removeCoupling(coupling)
            assert(couplings.isEmpty())
        }
    }

    @Test
    fun `check if removeCouplings successfully removes couplings`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        val neuron3 = Neuron()
        with(couplingManager) {
            fun neuronCoupling(source: Neuron, target: Neuron): Coupling {
                val producer = source.getProducer("getActivation")
                val consumer = target.getConsumer("addInputValue")
                return createCoupling(producer, consumer)
            }

            val coupling1 = neuronCoupling(neuron1, neuron2)
            val coupling2 = neuronCoupling(neuron2, neuron3)
            val coupling3 = neuronCoupling(neuron3, neuron1)

            removeCouplings(listOf(coupling1, coupling2))

            val expected = setOf(coupling3)
            val diff = expected complement couplings.toSet()

            assert(diff.isIdentical()) { "Diff: $diff" }
        }
    }

    @Test
    fun `check if removeAttributeContainer successfully removes corresponding couplings`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        val neuron3 = Neuron()
        with(couplingManager) {
            fun neuronCoupling(source: Neuron, target: Neuron): Coupling {
                val producer = source.getProducer("getActivation")
                val consumer = target.getConsumer("addInputValue")
                return createCoupling(producer, consumer)
            }

            val coupling1 = neuronCoupling(neuron1, neuron2)
            val coupling2 = neuronCoupling(neuron2, neuron3)
            val coupling3 = neuronCoupling(neuron3, neuron1)

            removeAttributeContainer(neuron1)

            val expected = setOf(coupling2)
            val diff = expected complement couplings.toSet()

            assert(diff.isIdentical()) { "Diff: $diff" }
        }
    }

    @Test
    fun `check if all producers are created on a neuron`() {
        val neuron = Neuron()
        network.addNetworkModel(neuron)
        val expected = setOf("getLabel", "getActivation", "getBias")
        val actual = with(couplingManager) { neuron.producers.map { it.method.name }.toSet() }
        val diff = expected complement actual // For error message if test fails
        assertTrue(diff.isIdentical(),"$diff")
    }

    @Test
    fun `check if all consumers are created on a neuron`() {
        val neuron = Neuron()
        network.addNetworkModel(neuron)
        val expected = setOf("setActivation", "addInputValue", "addInputValue", "setLabel", "setBias")
        val actual = with(couplingManager) { neuron.consumers.map { it.method.name }.toSet() }
        val diff = expected complement actual
        assertTrue(diff.isIdentical(),"$diff")
    }

    @Test
    fun `check neuron getActivation coupling with neuron setActivation`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        network.apply {
            addNetworkModel(neuron1)
            addNetworkModel(neuron2)
        }
        with(couplingManager) {
            neuron1.getProducer("getActivation") couple neuron2.getConsumer("setActivation")
        }
        neuron1.activation = 1.0
        neuron2.clamped = true
        workspace.simpleIterate()
        assertEquals(1.0, neuron2.activation, 0.01)
    }

    @Test
    fun `test many to one`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)
        neuron1.activation = .5
        neuron2.activation = -.2

        // Add a second network with a neuron
        val network2 = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net2", network2))
        val neuron3 = Neuron()
        network2.addNetworkModel(neuron3)

        // Now couple them
        with(couplingManager) {
            createCoupling(
                    neuron1.getProducer("getActivation"),
                    neuron3.getConsumer("addInputValue")
            )
            createCoupling(
                    neuron2.getProducer("getActivation"),
                    neuron3.getConsumer("addInputValue")
            )
        }

        // We expect neuron 3 to have value of .5-.2 = .3 after update
        workspace.simpleIterate()
        assertEquals(.3, neuron3.activation, 0.0)
    }

    @Test
    fun `test one to many`() {
        val neuron1 = Neuron()
        network.addNetworkModel(neuron1)
        neuron1.activation = .5

        // Add a second network with a neuron
        val network2 = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net2", network2))
        val neuron2 = Neuron()
        val neuron3 = Neuron()
        network2.addNetworkModel(neuron2)
        network2.addNetworkModel(neuron3)

        // Now couple them
        with(couplingManager) {
            couplingManager.createCoupling(
                    neuron1.getProducer("getActivation"),
                    neuron2.getConsumer("addInputValue")
            )
            couplingManager.createCoupling(
                    neuron1.getProducer("getActivation"),
                    neuron3.getConsumer("addInputValue")
            )
        }

        // We expect neurons 2 and 3 to have value of .5 after update
        workspace.simpleIterate()
        assertEquals(.5, neuron2.activation, 0.0)
        assertEquals(.5, neuron3.activation, 0.0)
    }
}
