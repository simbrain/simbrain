package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class SparseTest {

    var net = Network()
    lateinit var sparse: Sparse
    
    @BeforeEach
    fun setUp() {
        sparse = Sparse()
        net.addNetworkModels(List(10) { Neuron() })
    }

    // TODO: Check equalize efferents
    // TODO: Check cases of source and target being different

    @Test
    fun `check correct number of weights are created`() {
        sparse.connectionDensity = .1
        sparse.allowSelfConnection = true
        val syns = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList())
        assertEquals(10, syns.size)
    }

    @Test
    fun `check for case of many to one`() {
        sparse.connectionDensity = .1
        sparse.allowSelfConnection = true
        val syns = sparse.connectNeurons(net.freeNeurons.toList(), listOf(net.freeNeurons.first()))
        assertEquals(1, syns.size)
    }

    @Test
    fun `check adding and removing synapses`() {

        // Add weights
        sparse.connectionDensity = .1
        sparse.allowSelfConnection = true
        val syns1 = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModels(it) }

        // Up sparsity to 20% and add more weights.
        sparse.connectionDensity = .2
        val syns2 = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModels(it) }
        assertEquals(10, syns2.size) // Only the new synapses are return
        assertEquals(20, net.freeSynapses.size)
        // All the originally added synapses should still be there
        assertTrue(syns1.all { net.freeSynapses.contains(it) })

        // Reduce sparsity to 5%
        sparse.connectionDensity = .05
        sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModels(it) }
        assertEquals(5, net.freeSynapses.size)
        // All the originally added synapses should still be there
        assertTrue(net.freeSynapses.all { (syns1 + syns2).contains(it) })

    }

    @Test
    fun `check calculation of density based on source and target`() {

        // Add "recurrent" weights
        sparse.connectionDensity = .1
        sparse.allowSelfConnection = true
        val syns1 = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModels(it) }

        val newNeurons = listOf(Neuron())
        val syns2 = sparse.connectNeurons(net.freeNeurons.toList(), newNeurons).also { net.addNetworkModels(it) }

        assertEquals(1, syns2.size)

    }

}