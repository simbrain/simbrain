package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.createSimpleTensorClassificationDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.place

/**
 * Demo simulation showing a trainable CNN pipeline with synthetic data.
 *
 * Pipeline: Input(16x16x1) -> Conv1(3x3, 8 filters, SAME, ReLU) -> MaxPool(2x2) 
 *         -> Conv2(3x3, 16 filters, SAME, ReLU) -> MaxPool(2x2) -> Flatten -> Dense(3)
 *
 * Three synthetic pattern classes with random positions to demonstrate CNN translation invariance.
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

    val fullDataset = createSimpleTensorClassificationDataset(
        inputShape = inputShape,
        nOutputs = 3,
        samplesPerClass = 35,
        rngSeed = 42L
    )
    val (dataset, testDataset) = splitDataSet(fullDataset, 0.85)

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

    // Pre-load input with a sample from the third class.
    val thirdClassIndex = dataset.targets.indexOfFirst { it.getOrNull(2) == 1.0 }.takeIf { it >= 0 } ?: 0
    val thirdClassSample = dataset.inputs[thirdClassIndex]
    inputLayer.activations = thirdClassSample.toDoubleArray()

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

        # Simulation Details

        Three pattern classes are generated from reusable tensor-dataset utilities:

        - **Horizontal** line fragments
        - **Vertical** line fragments
        - **Diagonal** line fragments
        
        Each pattern appears at different locations to demonstrate the CNN's ability
        to learn position-invariant features.

        # What to Do
        1. Right-click the `CNN` outline and select `Train...` to open the training dialog
        2. Use `Step`, `Run`, and `Stop` buttons to train
        3. Watch both training (red) and testing (blue) error decrease
        4. After training, test the network:
           - Switch to the `Testing` tab
           - Browse test samples with the `Inputs` toolbar
           - Click `apply current row as input to network`
           - Watch the output layer neurons show predictions
        
        ## Things to Try
        Training accuracy typically ranges from 60-80%, varying with random weight 
        initialization. Try randomizing the network a few times and retraining to see 
        how high you can get it!
        
        **Why doesn't it reach 100%?** Possible causes:
        - **Small dataset**: Only 30 samples per class may not be enough
        - **Pattern ambiguity**: Very short lines (3 pixels) can be hard to distinguish
        - **Network capacity**: May need more filters or a different architecture
        - **Training dynamics**: Learning rate, batch size, or optimizer settings
        
        To improve it, edit the underlying simulation file and try:
        - Increase training samples (e.g., 100+ per class)
        - Add more filters to the convolutional layers
        - Adjust learning rate or batch size in the trainer config
        - Add data augmentation (rotations, shifts)
        - Try different weight initialization strategies
        """.trimIndent()
    )
}
