package org.simbrain.network.updaterules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class MorrisLecarTest {

    val net = Network()
    val mlRule = MorrisLecarRule()
    val n = Neuron(net, mlRule)
    init {
        net.addNetworkModelAsync(n)
    }

    @Test
    fun `todo`() {
        repeat(10) {
            net.update()
            println("t = ${net.time}: act=${n.activation} w_K=${(n.dataHolder as MorrisLecarData).w_K}")
        }
    }

}