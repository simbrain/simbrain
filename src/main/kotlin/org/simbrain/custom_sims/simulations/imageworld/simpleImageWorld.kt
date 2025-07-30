package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.updaterules.interfaces.BoundedUpdateRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.*
import smile.math.matrix.Matrix
import java.awt.image.BufferedImage

/**
 * Images can be drawn in a 10x10 array and sent to a 10x10 network.
 */
val simpleImageWorld = newSim {

    val numCategories = 7
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

    val hidden = network.addNeuronCollection(numHiddenNodes).apply {
        label = "Hidden"
        layout(GridLayout(40.0, 40.0))
    }

    // The number of neurons determines how many categories are shown
    val outputs = network.addNeuronCollection(numCategories).apply {
        label = "Outputs"
        (updateRule as? BoundedUpdateRule)?.apply {
            upperBound = 1.0
            lowerBound = -1.0
        }
    }

    val weights = listOf(
        WeightMatrix(inputs, hidden),
        WeightMatrix(hidden, outputs)
    ).onEach { it.randomize() }.also { network.addNetworkModels(it) }

    offsetNeuronCollections(inputs, hidden, Direction.NORTH, 200.0)
    alignNetworkModels(inputs, hidden, Alignment.VERTICAL)

    offsetNeuronCollections(hidden, outputs, Direction.NORTH, 200.0)
    alignNetworkModels(hidden, outputs, Alignment.VERTICAL)

    val supervisedModel = SupervisedModel(inputs, outputs, 1.0).also {
        network.addNetworkModel(it)
        it.trainingSet = MatrixDataset(
            Matrix(outputs.size, inputs.size),
            Matrix.eye(outputs.size, outputs.size)
        )
    }

    // Image world
    val imageWorldComponent = addImageWorld("Image World")
    val imageWorld = imageWorldComponent.world.apply {
        resetImageAlbum(10, 10)
        setCurrentFilter("Threshold 10x10")
        repeat(outputs.size) { index ->
            imageAlbum.addImage(BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB))
        }
    }

    // Place network in the desktop
    withGui {
        val networkDesktopComponent = getDesktopComponent(networkComponent) as NetworkDesktopComponent
        val networkPanel = networkDesktopComponent.networkPanel
        val controlPanel = createControlPanel("Control Panel", 5, 10) {
            repeat(outputs.size) { index ->
                addTextField("Category ${index + 1}", "") { category ->
                    outputs.neuronList[index].label = category
                }
                addButton("Save Image for Category ${index + 1}") {
                    supervisedModel.trainingSet.inputs.setRow(index, imageWorld.currentTransformation?.brightness ?: doubleArrayOf())
                    val image = imageWorld.currentImage.copy()
                    val frame = imageWorld.imageAlbum.frames[index]
                    frame.drawImage(image)
                    imageWorld.imageAlbum.fireImageUpdate()
                    if (index != imageWorld.imageAlbum.frameIndex) {
                        imageWorld.imageAlbum.clearCurrentImage()
                    }
                    supervisedModel.inputLayer.setActivations(supervisedModel.trainingSet.inputs.row(index))
                    supervisedModel.outputLayer.setActivations(DoubleArray(outputs.size) { if (it == index) 1.0 else 0.0 })
                }
            }
            addSeparator()
            addButton("Train...") {
                with(networkPanel) { supervisedModel.getSupervisedTrainingDialog().display() }
            }
        }
        var controlPanelHeight = controlPanel.size.height
        waitFor {
            val condition = controlPanel.size.height > 100 && controlPanelHeight == controlPanel.size.height
            controlPanelHeight = controlPanel.size.height
            condition
        }
        place(networkComponent, controlPanel.size.width + 10, 10, 350, controlPanel.size.height)
        place(
            imageWorldComponent,
            controlPanel.size.width + 10 + 350 + 10,
            10,
            600,
            600
        )
    }

    // Couple
    with(couplingManager) {
        imageWorld.transformationCollection.currentTransformation?.let { transformation ->
            createCoupling(
                transformation.getProducer(transformation::brightness),
                inputs.getConsumer(inputs::activationArray)
            )
        }
    }

}