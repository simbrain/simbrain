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
 * Pipeline: Input(16x16x1) -> Conv(3x3, 8 filters, SAME, ReLU) -> MaxPool(2x2) -> Flatten -> Dense(3)
 *
 * Three synthetic pattern classes with random positions to demonstrate CNN translation invariance:
 * - Class 0: horizontal line (1 pixel wide, 3-4 pixels long)
 * - Class 1: vertical line (1 pixel wide, 3-4 pixels long)
 * - Class 2: diagonal line (1 pixel wide, 3-4 pixels long)
 */
val cnnTrainingDemo = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("CNN Training")
    val network = networkComponent.network

    // Input: 16x16x1
    val inputShape = TensorShape(16, 16, 1)
    val inputLayer = TensorLayer(inputShape).apply {
        label = "Input Layer"
        isClamped = true
    }
    inputLayer.setLocation(-786.2994660296812, 398.3040858920673)

    // Conv: 3x3, 8 filters, SAME -> 16x16x8
    val convOutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 8)
    val convLayer = TensorLayer(convOutShape).apply {
        label = "Feature Map"
        activationFunction = TensorActivation.RELU
    }
    convLayer.setLocation(0.0, 400.0)
    val conv = ConvolutionConnector(inputLayer, convLayer, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME).apply {
        label = "Convolution"
    }

    // MaxPool: 2x2 -> 8x8x8
    val poolOutShape = convOutShape.poolOutputShape(2, 2)
    val poolOut = TensorLayer(poolOutShape).apply {
        label = "Pooled Layer"
    }
    poolOut.setLocation(-784.9973490798225, 75.65382689571582)
    val pool = PoolingConnector(convLayer, poolOut, poolSize = 2, stride = 2, poolingType = PoolingType.MAX).apply {
        label = "Pooling"
    }

    // Flatten: 8x8x8 = 512 -> NeuronArray(512)
    val flatSize = poolOutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flattened Layer"
    }
    flatArray.setLocation(72.39336683145689, -125.50848409739262)
    val flatten = FlattenConnector(poolOut, flatArray).apply {
        label = "Flatten"
    }

    // Dense output: 3 classes
    val outputLayer = NeuronArray(3).apply {
        label = "Output Layer"
        circleMode = true
        labelArray = arrayOf("Horizontal", "Vertical", "Diagonal")
    }
    outputLayer.setLocation(-773.5011725522106, -175.7068655854972)
    val dense = WeightMatrix(flatArray, outputLayer).apply {
        label = "Dense Connector"
    }

    // Generate synthetic training data

    val rng = Random(42)
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    val imageSize = 16

    // Class 0: horizontal line (1 pixel wide, 3-4 pixels long) at random positions
    repeat(30) {
        val img = MutableList(imageSize * imageSize) { 0.0 }
        val length = rng.nextInt(3, 5)
        val startRow = rng.nextInt(imageSize)
        val startCol = rng.nextInt(imageSize - length + 1)
        for (col in startCol until (startCol + length)) {
            img[startRow * imageSize + col] = 1.0
        }
        inputs.add(img)
        targets.add(mutableListOf(1.0, 0.0, 0.0))
    }

    // Class 1: vertical line (1 pixel wide, 3-4 pixels long) at random positions
    repeat(30) {
        val img = MutableList(imageSize * imageSize) { 0.0 }
        val length = rng.nextInt(3, 5)
        val startRow = rng.nextInt(imageSize - length + 1)
        val startCol = rng.nextInt(imageSize)
        for (row in startRow until (startRow + length)) {
            img[row * imageSize + startCol] = 1.0
        }
        inputs.add(img)
        targets.add(mutableListOf(0.0, 1.0, 0.0))
    }

    // Class 2: diagonal line (3-4 pixels long) at random positions
    repeat(30) {
        val img = MutableList(imageSize * imageSize) { 0.0 }
        val length = rng.nextInt(3, 5)
        val startRow = rng.nextInt(imageSize - length + 1)
        val startCol = rng.nextInt(imageSize - length + 1)
        for (i in 0 until length) {
            img[(startRow + i) * imageSize + (startCol + i)] = 1.0
        }
        inputs.add(img)
        targets.add(mutableListOf(0.0, 0.0, 1.0))
    }

    val dataset = TrainingDataset(inputs, targets, inputSize = imageSize * imageSize, targetSize = 3)

    // --- Generate testing data ---

    val testInputs = mutableListOf<MutableList<Double>>()
    val testTargets = mutableListOf<MutableList<Double>>()

    // Class 0: horizontal line at different positions (5 test samples)
    repeat(5) {
        val img = MutableList(imageSize * imageSize) { 0.0 }
        val length = rng.nextInt(3, 5)
        val startRow = rng.nextInt(imageSize)
        val startCol = rng.nextInt(imageSize - length + 1)
        for (col in startCol until (startCol + length)) {
            img[startRow * imageSize + col] = 1.0
        }
        testInputs.add(img)
        testTargets.add(mutableListOf(1.0, 0.0, 0.0))
    }

    // Class 1: vertical line at different positions (5 test samples)
    repeat(5) {
        val img = MutableList(imageSize * imageSize) { 0.0 }
        val length = rng.nextInt(3, 5)
        val startRow = rng.nextInt(imageSize - length + 1)
        val startCol = rng.nextInt(imageSize)
        for (row in startRow until (startRow + length)) {
            img[row * imageSize + startCol] = 1.0
        }
        testInputs.add(img)
        testTargets.add(mutableListOf(0.0, 1.0, 0.0))
    }

    // Class 2: diagonal at different positions (5 test samples)
    repeat(5) {
        val img = MutableList(imageSize * imageSize) { 0.0 }
        val length = rng.nextInt(3, 5)
        val startRow = rng.nextInt(imageSize - length + 1)
        val startCol = rng.nextInt(imageSize - length + 1)
        for (i in 0 until length) {
            img[(startRow + i) * imageSize + (startCol + i)] = 1.0
        }
        testInputs.add(img)
        testTargets.add(mutableListOf(0.0, 0.0, 1.0))
    }

    val testDataset = TrainingDataset(testInputs, testTargets, inputSize = imageSize * imageSize, targetSize = 3)

    // Automatically discovers pipeline from input to output
    val cnnModel = network.addConvolutionalNeuralNetwork(inputLayer, outputLayer) {
        label = "CNN Classifier"
        trainingSet = dataset
        testingSet = testDataset
    }
    cnnModel.trainerConfig.apply {
        learningRate = 0.001
        batchSize = 30
        lossFunction = CnnLossFunction.CrossEntropy
        computeAccuracy = true
        testConfiguration.enabled = true
        testConfiguration.testFrequency = 10
    }

    // Pre-load input with a random diagonal training image
    val diagonalSampleIndex = 60 + rng.nextInt(30)
    inputLayer.setActivations(inputs[diagonalSampleIndex].toDoubleArray())

    place(networkComponent, 0, 0, 850, 730)
    workspace.simpleIterate() // So some activations are shown

    addSidebarInfo(
        """
        # CNN Training Demo

        This simulation demonstrates training a small convolutional neural network
        on synthetic pattern data using backpropagation with Adam optimizer.
        
        The key feature is **translation invariance**: patterns appear at random positions
        in the 16x16 input images, and the CNN learns to recognize them regardless of 
        where they appear.

        ## Pipeline
        - **Input Layer**: 16x16x1 grayscale patterns
        - **Convolution Layer**: 3x3 kernel, 8 filters, SAME padding, ReLU
        - **Pooling Layer**: 2x2 max pool → 8x8x8
        - **Flatten Layer**: 512 features
        - **Output Layer**: 3 output neurons (cross-entropy + softmax)

        ## Training Data
        Three pattern classes (30 training, 5 test samples each):
        - **Horizontal**: 1 pixel wide, 3-4 pixels long horizontal line at random positions
        - **Vertical**: 1 pixel wide, 3-4 pixels long vertical line at random positions  
        - **Diagonal**: 1 pixel wide, 3-4 pixels long diagonal line at random positions
        
        Each pattern appears at different locations to demonstrate the CNN's ability
        to learn position-invariant features.

        ## How to use
        1. Right-click the CNN outline → **Train...** to open the training dialog
        2. Use Step/Run/Stop buttons to train
        3. Watch both training (red) and testing (blue) error decrease
        4. After training, test the network:
           - Switch to the **Testing** tab
           - Browse test samples with the "Inputs" toolbar
           - Click **apply current row as input to network**
           - Watch the output layer neurons show predictions
        """.trimIndent()
    )
}
