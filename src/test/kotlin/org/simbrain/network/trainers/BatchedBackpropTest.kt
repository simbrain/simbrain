/**
 * Batched backprop must train exactly as per-example backprop does: the same parameters after several updates for each
 * supported update rule and loss function, and per-example training for networks it does not support.
 */
package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.GELU
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.NeuronUpdateRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.math.SigmoidFunctionEnum
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class BatchedBackpropTest {

    private fun relu() = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu }
    private fun linear() = LinearRule().apply { clippingType = LinearRule.ClippingType.NoClipping }
    private fun sigmoid(type: SigmoidFunctionEnum) = SigmoidalRule().apply { this.type = type }

    private class Built(val network: Network, val model: SupervisedModel, val parameters: List<Matrix>)

    /**
     * A layered network with seeded weights, biases, and data. [skip] adds a connection from the input to the output.
     */
    private fun build(sizes: List<Int>, rules: List<() -> NeuronUpdateRule<*, *>>, oneHotTargets: Boolean, skip: Boolean): Built {
        val rng = Random(11)
        val net = Network()
        val layers = sizes.mapIndexed { i, size ->
            NeuronArray(size).apply {
                if (i == 0) isClamped = true else updateRule = rules[i - 1]()
                biases = Matrix.column(DoubleArray(size) { rng.nextDouble(-0.5, 0.5) })
            }
        }
        val connections = layers.zipWithNext().map { (a, b) -> WeightMatrix(a, b) } +
                if (skip) listOf(WeightMatrix(layers.first(), layers.last())) else emptyList()
        connections.forEach { wm ->
            for (i in 0 until wm.weights.nrow()) for (j in 0 until wm.weights.ncol()) wm.weights[i, j] = rng.nextDouble(-1.0, 1.0)
        }
        net.addNetworkModelsAsync(layers + connections, usePlacementManager = false)
        val model = SupervisedModel(layers.first(), layers.last())
        val rows = 23
        model.trainingSet = TrainingDataset(
            inputs = MutableList(rows) { MutableList(sizes.first()) { rng.nextDouble() } },
            targets = MutableList(rows) { row ->
                MutableList(sizes.last()) { i -> if (oneHotTargets) (if (i == row % sizes.last()) 1.0 else 0.0) else rng.nextDouble() }
            },
        )
        net.addNetworkModelsAsync(model)
        return Built(net, model, connections.map { it.weights } + layers.drop(1).map { it.biases })
    }

    private fun train(build: () -> Built, loss: BackpropLossFunction, batched: Boolean): Pair<List<DoubleArray>, List<Double>> {
        val built = build()
        built.model.trainerConfig.lossFunction = loss
        built.model.trainerConfig.optimizer = BasicOptimizer(momentum = 0.5).apply { learningRate = 0.05 }
        built.model.trainerConfig.computeAccuracy = true
        val trainer = SupervisedTrainer(built.network, built.model).apply { batchedTrainingEnabled = batched }
        // Uneven batch sizes, including a single example
        val losses = listOf(0 until 10, 10 until 11, 11 until 23, 3 until 17).map { trainer.trainBatch(it) }
        val parameters = (built.network.getModels<WeightMatrix>().map { it.weights } +
                built.network.getModels<NeuronArray>().map { it.biases }).map { it.toArray().flatMap { row -> row.asIterable() }.toDoubleArray() }
        return parameters to losses
    }

    private fun assertSameTraining(build: () -> Built, loss: BackpropLossFunction) {
        assertNotNull(build().let { BatchedBackprop.createOrNull(it.model) }, "network should be eligible for batching")
        val (batchedParameters, batchedLosses) = train(build, loss, batched = true)
        val (exampleParameters, exampleLosses) = train(build, loss, batched = false)
        batchedLosses.zip(exampleLosses).forEach { (a, b) -> assertClose(b, a) }
        batchedParameters.zip(exampleParameters).forEach { (a, b) ->
            assertEquals(b.size, a.size)
            a.indices.forEach { assertClose(b[it], a[it]) }
        }
    }

    private fun assertClose(expected: Double, actual: Double) {
        assertEquals(expected, actual, 1e-10 * max(1.0, abs(expected)))
    }

    @Test
    fun `relu and logistic hidden layers with softmax output and cross entropy train identically`() {
        assertSameTraining({
            build(listOf(6, 8, 5, 4), listOf(::relu, { sigmoid(SigmoidFunctionEnum.LOGISTIC) }, ::SoftmaxRule), oneHotTargets = true, skip = false)
        }, BackpropLossFunction.CrossEntropy)
    }

    @Test
    fun `gelu hidden layer with linear output, skip connection, and mean squared error trains identically`() {
        assertSameTraining({
            build(listOf(5, 7, 3), listOf(::GELU, ::linear), oneHotTargets = false, skip = true)
        }, BackpropLossFunction.MSE)
    }

    @Test
    fun `tanh hidden layer with logistic output and sum squared error trains identically`() {
        assertSameTraining({
            build(listOf(4, 6, 3), listOf({ sigmoid(SigmoidFunctionEnum.TANH) }, { sigmoid(SigmoidFunctionEnum.LOGISTIC) }), oneHotTargets = false, skip = false)
        }, BackpropLossFunction.SSE)
    }

    @Test
    fun `arctan hidden layer with linear output and root mean squared error trains identically`() {
        assertSameTraining({
            build(listOf(4, 6, 2), listOf({ sigmoid(SigmoidFunctionEnum.ARCTAN) }, ::linear), oneHotTargets = false, skip = false)
        }, BackpropLossFunction.RMSE)
    }

    @Test
    fun `layer norm falls back to per example training`() {
        val built = build(listOf(4, 5, 3), listOf(::relu, ::linear), oneHotTargets = false, skip = false)
        built.network.getModels<NeuronArray>().first { !it.isClamped }.useLayerNorm = true
        assertNull(BatchedBackprop.createOrNull(built.model))
    }

    @Test
    fun `unclamped input layer falls back to per example training`() {
        val built = build(listOf(4, 5, 3), listOf(::relu, ::linear), oneHotTargets = false, skip = false)
        built.model.inputLayer.isClamped = false
        assertNull(BatchedBackprop.createOrNull(built.model))
    }
}
