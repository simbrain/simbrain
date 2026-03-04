package org.simbrain.custom_sims.simulations.backprop

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.csvToDouble2DArray
import org.simbrain.util.fetchDataWithCache
import org.simbrain.util.place

/**
 * CNN version of the Tiny MNIST simulation.
 *
 * Pipeline: Input(20x20x1) → Conv(3x3, 5 filters, SAME, ReLU) → MaxPool(2x2)
 *         → Conv(3x3, 8 filters, SAME, ReLU) → MaxPool(2x2)
 *         → Flatten(200) → Dense(10)
 *
 * Uses the same 20x20 downsampled MNIST data as tinyMNIST.
 */
val cnnMNIST = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("CNN MNIST")
    val network = networkComponent.network

    // --- Load data ---

    val trainInputsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_inputs.csv") ?: return@newSim
    val trainLabelsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_train_labels.csv") ?: return@newSim
    val testInputsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_inputs.csv") ?: return@newSim
    val testLabelsCSV =
        fetchDataWithCache("https://downloads.simbrain.net/simbraindata/tiny_mnist_test_labels.csv") ?: return@newSim

    val trainingSet = TrainingDataset(
        inputs = csvToDouble2DArray(trainInputsCSV).map { it.toMutableList() }.toMutableList(),
        targets = csvToDouble2DArray(trainLabelsCSV).map { it.toMutableList() }.toMutableList(),
        inputSize = 400,
        targetSize = 10,
    )
    val testingSet = TrainingDataset(
        inputs = csvToDouble2DArray(testInputsCSV).map { it.toMutableList() }.toMutableList(),
        targets = csvToDouble2DArray(testLabelsCSV).map { it.toMutableList() }.toMutableList(),
        inputSize = 400,
        targetSize = 10,
    )

    // --- Build CNN Pipeline ---

    // Compact U-shaped layout coordinates
    val leftX = 0.0
    val rightX = 400.0
    val topY = 0.0
    val stepY = 350.0

    // Input: 20x20x1
    val inputShape = TensorShape(20, 20, 1)
    val inputTensor = Tensor(inputShape).apply {
        label = "Input (20x20x1)"
        isClamped = true
    }
    inputTensor.setLocation(leftX, topY)

    // Conv1: 3x3, 5 filters, SAME → 20x20x5
    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 5)
    val conv1Out = Tensor(conv1OutShape).apply {
        label = "Conv1 (${conv1OutShape})"
        activationFunction = TensorActivation.RELU
    }
    conv1Out.setLocation(leftX, topY + stepY)
    val conv1 = ConvolutionConnector(inputTensor, conv1Out, kernelSize = 3, numFilters = 5, stride = 1, padding = Padding.SAME)

    // Pool1: 2x2 → 10x10x5
    val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
    val pool1Out = Tensor(pool1OutShape).apply {
        label = "Pool1 (${pool1OutShape})"
    }
    pool1Out.setLocation(leftX, topY + stepY * 2)
    val pool1 = PoolingConnector(conv1Out, pool1Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    // Conv2: 3x3, 8 filters, SAME → 10x10x8
    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv2Out = Tensor(conv2OutShape).apply {
        label = "Conv2 (${conv2OutShape})"
        activationFunction = TensorActivation.RELU
    }
    conv2Out.setLocation(leftX, topY + stepY * 3)
    val conv2 = ConvolutionConnector(pool1Out, conv2Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

    // Pool2: 2x2 → 5x5x8
    val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
    val pool2Out = Tensor(pool2OutShape).apply {
        label = "Pool2 (${pool2OutShape})"
    }
    pool2Out.setLocation(rightX, topY + stepY * 3)
    val pool2 = PoolingConnector(conv2Out, pool2Out, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    // Flatten: 5x5x8 = 200
    val flatSize = pool2OutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flatten ($flatSize)"
    }
    flatArray.setLocation(rightX, topY + stepY * 2)
    val flatten = FlattenConnector(pool2Out, flatArray)

    // Dense output: 10 classes
    val outputArray = NeuronArray(10).apply {
        label = "Output (10)"
        updateRule = SoftmaxRule()
        circleMode = true
        gridMode = true
        labelArray = Array(10) { "$it" }
    }
    outputArray.setLocation(rightX, topY)
    val dense = WeightMatrix(flatArray, outputArray)

    // --- Create ConvolutionalNeuralNetwork ---

    val cnnModel = network.addConvolutionalNeuralNetwork(inputTensor, outputArray) {
        label = "CNN MNIST"
        this.trainingSet = trainingSet
        this.testingSet = testingSet
    }
    cnnModel.trainerConfig.apply {
        learningRate = 0.001
        batchSize = 32
        lossFunction = CnnLossFunction.CrossEntropy
    }
    // --- GUI ---

    place(networkComponent, 0, 0, 600, 800)

    addSidebarInfo(
        """
        # CNN MNIST

        A convolutional neural network trained on the same [Tiny MNIST](https://en.wikipedia.org/wiki/MNIST_database)
        dataset as the backprop version, but using convolutional layers to learn spatial features.

        ## Pipeline
        - **Input**: 20x20x1 grayscale digit images
        - **Conv1**: 3x3 kernel, 5 filters, SAME padding, ReLU → 20x20x5
        - **Pool1**: 2x2 max pooling → 10x10x5
        - **Conv2**: 3x3 kernel, 8 filters, SAME padding, ReLU → 10x10x8
        - **Pool2**: 2x2 max pooling → 5x5x8
        - **Flatten**: 5x5x8 = 200
        - **Dense**: 200 → 10 (cross-entropy + softmax)

        ## Training Data
        - **Training**: 10,000 images of handwritten digits
        - **Testing**: 1,000 images

        ## How to Use
        1. Right-click the **CNN MNIST** outline and select **Train...**
        2. Click **Run** to start training, **Stop** to pause
        3. Watch the loss plot decrease
        4. Use the **Training data** / **Testing data** tabs to browse examples
        5. Click **Apply inputs** on a row to see the network's prediction
        """.trimIndent()
    )
}
