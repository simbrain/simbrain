package org.simbrain.network.updaterules

import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.Utils.round

class IntegrateAndFireTest {

    val net = Network()

    @Test
    fun `test integrate and fire`() {
        val intFire = IntegrateAndFireRule()
        intFire.timeConstant = 1.0
        // intFire.backgroundCurrent = 100.0
        net.timeStep = 1.0
        val n = Neuron(net, intFire)
        net.addNetworkModel(n)

        // Resting is -70
        // RefractoryPeriod = 3
        // Reset is -55
        // Threshold is -50
        repeat(100) {

            val act = round(n.activation, 3)
            println("t = ${net.time}: act=$act")

            if (it in 4..10) {
                n.addInputValue(1000.0)
            }
            net.update()
        }
    }

}