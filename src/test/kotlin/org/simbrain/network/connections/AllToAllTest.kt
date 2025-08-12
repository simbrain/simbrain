package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

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

}