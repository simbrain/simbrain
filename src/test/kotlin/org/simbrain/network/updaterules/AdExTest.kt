package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class AdExTest {

    val net = Network()
    val adEx = AdExIFRule()
    val n = Neuron(net, adEx)
    init {
        net.addNetworkModel(n)
    }

    @Test
    fun `adex should remain near leak reversal if unperturbed`() {
        // TODO: When initializing the rule set neuron activation to this
        n.activation = adEx.leakReversal
        repeat(10) {
            net.update()
            println("t = ${net.time}: act=${n.activation} w=${(n.dataHolder as AdexData).w}")
        }
        // A pretty big delta was needed, can we do better?
        assertEquals(adEx.leakReversal, n.activation, 2.0)
    }

    @Test
    fun `converges the leak potential sub-threshold`() {
        n.activation = adEx.v_Th - 1.0
        net.timeStep = 10.0
        repeat(20) {
            net.update()
            // println("t = ${net.time}: act=${n.activation} w=${(n.neuronDataHolder as AdexData).w}")
        }
        assertEquals(adEx.leakReversal, n.activation, .01)
    }

    @Test
    fun `spike occurs when membrane potential is set to peak voltage`() {
        n.activation = adEx.v_Peak
        net.update()
        assertEquals(true, n.isSpike)
    }

    /**
     * First update produces the spike and sets voltage to peak value. Second update (first after spike) put its at the
     * reset value and some of the other dyanmics are applied.
     */
    @Test
    fun `membrane potential goes near the reset potential two iterations after a spike`() {
        n.activation = adEx.v_Peak
        net.update()
        net.update()
        assertEquals(adEx.v_Reset, n.activation, .3)
    }

    @Test
    fun `adaptation value does TODSO after a spike`() {
        n.activation = adEx.v_Peak
        net.timeStep = .0001
        net.update()
        // assertEquals(adEx.b + 200, (n.neuronDataHolder as AdexData).w)
    }

}