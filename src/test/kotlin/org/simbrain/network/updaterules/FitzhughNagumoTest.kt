package org.simbrain.network.updaterules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.Utils.round

class FitzhughNagumoTest {

    val net = Network()
    val fhRule = FitzhughNagumo()
    val n = Neuron(fhRule)
    init {
        net.addNetworkModel(n)
    }

    @Test
    fun `todo`() {
        repeat(10) {
            net.update()
            println("act=${round(n.activation, 3)}, w=${round((n.dataHolder as FitzHughData).w, 3)}")
        }
    }

}