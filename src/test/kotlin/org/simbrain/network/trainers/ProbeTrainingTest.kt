package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
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

}
