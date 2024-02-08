package org.simbrain.network.updaterules

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.util.linspace
import org.simbrain.util.toDoubleArray
import org.simbrain.util.toMatrix

class LinearRuleArrayTest {

    val net = Network()
    var input1 = NeuronArray( 2)
    var input2 = NeuronArray( 2)
    val output = NeuronArray( 2)
    var w13 = WeightMatrix(input1, output)
    var w23 = WeightMatrix(input2, output)

    init {
        net.addNetworkModelsAsync(input1, input2, output, w13, w23)
        input1.activations = doubleArrayOf(1.0, -1.0).toMatrix()
        input1.isClamped = true
        input2.activations = doubleArrayOf(-1.0, 1.0).toMatrix()
        input2.isClamped = true
        // Net input will be (1,-1) + (-1,1) = (0.0)
    }

    @Test
    fun `test basic update`() {
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), output.activationArray, 0.0)
    }

    @Test
    fun `test with negative weights`() {
        w13.weightMatrix.mul(-1.0)
        net.update()
        assertArrayEquals(doubleArrayOf(-2.0, 2.0), output.activationArray, 0.0)
    }

    @Test
    fun `test piecewise linear`() {
        input1.activations = doubleArrayOf(10.0, 10.0).toMatrix()
        (output.updateRule as LinearRule).apply {
            clippingType = LinearRule.ClippingType.PiecewiseLinear
            lowerBound = -.5
            upperBound = .5
        }
        net.update()
        assertArrayEquals(doubleArrayOf(.5,.5), output.activationArray)
    }

    @Test
    fun `test relu`() {
        input1.activations = doubleArrayOf(10.0, 10.0).toMatrix()
        // reminder: input 2 = (-1,1)
        (output.updateRule as LinearRule).apply {
            clippingType = LinearRule.ClippingType.Relu
        }
        net.update()
        assertArrayEquals(doubleArrayOf(9.0,11.0), output.activationArray)
        input1.activations = doubleArrayOf(-10.0, -10.0).toMatrix()
        net.update()
        assertArrayEquals(doubleArrayOf(0.0,0.0), output.activationArray)
    }

    @Test
    fun `test no clipping`() {
        input1.activations = doubleArrayOf(10.0, 10.0).toMatrix()
        // reminder: input 2 = (-1,1)
        (output.updateRule as LinearRule).apply {
            clippingType = LinearRule.ClippingType.NoClipping
        }
        net.update()
        assertArrayEquals(doubleArrayOf(9.0,11.0), output.activationArray)
        input1.activations = doubleArrayOf(-10.0, -10.0).toMatrix()
        net.update()
        assertArrayEquals(doubleArrayOf(-11.0,-9.0), output.activationArray)
    }

    @Test
    fun `test bias`() {
        (output.dataHolder as BiasedMatrixData).biases = doubleArrayOf(1.0, -1.0).toMatrix()
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, -1.0), output.activationArray, 0.0)
    }

    @Test
    fun `test piecewise linear derivative`() {
        val lr = LinearRule()
        lr.clippingType = LinearRule.ClippingType.PiecewiseLinear
        lr.upperBound = 10.0
        lr.lowerBound = -10.0
        lr.slope = 5.0

        // Above upper bound should return 0
        var deriv = lr.getDerivative(doubleArrayOf(11.0, 100.0).toMatrix()).toDoubleArray()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), deriv, 0.0)

        // Below lower bound should return 0
        deriv = lr.getDerivative(doubleArrayOf(-11.0, -100.0).toMatrix()).toDoubleArray()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), deriv, 0.0)

        // Between lower and upper bound returns the slope
        deriv = lr.getDerivative(doubleArrayOf(0.0, 0.0).toMatrix()).toDoubleArray()
        assertArrayEquals(doubleArrayOf(5.0, 5.0), deriv, 0.0)
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