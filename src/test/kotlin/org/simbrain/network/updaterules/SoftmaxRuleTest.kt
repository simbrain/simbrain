package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray

class SoftmaxRuleTest {

    val net = Network()
    val rule = SoftmaxRule()
    var na = NeuronArray(2).apply {
        updateRule = rule
    }

    init {
        net.addNetworkModels(na)
    }

    @Test
    fun `Values should sum to 1`() {
        na.setActivations(doubleArrayOf(.5, .7))
        net.update()
        assertEquals(1.0, na.activationArray.sum())
    }

    @Test
    fun `Equal inputs should produce equal outputs`() {
        na.setActivations(doubleArrayOf(.85, .85))
        net.update()
        assertEquals(na.activationArray[0], na.activationArray[1])
    }

    @Test
    fun `The component receiving the most input should have the highest value`() {
        na.setActivations(doubleArrayOf(1.0, 0.5))
        na.isClamped = true
        net.update()
        assertTrue(na.activationArray[0] > na.activationArray[1])
    }

    // TODO: The softmax derivative is questionable
    //@Test
    //fun `Test derivative`() {
    //    print(rule.getDerivative(doubleArrayOf(.1, .2, .3).toMatrix()))
    //}

}
