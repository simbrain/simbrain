package org.simbrain.network.updaterules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.neuron_update_rules.AdExIFRule

class AdExTest {

    val net = Network()
    val adEx = AdExIFRule()
    val n = Neuron(net, adEx)
    init {
        net.addNetworkModel(n)
    }

    @Test
    fun `todo`() {
        repeat(10) {
            net.update()
            println("t = ${net.time}: act=${n.activation}")
        }
    }

}