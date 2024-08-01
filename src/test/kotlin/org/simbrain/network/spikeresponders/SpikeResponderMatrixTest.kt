package org.simbrain.network.spikeresponders

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.network.updaterules.SpikingThresholdRule
import org.simbrain.network.util.SpikingMatrixData
import smile.math.matrix.Matrix

/**
 * This will hold all matrix based spike responder tests since they all require the same setup.
 */
class SpikeResponderMatrixTest {

    val net = Network()
    val n1 = NeuronArray(2) // Input
    val n2 = NeuronArray(2) // spiking neurons
    val n3 = NeuronArray(3) // receives spike response
    val wm1 = WeightMatrix(n1, n2)
    val wm2 = WeightMatrix(n2, n3) // This one has the spike responder

    init {
        wm2.setWeights(
            arrayOf(
                doubleArrayOf(1.0, 0.0),
                doubleArrayOf(0.0, -1.0),
                doubleArrayOf(.5, .5)));
        n2.updateRule = SpikingThresholdRule()
        listOf(n1, n2, n3).forEach {
            it.clear()
        }
        net.addNetworkModels(n1, n2, n3, wm1, wm2)
    }

    @Test
    fun `neuron arrays and copies`() {
        // Change type of neuron array, copy, and create a spike responder
        val net2 = Network()
        val arr1 = NeuronArray(4) // Input
        arr1.updateRule = IntegrateAndFireRule()
        val arr2 = arr1.copy()
        val wmArr1Arr2 = WeightMatrix(arr1, arr2)
        wmArr1Arr2.spikeResponder = StepResponder()
        net2.addNetworkModels(arr1, arr2, wmArr1Arr2)
        net2.update() // Caused exceptions in earlier iterations.
        assertEquals(4, arr2.size)
        assertTrue(arr2.updateRule is IntegrateAndFireRule)
    }

    @Test
    fun `non-responder results in weight matrix times input vector`() {

        val nr = NonResponder()
        wm2.spikeResponder = nr

        n2.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, -1.0, 1.0), n3.activationArray)
    }

    @Test
    fun `test probabilistic response`() {

        val pr = StepResponder()
        pr.responseDuration = 1
        pr.spikeProbability = 1.0
        wm2.spikeResponder = pr
        n1.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update() // extra update to propagate from layer 1 to 2
        assertArrayEquals(doubleArrayOf(1.0, -1.0, 1.0), n3.activationArray)

        pr.spikeProbability = 0.0
        n1.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray)
    }


    @Test
    fun `test step responder values before during and after its duration `() {

        val step = StepResponder()
        wm2.spikeResponder = step

        step.responseHeight = .5
        step.responseDuration = 3

        n1.activations = Matrix.column(doubleArrayOf(1.0, 0.0))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(booleanArrayOf(true, false), (n2.dataHolder as SpikingMatrixData).spikes)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update() // Step response ends
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(booleanArrayOf(false, false), (n2.dataHolder as SpikingMatrixData).spikes)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
    }

    @Test
    fun `test step responder values with both nodes spiking`() {

        val step = StepResponder()
        wm2.spikeResponder = step

        step.responseHeight = 1.0
        step.responseDuration = 2

        n1.activations = Matrix.column(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update()
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), wm2.getSummedPSRs())
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), wm2.getSummedPSRs())
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), wm2.getSummedPSRs())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
    }


}