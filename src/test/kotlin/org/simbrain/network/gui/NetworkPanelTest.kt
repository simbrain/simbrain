package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.ScreenElement
import org.junit.jupiter.api.Test
import org.simbrain.plot.awaitUntil
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.nodes.SynapseNode

class NetworkPanelTest {
    @Test
    fun testAddingScreenElements() {
        runBlocking {
            val net = Network()
            val nc = NetworkComponent("Test", net)
            val np = NetworkPanel(nc)
            val n1 = Neuron()
            val n2 = Neuron()
            net.addNetworkModels(n1, n2)
            assertEquals(2, np.screenElements.size)
        }
    }

    @Test
    fun `applying sparse connection strategy multiple times should not create fake synapse nodes`() {
        runBlocking {
            val net = Network()
            val nc = NetworkComponent("Test", net)
            val np = NetworkPanel(nc)
            
            // Create two neuron groups with 10 neurons each
            val sourceGroup = NeuronCollection(List(10) { Neuron() }.also { net.addNetworkModels(it) })
                .apply { net.addNetworkModel(this) }
            val targetGroup = NeuronCollection(List(10) { Neuron() }.also { net.addNetworkModels(it) })
                .apply { net.addNetworkModel(this) }
            
            // Create a SynapseGroup with Sparse connection strategy
            val sparse = Sparse().apply { 
                connectionDensity = 0.1
                allowSelfConnection = true
            }
            val synapseGroup = SynapseGroup(sourceGroup, targetGroup, sparse)
            net.addNetworkModel(synapseGroup)
            
            // Initial state: 10% of 10x10 = 10 synapses
            assertEquals(10, synapseGroup.size())
            val initialSynapseNodes = np.filterScreenElements<SynapseNode>().size
            
            // Change density to 100% and apply connection strategy
            sparse.connectionDensity = 1.0
            synapseGroup.applyConnectionStrategy()
            
            // Should now have 100 synapses in the model
            assertEquals(100, synapseGroup.size())
            
            // Change density to 1% and apply connection strategy  
            sparse.connectionDensity = 0.01
            synapseGroup.applyConnectionStrategy()
            
            // Should now have 1 synapse in the model
            assertEquals(1, synapseGroup.size())
            
            // Node creation and removal both arrive through asynchronous events, so wait for the canvas to settle
            fun validSynapseNodes() = np.filterScreenElements<SynapseNode>().filter { it.synapse in synapseGroup.synapses }
            awaitUntil(message = "one node for the surviving synapse and none for deleted ones") {
                validSynapseNodes().size == 1 && np.filterScreenElements<SynapseNode>().size == 1
            }
            
            // Repeat the cycle to show the bug gets worse
            sparse.connectionDensity = 1.0
            synapseGroup.applyConnectionStrategy()
            sparse.connectionDensity = 0.01
            synapseGroup.applyConnectionStrategy()
            
            // Should still have 1 synapse in the model
            assertEquals(1, synapseGroup.size())
            
            awaitUntil(message = "after the second cycle, still one node for the surviving synapse") {
                validSynapseNodes().size == 1 && np.filterScreenElements<SynapseNode>().size == 1
            }
        }
    }

    @Test
    fun `a weight matrix added before its arrays gets its node without freezing the panel`() = runBlocking {
        val net = Network()
        val nc = NetworkComponent("Test", net)
        val np = NetworkPanel(nc)
        val source = NeuronArray(3)
        val target = NeuronArray(2)
        val weightMatrix = WeightMatrix(source, target)

        val start = System.nanoTime()
        // the matrix's node creation starts first and must wait for the array nodes rather than block on them
        val matrixAdded = net.addNetworkModelAsync(weightMatrix)
        net.addNetworkModels(source, target)
        matrixAdded?.await()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertNotNull(np.modelNodeMap.getImmediately<ScreenElement>(weightMatrix))
        assertNotNull(np.modelNodeMap.getImmediately<ScreenElement>(source))
        assertTrue(elapsedMs < 5000, "node creation should not wait out the blocking lookup's timeout, took $elapsedMs ms")
    }
}
