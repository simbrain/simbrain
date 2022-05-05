package org.simbrain.network.connections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class SparseTest {

    var net = Network()
    lateinit var sparse: Sparse
    
    @BeforeEach
    fun setUp() {
        sparse = Sparse()
        net.addNetworkModels(List(10) { Neuron(net) })
    }

    // TODO: Check equalize efferents
    //  Check adding and removing

    @Test
    fun `check correct number of weights are created`() {
        sparse.connectionDensity = .1
        sparse.allowSelfConnection = true
        val syns = sparse.connectNeurons(net, net.looseNeurons.toList()
            , net.looseNeurons.toList())
        assertEquals(10, syns.size)
    }



}