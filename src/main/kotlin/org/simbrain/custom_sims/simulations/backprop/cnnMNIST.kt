package org.simbrain.custom_sims.simulations.backprop

import org.simbrain.custom_sims.*
import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.getCnnTrainingDialog
import org.simbrain.network.trainers.CnnLossFunction
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.place
import org.simbrain.util.placeBeside
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import javax.swing.SwingUtilities

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

    // Load data

    val data = loadTinyMnist() ?: return@newSim
    val trainingSet = data.trainingSet
    val testingSet = data.testingSet

    // Build CNN Pipeline

    // U-shaped layout coordinates use layer centers. Keep the second convolution on the right leg
    // so the convolution connector previews do not crowd the left stack.
    val leftX = -275.0
    val rightX = 275.0
    val inputY = -446.0
    val conv1Y = 24.0
    val pool1Y = 449.0
    val conv2Y = 446.0
    val pool2Y = 144.0
    val flatY = -125.0
    val outputY = -442.0

    // Input: 20x20x1
    val inputShape = TensorShape(20, 20, 1)
    val inputTensorLayer = TensorLayer(inputShape).apply {
        label = "Input"
        isClamped = true
    }
    inputTensorLayer.setLocation(leftX, inputY)

    // Conv1: 3x3, 5 filters, SAME → 20x20x5
    val conv1OutShape = inputShape.convOutputShape(3, 1, Padding.SAME, 5)
    val conv1Out = TensorLayer(conv1OutShape).apply {
        label = "Conv1"
        activationFunction = TensorActivation.RELU
    }
    conv1Out.setLocation(leftX, conv1Y)
    val conv1 = ConvolutionConnector(inputTensorLayer, conv1Out, kernelSize = 3, numFilters = 5, stride = 1, padding = Padding.SAME)

    // Pool1: 2x2 → 10x10x5
    val pool1OutShape = conv1OutShape.poolOutputShape(2, 2)
    val poolLayer1 = TensorLayer(pool1OutShape).apply {
        label = "Pool1"
    }
    poolLayer1.setLocation(leftX, pool1Y)
    val pool1 = PoolingConnector(conv1Out, poolLayer1, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    // Conv2: 3x3, 8 filters, SAME → 10x10x8
    val conv2OutShape = pool1OutShape.convOutputShape(3, 1, Padding.SAME, 8)
    val conv2Out = TensorLayer(conv2OutShape).apply {
        label = "Conv2"
        activationFunction = TensorActivation.RELU
    }
    conv2Out.setLocation(rightX, conv2Y)
    val conv2 = ConvolutionConnector(poolLayer1, conv2Out, kernelSize = 3, numFilters = 8, stride = 1, padding = Padding.SAME)

    // Pool2: 2x2 → 5x5x8
    val pool2OutShape = conv2OutShape.poolOutputShape(2, 2)
    val poolLayer2 = TensorLayer(pool2OutShape).apply {
        label = "Pool2"
    }
    poolLayer2.setLocation(rightX, pool2Y)
    val pool2 = PoolingConnector(conv2Out, poolLayer2, poolSize = 2, stride = 2, poolingType = PoolingType.MAX)

    // Flatten: 5x5x8 = 200
    val flatSize = pool2OutShape.size
    val flatArray = NeuronArray(flatSize).apply {
        label = "Flatten"
    }
    flatArray.setLocation(rightX, flatY)
    val flatten = FlattenConnector(poolLayer2, flatArray)

    // Dense output: 10 classes
    val outputArray = NeuronArray(10).apply {
        label = "Output"
        updateRule = SoftmaxRule()
        circleMode = true
        gridMode = true
        labelArray = Array(10) { "$it" }
    }
    outputArray.setLocation(rightX, outputY)
    val dense = WeightMatrix(flatArray, outputArray)

    // Create ConvolutionalNeuralNetwork

    val cnnModel = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
        label = "CNN MNIST"
        this.trainingSet = trainingSet
        this.testingSet = testingSet
    }
    cnnModel.trainerConfig.apply {
        learningRate = 0.001
        batchSize = 32
        lossFunction = CnnLossFunction.CrossEntropy
        computeAccuracy = true
        testConfiguration.enabled = true
        testConfiguration.testFrequency = 10
    }

    // Used only for its forward pass, so stop the task processor every trainer starts
    val evaluationTrainer = CnnTrainer(network, inputTensorLayer, outputArray, cnnModel.trainerConfig)
        .apply { job.cancel() }

    // Pre-load input with a random training image
    val randomSampleIndex = kotlin.random.Random.nextInt(trainingSet.inputs.size)
    inputTensorLayer.activations = trainingSet.inputs[randomSampleIndex]
    
    // GUI

    withGui {
        val networkPanel = getNetworkPanel(networkComponent)
        var drawingWorld: ImageWorldComponent? = null
        val model = object : TinyMnistModel {
            override var trainingSet: TrainingDataset
                get() = cnnModel.trainingSet
                set(value) {
                    cnnModel.trainingSet = value
                }
            override var testingSet: TrainingDataset
                get() = cnnModel.testingSet
                set(value) {
                    cnnModel.testingSet = value
                }

            override suspend fun show(image: DoubleArray): DoubleArray {
                beginEvaluation()
                val output = predict(image)
                // The trainer's forward pass writes the tensor stages directly, so copy its results into the
                // remaining layers and fire update events so the GUI repaints, as the training dialog does
                outputArray.setActivations(output)
                flatArray.setActivations(evaluationTrainer.tensorLayerStages.last().activations.copyOf())
                inputTensorLayer.events.updated.fire()
                evaluationTrainer.tensorLayerStages.forEach { it.events.updated.fire() }
                flatArray.events.updated.fire()
                outputArray.events.updated.fire()
                return output
            }

            override suspend fun predict(image: DoubleArray) = evaluationTrainer.forwardPass(image)

            override suspend fun beginEvaluation() {
                // Copy the latest trained weights into the trainer
                evaluationTrainer.syncFromNetwork()
            }

            override suspend fun openTrainingDialog() {
                SwingUtilities.invokeLater {
                    with(networkPanel) { cnnModel.getCnnTrainingDialog() }.run {
                        pack()
                        setLocationRelativeTo(null)
                        isVisible = true
                    }
                }
            }

            override suspend fun addDrawingWorld() {
                if (drawingWorld?.let { it in workspace.componentList } == true) return
                val imageWorld = ImageWorldComponent("Draw a digit")
                workspace.addWorkspaceComponent(imageWorld)
                imageWorld.world.resetImageAlbum(inputShape.width, inputShape.height)
                with(workspace.couplingManager) {
                    imageWorld.world.imagePipelineCollection.currentPipeline
                        .getProducer(ImageProcessingPipeline::brightness) couple
                        inputTensorLayer.channelContainers[0].getConsumer(TensorLayer.ChannelContainer::setValues)
                }
                inputTensorLayer.isClamped = true
                placeBeside(imageWorld, networkComponent)
                drawingWorld = imageWorld
            }
        }
        val controlPanel = createTinyMnistControlPanel(data, model).awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 600, 800)
    }
    workspace.simpleIterate()

    addSidebarInfo(
        """
        # CNN MNIST

        A convolutional neural network trained on the same [Tiny MNIST](https://en.wikipedia.org/wiki/MNIST_database)
        dataset as the backprop version, but using convolutional layers to learn spatial features.

        # Simulation Details

        The dataset contains:

        - **Training**: 10,000 images of handwritten digits
        - **Testing**: 1,000 images

        # Control Panel

        - `Train network...`: Opens the training dialog. This is a convenience; right-clicking the `CNN MNIST` outline and choosing `Train...` opens the same dialog.

        - `Training examples`: Train on the first `10,000`, `1,000`, or `300` training images. With fewer examples the network memorizes the training set, so training error falls well below testing error (overfitting).

        - `Testing set`: Test on the original images, or on copies shifted `1` or `2` pixels, rotated `20` degrees, or with added noise. This sets the testing set used during training and shown in the `Testing data` tab.

        - `Random training image` and `Random test image`: Run a randomly chosen training or test image through the network so you can see how it classifies it.

        - `Draw your own digit`: Opens an image world coupled to the input layer. Draw a digit, then press `Run` or `Step` to see the network's answer.

        - `Test generalization`: Reports accuracy on the training set and on every testing set. The progress bar below it shows how far along it is. Stop the simulation first if it is running.

        # What to Do
        1. Press `Train network...` in the control panel, or right-click the `CNN MNIST` outline and select `Train...`
        2. Click `Run` to start training, `Stop` to pause
        3. Watch the loss plot decrease
        4. Close the dialog and use `Random training image` and `Random test image` to see how the network classifies individual images
        5. In the training dialog, use the `Training data` / `Testing data` tabs to browse examples, and click `Apply inputs` on a row to see the network's prediction
        6. Press `Test generalization`. Compare the results with the `Feed-forward MNIST` simulation: the CNN usually keeps more of its accuracy on digits shifted a pixel or two, because max pooling lets a feature move slightly without changing the pooled output. Both networks fail once digits move several pixels, since the final dense layer still depends on where the pooled features are.
        """.trimIndent()
    )
}
