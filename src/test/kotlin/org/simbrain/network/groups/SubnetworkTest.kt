package org.simbrain.network.groups

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.FeedForward
import java.awt.geom.Point2D

class SubnetworkTest {

    var net = Network()

    @Test
    fun `ff creation` () {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        assertEquals(5, ff.modelList.size)
    }

    @Test
    fun `ff layer deletion` () = runBlocking {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        val firstLayer = ff.modelList.get<NeuronArray>().first()
        firstLayer.delete() // This should get rid of a weight matrix
        assertEquals(3, ff.modelList.size)
    }

    @Test
    fun `subnet deleted when empty` () = runBlocking {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        net.addNetworkModelAsync(ff);
        ff.modelList.all.forEach { it.delete() }
        assertEquals(0, net.allModels.size)
    }

    @Test
    fun `clear subnetwork clears all components` () {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        ff.layerList.forEach { layer ->
            layer.activationArray = DoubleArray(layer.size) { 1.0 }
        }
        ff.layerList.forEach { layer ->
            assert(layer.activationArray.all { it == 1.0 })
        }
        ff.clear()
        ff.layerList.forEach { layer ->
            layer.activationArray.forEach { activation ->
                assertEquals(0.0, activation, 0.0001)
            }
        }
    }
}


