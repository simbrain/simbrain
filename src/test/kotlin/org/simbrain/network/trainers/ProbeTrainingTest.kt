package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.flatten

class ProbeTrainingTest {

    @Test
    fun `training a probe on an interior layer leaves the host network untouched`() = runBlocking {
        val network = Network()

        val hostInput = NeuronArray(4).apply { isClamped = true; label = "Host input" }
        val hostHidden = NeuronArray(3).apply { updateRule = SigmoidalRule(); label = "Host hidden" }
        val hostOutput = NeuronArray(2).apply { updateRule = SigmoidalRule(); label = "Host output" }
        val hostWm1 = WeightMatrix(hostInput, hostHidden)
        val hostWm2 = WeightMatrix(hostHidden, hostOutput)

        val readout = NeuronArray(2).apply { updateRule = SigmoidalRule(); label = "Probe readout" }
        val probeWm = WeightMatrix(hostHidden, readout)
        val probe = SupervisedModel(hostHidden, readout)

        network.addNetworkModelsAsync(
            hostInput, hostHidden, hostOutput, hostWm1, hostWm2, readout, probeWm, probe
        )

        probe.trainingSet = TrainingDataset(
            inputs = mutableListOf(
                mutableListOf(0.0, 0.5, 1.0),
                mutableListOf(1.0, 0.0, 0.5),
                mutableListOf(0.5, 1.0, 0.0),
            ),
            targets = mutableListOf(
                mutableListOf(1.0, 0.0),
                mutableListOf(0.0, 1.0),
                mutableListOf(1.0, 1.0),
            )
        )

        val hostWm1Before = hostWm1.weights.flatten()
        val hostWm2Before = hostWm2.weights.flatten()
        val hostInputBiasesBefore = hostInput.biases.flatten()
        val hostHiddenBiasesBefore = hostHidden.biases.flatten()
        val hostOutputBiasesBefore = hostOutput.biases.flatten()
        val probeWmBefore = probeWm.weights.flatten()

        val trainer = SupervisedTrainer(network, probe)
        with(network) {
            repeat(10) {
                trainer.trainBatch(0 until probe.trainingSet.size)
            }
        }

        assertArrayEquals(hostWm1Before, hostWm1.weights.flatten(), 0.0) {
            "Host weights upstream of the probed layer should be untouched by probe training"
        }
        assertArrayEquals(hostWm2Before, hostWm2.weights.flatten(), 0.0) {
            "Host weights downstream of the probed layer should be untouched by probe training"
        }
        assertArrayEquals(hostInputBiasesBefore, hostInput.biases.flatten(), 0.0)
        assertArrayEquals(hostHiddenBiasesBefore, hostHidden.biases.flatten(), 0.0) {
            "Probed layer biases should be untouched by probe training"
        }
        assertArrayEquals(hostOutputBiasesBefore, hostOutput.biases.flatten(), 0.0)

        assertFalse(probeWmBefore.contentEquals(probeWm.weights.flatten())) {
            "Probe weights should change during probe training"
        }
    }

    @Test
    fun `forward pass does not overwrite an unclamped interior input layer's activations`() = runBlocking {
        val network = Network()

        val hostInput = NeuronArray(2).apply { isClamped = true; label = "Host input" }
        val hostHidden = NeuronArray(2).apply { updateRule = SigmoidalRule(); label = "Host hidden" }
        val hostWm = WeightMatrix(hostInput, hostHidden)

        val readout = NeuronArray(2).apply { updateRule = SigmoidalRule(); label = "Probe readout" }
        val probeWm = WeightMatrix(hostHidden, readout)
        val probe = SupervisedModel(hostHidden, readout)

        network.addNetworkModelsAsync(hostInput, hostHidden, hostWm, readout, probeWm, probe)

        // Host input chosen so that recomputing the hidden layer from it would produce
        // something other than the harvested values set below
        hostInput.setActivations(doubleArrayOf(5.0, -5.0))

        val harvestedRow = doubleArrayOf(0.25, 0.75)
        with(network) {
            hostHidden.setActivations(harvestedRow)
            probe.forwardPass()
        }

        assertArrayEquals(harvestedRow, hostHidden.activationArray, 0.0) {
            "Forward pass should preserve the activations set on the probe's input layer"
        }
    }

    @Test
    fun `probe on a backprop subnetwork hidden layer trains on harvested activations without touching the host`() = runBlocking {
        val network = Network()

        val bp = BackpropNetwork(intArrayOf(4, 3, 2), null)
        network.addNetworkModelsAsync(bp)
        val hidden = bp.hiddenLayers().first()

        val readout = NeuronArray(2).apply { updateRule = SigmoidalRule(); label = "Probe readout" }
        val probeWm = WeightMatrix(hidden, readout)
        val probe = SupervisedModel(hidden, readout)
        network.addNetworkModelsAsync(readout, probeWm, probe)

        val hostInputs = listOf(
            doubleArrayOf(0.0, 0.0, 1.0, 1.0),
            doubleArrayOf(1.0, 1.0, 0.0, 0.0),
            doubleArrayOf(1.0, 0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0, 1.0),
        )
        val harvested = hostInputs.map { row ->
            with(network) {
                bp.inputLayer.setActivations(row)
                bp.forwardPass()
            }
            hidden.activationArray.toMutableList()
        }.toMutableList()

        probe.trainingSet = TrainingDataset(
            inputs = harvested,
            targets = hostInputs.map { row ->
                if (row[0] > 0.5) mutableListOf(1.0, 0.0) else mutableListOf(0.0, 1.0)
            }.toMutableList(),
            inputSize = hidden.size,
            targetSize = 2,
        )

        val hostWeightsBefore = bp.wmList.map { it.weights.flatten() }
        val hostBiasesBefore = bp.layerList.map { it.biases.flatten() }
        val probeWmBefore = probeWm.weights.flatten()

        val trainer = SupervisedTrainer(network, probe)
        with(network) {
            repeat(10) {
                trainer.trainBatch(0 until probe.trainingSet.size)
            }
        }

        bp.wmList.zip(hostWeightsBefore).forEach { (wm, before) ->
            assertArrayEquals(before, wm.weights.flatten(), 0.0) {
                "Host subnetwork weights should be untouched by probe training"
            }
        }
        bp.layerList.zip(hostBiasesBefore).forEach { (layer, before) ->
            assertArrayEquals(before, layer.biases.flatten(), 0.0) {
                "Host subnetwork biases should be untouched by probe training"
            }
        }
        assertFalse(probeWmBefore.contentEquals(probeWm.weights.flatten())) {
            "Probe weights should change during probe training"
        }
    }

}
