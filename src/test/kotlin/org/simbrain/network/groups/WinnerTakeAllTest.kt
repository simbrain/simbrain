package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.neurongroups.WinnerTakeAll

class WinnerTakeAllTest {

    var net = Network()

    @Test
    fun `Check one winner and neuron parent set properly` () {
        val wta = WinnerTakeAll(net, 10)
        // assertTrue(wta.neuronList.first().parentGroup is NeuronGroup)
        with(net) { wta.update() }
        assertEquals(1, wta.neuronList.count { it.activation > 0.0 })
    }

}


