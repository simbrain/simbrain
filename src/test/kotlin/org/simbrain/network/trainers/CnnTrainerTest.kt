package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*

class CnnTrainerTest {

    /**
     * Build a tiny CNN pipeline: 4x4x1 -> Conv(3x3, 2 filters, SAME) -> ReLU -> Flatten -> Dense(2)
     * Train on synthetic data and verify loss decreases.
     */
    @Test
    fun `loss decreases on tiny pipeline`() {
        val net = Network()

        // Input: 4x4x1
        val inputShape = TensorShape(4, 4, 1)
        val inputTensor = Tensor(inputShape).apply {
            label = "Input"
            isClamped = true
        }
        net.addNetworkModelAsync(inputTensor, usePlacementManager = false)

        // Conv: 3x3, 2 filters, SAME -> 4x4x2
        val convOutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = Tensor(convOutShape).apply {
            label = "ConvOut"
            activationFunction = TensorActivation.RELU
        }
        net.addNetworkModelAsync(convOut, usePlacementManager = false)
        val conv = ConvolutionConnector(inputTensor, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        net.addNetworkModelAsync(conv, usePlacementManager = false)

        // Flatten: 4x4x2 = 32 -> NeuronArray(32)
        val flatSize = convOutShape.size
        val flatArray = NeuronArray(flatSize).apply { label = "Flat" }
        net.addNetworkModelAsync(flatArray, usePlacementManager = false)
        val flatten = FlattenConnector(convOut, flatArray)
        net.addNetworkModelAsync(flatten, usePlacementManager = false)

        // Dense: 32 -> 2
        val outputArray = NeuronArray(2).apply { label = "Output" }
        net.addNetworkModelAsync(outputArray, usePlacementManager = false)
        val dense = WeightMatrix(flatArray, outputArray)
        net.addNetworkModelAsync(dense, usePlacementManager = false)

        // Create training data: 2 classes, simple patterns
        val inputs = mutableListOf<MutableList<Double>>()
        val targets = mutableListOf<MutableList<Double>>()

        // Class 0: top-left quadrant active
        repeat(10) {
            val inp = MutableList(16) { 0.0 }
            inp[0] = 1.0; inp[1] = 1.0; inp[4] = 1.0; inp[5] = 1.0
            inputs.add(inp)
            targets.add(mutableListOf(1.0, 0.0))
        }
        // Class 1: bottom-right quadrant active
        repeat(10) {
            val inp = MutableList(16) { 0.0 }
            inp[10] = 1.0; inp[11] = 1.0; inp[14] = 1.0; inp[15] = 1.0
            inputs.add(inp)
            targets.add(mutableListOf(0.0, 1.0))
        }

        val dataset = TrainingDataset(inputs, targets, inputSize = 16, targetSize = 2)

        // Create trainer
        val config = CnnTrainerConfig().apply {
            learningRate = 0.01
            batchSize = 20
            lossFunction = CnnLossFunction.CrossEntropy
        }
        val trainer = CnnTrainer(net, inputTensor, outputArray, config)
        trainer.trainingData = dataset

        // Record initial loss
        val initialLoss = trainer.trainBatch(0 until 20)

        // Train several more iterations
        var lastLoss = initialLoss
        repeat(30) {
            lastLoss = trainer.trainBatch(0 until 20)
        }

        // Loss should decrease significantly
        assertTrue(lastLoss < initialLoss * 0.5,
            "Loss should decrease: initial=$initialLoss, final=$lastLoss")
    }

    /**
     * Verify all parameter gradients are non-zero after one backward pass.
     */
    @Test
    fun `gradient flow test - all gradients non-zero`() {
        val net = Network()

        val inputShape = TensorShape(4, 4, 1)
        val inputTensor = Tensor(inputShape).apply {
            isClamped = true
        }
        net.addNetworkModelAsync(inputTensor, usePlacementManager = false)

        val convOutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = Tensor(convOutShape).apply {
            activationFunction = TensorActivation.RELU
        }
        net.addNetworkModelAsync(convOut, usePlacementManager = false)
        val conv = ConvolutionConnector(inputTensor, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        net.addNetworkModelAsync(conv, usePlacementManager = false)

        val flatSize = convOutShape.size
        val flatArray = NeuronArray(flatSize)
        net.addNetworkModelAsync(flatArray, usePlacementManager = false)
        val flatten = FlattenConnector(convOut, flatArray)
        net.addNetworkModelAsync(flatten, usePlacementManager = false)

        val outputArray = NeuronArray(2)
        net.addNetworkModelAsync(outputArray, usePlacementManager = false)
        val dense = WeightMatrix(flatArray, outputArray)
        net.addNetworkModelAsync(dense, usePlacementManager = false)
        // Make gradients deterministic and avoid dead ReLUs from random initialization.
        conv.kernels.fill(0.1)
        conv.filterBiases.fill(0.1)
        dense.setWeights(
            DoubleArray(outputArray.size * flatArray.size) { idx ->
                if (idx < flatArray.size) 0.2 else 0.05
            }
        )

        // Non-zero input
        val input = DoubleArray(16) { 1.0 }
        val target = doubleArrayOf(1.0, 0.0)

        val config = CnnTrainerConfig().apply {
            lossFunction = CnnLossFunction.CrossEntropy
        }
        val trainer = CnnTrainer(net, inputTensor, outputArray, config)

        // Clear and run forward + backward
        convOut.clearGradients()
        inputTensor.clearGradients()
        conv.clearGrads()

        trainer.forwardPass(input)
        trainer.backwardPass(target)

        // Check conv kernel gradients are non-zero
        val kernelGradSum = conv.kernelGrads.sumOf { kotlin.math.abs(it) }
        assertTrue(kernelGradSum > 0.0, "Conv kernel gradients should be non-zero")

        // Check conv bias gradients are non-zero
        val biasGradSum = conv.biasGrads.sumOf { kotlin.math.abs(it) }
        assertTrue(biasGradSum > 0.0, "Conv bias gradients should be non-zero")

        // Check that tensor gradients flowed back
        val tensorGradSum = convOut.gradients.sumOf { kotlin.math.abs(it) }
        assertTrue(tensorGradSum > 0.0, "ConvOut tensor gradients should be non-zero")
    }

    /**
     * Test with SSE loss function.
     */
    @Test
    fun `SSE loss decreases on tiny pipeline`() {
        val net = Network()

        val inputShape = TensorShape(4, 4, 1)
        val inputTensor = Tensor(inputShape).apply { isClamped = true }
        net.addNetworkModelAsync(inputTensor, usePlacementManager = false)

        val convOutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = Tensor(convOutShape).apply { activationFunction = TensorActivation.RELU }
        net.addNetworkModelAsync(convOut, usePlacementManager = false)
        val conv = ConvolutionConnector(inputTensor, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        net.addNetworkModelAsync(conv, usePlacementManager = false)

        val flatArray = NeuronArray(convOutShape.size)
        net.addNetworkModelAsync(flatArray, usePlacementManager = false)
        val flatten = FlattenConnector(convOut, flatArray)
        net.addNetworkModelAsync(flatten, usePlacementManager = false)

        val outputArray = NeuronArray(2)
        net.addNetworkModelAsync(outputArray, usePlacementManager = false)
        val dense = WeightMatrix(flatArray, outputArray)
        net.addNetworkModelAsync(dense, usePlacementManager = false)

        val inputs = mutableListOf<MutableList<Double>>()
        val targets = mutableListOf<MutableList<Double>>()
        repeat(10) {
            inputs.add(MutableList(16) { if (it < 8) 1.0 else 0.0 })
            targets.add(mutableListOf(1.0, 0.0))
        }
        repeat(10) {
            inputs.add(MutableList(16) { if (it >= 8) 1.0 else 0.0 })
            targets.add(mutableListOf(0.0, 1.0))
        }

        val config = CnnTrainerConfig().apply {
            learningRate = 0.001
            batchSize = 20
            lossFunction = CnnLossFunction.SSE
        }
        val trainer = CnnTrainer(net, inputTensor, outputArray, config)
        trainer.trainingData = TrainingDataset(inputs, targets, 16, 2)

        val initialLoss = trainer.trainBatch(0 until 20)
        var lastLoss = initialLoss
        repeat(50) { lastLoss = trainer.trainBatch(0 until 20) }

        assertTrue(lastLoss < initialLoss, "SSE loss should decrease: initial=$initialLoss, final=$lastLoss")
    }

    @Test
    fun `trainOnce increments iteration and computes testing accuracy when enabled`() = runBlocking {
        val net = Network()
        val inputShape = TensorShape(4, 4, 1)
        val inputTensor = Tensor(inputShape).apply { isClamped = true }
        net.addNetworkModelAsync(inputTensor, usePlacementManager = false)

        val convOutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = Tensor(convOutShape).apply { activationFunction = TensorActivation.RELU }
        net.addNetworkModelAsync(convOut, usePlacementManager = false)
        val conv = ConvolutionConnector(inputTensor, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        net.addNetworkModelAsync(conv, usePlacementManager = false)

        val flatArray = NeuronArray(convOutShape.size)
        net.addNetworkModelAsync(flatArray, usePlacementManager = false)
        val flatten = FlattenConnector(convOut, flatArray)
        net.addNetworkModelAsync(flatten, usePlacementManager = false)

        val outputArray = NeuronArray(2)
        net.addNetworkModelAsync(outputArray, usePlacementManager = false)
        val dense = WeightMatrix(flatArray, outputArray)
        net.addNetworkModelAsync(dense, usePlacementManager = false)

        val inputs = mutableListOf<MutableList<Double>>()
        val targets = mutableListOf<MutableList<Double>>()
        repeat(8) {
            val inp = MutableList(16) { 0.0 }
            inp[0] = 1.0; inp[1] = 1.0; inp[4] = 1.0; inp[5] = 1.0
            inputs.add(inp)
            targets.add(mutableListOf(1.0, 0.0))
        }
        repeat(8) {
            val inp = MutableList(16) { 0.0 }
            inp[10] = 1.0; inp[11] = 1.0; inp[14] = 1.0; inp[15] = 1.0
            inputs.add(inp)
            targets.add(mutableListOf(0.0, 1.0))
        }

        val dataset = TrainingDataset(inputs, targets, inputSize = 16, targetSize = 2)
        val config = CnnTrainerConfig().apply {
            learningRate = 0.005
            batchSize = 8
            lossFunction = CnnLossFunction.CrossEntropy
            computeAccuracy = true
            testConfiguration.enabled = true
            testConfiguration.testFrequency = 1
        }
        val trainer = CnnTrainer(net, inputTensor, outputArray, config)
        trainer.trainingData = dataset
        trainer.testingData = dataset

        trainer.trainOnce()

        assertEquals(1, trainer.iteration)
        assertTrue(trainer.lastTrainingError.isFinite())
        assertNotNull(trainer.lastTrainingAccuracy)
        assertNotNull(trainer.lastTestingAccuracy)
    }

    @Test
    fun `flatten backward overwrites stale tensor gradients`() {
        val source = Tensor(TensorShape(2, 2, 1))
        val target = NeuronArray(4)
        val flatten = FlattenConnector(source, target)

        source.gradients.fill(1.0)
        flatten.backward(doubleArrayOf(0.2, 0.3, 0.4, 0.5))

        assertArrayEquals(doubleArrayOf(0.2, 0.3, 0.4, 0.5), source.gradients, 1e-12)
    }
}
