package org.simbrain.network.gui

import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.neurongroups.NeuronGroup

class NetworkPanelTest {
    @Test
    fun testAddingScreenElements() {
        runBlocking {
            val net = Network()
            val nc = NetworkComponent("Test", net)
            val np = NetworkPanel(nc)
            val n1 = Neuron()
            val n2 = Neuron()
            net.addNetworkModels(n1, n2).awaitAll()
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
            val sourceGroup = NeuronGroup(List(10) { Neuron() }.also { net.addNetworkModels(it).awaitAll() })
                .apply { net.addNetworkModel(this)?.await() }
            val targetGroup = NeuronGroup(List(10) { Neuron() }.also { net.addNetworkModels(it).awaitAll() })
                .apply { net.addNetworkModel(this)?.await() }
            
            // Create a SynapseGroup with Sparse connection strategy
            val sparse = Sparse().apply { 
                connectionDensity = 0.1
                allowSelfConnection = true
            }
            val synapseGroup = SynapseGroup(sourceGroup, targetGroup, sparse)
            net.addNetworkModel(synapseGroup)?.await()
            
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
            
            // The bug: NetworkPanel should only have SynapseNodes for actual synapses in the network
            val actualSynapseNodes = np.filterScreenElements<SynapseNode>()
            val synapseNodesWithValidModels = actualSynapseNodes.filter { synapseNode ->
                // Check if this SynapseNode's synapse is actually in the current synapseGroup
                synapseGroup.synapses.contains(synapseNode.synapse)
            }
            
            // This should pass but currently fails due to the bug
            assertEquals(1, synapseNodesWithValidModels.size, 
                "Expected 1 SynapseNode with valid model, but found ${synapseNodesWithValidModels.size}. " +
                "Total SynapseNodes: ${actualSynapseNodes.size}")
            
            // Repeat the cycle to show the bug gets worse
            sparse.connectionDensity = 1.0
            synapseGroup.applyConnectionStrategy()
            sparse.connectionDensity = 0.01
            synapseGroup.applyConnectionStrategy()
            
            // Should still have 1 synapse in the model
            assertEquals(1, synapseGroup.size())
            
            // But now we should have even more fake SynapseNodes
            val finalSynapseNodes = np.filterScreenElements<SynapseNode>()
            val finalValidSynapseNodes = finalSynapseNodes.filter { synapseNode ->
                synapseGroup.synapses.contains(synapseNode.synapse)
            }
            
            assertEquals(1, finalValidSynapseNodes.size,
                "After second cycle: Expected 1 SynapseNode with valid model, but found ${finalValidSynapseNodes.size}. " +
                "Total SynapseNodes: ${finalSynapseNodes.size}")
        }
    }
}