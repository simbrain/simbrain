package org.simbrain.network.matrix

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.neurongroups.NeuronGroup
import java.util.List

class WeightMatrixTest {
    lateinit var net: Network
    lateinit var na1: NeuronArray
    lateinit var na2: NeuronArray
    lateinit var wm: WeightMatrix

    @BeforeEach
    fun setUp() {
        net = Network()
        na1 = NeuronArray(2)
        na2 = NeuronArray(2)
        wm = WeightMatrix(na1, na2)
        net.addNetworkModelsAsync(na1, na2, wm)
    }

    @Test
    fun testMatrixOperations() {
        // Set first entry to 4

        wm.weightMatrix[0, 0] = 4.0
        Assertions.assertEquals(4.0, wm.weightMatrix[0, 0], 0.0)

        // Set to ((0,0);(0,0)) and check sum
        wm.setWeights(doubleArrayOf(0.0, 0.0, 0.0, 0.0))
        Assertions.assertEquals(0.0, wm.weightMatrix.sum(), 0.0)

        // Add 1 to each entry. Should get ((1,1);(1,1))
        wm.weightMatrix.add(1.0)
        Assertions.assertEquals(4.0, wm.weightMatrix.sum(), 0.0)
    }

    @Test
    fun testSetWeights() {
        wm.setWeights(doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        Assertions.assertEquals(1.0, wm.weightMatrix[0, 0], 0.0)
        Assertions.assertEquals(2.0, wm.weightMatrix[0, 1], 0.0)
        Assertions.assertEquals(3.0, wm.weightMatrix[1, 0], 0.0)
        Assertions.assertEquals(4.0, wm.weightMatrix[1, 1], 0.0)
    }

    @Test
    fun testDiagonalize() {
        wm.diagonalize() // assume wm is 2x2
        Assertions.assertEquals(2.0, wm.weightMatrix.sum(), 0.0)
    }

    @Test
    fun testMatrixProduct() {
        with(net) {
            na1.setActivations(doubleArrayOf(1.0, 2.0))
            wm.setWeights(doubleArrayOf(1.0, 2.0, 3.0, 4.0))
            Assertions.assertArrayEquals(doubleArrayOf(5.0, 11.0), wm.output.col(0), 0.0)
        }
    }


    @Test
    fun testArrayToArray() {
        na1.setActivations(doubleArrayOf(.5, -.5))
        wm.diagonalize()
        net.update() // input should be cleared and second array updated. This is buffered update.
        Assertions.assertArrayEquals(doubleArrayOf(0.0, 0.0), na1.activations.col(0), 0.0)
        Assertions.assertArrayEquals(doubleArrayOf(.5, -.5), na2.activations.col(0), 0.0)
        net.update() // All should be cleared on second update
        Assertions.assertArrayEquals(doubleArrayOf(0.0, 0.0), na1.activations.col(0), 0.0)
        Assertions.assertArrayEquals(doubleArrayOf(0.0, 0.0), na2.activations.col(0), 0.0)
    }

    @Test
    fun testInputPropagation() {
        na1.clear()
        na1.addInputs(doubleArrayOf(.5, -.5))
        wm.diagonalize()
        net.update() // First update puts inputs to activation
        Assertions.assertArrayEquals(doubleArrayOf(.5, -.5), na1.activations.col(0), 0.0)
        Assertions.assertArrayEquals(doubleArrayOf(0.0, 0.0), na2.activations.col(0), 0.0)
        net.update() // Second update propagates
        Assertions.assertArrayEquals(doubleArrayOf(0.0, 0.0), na1.activations.col(0), 0.0)
        Assertions.assertArrayEquals(doubleArrayOf(.5, -.5), na2.activations.col(0), 0.0)
    }

    @Test
    fun testExcitatoryOutputs() {
        na1.setActivations(doubleArrayOf(1.0, 1.0))
        val na3 = NeuronArray(3)
        val wm2 = WeightMatrix(na1, na3)
        wm2.setWeights(doubleArrayOf(5.0, -1.0, 1.0, 1.0, -1.0, -1.0))
        // Expecting 5 for first row, 2 for second row, and 0 for the last row
        Assertions.assertArrayEquals(doubleArrayOf(5.0, 2.0, 0.0), wm2.excitatoryOutputs)
    }

    @Test
    fun testInhibitoryOutputs() {
        na1.setActivations(doubleArrayOf(1.0, 1.0))
        val na3 = NeuronArray(3)
        val wm2 = WeightMatrix(na1, na3)
        wm2.setWeights(doubleArrayOf(1.0, -2.0, 1.0, 1.0, -1.0, -1.0))
        Assertions.assertArrayEquals(doubleArrayOf(-2.0, 0.0, -2.0), wm2.inhibitoryOutputs)
        // TODO: Test with spike responders so that we can check for positive inhib outputs, the more standard case
    }

    @Test
    fun testArrayToNeuronGroup() {
        na1.setActivations(doubleArrayOf(.5, -.5))
        val ng = NeuronGroup(2)
        val wm2 = WeightMatrix(na1, ng)
        wm2.diagonalize()
        net.addNetworkModelsAsync(List.of(ng, wm2))
        net.update()
        Assertions.assertArrayEquals(doubleArrayOf(.5, -.5), ng.activations, 0.0)
        net.update() // All should be cleared on second update
        Assertions.assertArrayEquals(doubleArrayOf(0.0, 0.0), ng.activations, 0.0)
    }

    // @Test
    fun large_matrix_multiplication() {
        val net = Network()
        val ng1 = NeuronGroup(1000)
        val ng2 = NeuronGroup(1000)
        val wm = WeightMatrix(ng1, ng2)
        wm.weightMatrix.add(1.0001)

        val start_time = System.currentTimeMillis()
        for (i in 0..999) {
            wm.weightMatrix.mm(wm.weightMatrix)
        }
        val stop_time = System.currentTimeMillis()
        val difference = stop_time - start_time
        println("Compute time for large matrix product: $difference ms")
    }
}