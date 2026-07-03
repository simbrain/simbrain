package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.*
import java.awt.image.BufferedImage

/**
 * Images can be drawn in a 10x10 array and sent to a 10x10 network.
 */
val simpleImageWorld = newSim {

    val numCategories = 8
    val numHiddenNodes = 64

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Input nodes
    val inputs = network.addNeuronCollection(100).apply {
        label = "Inputs"
        isClamped = true
        setUpperBound(1.0)
        layout(GridLayout(40.0, 40.0))
    }

    val hidden = network.addNeuronCollection(numHiddenNodes) {
        updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.Relu }
    }.apply {
        label = "Hidden"
        layout(GridLayout(40.0, 40.0))
    }

    // The number of neurons determines how many categories are shown
    val outputs = network.addNeuronCollection(numCategories) {
        updateRule = SigmoidalRule()
    }

    val weights = listOf(
        WeightMatrix(inputs, hidden),
        WeightMatrix(hidden, outputs)
    ).onEach { it.randomize() }.also { network.addNetworkModelsAsync(it) }

    offsetNeuronCollections(inputs, hidden, Direction.NORTH, 200.0)
    alignNetworkModels(inputs, hidden, Alignment.VERTICAL)

    offsetNeuronCollections(hidden, outputs, Direction.NORTH, 200.0)
    alignNetworkModels(hidden, outputs, Alignment.VERTICAL)

    val supervisedModel = SupervisedModel(inputs, outputs, 1.0).also {
        network.addNetworkModelAsync(it)
        it.trainingSet = TrainingDataset(
            inputs = MutableList(outputs.size) { MutableList(inputs.size) { 0.0 } },
            targets = MutableList(outputs.size) { i -> MutableList(outputs.size) { j -> if (i == j) 1.0 else 0.0 } }
        )
    }

    // Image world
    val imageWorldComponent = addImageWorld("Image World")
    val imageWorld = imageWorldComponent.world.apply {
        resetImageAlbum(10, 10)
        setCurrentPipeline("Threshold 10x10")
        repeat(outputs.size) { index ->
            imageAlbum.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        }
    }

    // Place network in the desktop
    withGui {
        val networkPanel = getNetworkPanel(networkComponent)
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            repeat(outputs.size) { index ->
                addTextField("Category ${index + 1}", "") { category ->
                    outputs.neuronList[index].label = category
                }
                addButton("Save Image for Category ${index + 1}") {
                    supervisedModel.trainingSet.inputs[index] = (imageWorld.currentPipeline?.brightness ?: doubleArrayOf()).toMutableList()
                    val image = imageWorld.currentImage.copy()
                    val frame = imageWorld.imageAlbum.frames[index]
                    frame.drawImage(image)
                    imageWorld.imageAlbum.fireImageUpdate()
                    if (index != imageWorld.imageAlbum.frameIndex) {
                        imageWorld.imageAlbum.clearCurrentImage()
                    }
                    supervisedModel.inputLayer.setActivations(supervisedModel.trainingSet.inputs[index].toDoubleArray())
                    supervisedModel.outputLayer.setActivations(DoubleArray(outputs.size) { if (it == index) 1.0 else 0.0 })
                }
            }
            addSeparator()
            addButton("Train...") {
                with(networkPanel) { supervisedModel.getSupervisedTrainingDialog().display() }
            }
        }.awaitLayout()
        var controlPanelHeight = controlPanel.size.height
        waitFor {
            val condition = controlPanel.size.height > 100 && controlPanelHeight == controlPanel.size.height
            controlPanelHeight = controlPanel.size.height
            condition
        }
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 350, controlPanel.size.height)
        place(
            imageWorldComponent,
            controlPanel.rightEdgeWithGap() + 350 + SIM_WINDOW_GAP,
            SIM_WINDOW_GAP,
            600,
            600
        )
    }

    // Couple
    with(couplingManager) {
        imageWorld.imagePipelineCollection.currentPipeline.let { pipeline ->
            createCoupling(
                pipeline.getProducer(pipeline::brightness),
                inputs.getConsumer(inputs::activationArray)
            )
        }
    }

    addSidebarInfo(
        """
        # Simple Image World
        
        This simulation demonstrates basic image processing using a `10x10` pixel drawing canvas connected to a neural network. 
        You can draw simple images and train a network to classify them into different categories.
        
        The simulation is useful for seeing what the process of training a network is like without having to create a bunch of data
        and load it into the trainer. Instead, you can simply draw patterns, press a button, and train the associations.
        
        The simulation is useful for exploring generalization. If you make `8` different categories, it will probably not do well at recognizing variants. 
        But if you make `4` versions of two categories, for exmaple `x1`, `x2`, `x3`, `x4` and `o1`, `o2`, `o3`, `o4`, that vary in position, size, or style, you 
        will probably get better results.
        
        To view your training data,  you can open the training dialog and click the step button in the input table. This will place the patters back 
         in the input layer, but not in the image world.
        
        # What to Do
        
        ## Basic Workflow
        
        1. Draw images in the Image World canvas using the drawing tools
        
        2. Save training examples: For each category, draw an image and click the corresponding `Save Image for Category N` button. The image will be saved to the training set and displayed in the image album.
        
        3. Train the network: Click the `Train...` button to open the training dialog. Run training iterations until the error decreases to an acceptable level.
        
        4. Test classification: Draw new images and observe which output neuron activates most strongly, indicating the network's predicted category.
        
        You can review your training dialog. This dialog shows all training examples and their target outputs. Use this to verify what patterns you've saved and to see the actual input arrays (note that the images themselves won't display in the training dialog, only the numerical input data).

        ## Training Strategies
        
        You can approach this simulation in several ways:
        
        - Two-category classification: Train the network to distinguish between two types of patterns (e.g., `X` vs `O`). This is the simplest approach.
        
        - Multiple variations of one pattern: Use all ${numCategories} categories to teach the network to recognize different variations of a single pattern type (e.g., `X` in different positions or orientations).
        
        - Multiple pattern types: Train on several distinct pattern types (e.g., `X`, `O`, `lines`, `dots`) to create a multi-category classifier.
        
        - Multiple instances per category: For better generalization, create `3-4` variations of each pattern type and train on all of them. This helps the network learn the essential features rather than memorizing specific pixel configurations.
        

        # Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
        
        """.trimIndent()
    )

}
