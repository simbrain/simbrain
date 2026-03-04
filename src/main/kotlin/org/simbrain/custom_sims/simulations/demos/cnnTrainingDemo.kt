package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.place
import kotlin.random.Random

/**
 * Demo simulation showing a trainable CNN pipeline with synthetic data.
 *
 * Pipeline: Input(8x8x1) -> Conv(3x3, 4 filters, SAME, ReLU) -> MaxPool(2x2) -> Flatten -> Dense(3)
 *
 * Three synthetic pattern classes:
 * - Class 0: horizontal stripe (rows 2-3)
 * - Class 1: vertical stripe (cols 2-3)
 * - Class 2: diagonal pattern
 */
val cnnTrainingDemo = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("CNN Training")
    val network = networkComponent.network

    // --- Build CNN Pipeline ---

    // Input: 8x8x1
    val inputShape = TensorShape(8, 8, 1)
    val inputTensor = Tensor(inputShape).apply {
        label = "Input (8x8x1)"
        isClamped = true
    }
    inputTensor.setLocation(0.0, 0.0)

    // Conv: 3x3, 4 filters, SAME -> 8x8x4
    val convOutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 4)
    val convOut = Tensor(convOutShape).apply {
        label = "Conv (${convOutShape})"
        activationFunction = TensorActivation.RELU
    }
    convOut.setLocation(0.0, 200.0)
    val conv = ConvolutionConnector(inputTensor, convOut, kernelSize = 3, numFilters = 4, stride = 1, padding = Padding.SAME)

    // MaxPool: 2x2 -> 4x4x4
    val poolOutShape = convOutShape.poolOutputShape(2, 2)
    val poolOut = Tensor(poolOutShape).apply {
        label = "Pool (${poolOutShape})"
    }
    poolOut.setLocation(0.0, 400.0)
    val pool = PoolingConnector(convOut, poolOut, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    // Flatten: 4x4x4 = 64 -> NeuronArray(64)
    val flatSize = poolOutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flatten ($flatSize)"
    }
    flatArray.setLocation(0.0, 600.0)
    val flatten = FlattenConnector(poolOut, flatArray)

    // Dense output: 3 classes
    val outputArray = NeuronArray(3).apply {
        label = "Output (3)"
    }
    outputArray.setLocation(0.0, 800.0)
    val dense = WeightMatrix(flatArray, outputArray)

    // --- Generate synthetic training data ---

    val rng = Random(42)
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()

    // Class 0: horizontal stripe (rows 3-4)
    repeat(30) {
        val img = MutableList(64) { 0.0 }
        for (w in 0 until 8) {
            img[3 * 8 + w] = 0.8 + rng.nextDouble() * 0.2
            img[4 * 8 + w] = 0.8 + rng.nextDouble() * 0.2
        }
        // Add small noise
        for (i in img.indices) img[i] += rng.nextDouble() * 0.1
        inputs.add(img)
        targets.add(mutableListOf(1.0, 0.0, 0.0))
    }

    // Class 1: vertical stripe (cols 3-4)
    repeat(30) {
        val img = MutableList(64) { 0.0 }
        for (h in 0 until 8) {
            img[h * 8 + 3] = 0.8 + rng.nextDouble() * 0.2
            img[h * 8 + 4] = 0.8 + rng.nextDouble() * 0.2
        }
        for (i in img.indices) img[i] += rng.nextDouble() * 0.1
        inputs.add(img)
        targets.add(mutableListOf(0.0, 1.0, 0.0))
    }

    // Class 2: diagonal
    repeat(30) {
        val img = MutableList(64) { 0.0 }
        for (k in 0 until 8) {
            img[k * 8 + k] = 0.8 + rng.nextDouble() * 0.2
            if (k + 1 < 8) img[k * 8 + k + 1] = 0.5 + rng.nextDouble() * 0.2
        }
        for (i in img.indices) img[i] += rng.nextDouble() * 0.1
        inputs.add(img)
        targets.add(mutableListOf(0.0, 0.0, 1.0))
    }

    val dataset = TrainingDataset(inputs, targets, inputSize = 64, targetSize = 3)

    // --- Create ConvolutionalNeuralNetwork ---

    val cnnModel = network.addConvolutionalNeuralNetwork(inputTensor, outputArray) {
        label = "CNN Classifier"
        trainingSet = dataset
    }
    cnnModel.trainerConfig.apply {
        learningRate = 0.001
        batchSize = 30
        lossFunction = CnnLossFunction.CrossEntropy
    }
    // --- GUI ---

    place(networkComponent, 0, 0, 500, 900)

    addSidebarInfo(
        """
        # CNN Training Demo

        This simulation demonstrates training a small convolutional neural network
        on synthetic pattern data using backpropagation with Adam optimizer.

        ## Pipeline
        - **Input**: 8x8x1 grayscale patterns
        - **Conv**: 3x3 kernel, 4 filters, SAME padding, ReLU
        - **MaxPool**: 2x2 pool
        - **Flatten**: 4x4x4 = 64 -> NeuronArray
        - **Dense**: 64 -> 3 output neurons (cross-entropy + softmax)

        ## Training Data
        Three synthetic pattern classes (30 samples each):
        - **Class 0**: Horizontal stripe
        - **Class 1**: Vertical stripe
        - **Class 2**: Diagonal stripe

        ## How to use
        - Right-click the CNN outline -> **Train...** to open the training dialog
        - Use Step/Run/Stop buttons to train
        - Watch the loss plot decrease as training progresses
        """.trimIndent()
    )
}
