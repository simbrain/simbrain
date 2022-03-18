package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.point

class RadialSimpleTest {

    var net = Network()
    lateinit var n1: Neuron
    lateinit var n2: Neuron
    lateinit var n3: Neuron
    lateinit var rs: RadialSimple

    @BeforeEach
    fun setUp() {
        rs = RadialSimple()
        rs.conMethod = RadialSimple.ConnectStyle.DETERMINISTIC
        rs.selectMethod = SelectionStyle.IN
        rs.excitatoryRadius = 60.0
        rs.inhibitoryRadius = 60.0

        n1 = Neuron(net);
        n1.location = point(0, 0)
        n2 = Neuron(net);
        n2.location = point(0, 50)
        n3 = Neuron(net);
        n3.location = point(0, 100)
        net.addNetworkModels(listOf(n1, n2, n3))
    }

    @Test
    fun `check deterministic-in produces correct number of weights`() {
        rs.excCons = 1.0
        rs.inhCons = 0.0
        val syns = rs.connectNeurons(net, listOf(n1, n2, n3), listOf(n1, n2, n3))
        assertEquals(3, syns.size)
    }

    @Test
    fun `check deterministic-in produces excitatory weights`() {
        rs.excCons = 1.0
        rs.inhCons = 0.0
        val syns = rs.connectNeurons(net, listOf(n1, n2, n3), listOf(n1, n2, n3))
        assertTrue(syns[0].strength > 0.0)
    }

    @Test
    fun `check deterministic-in produces inhibitory weights`() {
        rs.excCons = 0.0
        rs.inhCons = 1.0
        val syns = rs.connectNeurons(net, listOf(n1, n2, n3), listOf(n1, n2, n3))
        print(net)
        assertTrue(syns[0].strength < 0.0)
    }
}