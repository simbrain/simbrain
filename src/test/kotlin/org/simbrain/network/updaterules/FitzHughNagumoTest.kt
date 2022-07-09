package org.simbrain.network.updaterules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.neuron_update_rules.FitzhughNagumo

class FitzHughNagumoTest {

    val net = Network()
    val fhRule = FitzhughNagumo()
    val n = Neuron(net, fhRule)
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