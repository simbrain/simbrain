package org.simbrain.network.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.util.rowMatrixTransposed
import org.simbrain.util.sse
import smile.math.matrix.Matrix

class NetworkUtilTest {

    val net = Network()
    val na1 = NeuronArray(net, 2)
    val na2 = NeuronArray(net, 3)
    val na3 = NeuronArray(net, 2)
    val wm1 = WeightMatrix(net, na1, na2)
    val wm2 = WeightMatrix(net, na2, na3)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
        }
        net.addNetworkModels(na1, na2, na3, wm1, wm2)
    }

    @Test
    fun `test neuron array error`() {
        na1.setActivations(doubleArrayOf(-1.0, 1.0))
        val error = na1.getError(Matrix(2,1, 2.0))
        assertArrayEquals(doubleArrayOf(3.0, 1.0 ), error.col(0))
    }

    @Test
    fun `test weight lms weight updates`() {
        val outputError = Matrix(3,1, 2.0)
        na1.setActivations(doubleArrayOf(-1.0, 1.0))
        // outputError * na1.activations + wm2.weights
        wm1.applyLMS(outputError, 1.0)
        // Col 1 = -1, -2, -2
        // Col 2 = 2, 3, 2
        // println(wm1.weightMatrix)
        assertArrayEquals(doubleArrayOf(-1.0, -2.0, -2.0), wm1.weightMatrix.col(0) )
        assertArrayEquals(doubleArrayOf(2.0, 3.0, 2.0), wm1.weightMatrix.col(1) )
    }

    @Test
    fun `test forward pass`() {
        val inputs = Matrix(doubleArrayOf(-1.0, 1.0))
        listOf(wm1, wm2).forwardPass(inputs)
        listOf(wm1, wm2).printActivationsAndWeights(true)
        assertArrayEquals(inputs.col(0), wm2.target.outputs.col(0))
    }


    @Test
    fun `test lms`() {
        na1.setActivations(doubleArrayOf(-1.0, 1.0))
        val target = doubleArrayOf(5.0, -1.0, .5)
        na2.setActivations(target)
        // println("Before: ${wm1.output}")
        repeat(100) {
            wm1.trainCurrentOutputLMS()
        }
        // println("After: ${wm1.output}")
        assertArrayEquals(target, wm1.output.col(0), .01)
    }

    @Test
    fun `test backprop`() {
        val inputs = Matrix(doubleArrayOf(-1.0, 1.0))
        // TODO: Blows up for larger targets, like 30
        val targets = Matrix(doubleArrayOf(1.75, -.5))
        (na3.updateRule as LinearRule).lowerBound = -100.0
        (na3.updateRule as LinearRule).upperBound = 100.0
        wm1.randomize()
        wm2.randomize()
        repeat(50) {
            listOf(wm1, wm2).applyBackprop(inputs, targets, .1)
            // println(targets.col(0) sse wm2.output.col(0))
        }
        // println("After: ${wm2.output}")
        assertEquals(0.0, targets.col(0) sse wm2.output.col(0), .01)
    }

    @Test
    fun `test lms in a feed forward net`() {
        val ff = LMSNetwork(net, 5, 5)
        val target =  ff.trainingSet.targets.rowMatrixTransposed(1)

        ff.inputLayer.isClamped = true
        ff.inputLayer.setActivations(ff.trainingSet.inputs.row(1))
        ff.update()
        val outputs = ff.outputLayer.activations
        val error = target.sub(outputs)
        // TODO: Make an actual test; this was just to recreate a crash
        ff.weightMatrix.applyLMS(error, .1)
    }

}