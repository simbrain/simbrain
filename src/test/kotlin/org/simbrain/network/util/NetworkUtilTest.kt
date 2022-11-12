package org.simbrain.network.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.applyLMS
import org.simbrain.network.core.getError
import org.simbrain.network.core.learnCurrentOutput
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
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
    fun `test weight matrix add error`() {
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
    fun `test lms`() {
        na1.setActivations(doubleArrayOf(-1.0, 1.0))
        val target = doubleArrayOf(1.0, -1.0, .5)
        na2.setActivations(target)
        // println("Before: ${wm1.output}")
        repeat(100) {
            wm1.learnCurrentOutput()
        }
        // println("After: ${wm1.output}")
        assertArrayEquals(target, wm1.output.col(0), .01)
    }

}