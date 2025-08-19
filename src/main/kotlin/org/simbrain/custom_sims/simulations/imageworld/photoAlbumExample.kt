package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.getFilesWithExtension
import org.simbrain.util.math.SigmoidFunctionEnum
import org.simbrain.util.place
import org.simbrain.world.imageworld.filters.EdgeDetectionFilter
import org.simbrain.world.imageworld.filters.ResizeOperation

/**
 * Load image world with photo album coupled to a 100x100 neuron array.
 */
val photoAlbumExample = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Input nodes
    val inputArray = NeuronArray(100*100).apply {
        label = "Inputs"
        isClamped = true
        gridMode = true
    }
    network.addNetworkModelAsync(inputArray)

    // Hidden layer
    val hiddenLayer = NeuronArray(60).apply {
        label = "Hidden"
        updateRule = SigmoidalRule().apply {
            type = SigmoidFunctionEnum.TANH
            lowerBound = -1.0
        }
        gridMode = true
    }
    network.addNetworkModelAsync(hiddenLayer)

    // Output layer (4 categories: bird, crocodile, flower, plane)
    val outputLayer = NeuronArray(4).apply {
        label = "Output"
        isClamped = false
        circleMode = true
        updateRule = SoftmaxRule()
        labelArray = arrayOf("Bird", "Crocodile", "Flower", "Plane")
    }
    network.addNetworkModelAsync(outputLayer)

    offsetNetworkModel(inputArray, hiddenLayer, Direction.NORTH, 300.0)
    alignNetworkModels(inputArray, hiddenLayer, Alignment.VERTICAL)

    offsetNetworkModel(hiddenLayer, outputLayer, Direction.NORTH, 300.0)
    alignNetworkModels(hiddenLayer, outputLayer, Alignment.VERTICAL)


    // Connect layers with weight matrices
    val weights = listOf(
        WeightMatrix(inputArray, hiddenLayer).apply { label = "Input to Hidden" },
        WeightMatrix(hiddenLayer, outputLayer).apply { label = "Hidden to Output" }
    ).onEach {
        it.randomize()
    }.also {
        network.addNetworkModelsAsync(it)
    }

    // Create supervised model
    val supervisedModel = SupervisedModel(inputArray, outputLayer).apply {
        label = "Image Classifier"
        trainerConfig.testConfiguration.enabled = true
    }
    network.addNetworkModelAsync(supervisedModel)

    place(networkComponent,0,0,340,800)

    // Image world
    val component = addImageWorld("Image World")
    placeComponent(component,350,0,720,600)
    val imageWorld = component.world
    imageWorld.imagePipelineCollection.addPipeline("Edge Detector 100x100") {
        addOperation(ResizeOperation(100, 100))
        addOperation(EdgeDetectionFilter())
    }
    val imageFiles = getFilesWithExtension("simulations/images/Caltech101Sample", "jpg")
    val imageNames = imageFiles.map { it.nameWithoutExtension }
    val imageCategories = imageNames.associateWith { it.takeWhile { ch -> !ch.isDigit() } }
    imageWorld.loadImages(imageFiles)
    imageWorld.setCurrentPipeline("Edge Detector 100x100")

    // Create training data
    val categoryNames = listOf("bird", "crocodile", "flower", "plane")
    val categoryIndices = categoryNames.withIndex().associate { it.value to it.index }
    
    // Prepare input and target lists
    val inputs = mutableListOf<MutableList<Double>>()
    val targets = mutableListOf<MutableList<Double>>()
    
    // Load each image and create training data
    imageFiles.forEach { imageFile ->
        // Set the current image to load its pixel data
        imageWorld.imageAlbum.setFrame(imageFiles.indexOf(imageFile))
        workspace.simpleIterate() // Update to load the image
        
        // Get pixel values from the current filter
        val pixelValues = imageWorld.imagePipelineCollection.currentPipeline.brightness
        inputs.add(pixelValues.toMutableList())
        
        // Create one-hot encoded target based on image category
        val imageName = imageFile.nameWithoutExtension
        val category = imageName.takeWhile { !it.isDigit() }
        val categoryIndex = categoryIndices[category] ?: 0
        
        // Create one-hot encoded target
        val oneHotTarget = MutableList(4) { 0.0 }
        oneHotTarget[categoryIndex] = 1.0
        targets.add(oneHotTarget)
    }

    // Split data into training and testing
    val (training, testing) = splitDataSet(inputs, targets, .8)
    val (trainingInputs, trainingTargets) = training
    val (testingInputs, testingTargets) = testing
    
    // Set training data for supervised model
    supervisedModel.trainingSet = TrainingDataset(
        inputs = trainingInputs,
        targets = trainingTargets,
    )
    supervisedModel.testingSet = TrainingDataset(
        inputs = testingInputs,
        targets = testingTargets,
    )

    supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy

    // Couple
    with(couplingManager) {
        imageWorld.imagePipelineCollection.currentPipeline.let { pipeline ->
            createCoupling(
                pipeline.getProducer(pipeline::brightness),
                inputArray.getConsumer(inputArray::setActivations)
            )
        }
    }

    // Force first image to load
    workspace.iterateSuspend(1)

    imageWorld.setCurrentPipeline("Unfiltered")

}