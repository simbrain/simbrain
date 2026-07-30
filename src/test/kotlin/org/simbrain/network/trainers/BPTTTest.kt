/**
 * Correctness tests for [accumulateBPTT], the time-unrolled gradient accumulator.
 *
 * The central test compares its weight deltas against finite differences of the windowed loss.
 * Everything built on top of BPTT assumes these numbers are right, so this is the gate for the rest
 * of the feature.
 */
package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.SigmoidalRule
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.random.Random

class BPTTTest {

    /**
     * A three layer network whose hidden layer feeds back into itself, with deterministic weights
     * and biases so that gradient comparisons are reproducible.
     */
    private class RecurrentFixture(seed: Int, sequenceLength: Int) {

        val net = Network()
        val inputLayer = NeuronArray(2).apply { label = "Input"; isClamped = true }
        val hiddenLayer = NeuronArray(3).apply { label = "Hidden"; updateRule = SigmoidalRule() }
        val outputLayer = NeuronArray(2).apply { label = "Output"; updateRule = SigmoidalRule() }

        val inputToHidden: WeightMatrix
        val hiddenToHidden: WeightMatrix
        val hiddenToOutput: WeightMatrix

        val layers: LinkedHashSet<Layer>
        val inputs: List<Matrix>
        val targets: List<Matrix>

        init {
            val random = Random(seed)
            runBlocking { net.addNetworkModelsAsync(inputLayer, hiddenLayer, outputLayer) }

            inputToHidden = WeightMatrix(inputLayer, hiddenLayer)
            hiddenToHidden = WeightMatrix(hiddenLayer, hiddenLayer)
            hiddenToOutput = WeightMatrix(hiddenLayer, outputLayer)
            runBlocking { net.addNetworkModelsAsync(inputToHidden, hiddenToHidden, hiddenToOutput) }

            weightMatrices.forEach { wm ->
                for (i in 0 until wm.weights.nrow()) {
                    for (j in 0 until wm.weights.ncol()) {
                        wm.weights[i, j] = random.nextDouble(-1.0, 1.0)
                    }
                }
            }
            listOf(hiddenLayer, outputLayer).forEach { layer ->
                layer.biases = Matrix.column(DoubleArray(layer.size) { random.nextDouble(-0.5, 0.5) })
            }

            layers = computeOrderedUpdatePath(setOf(inputLayer), outputLayer)
            inputs = List(sequenceLength) { Matrix.column(DoubleArray(2) { random.nextDouble(0.0, 1.0) }) }
            targets = List(sequenceLength) { Matrix.column(DoubleArray(2) { random.nextDouble(0.0, 1.0) }) }
        }

        val weightMatrices get() = listOf(inputToHidden, hiddenToHidden, hiddenToOutput)

        /**
         * Sum of per-step loss for the whole window, replayed from a zeroed hidden state so that it
         * matches the initial condition [accumulateBPTT] uses by default.
         */
        fun windowLoss(): Double {
            hiddenLayer.activations = Matrix(hiddenLayer.size, 1)
            return inputs.indices.sumOf { t ->
                with(net) { layers.forwardPass(listOf(inputs[t]), listOf(inputLayer)) }
                BackpropLossFunction.SSE.scalarLoss(outputLayer.activations, targets[t])
            }
        }

        fun accumulateDeltas(): HashMap<WeightMatrix, Matrix> {
            val weightAccumulator = HashMap<WeightMatrix, Matrix>()
            with(net) {
                layers.accumulateBPTT(
                    inputLayer = inputLayer,
                    outputLayer = outputLayer,
                    inputSequence = inputs,
                    targetSequence = targets,
                    temporalConnectors = listOf(hiddenToHidden),
                    weightAccumulator = weightAccumulator,
                    synapseGroupAccumulator = HashMap(),
                    biasesAccumulator = HashMap(),
                    rawMatrixAccumulator = HashMap(),
                    lossFunction = BackpropLossFunction.SSE
                )
            }
            return weightAccumulator
        }
    }

    @Test
    fun `bptt weight deltas match finite differences of the windowed loss`() {
        val fixture = RecurrentFixture(seed = 42, sequenceLength = 4)
        val deltas = fixture.accumulateDeltas()

        // SSE reports its output error as 2(target - actual), the negative of the loss gradient, and
        // deltas are added to weights. So an accumulated delta is minus the gradient of the loss.
        val epsilon = 1e-6
        fixture.weightMatrices.forEach { wm ->
            val analytic = deltas.getValue(wm)
            for (i in 0 until wm.weights.nrow()) {
                for (j in 0 until wm.weights.ncol()) {
                    val original = wm.weights[i, j]
                    wm.weights[i, j] = original + epsilon
                    val lossAbove = fixture.windowLoss()
                    wm.weights[i, j] = original - epsilon
                    val lossBelow = fixture.windowLoss()
                    wm.weights[i, j] = original

                    val numericalGradient = (lossAbove - lossBelow) / (2 * epsilon)
                    assertEquals(numericalGradient, -analytic[i, j], 1e-6) {
                        "Gradient mismatch for ${wm.displayName} at ($i, $j)"
                    }
                }
            }
        }
    }

    @Test
    fun `gradient reaches the recurrent weights rather than passing them by`() {
        val fixture = RecurrentFixture(seed = 7, sequenceLength = 4)
        val recurrentDeltas = fixture.accumulateDeltas().getValue(fixture.hiddenToHidden)

        val largest = (0 until recurrentDeltas.nrow()).flatMap { i ->
            (0 until recurrentDeltas.ncol()).map { j -> abs(recurrentDeltas[i, j]) }
        }.max()

        assertTrue(largest > 1e-6) {
            "Recurrent weight deltas were all ~zero, so the finite-difference test would pass vacuously"
        }
    }

    @Test
    fun `a single step window leaves the recurrent weights alone`() {
        val fixture = RecurrentFixture(seed = 3, sequenceLength = 1)
        val recurrentDeltas = fixture.accumulateDeltas().getValue(fixture.hiddenToHidden)

        // With one timestep the recurrent matrix only ever sees the zeroed initial hidden state, so
        // it has nothing to learn from. This is the truncation-depth-1 case that an SRN approximates.
        for (i in 0 until recurrentDeltas.nrow()) {
            for (j in 0 until recurrentDeltas.ncol()) {
                assertEquals(0.0, recurrentDeltas[i, j], 1e-12)
            }
        }
    }

    @Test
    fun `the activation trace records every timestep in the window`() {
        val fixture = RecurrentFixture(seed = 21, sequenceLength = 4)
        val trace = mutableListOf<Map<Layer, Matrix>>()

        with(fixture.net) {
            fixture.layers.accumulateBPTT(
                inputLayer = fixture.inputLayer,
                outputLayer = fixture.outputLayer,
                inputSequence = fixture.inputs,
                targetSequence = fixture.targets,
                temporalConnectors = listOf(fixture.hiddenToHidden),
                weightAccumulator = HashMap(),
                synapseGroupAccumulator = HashMap(),
                biasesAccumulator = HashMap(),
                rawMatrixAccumulator = HashMap(),
                lossFunction = BackpropLossFunction.SSE,
                activationTrace = trace
            )
        }

        assertEquals(4, trace.size)

        // The input layer is clamped to the sequence, so each step's entry has to be that step's input.
        trace.forEachIndexed { step, byLayer ->
            val recorded = byLayer.getValue(fixture.inputLayer)
            for (i in 0 until recorded.nrow()) {
                assertEquals(fixture.inputs[step][i, 0], recorded[i, 0], 1e-12) {
                    "Trace step $step should hold that step's input"
                }
            }
        }

        // A recurrent network's hidden state depends on the sequence so far, so it must not be static.
        val hiddenAcrossSteps = trace.map { it.getValue(fixture.hiddenLayer)[0, 0] }
        assertTrue(hiddenAcrossSteps.distinct().size > 1) {
            "Hidden activations were identical at every timestep, so the trace is not per-step"
        }
    }

    @Test
    fun `bptt returns the loss summed over the window`() {
        val fixture = RecurrentFixture(seed = 11, sequenceLength = 5)

        val weightAccumulator = HashMap<WeightMatrix, Matrix>()
        val reportedLoss = with(fixture.net) {
            fixture.layers.accumulateBPTT(
                inputLayer = fixture.inputLayer,
                outputLayer = fixture.outputLayer,
                inputSequence = fixture.inputs,
                targetSequence = fixture.targets,
                temporalConnectors = listOf(fixture.hiddenToHidden),
                weightAccumulator = weightAccumulator,
                synapseGroupAccumulator = HashMap(),
                biasesAccumulator = HashMap(),
                rawMatrixAccumulator = HashMap(),
                lossFunction = BackpropLossFunction.SSE
            )
        }

        assertEquals(fixture.windowLoss(), reportedLoss, 1e-10)
    }
}
