package org.simbrain.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.point

class LocatableModelKtTest {

    val net = Network()
    val n1 = Neuron(net)
    val n2 = Neuron(net)

    @Test
    fun `test location functions`() {
        n1.location = point(0, 0)
        n2.location = point(10, 10)
        val neurons = listOf(n1, n2)
        assertEquals(0.0, neurons.topLeftLocation.x, 0.01 )
        assertEquals(5.0, neurons.centerLocation.x, 0.01 )
    }

    @Test
    fun `test translation`() {
        n1.location = point(0, 0)
        n2.location = point(10, 10)
        val neurons = listOf(n1, n2)
        neurons.translate(10.0,10.0)
        assertEquals(10.0, neurons.topLeftLocation.x, 0.01 )
        assertEquals(15.0, neurons.centerLocation.x, 0.01 )
    }

    @Test
    fun `test moveTo`() {
        n1.location = point(0, 0)
        n2.location = point(10, 10)
        val neurons = listOf(n1, n2)
        neurons.translate(10.0,10.0)
        neurons.moveTo(point(0,0))
        assertEquals(0.0, neurons.topLeftLocation.x, 0.01 )
        assertEquals(5.0, neurons.centerLocation.x, 0.01 )
    }
}