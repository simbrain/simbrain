package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.point

class FixedDegreeTest {

    var net = Network()
    lateinit var n1: Neuron
    lateinit var n2: Neuron
    lateinit var n3: Neuron
    lateinit var conn: FixedDegree

    @BeforeEach
    fun setUp() {
        conn = FixedDegree()
        n1 = Neuron();
        n1.location = point(0, 0)
        n2 = Neuron();
        n2.location = point(0, 50)
        n3 = Neuron();
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

}