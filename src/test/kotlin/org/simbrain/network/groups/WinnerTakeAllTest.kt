package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.WinnerTakeAllRule

class WinnerTakeAllTest {

    var net = Network()
    val source = NeuronArray(2).apply { isClamped = true }
    val wta = NeuronArray(2).apply {
        updateRule = WinnerTakeAllRule()
    }
    val wm = WeightMatrix(source, wta)

    init {
        net.addNetworkModelsAsync(source, wta, wm)
    }

    @Test
    fun `Check that at any time there is just one winner`() {
        source.activationArray = doubleArrayOf(0.5, 0.3)
        net.update()
        assertEquals(1, wta.activationArray.count { it > 0.0 })
    }

    @Test
    fun `Check that node with most input wins`() {
        source.activationArray = doubleArrayOf(1.0, 0.9)
        net.update()
        assertEquals(1.0, wta.activationArray[0])
        assertEquals(0.0, wta.activationArray[1])
        source.activationArray = doubleArrayOf(-1.0, 0.2)
        net.update()
        assertEquals(0.0, wta.activationArray[0])
        assertEquals(1.0, wta.activationArray[1])
    }

    @Test
    fun `Check that if equal input a random node wins`() {
        val winners = (0..100).map {
            source.activationArray = doubleArrayOf(1.0, 1.0)
            net.update()
            wta.activationArray.indexOfFirst { v -> v > 0.0 }
        }.toSet().size
        assertEquals(2, winners)
    }

    @Test
    fun `Check that winning and losing value works`() {
        (wta.updateRule as WinnerTakeAllRule).apply {
            winValue = 2.0
            loseValue = -0.5
        }
        source.activationArray = doubleArrayOf(1.0, 0.9)
        net.update()
        assertEquals(2.0, wta.activationArray[0])
        assertEquals(-0.5, wta.activationArray[1])
    }
}
