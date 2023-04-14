package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.subnetworks.FeedForward
import java.awt.geom.Point2D

class SubnetworkTest {

    var net = Network()

    @Test
    fun `ff creation` () {
        val ff = FeedForward(net, intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        assertEquals(5, ff.modelList.size)
    }

    @Test
    fun `ff layer deletion` () {
        val ff = FeedForward(net, intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        val firstLayer = ff.modelList.get<NeuronArray>().first()
        firstLayer.delete() // This should get rid of a weight matrix
        assertEquals(3, ff.modelList.size)
    }

    @Test
    fun `subnet deleted when empty` () {
        val ff = FeedForward(net, intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        net.addNetworkModelAsync(ff);
        ff.modelList.all.forEach(NetworkModel::delete)
        assertEquals(0, net.allModels.size)
    }
}


