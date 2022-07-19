package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule
import org.simbrain.network.spikeresponders.StepResponder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.network.util.StepMatrixData
import smile.math.matrix.Matrix

class SpikeResponderMatrixTest {

    val net = Network()
    val n1 = NeuronArray(net, 2) // Input
    val n2 = NeuronArray(net, 2) // spiking neurons
    val n3 = NeuronArray(net, 3) // receives spike response
    val wm1 = WeightMatrix(net, n1, n2)
    val wm2 = WeightMatrix(net, n2, n3) // This one has the spike responder
    val step = StepResponder()

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
        wm2.setSpikeResponder(step)
        net.addNetworkModels(n1, n2, n3, wm1, wm2)
    }

    @Test
    fun `test step responder values before during and after its duration `() {

        step.responseHeight = .5
        step.responseDuration = 3

        n1.activations = Matrix(doubleArrayOf(1.0, 0.0))
        net.update()
        assertArrayEquals(doubleArrayOf(1.0, 0.0), wm1.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(1.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(booleanArrayOf(true, false), (n2.dataHolder as SpikingMatrixData).spikes)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), wm1.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(3.0,3.0,3.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(0))
        assertArrayEquals(doubleArrayOf(0.0,0.0,0.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(1))
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), wm1.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(2.0,2.0,2.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(0))
        assertArrayEquals(doubleArrayOf(0.0,0.0,0.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(1))
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0), wm1.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(doubleArrayOf(1.0,1.0,1.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(0))
        assertArrayEquals(doubleArrayOf(0.0,0.0,0.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(1))
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.5, 0.0, 0.25), n3.activationArray, .001)
        net.update() // Step response ends
        assertArrayEquals(doubleArrayOf(0.0, 0.0), wm1.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0), n2.activationArray, .001)
        assertArrayEquals(booleanArrayOf(false, false), (n2.dataHolder as SpikingMatrixData).spikes)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
    }

    @Test
    fun `test step responder values with both nodes spiking`() {

        step.responseHeight = 1.0
        step.responseDuration = 2

        n1.activations = Matrix(doubleArrayOf(1.0, 1.0))
        net.update()
        net.update()
        assertArrayEquals(doubleArrayOf(2.0,2.0,2.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(0))
        assertArrayEquals(doubleArrayOf(2.0,2.0,2.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(1))
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(1.0,1.0,1.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(0))
        assertArrayEquals(doubleArrayOf(1.0,1.0,1.0), (wm2.spikeResponseData as StepMatrixData).counterMatrix.col(1))
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(1.0,-1.0, 1.0), n3.activationArray, .001)
        net.update()
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), wm2.psrMatrix.rowSums())
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), n3.activationArray, .001)
    }


}