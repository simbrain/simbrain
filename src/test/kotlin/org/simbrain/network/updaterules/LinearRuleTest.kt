package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.linspace
import org.simbrain.util.toDoubleArray
import org.simbrain.util.toMatrix

class LinearRuleTest {

    // Default update rule is linear
    val net = Network()
    var input1 = Neuron(net)
    var input2 = Neuron(net)
    val output = Neuron(net)
    var outputRule: LinearRule = output.updateRule as LinearRule

    var w13 = Synapse(input1, output)
    var w23 = Synapse(input2, output)

    init {
        net.addNetworkModelsAsync(input1, input2, output, w13, w23)
        input1.activation = 1.0
        input1.isClamped = true

        input2.activation = -1.0
        input2.isClamped = true

        w13.strength = .5
        w23.strength = -1.0

    }

    @Test
    fun `test basic update`() {

        // 1*.5 + -1*-1 = .5 + 1 = 1.5
        net.update()
        assertEquals(1.5, output.activation, 0.0)
    }

    @Test
    fun `test two negative weights`() {
        w13.strength = -0.8
        w23.strength = -0.2
        net.update()
        // 0.6 with the epsilon(threshold) 0.0001
        assertEquals(-0.6, output.activation, 0.0001)
    }

    @Test
    fun `test piecewise linear clipping`() {
        outputRule.lowerBound = -.5
        outputRule.upperBound = .5
        outputRule.clippingType = LinearRule.ClippingType.PiecewiseLinear
        net.update()
        assertEquals(.5, output.activation)
        w13.strength = -10.0
        net.update()
        assertEquals(-.5, output.activation)
    }

    @Test
    fun `test relu`() {
        outputRule.lowerBound = -.5
        outputRule.upperBound = .5
        outputRule.clippingType = LinearRule.ClippingType.Relu
        net.update()
        assertEquals(1.5, output.activation)
        w13.strength = -10.0
        net.update()
        assertEquals(0.0, output.activation)
    }

    @Test
    fun `test no clipping`() {
        // Bounds ignored
        outputRule.lowerBound = -.5
        outputRule.upperBound = .5
        outputRule.clippingType = LinearRule.ClippingType.NoClipping
        net.update()
        assertEquals(1.5, output.activation)
        // 1*-10 + -1*-1 = -9
        w13.strength = -10.0
        net.update()
        assertEquals(-9.0, output.activation)
    }

    @Test
    fun `test bias`() {
        (output.dataHolder as BiasedScalarData).bias = .2
        w13.strength = 0.0
        w23.strength = 0.0
        net.update()
        assertEquals(.2, output.activation)
    }

    @Test
    fun `test piecewise linear derivative`() {
        val lr = LinearRule()
        lr.clippingType = LinearRule.ClippingType.PiecewiseLinear
        lr.upperBound = 10.0
        lr.lowerBound = -10.0
        lr.slope = 5.0

        // Above upper bound should return 0
        assertEquals(0.0, lr.getDerivative(11.0), 0.0)

        // Below lower bound should return 0
        assertEquals(0.0, lr.getDerivative(-11.0), 0.0)

        // Between lower and upper bound returns the slope
        assertEquals(5.0, lr.getDerivative(0.0), 0.0)
    }

    @Test
    fun `test array linear derivative with relu`() {
        val lr = LinearRule()
        val array = linspace(-10.0,10.0,4).toMatrix()
        lr.clippingType = LinearRule.ClippingType.Relu
        assertArrayEquals(doubleArrayOf(0.0,0.0,1.0,1.0), lr.getDerivative(array).toDoubleArray())
    }

    @Test
    fun `test array linear derivative with no bounds`() {
        val lr = LinearRule()
        val array = linspace(-10.0,10.0,4).toMatrix()
        lr.clippingType = LinearRule.ClippingType.NoClipping
        assertArrayEquals(doubleArrayOf(1.0,1.0,1.0,1.0), lr.getDerivative(array).toDoubleArray())
    }

    @Test
    fun `test array linear derivative with piecewise linear`() {
        val lr = LinearRule()
        lr.lowerBound = -5.0
        lr.upperBound = 5.0
        val array = linspace(-10.0,10.0,4).toMatrix()
        lr.clippingType = LinearRule.ClippingType.PiecewiseLinear
        assertArrayEquals(doubleArrayOf(0.0,1.0,1.0,0.0), lr.getDerivative(array).toDoubleArray())
    }
}