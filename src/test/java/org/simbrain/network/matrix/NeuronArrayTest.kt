package org.simbrain.network.matrix

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import smile.math.matrix.Matrix

class NeuronArrayTest {
    var net: Network = Network()
    var na: NeuronArray = NeuronArray(10)

    @BeforeEach
    fun setUp() {
        net.addNetworkModel(na)
    }

    @Test
    fun testUpdate() {
        with(net) {
            val inputs = Matrix(10, 1)
            inputs.fill(1.0)
            na.addInputs(inputs)
            Assertions.assertEquals(10.0, na.inputs.sum(), 0.0)
            na.update()
            Assertions.assertEquals(10.0, na.activations.sum(), 0.0)
            na.update() // Should clear to 0
            Assertions.assertEquals(0.0, na.activations.sum(), 0.0)
        }
    }

    @Test
    fun testSetLocation() {
        val location = na.location
        na.location = location
        Assertions.assertEquals(location.x, na.location.x, 0.001)
        Assertions.assertEquals(location.y, na.location.y, 0.001)
    }

    @Test
    fun testExcitatoryInputs() {
        val na1 = NeuronArray(2)
        na1.setActivations(doubleArrayOf(1.0, 1.0))
        val naTarget = NeuronArray(3)
        val wm1 = WeightMatrix(na1, naTarget)
        wm1.setWeights(doubleArrayOf(5.0, -1.0, 1.0, 1.0, -1.0, -1.0))
        net.addNetworkModels(na1, naTarget, wm1)
        // Expecting 5 for first row, 2 for second row, and 0 for the last row
        Assertions.assertArrayEquals(doubleArrayOf(5.0, 2.0, 0.0), naTarget.excitatoryInputs)

        // Add a second input array
        val na2 = NeuronArray(2)
        na2.setActivations(doubleArrayOf(1.0, 1.0))
        val wm2 = WeightMatrix(na2, naTarget)
        wm2.setWeights(doubleArrayOf(1.0, 0.0, -1.0, -1.0, 1.0, 1.0))
        net.addNetworkModels(na2, wm2)
        // Now expecting 6, 2, 2
        Assertions.assertArrayEquals(doubleArrayOf(6.0, 2.0, 2.0), naTarget.excitatoryInputs)
    }

    @Test
    fun testInhibitoryInputs() {
        val na1 = NeuronArray(2)
        na1.setActivations(doubleArrayOf(1.0, 1.0))
        val naTarget = NeuronArray(3)
        val wm1 = WeightMatrix(na1, naTarget)
        wm1.setWeights(doubleArrayOf(5.0, -1.0, 1.0, 1.0, -1.0, -1.0))
        net.addNetworkModels(na1, naTarget, wm1)
        // Expecting -1 for first row, 0 for second row, and -2 for the last row
        Assertions.assertArrayEquals(doubleArrayOf(-1.0, 0.0, -2.0), naTarget.inhibitoryInputs)

        // Add a second input array
        val na2 = NeuronArray(2)
        na2.setActivations(doubleArrayOf(1.0, 1.0))
        val wm2 = WeightMatrix(na2, naTarget)
        wm2.setWeights(doubleArrayOf(1.0, 0.0, -1.0, -1.0, 1.0, 1.0))
        net.addNetworkModels(na2, wm2)
        // Now expecting -1, -2, -2
        Assertions.assertArrayEquals(doubleArrayOf(-1.0, -2.0, -2.0), naTarget.inhibitoryInputs)
    }
}