package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.point

class FixedDegreeTest {

    private val net = Network()
    private val n1: Neuron
    private val n2: Neuron
    private val n3: Neuron
    private val conn: FixedDegree

    init {
        conn = FixedDegree()
        n1 = Neuron()
        n1.location = point(0, 0)
        n2 = Neuron()
        n2.location = point(0, 50)
        n3 = Neuron()
        n3.location = point(0, 100)
        net.addNetworkModelsAsync(listOf(n1, n2, n3))
    }

    @Test
    fun `check correct number of weights`() {
        conn.degree = 2
        val syns = conn.connectNeurons(listOf(n1), listOf(n1, n2, n3))
        assertEquals(2, syns.size)
        assertEquals(2, n1.fanIn.size)
    }

    @Test
    fun `check correct number of weights (fan out)`() {
        conn.degree = 2
        conn.direction = Direction.OUT
        val syns = conn.connectNeurons(listOf(n1), listOf(n1, n2, n3))
        assertEquals(2, syns.size)
        assertEquals(2, n1.fanOut.size)
    }

    @Test
    fun `check no self connection`() {
        conn.degree = 2
        conn.allowSelfConnections = false
        val syns = conn.connectNeurons(listOf(n1), listOf(n1, n2))
        assertEquals(1, syns.size)
    }

    @Test
    fun `check for self connections`() {
        conn.degree = 2
        conn.allowSelfConnections = true
        val syns = conn.connectNeurons(listOf(n1), listOf(n1))
        assertEquals(1, syns.size)
        assertEquals(syns[0], n1.fanIn[0])
    }

    @Test
    fun `check connections in a radius`() {
        conn.degree = 3
        conn.useRadius = true
        conn.radius = 200.0
        val syns = conn.connectNeurons(listOf(n1), listOf(n1, n2, n3))
        assertEquals(2, syns.size)
        assertEquals(2, n1.fanIn.size)
    }

    @Test
    fun `EXCITATORY source neurons with Direction IN should produce positive weights`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val fixedDegree = FixedDegree(degree = 3, direction = Direction.IN)
        
        fixedDegree.percentExcitatory = 0.0
        val syns = fixedDegree.connectNeurons(targets, sources)
        
        assertTrue(syns.all { it.strength > 0 }) {
            "All synapses FROM EXCITATORY neurons should be positive, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY source neurons with Direction IN should produce negative weights`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val fixedDegree = FixedDegree(degree = 3, direction = Direction.IN)
        
        fixedDegree.percentExcitatory = 100.0
        val syns = fixedDegree.connectNeurons(targets, sources)
        
        assertTrue(syns.all { it.strength < 0 }) {
            "All synapses FROM INHIBITORY neurons should be negative, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `EXCITATORY source neurons with Direction OUT should produce positive weights`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.EXCITATORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val fixedDegree = FixedDegree(degree = 3, direction = Direction.OUT)
        
        fixedDegree.percentExcitatory = 0.0
        val syns = fixedDegree.connectNeurons(sources, targets)
        
        assertTrue(syns.all { it.strength > 0 }) {
            "All synapses FROM EXCITATORY neurons should be positive, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `INHIBITORY source neurons with Direction OUT should produce negative weights`() {
        val sources = List(5) { i -> Neuron().apply {
            polarity = Polarity.INHIBITORY
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val fixedDegree = FixedDegree(degree = 3, direction = Direction.OUT)
        
        fixedDegree.percentExcitatory = 100.0
        val syns = fixedDegree.connectNeurons(sources, targets)
        
        assertTrue(syns.all { it.strength < 0 }) {
            "All synapses FROM INHIBITORY neurons should be negative, but found: ${syns.map { it.strength }}"
        }
    }

    @Test
    fun `percentExcitatory should work with BOTH polarity neurons`() {
        val sources = List(10) { i -> Neuron().apply {
            polarity = Polarity.BOTH
            x = i * 10.0
            y = 0.0
        }}
        val targets = List(10) { i -> Neuron().apply {
            x = i * 10.0
            y = 50.0
        }}
        val fixedDegree = FixedDegree(degree = 5, direction = Direction.OUT)
        
        fixedDegree.percentExcitatory = 50.0
        val syns = fixedDegree.connectNeurons(sources, targets)
        
        val excitatoryCount = syns.count { it.strength > 0 }
        val expectedExcitatory = (syns.size * 0.5).toInt()
        assertEquals(expectedExcitatory, excitatoryCount)
    }

}