package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.util.point

class PlacementManagerTest {

    val net = Network()
    val pm = PlacementManager()

    @Test
    fun `test default placements`() {

        // Place two neurons. They should be offset by default amount
        val n1 = Neuron(net)
        net.addNetworkModel(n1)
        pm.placeObject(n1)
        val n2 = Neuron(net)
        net.addNetworkModel(n2)
        pm.placeObject(n2)
        val neuronOffset = PlacementManager.DefaultOffsets.get(n1).getX()
        assertEquals(neuronOffset, n2.x, .01)
    }

    @Test
    fun `test initial location change`() {

        pm.lastClickedLocation = point(100.0, 0.0)
        val n1 = Neuron(net)
        net.addNetworkModel(n1)
        pm.placeObject(n1)
        assertEquals(100.0, n1.x, .01 )

        // Subsequent should be offset from there by default amount
        val n2 = Neuron(net)
        net.addNetworkModel(n2)
        pm.placeObject(n2)
        val neuronOffset = PlacementManager.DefaultOffsets.get(n1).getX()
        assertEquals(100 + neuronOffset, n2.x, .01)
    }

    @Test
    fun `test neuron array`() {
        val na1 = NeuronArray(net, 20)
        net.addNetworkModel(na1)
        pm.placeObject(na1)
        val na2 = NeuronArray(net, 20)
        net.addNetworkModel(na2)
        pm.placeObject(na2)
        val offset = PlacementManager.DefaultOffsets.get(na2).getY()
        assertEquals(offset, na2.location.y, .01 )
    }

}