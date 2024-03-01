package org.simbrain.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.IntegrateAndFireRule

class NetworkTimeTests {

    val net = Network()
    val n1 = Neuron()
    val n2 = Neuron()

    init {
        net.addNetworkModelsAsync(n1, n2)
    }

    @Test
    fun `test iteration updates and reset`() {
        assertEquals(0, net.iterations)
        repeat(2) {
            net.update()
        }
        assertEquals(2, net.iterations)
        net.resetTime()
        assertEquals(0, net.iterations)
    }

    @Test
    fun `test continuous time iterations and reset`() {
        net.timeStep = .1
        assertEquals(0.0, net.time)
        repeat(2) {
            net.update()
        }
        assertEquals(.2, net.time)
        net.resetTime()
        assertEquals(0.0, net.time)
    }

    @Test
    fun `test custom time step`() {
        net.timeStep = .2
        repeat(2) {
            net.update()
        }
        assertEquals(.4, net.time)
    }

    @Test
    fun `test automatic change to continuous with continuous rule`() {
        net.timeType = Network.TimeType.DISCRETE
        assertTrue(net.timeType == Network.TimeType.DISCRETE)
        n1.updateRule = IntegrateAndFireRule()
        assertTrue(net.timeType == Network.TimeType.CONTINUOUS)
        net.timeType = Network.TimeType.DISCRETE
        assertTrue(net.timeType == Network.TimeType.DISCRETE)
    }

}