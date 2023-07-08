package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.util.sse
import org.simbrain.util.toDoubleArray
import org.simbrain.util.toMatrix
import smile.math.matrix.Matrix

class BackpropTests {

    val net = Network()
    val na1 = NeuronArray(net, 2)
    val na2 = NeuronArray(net, 3)
    val na3 = NeuronArray(net, 2)
    var na1DataHolder = (na1.dataHolder as BiasedMatrixData)
    val wm1 = WeightMatrix(net, na1, na2)
    val wm2 = WeightMatrix(net, na2, na3)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
            (it.updateRule as BoundedUpdateRule).upperBound = 1.0
            (it.updateRule as BoundedUpdateRule).lowerBound = -1.0
        }
        net.addNetworkModelsAsync(na1, na2, na3, wm1, wm2)
    }

    // TODO: Blows up for larger targets, like 30
    val inputs = doubleArrayOf(-1.0, 1.0).toMatrix()
    val targets = Matrix.column(doubleArrayOf(.5, -.5))

    @Test
    fun `test backprop relu`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.Relu
        testBackprop()
    }

    @Test
    fun `test backprop no clipping`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.NoClipping
        testBackprop()
    }

    @Test
    fun `test backprop piecewise linear`() {
        (na2.updateRule as LinearRule).clippingType = LinearRule.ClippingType.PiecewiseLinear
        testBackprop()
    }

    private fun testBackprop() {
        wm1.randomize()
        wm2.randomize()
        // na2.randomizeBiases()
        // na3.randomizeBiases()
        repeat(1000) {
            listOf(wm1, wm2).applyBackprop(inputs, targets, .1)
            // println(targets.toDoubleArray() sse wm2.output.toDoubleArray())
        }
        println("Outputs: ${wm2.output}, SSE = ${targets sse wm2.output}")
        assertEquals(0.0, targets.toDoubleArray() sse wm2.output.toDoubleArray(), .01)
    }

}