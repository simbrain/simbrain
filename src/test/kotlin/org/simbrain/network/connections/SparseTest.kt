package org.simbrain.network.connections

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.util.SimbrainConstants.Polarity

class SparseTest {

    private val net = Network()
    private val sparse: Sparse
    
    init {
        sparse = Sparse()
        net.addNetworkModelsAsync(List(10) { Neuron() })
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
        val syns1 = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModelsAsync(it) }

        // Up sparsity to 20% and add more weights.
        sparse.connectionDensity = .2
        val syns2 = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModelsAsync(it) }
        assertEquals(10, syns2.size) // Only the new synapses are return
        assertEquals(20, net.freeSynapses.size)
        // All the originally added synapses should still be there
        assertTrue(syns1.all { net.freeSynapses.contains(it) })

        // Reduce sparsity to 5%
        sparse.connectionDensity = .05
        sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModelsAsync(it) }
        assertEquals(5, net.freeSynapses.size)
        // All the originally added synapses should still be there
        assertTrue(net.freeSynapses.all { (syns1 + syns2).contains(it) })

    }

    @Test
    fun `check calculation of density based on source and target`() {

        // Add "recurrent" weights
        sparse.connectionDensity = .1
        sparse.allowSelfConnection = true
        val syns1 = sparse.connectNeurons(net.freeNeurons.toList(), net.freeNeurons.toList()).also { net.addNetworkModelsAsync(it) }

        val newNeurons = listOf(Neuron())
        val syns2 = sparse.connectNeurons(net.freeNeurons.toList(), newNeurons).also { net.addNetworkModelsAsync(it) }

        assertEquals(1, syns2.size)

    }

    @Test
    fun `equalizeEfferents matches connections with the correct density`() {
        runBlocking {
            with(net) {
                (0..10).map { it / 10.0 }.forEach { checkEqualFanouts(it) }
            }
        }
    }

    private suspend fun Network.checkEqualFanouts(density: Double) {
        val sparse = Sparse(connectionDensity = density, equalizeEfferents = true, allowSelfConnection = true)
        val neurons = addNeuronCollection(10).neuronList
        val syns = sparse.connectNeurons(neurons, neurons)
        val expectedSize = (neurons.size * density).toInt()
        assert(neurons.all { it.fanOut.size == expectedSize }) {
            "Expected $expectedSize synapses for each source neuron, but got ${neurons.map { it.fanOut.size }}"
        }
    }

    @Test
    fun `strategy created with the same seed should produce the same same pattern`() {
        assertStrategiesPatterns(
            net,
            Sparse(seed = 42L),
            Sparse(seed = 42L)
        )
    }


    @Test
    fun `strategy created with different seeds should produce different patterns`() {
        assertStrategiesPatterns(
            net,
            Sparse(seed = 42L),
            Sparse(seed = 43L),
            expectIdentical = false
        )
    }

    @Test
    fun `calling connectNeurons on the same strategy object should produce different patterns each time`() {
        val sparse = Sparse(seed = 42L)
        assertStrategiesPatterns(
            net,
            sparse,
            sparse,
            expectIdentical = false
        )
    }

    @Test
    fun `percentExcitatory should be respected with all BOTH polarity neurons`() {
        val sources = List(10) { Neuron() }
        val targets = List(10) { Neuron() }
        
        sparse.connectionDensity = 0.5
        sparse.allowSelfConnection = true
        
        sparse.percentExcitatory = 50.0
        val syns = sparse.connectNeurons(sources, targets)
        val excitatoryCount = syns.count { it.strength > 0 }
        val expectedExcitatory = (syns.size * 0.5).toInt()
        assertEquals(expectedExcitatory, excitatoryCount)
        
        sparse.percentExcitatory = 0.0
        val syns2 = sparse.connectNeurons(sources, targets)
        val excitatoryCount2 = syns2.count { it.strength > 0 }
        assertEquals(0, excitatoryCount2)
        
        sparse.percentExcitatory = 100.0
        val syns3 = sparse.connectNeurons(sources, targets)
        assertTrue(syns3.all { it.strength > 0 })
    }

    @Test
    fun `EXCITATORY neurons should always produce positive weights regardless of percentExcitatory`() {
        val sources = List(10) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val targets = List(10) { Neuron() }
        
        sparse.connectionDensity = 0.3
        sparse.percentExcitatory = 0.0
        val syns = sparse.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY neurons should always produce negative weights regardless of percentExcitatory`() {
        val sources = List(10) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val targets = List(10) { Neuron() }
        
        sparse.connectionDensity = 0.5
        sparse.allowSelfConnection = true
        sparse.percentExcitatory = 100.0
        val syns = sparse.connectNeurons(sources, targets)
        assertTrue(syns.isNotEmpty() && syns.all { it.strength < 0 }) {
            "All synapses from INHIBITORY neurons should be negative, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `mixed polarity neurons should respect both pre-polarized and BOTH neurons`() {
        val excitatoryNeurons = List(3) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val inhibitoryNeurons = List(3) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val bothNeurons = List(4) { Neuron().apply { polarity = Polarity.BOTH } }
        val sources = excitatoryNeurons + inhibitoryNeurons + bothNeurons
        val targets = List(10) { Neuron() }
        
        sparse.connectionDensity = 0.5
        sparse.percentExcitatory = 50.0
        val syns = sparse.connectNeurons(sources, targets)
        
        val excitatorySourceSyns = syns.filter { it.source in excitatoryNeurons }
        assertTrue(excitatorySourceSyns.all { it.strength > 0 })
        
        val inhibitorySourceSyns = syns.filter { it.source in inhibitoryNeurons }
        assertTrue(inhibitorySourceSyns.all { it.strength < 0 })
        
        val totalSyns = syns.size
        val expectedExcitatory = (totalSyns * 0.5).toInt()
        val actualExcitatory = syns.count { it.strength > 0 }
        assertEquals(expectedExcitatory, actualExcitatory)
    }

    @Test
    fun `with only polarized neurons percentExcitatory should be ignored`() {
        val excitatoryNeurons = List(5) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val inhibitoryNeurons = List(5) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val sources = excitatoryNeurons + inhibitoryNeurons
        val targets = List(10) { Neuron() }
        
        sparse.connectionDensity = 0.5
        sparse.allowSelfConnection = true
        sparse.percentExcitatory = 30.0
        val syns = sparse.connectNeurons(sources, targets)
        
        val excitatoryCount = syns.count { it.strength > 0 }
        val inhibitoryCount = syns.count { it.strength < 0 }
        assertTrue(excitatoryCount > 0 && inhibitoryCount > 0)
    }

}