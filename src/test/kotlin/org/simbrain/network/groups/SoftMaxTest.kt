package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.SoftmaxRule

class SoftMaxTest {

    var net = Network()
    val source = NeuronArray(2).apply { isClamped = true }
    val softmax = NeuronArray(2).apply { updateRule = SoftmaxRule() }
    val wm = WeightMatrix(source, softmax)

    init {
        net.addNetworkModelsAsync(source, softmax, wm)
    }

    @Test
    fun `Softmax activations sum to 1`() {
        source.activationArray = doubleArrayOf(1.0, 2.0)
        net.update()
        assertEquals(1.0, softmax.activations.sum(), 0.01)
    }

    @Test
    fun `Equal inputs should produce equal outputs`() {
        source.activationArray = doubleArrayOf(0.85, 0.85)
        net.update()
        assertEquals(softmax.activationArray[0], softmax.activationArray[1], 0.001)
    }

    @Test
    fun `The node receiving the most input should have the highest value`() {
        source.activationArray = doubleArrayOf(1.0, 0.5)
        net.update()
        assertTrue(softmax.activationArray[0] > softmax.activationArray[1])
    }

    @Test
    fun `Test copy function`() {
        val softmax2 = softmax.copy()
        net.addNetworkModelsAsync(softmax2)
        assertEquals(2, softmax2.size)
        assertTrue(softmax2.updateRule is SoftmaxRule)
    }
}
