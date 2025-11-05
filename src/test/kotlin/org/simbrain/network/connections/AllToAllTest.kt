package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.SimbrainConstants.Polarity

class AllToAllTest {

    private val net = Network()
    private val n1: Neuron
    private val n2: Neuron
    private val conn: AllToAll
    
    init {
        conn = AllToAll()
        n1 = Neuron()
        n2 = Neuron()
        net.addNetworkModelsAsync(listOf(n1, n2))
    }

    @Test
    fun `check correct number of weights are created`() {
        conn.allowSelfConnection = false
        val syns = conn.connectNeurons(listOf(n1, n2), listOf(n1, n2))
        assertEquals(2, syns.size)
    }

    @Test
    fun `check self-connections are created`() {
        conn.allowSelfConnection = true
        val syns = conn.connectNeurons(listOf(n1, n2), listOf(n1, n2))
        assertEquals(4, syns.size)
    }

    @Test
    fun `percentExcitatory should be respected with all BOTH polarity neurons`() {
        val sources = List(10) { Neuron() }
        val targets = List(10) { Neuron() }
        
        conn.percentExcitatory = 50.0
        val syns = conn.connectNeurons(sources, targets)
        val excitatoryCount = syns.count { it.strength > 0 }
        assertEquals(50, excitatoryCount)
        
        conn.percentExcitatory = 0.0
        val syns2 = conn.connectNeurons(sources, targets)
        val excitatoryCount2 = syns2.count { it.strength > 0 }
        assertEquals(0, excitatoryCount2)
        
        conn.percentExcitatory = 100.0
        val syns3 = conn.connectNeurons(sources, targets)
        val excitatoryCount3 = syns3.count { it.strength > 0 }
        assertEquals(100, excitatoryCount3)
    }

    @Test
    fun `EXCITATORY neurons should always produce positive weights regardless of percentExcitatory`() {
        val sources = List(5) { Neuron().apply { polarity = Polarity.EXCITATORY } }
        val targets = List(5) { Neuron() }
        
        conn.percentExcitatory = 0.0
        val syns = conn.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength > 0 }) {
            "All synapses from EXCITATORY neurons should be positive, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY neurons should always produce negative weights regardless of percentExcitatory`() {
        val sources = List(5) { Neuron().apply { polarity = Polarity.INHIBITORY } }
        val targets = List(5) { Neuron() }
        
        conn.percentExcitatory = 100.0
        val syns = conn.connectNeurons(sources, targets)
        assertTrue(syns.all { it.strength < 0 }) {
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
        
        conn.percentExcitatory = 50.0
        val syns = conn.connectNeurons(sources, targets)
        
        val excitatorySourceSyns = syns.filter { it.source in excitatoryNeurons }
        assertTrue(excitatorySourceSyns.all { it.strength > 0 })
        
        val inhibitorySourceSyns = syns.filter { it.source in inhibitoryNeurons }
        assertTrue(inhibitorySourceSyns.all { it.strength < 0 })
        
        val bothSourceSyns = syns.filter { it.source in bothNeurons }
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
        
        conn.percentExcitatory = 30.0
        val syns = conn.connectNeurons(sources, targets)
        
        val excitatoryCount = syns.count { it.strength > 0 }
        assertEquals(50, excitatoryCount)
    }

}