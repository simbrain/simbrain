package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class AllToAllTest {

    var net = Network()
    lateinit var n1: Neuron
    lateinit var n2: Neuron
    lateinit var conn: AllToAll
    
    @BeforeEach
    fun setUp() {
        conn = AllToAll()
        n1 = Neuron(net);
        n2 = Neuron(net);
        net.addNetworkModels(listOf(n1, n2))
    }

    @Test
    fun `check correct number of weights are created`() {
        conn.allowSelfConnection = false
        val syns = conn.connectNeurons(net, listOf(n1, n2), listOf(n1, n2))
        assertEquals(2, syns.size)
    }

    @Test
    fun `check self-connections are created`() {
        conn.allowSelfConnection = true
        val syns = conn.connectNeurons(net, listOf(n1, n2), listOf(n1, n2))
        assertEquals(4, syns.size)
    }

}