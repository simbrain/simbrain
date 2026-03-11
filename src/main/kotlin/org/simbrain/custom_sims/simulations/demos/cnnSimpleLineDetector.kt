package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.place
import kotlin.random.Random

/**
 * Demo simulation showing a trainable CNN pipeline with synthetic data.
 *
 * Pipeline: Input(16x16x1) -> Conv1(3x3, 8 filters, SAME, ReLU) -> MaxPool(2x2) 
 *         -> Conv2(3x3, 16 filters, SAME, ReLU) -> MaxPool(2x2) -> Flatten -> Dense(3)
 *
 * Three synthetic pattern classes with random positions to demonstrate CNN translation invariance:
 * - Class 0: horizontal line (1 pixel wide, 3-4 pixels long)
 * - Class 1: vertical line (1 pixel wide, 3-4 pixels long)
 * - Class 2: diagonal line (1 pixel wide, 3-4 pixels long)
 */
val cnnSimpleLineDetector = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("CNN Training")
    val network = networkComponent.network

    // Input: 16x16x1
    val inputShape = TensorShape(16, 16, 1)
    val inputLayer = TensorLayer(inputShape).apply {
        label = "Input (16x16x1)"
        isClamped = true
    }
    inputLayer.setLocation(-430.0, 373.0)

    // Conv1: 3x3, 8 filters, SAME -> 16x16x8
    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv1Layer = TensorLayer(conv1OutShape).apply {
        label = "Conv1 (${conv1OutShape})"
        activationFunction = TensorActivation.RELU
    }
    conv1Layer.setLocation(357.0, 375.0)
    val conv1 = ConvolutionConnector(inputLayer, conv1Layer, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME).apply {
        label = "Convolution 1"
    }

    // Pool1: 2x2 -> 8x8x8
    val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
    val pool1Layer = TensorLayer(pool1OutShape).apply {
        label = "Pool1 (${pool1OutShape})"
    }
    pool1Layer.setLocation(-436.0, 127.0)
    val pool1 = PoolingConnector(conv1Layer, pool1Layer, poolSize = 2, stride = 2, poolingType = PoolingType.MAX).apply {
        label = "Pooling 1"
    }

    // Conv2: 3x3, 16 filters, SAME -> 8x8x16
    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 16)
    val conv2Layer = TensorLayer(conv2OutShape).apply {
        label = "Conv2 (${conv2OutShape})"
        activationFunction = TensorActivation.RELU
    }
    conv2Layer.setLocation(391.0, 60.0)
    val conv2 = ConvolutionConnector(pool1Layer, conv2Layer, kernelSize = 3, numFilters = 16, stride = 1, padding = Padding.SAME).apply {
        label = "Convolution 2"
    }

    // Pool2: 2x2 -> 4x4x16
    val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
    val pool2Layer = TensorLayer(pool2OutShape).apply {
        label = "Pool2 (${pool2OutShape})"
    }
    pool2Layer.setLocation(-408.0, -80.0)
    val pool2 = PoolingConnector(conv2Layer, pool2Layer, poolSize = 2, stride = 2, poolingType = PoolingType.MAX).apply {
        label = "Pooling 2"
    }

    // Flatten: 4x4x16 = 256 -> NeuronArray(256)
    val flatSize = pool2OutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flattened ($flatSize)"
        gridMode = true
    }
    flatArray.setLocation(412.0, -294.0)
    val flatten = FlattenConnector(pool2Layer, flatArray).apply {
        label = "Flatten"
    }

    // Dense output: 3 classes
    val outputLayer = NeuronArray(3).apply {
        label = "Output Layer"
        updateRule = SoftmaxRule()
        circleMode = true
        labelArray = arrayOf("Horizontal", "Vertical", "Diagonal")
    }
    outputLayer.setLocation(-430.0, -363.0)
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
        learningRate = 0.01
        batchSize = 30
        lossFunction = CnnLossFunction.CrossEntropy
        computeAccuracy = true
        testConfiguration.enabled = true
        testConfiguration.testFrequency = 10
    }

    // Pre-load input with a random diagonal training image
    val diagonalSampleIndex = 60 + rng.nextInt(30)
    inputLayer.activations = inputs[diagonalSampleIndex].toDoubleArray()

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
        
        You can edit the file corresponding to this to get a sense of performance. For example, with just one CNN layer training performance is perfect but it can't generalize at all.

        ## Pipeline
        - **Input Layer**: 16x16x1 grayscale patterns
        - **Convolution Layer 1**: 3x3 kernel, 8 filters, SAME padding, ReLU → 16x16x8
        - **Pooling Layer 1**: 2x2 max pool → 8x8x8
        - **Convolution Layer 2**: 3x3 kernel, 16 filters, SAME padding, ReLU → 8x8x16
        - **Pooling Layer 2**: 2x2 max pool → 4x4x16
        - **Flatten Layer**: 256 features
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
        
        ## Performance Notes
        Training accuracy typically ranges from 60-80%, varying with random weight 
        initialization. Try randomizing the network a few times and retraining to see 
        how high you can get it!
        
        **Why doesn't it reach 100%?** Possible causes:
        - **Small dataset**: Only 30 samples per class may not be enough
        - **Pattern ambiguity**: Very short lines (3 pixels) can be hard to distinguish
        - **Network capacity**: May need more filters or a different architecture
        - **Training dynamics**: Learning rate, batch size, or optimizer settings
        
        **Want to improve it?** If interested edit the underlying simulation file. You can try:
        - Increase training samples (e.g., 100+ per class)
        - Add more filters to the convolutional layers
        - Adjust learning rate or batch size in the trainer config
        - Add data augmentation (rotations, shifts)
        - Try different weight initialization strategies
        """.trimIndent()
    )
}
