package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.workspace.gui.SimbrainDesktop
import javax.swing.JButton

class UndoManagerTest {

    @Test
    fun testUndoableAction() {
        // Test that the undoableAction function correctly creates an UndoableAction
        // with the expected behavior

        val testDescription = "Test Action"

        // Create variables to track if undo and redo were called
        var undoCalled = false
        var redoCalled = false
        var contextPassedToUndo: Any? = null
        var contextPassedToRedo: Any? = null

        // Create an UndoableAction using the undoableAction function
        val action = undoableAction(
            description = testDescription,
            undo = {
                undoCalled = true
            },
            redo = {
                redoCalled = true
            }
        )

        // Verify that the context and description are correctly stored
        assertEquals(testDescription, action.description)

        // Test the undo function
        runBlocking {
            action.undo()
        }

        // Verify that undo was called with the correct context
        assertEquals(true, undoCalled)

        // Test the redo function
        runBlocking {
            action.redo()
        }

        // Verify that redo was called with the correct context
        assertEquals(true, redoCalled)
    }

    @Test
    fun testNewNeuronActionUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Get the initial number of neurons in the network
        val initialNeuronCount = network.flatNeuronList.size

        // Get the action for this test
        val newNeuronAction = networkPanel.networkActions.newNeuronAction

        val stubButton = JButton(newNeuronAction)

        withContext(Dispatchers.Swing) {
            stubButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Verify that a neuron was added
        assertEquals(initialNeuronCount + 1, network.flatNeuronList.size, "A neuron should be added to the network")
        val addedNeuron = network.flatNeuronList.last()
        val addedNeuronId = addedNeuron.id

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify that the neuron was removed
        assertEquals(initialNeuronCount, network.flatNeuronList.size, "The neuron should be removed after undo")
        assertFalse(
            network.flatNeuronList.any { it.id == addedNeuronId },
            "The added neuron should not be in the network after undo"
        )

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that the neuron was added back
        assertEquals(initialNeuronCount + 1, network.flatNeuronList.size, "The neuron should be added back after redo")
        assertTrue(
            network.flatNeuronList.any { it.id == addedNeuronId },
            "A neuron with the same ID should be in the network after redo"
        )
    }

    @Test
    fun testNeuronCollectionActionUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)

        // Get the initial number of neuron collections in the network
        val initialCollectionCount = network.getModels<NeuronCollection>().size

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Get the action for this test
        val neuronCollectionAction = networkPanel.networkActions.neuronCollectionAction

        val stubButton = JButton(neuronCollectionAction)

        withContext(Dispatchers.Swing) {
            stubButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Verify that a neuron collection was added
        assertEquals(initialCollectionCount + 1, network.getModels<NeuronCollection>().size, "A neuron collection should be added to the network")
        val addedCollection = network.getModels<NeuronCollection>().last()
        val addedCollectionId = addedCollection.id

        // Verify that the collection contains the selected neurons
        assertEquals(2, addedCollection.neuronList.size, "The collection should contain 2 neurons")
        assertTrue(addedCollection.neuronList.contains(neuron1), "The collection should contain neuron1")
        assertTrue(addedCollection.neuronList.contains(neuron2), "The collection should contain neuron2")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify that the collection was removed
        assertEquals(initialCollectionCount, network.getModels<NeuronCollection>().size, "The neuron collection should be removed after undo")
        assertFalse(
            network.getModels<NeuronCollection>().any { it.id == addedCollectionId },
            "The added collection should not be in the network after undo"
        )

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that the collection was added back
        assertEquals(initialCollectionCount + 1, network.getModels<NeuronCollection>().size, "The neuron collection should be added back after redo")
        val redoCollection = network.getModels<NeuronCollection>().last()
        assertEquals(addedCollectionId, redoCollection.id, "A collection with the same ID should be in the network after redo")

        // Verify that the collection contains the selected neurons
        assertEquals(2, redoCollection.neuronList.size, "The collection should contain 2 neurons")
        assertTrue(redoCollection.neuronList.contains(neuron1), "The collection should contain neuron1")
        assertTrue(redoCollection.neuronList.contains(neuron2), "The collection should contain neuron2")
    }

    @Test
    fun testAddTextActionUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Get the initial number of text objects in the network
        val initialTextCount = network.getModels<NetworkTextObject>().size

        // Create a text object directly
        val testText = "Test Text"
        val textObject = NetworkTextObject(testText)

        // Add the text object to the network and create an undoable action
        network.addNetworkModelAsync(textObject)
        networkPanel.undoManager.addUndoableAction(
            description = "Add text object",
            undo = { textObject.delete() },
            redo = { network.addNetworkModel(textObject, usePlacementManager = false, useAutoAssignedId = false) }
        )

        // Verify that a text object was added
        assertEquals(initialTextCount + 1, network.getModels<NetworkTextObject>().size, "A text object should be added to the network")
        val addedText = network.getModels<NetworkTextObject>().last()
        val addedTextId = addedText.id

        // Verify that the text object has the correct text
        assertEquals(testText, addedText.text, "The text object should have the correct text")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify that the text object was removed
        assertEquals(initialTextCount, network.getModels<NetworkTextObject>().size, "The text object should be removed after undo")
        assertFalse(
            network.getModels<NetworkTextObject>().any { it.id == addedTextId },
            "The added text object should not be in the network after undo"
        )

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that the text object was added back
        assertEquals(initialTextCount + 1, network.getModels<NetworkTextObject>().size, "The text object should be added back after redo")
        val redoText = network.getModels<NetworkTextObject>().last()
        assertEquals(addedTextId, redoText.id, "A text object with the same ID should be in the network after redo")
        assertEquals(testText, redoText.text, "The text object should have the correct text")
    }

    @Test
    fun testCopyPasteActionUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network
        val neuron1 = Neuron().apply { 
            label = "neuron_1"
            activation = 0.5
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply { 
            label = "neuron_2"
            activation = -0.3
            x = 200.0
            y = 200.0
        }
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)

        // Get the initial number of neurons in the network
        val initialNeuronCount = network.flatNeuronList.size

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Get the actions for this test
        val copyAction = networkPanel.networkActions.copyAction
        val pasteAction = networkPanel.networkActions.pasteAction

        // Execute the copy action
        val copyButton = JButton(copyAction)
        withContext(Dispatchers.Swing) {
            copyButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Execute the paste action
        val pasteButton = JButton(pasteAction)
        withContext(Dispatchers.Swing) {
            pasteButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Verify that neurons were added
        assertEquals(initialNeuronCount + 2, network.flatNeuronList.size, "Two neurons should be added to the network")

        // Get the pasted neurons
        val pastedNeurons = network.flatNeuronList.takeLast(2)
        val pastedNeuronIds = pastedNeurons.map { it.id }

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify that the pasted neurons were removed
        assertEquals(initialNeuronCount, network.flatNeuronList.size, "The pasted neurons should be removed after undo")
        pastedNeuronIds.forEach { id ->
            assertFalse(
                network.flatNeuronList.any { it.id == id },
                "The pasted neuron should not be in the network after undo"
            )
        }

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that the neurons were added back
        assertEquals(initialNeuronCount + 2, network.flatNeuronList.size, "The neurons should be added back after redo")

        // Verify that the pasted neurons have the same properties as the original neurons
        assertEquals(neuron1.activation, network.getNeuronByLabel("neuron_1").activation, "The pasted neuron should have the same activation as the original")
        assertEquals(neuron2.activation, network.getNeuronByLabel("neuron_2").activation, "The pasted neuron should have the same activation as the original")
    }

    @Test
    fun testCutActionUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network
        val neuron1 = Neuron().apply { 
            activation = 0.5
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply { 
            activation = -0.3
            x = 200.0
            y = 200.0
        }
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)

        // Get the initial number of neurons in the network
        val initialNeuronCount = network.flatNeuronList.size

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Store the neuron IDs for later verification
        val neuronIds = listOf(neuron1.id, neuron2.id)

        // Get the action for this test
        val cutAction = networkPanel.networkActions.cutAction

        // Execute the cut action
        val cutButton = JButton(cutAction)
        withContext(Dispatchers.Swing) {
            cutButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Verify that neurons were removed
        assertEquals(initialNeuronCount - 2, network.flatNeuronList.size, "Two neurons should be removed from the network")
        neuronIds.forEach { id ->
            assertFalse(
                network.flatNeuronList.any { it.id == id },
                "The cut neuron should not be in the network after cut"
            )
        }

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify that the cut neurons were restored
        assertEquals(initialNeuronCount, network.flatNeuronList.size, "The cut neurons should be restored after undo")
        neuronIds.forEach { id ->
            assertTrue(
                network.flatNeuronList.any { it.id == id },
                "The cut neuron should be back in the network after undo"
            )
        }

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that the neurons were removed again
        assertEquals(initialNeuronCount - 2, network.flatNeuronList.size, "The neurons should be removed again after redo")
        neuronIds.forEach { id ->
            assertFalse(
                network.flatNeuronList.any { it.id == id },
                "The cut neuron should not be in the network after redo"
            )
        }
    }

    @Test
    fun testAlignHorizontalUndoRedo() = runBlocking {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network with different Y positions
        val neuron1 = Neuron().apply { 
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply { 
            x = 200.0
            y = 200.0
        }
        val neuron3 = Neuron().apply { 
            x = 300.0
            y = 150.0
        }
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)
        network.addNetworkModelAsync(neuron3)

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2, neuron3))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Record the original Y positions
        val originalY1 = neuron1.y
        val originalY2 = neuron2.y
        val originalY3 = neuron3.y

        // Execute alignHorizontal
        networkPanel.alignHorizontal()

        // Verify that neurons were aligned horizontally (all have the same Y value, which should be the minimum)
        val minY = minOf(originalY1, originalY2, originalY3)
        assertEquals(minY, neuron1.y, "Neuron1 should be aligned to the minimum Y")
        assertEquals(minY, neuron2.y, "Neuron2 should be aligned to the minimum Y")
        assertEquals(minY, neuron3.y, "Neuron3 should be aligned to the minimum Y")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify original positions were restored
        assertEquals(originalY1, neuron1.y, "Neuron1 should be restored to original Y")
        assertEquals(originalY2, neuron2.y, "Neuron2 should be restored to original Y")
        assertEquals(originalY3, neuron3.y, "Neuron3 should be restored to original Y")

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that neurons were aligned horizontally again
        assertEquals(minY, neuron1.y, "Neuron1 should be aligned to the minimum Y after redo")
        assertEquals(minY, neuron2.y, "Neuron2 should be aligned to the minimum Y after redo")
        assertEquals(minY, neuron3.y, "Neuron3 should be aligned to the minimum Y after redo")
    }

    @Test
    fun testAlignVerticalUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network with different X positions
        val neuron1 = Neuron().apply { 
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply { 
            x = 200.0
            y = 200.0
        }
        val neuron3 = Neuron().apply { 
            x = 150.0
            y = 300.0
        }
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)
        network.addNetworkModelAsync(neuron3)

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2, neuron3))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Record the original X positions
        val originalX1 = neuron1.x
        val originalX2 = neuron2.x
        val originalX3 = neuron3.x

        // Execute alignVertical
        networkPanel.alignVertical()

        // Verify that neurons were aligned vertically (all have the same X value, which should be the minimum)
        val minX = minOf(originalX1, originalX2, originalX3)
        assertEquals(minX, neuron1.x, "Neuron1 should be aligned to the minimum X")
        assertEquals(minX, neuron2.x, "Neuron2 should be aligned to the minimum X")
        assertEquals(minX, neuron3.x, "Neuron3 should be aligned to the minimum X")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify original positions were restored
        assertEquals(originalX1, neuron1.x, "Neuron1 should be restored to original X")
        assertEquals(originalX2, neuron2.x, "Neuron2 should be restored to original X")
        assertEquals(originalX3, neuron3.x, "Neuron3 should be restored to original X")

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that neurons were aligned vertically again
        assertEquals(minX, neuron1.x, "Neuron1 should be aligned to the minimum X after redo")
        assertEquals(minX, neuron2.x, "Neuron2 should be aligned to the minimum X after redo")
        assertEquals(minX, neuron3.x, "Neuron3 should be aligned to the minimum X after redo")
    }

    @Test
    fun testSpaceHorizontalUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network with specific X positions
        val neuron1 = Neuron().apply { 
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply { 
            x = 200.0
            y = 200.0
        }
        val neuron3 = Neuron().apply { 
            x = 300.0
            y = 150.0
        }
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)
        network.addNetworkModelAsync(neuron3)

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2, neuron3))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Record the original X positions
        val originalX1 = neuron1.x
        val originalX2 = neuron2.x
        val originalX3 = neuron3.x

        // Execute spaceHorizontal
        networkPanel.spaceHorizontal()

        // Verify that neurons were evenly spaced horizontally
        val min = originalX1
        val max = originalX3
        val spacing = (max - min) / 2.0

        assertEquals(min, neuron1.x, "First neuron should remain at the minimum X")
        assertEquals(min + spacing, neuron2.x, "Second neuron should be at min + spacing")
        assertEquals(max, neuron3.x, "Last neuron should remain at the maximum X")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify original positions were restored
        assertEquals(originalX1, neuron1.x, "Neuron1 should be restored to original X")
        assertEquals(originalX2, neuron2.x, "Neuron2 should be restored to original X")
        assertEquals(originalX3, neuron3.x, "Neuron3 should be restored to original X")

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that neurons were evenly spaced horizontally again
        assertEquals(min, neuron1.x, "First neuron should remain at the minimum X after redo")
        assertEquals(min + spacing, neuron2.x, "Second neuron should be at min + spacing after redo")
        assertEquals(max, neuron3.x, "Last neuron should remain at the maximum X after redo")
    }

    @Test
    fun testSpaceVerticalUndoRedo() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some neurons to the network with specific Y positions
        val neuron1 = Neuron().apply { 
            x = 100.0
            y = 100.0
        }
        val neuron2 = Neuron().apply { 
            x = 200.0
            y = 200.0
        }
        val neuron3 = Neuron().apply { 
            x = 150.0
            y = 300.0
        }
        network.addNetworkModelAsync(neuron1)
        network.addNetworkModelAsync(neuron2)
        network.addNetworkModelAsync(neuron3)

        // Select the neurons
        network.selectModels(listOf(neuron1, neuron2, neuron3))

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Record the original Y positions
        val originalY1 = neuron1.y
        val originalY2 = neuron2.y
        val originalY3 = neuron3.y

        // Execute spaceVertical
        networkPanel.spaceVertical()

        // Verify that neurons were evenly spaced vertically
        val min = originalY1
        val max = originalY3
        val spacing = (max - min) / 2.0

        assertEquals(min, neuron1.y, "First neuron should remain at the minimum Y")
        assertEquals(min + spacing, neuron2.y, "Second neuron should be at min + spacing")
        assertEquals(max, neuron3.y, "Last neuron should remain at the maximum Y")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify original positions were restored
        assertEquals(originalY1, neuron1.y, "Neuron1 should be restored to original Y")
        assertEquals(originalY2, neuron2.y, "Neuron2 should be restored to original Y")
        assertEquals(originalY3, neuron3.y, "Neuron3 should be restored to original Y")

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that neurons were evenly spaced vertically again
        assertEquals(min, neuron1.y, "First neuron should remain at the minimum Y after redo")
        assertEquals(min + spacing, neuron2.y, "Second neuron should be at min + spacing after redo")
        assertEquals(max, neuron3.y, "Last neuron should remain at the maximum Y after redo")
    }

    @Test
    fun testCreateSupervisedModelActionUndoRedo() = runBlocking {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)

        SimbrainDesktop.workspace.addWorkspaceComponent(networkComponent)

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Create input and output layers
        val inputLayer = NeuronArray(5).apply { 
            label = "Input Layer"
            isClamped = true
        }
        val outputLayer = NeuronArray(3).apply { 
            label = "Output Layer"
        }

        // Create a weight matrix connecting the layers
        val weightMatrix = WeightMatrix(inputLayer, outputLayer)

        // Add the layers and weight matrix to the network
        network.addNetworkModels(inputLayer, outputLayer, weightMatrix)

        // Get the initial number of supervised models in the network
        val initialSupervisedModelCount = network.getModels(SupervisedModel::class.java).size

        // Select the input layer as source and output layer as target.
        val screenElements = networkPanel.screenElements.associateBy { it.model.label }

        networkPanel.selectionManager.add(screenElements["Input Layer"]!!)
        networkPanel.selectionManager.convertSelectedNodesToSourceNodes()
        networkPanel.selectionManager.clear()
        networkPanel.selectionManager.add(screenElements["Output Layer"]!!)

        // Get the action for this test
        val createSupervisedModelAction = networkPanel.networkActions.createSupervisedModelAction

        val stubButton = JButton(createSupervisedModelAction)

        withContext(Dispatchers.Swing) {
            stubButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Verify that a supervised model was added
        assertEquals(initialSupervisedModelCount + 1, network.getModels(SupervisedModel::class.java).size,
            "A supervised model should be added to the network")
        val addedModel = network.getModels(SupervisedModel::class.java).last()
        val addedModelId = addedModel.id

        // Verify that the model has the correct input and output layers
        assertEquals(inputLayer, addedModel.inputLayer, "The model should have the correct input layer")
        assertEquals(outputLayer, addedModel.outputLayer, "The model should have the correct output layer")

        // Undo the action
        networkPanel.undoManager.undo()

        // Verify that the supervised model was removed
        assertEquals(initialSupervisedModelCount, network.getModels(SupervisedModel::class.java).size,
            "The supervised model should be removed after undo")
        assertFalse(
            network.getModels(SupervisedModel::class.java).any { it.id == addedModelId },
            "The added supervised model should not be in the network after undo"
        )

        // Redo the action
        networkPanel.undoManager.redo()

        // Verify that the supervised model was added back
        assertEquals(initialSupervisedModelCount + 1, network.getModels(SupervisedModel::class.java).size,
            "The supervised model should be added back after redo")
        val redoModel = network.getModels(SupervisedModel::class.java).last()
        assertEquals(addedModelId, redoModel.id,
            "A supervised model with the same ID should be in the network after redo")
        assertEquals(inputLayer, redoModel.inputLayer, 
            "The model should have the correct input layer after redo")
        assertEquals(outputLayer, redoModel.outputLayer, 
            "The model should have the correct output layer after redo")
    }

    @Test
    fun testCreateSupervisedModelActionUndoRedoAll() = runTest {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)

        SimbrainDesktop.workspace.addWorkspaceComponent(networkComponent)

        val networkPanel = (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel

        // Create input and output layers
        val inputLayer = NeuronArray(5).apply { 
            label = "Input Layer"
            isClamped = true
        }
        val outputLayer = NeuronArray(3).apply { 
            label = "Output Layer"
        }

        // Create a weight matrix connecting the layers
        val weightMatrix = WeightMatrix(inputLayer, outputLayer)

        // Add the layers and weight matrix to the network
        network.addNetworkModels(inputLayer, outputLayer, weightMatrix)

        // Get the initial number of supervised models in the network
        val initialSupervisedModelCount = network.getModels(SupervisedModel::class.java).size

        // Select the input layer as source and output layer as target.
        val screenElements = networkPanel.screenElements.associateBy { it.model.label }

        networkPanel.selectionManager.add(screenElements["Input Layer"]!!)
        networkPanel.selectionManager.convertSelectedNodesToSourceNodes()
        networkPanel.selectionManager.clear()
        networkPanel.selectionManager.add(screenElements["Output Layer"]!!)

        // Get the action for this test
        val createSupervisedModelAction = networkPanel.networkActions.createSupervisedModelAction

        val stubButton = JButton(createSupervisedModelAction)

        // Perform multiple actions that can be undone/redone

        // Action 1: Create a supervised model
        withContext(Dispatchers.Swing) {
            stubButton.doClick()
        }

        withContext(Dispatchers.Swing) {
            delay(10)
        }

        // Verify that a supervised model was added
        assertEquals(initialSupervisedModelCount + 1, network.getModels(SupervisedModel::class.java).size,
            "A supervised model should be added to the network")
        val addedModel = network.getModels(SupervisedModel::class.java).last()
        val addedModelId = addedModel.id

        // Verify that the model has the correct input and output layers
        assertEquals(inputLayer, addedModel.inputLayer, "The model should have the correct input layer")
        assertEquals(outputLayer, addedModel.outputLayer, "The model should have the correct output layer")

        // Action 2: Add a neuron to the network
        val newNeuron = Neuron().apply { 
            label = "New Neuron"
            x = 300.0
            y = 300.0
        }
        network.addNetworkModelAsync(newNeuron)
        networkPanel.undoManager.addUndoableAction(
            description = "Add neuron",
            undo = { newNeuron.delete() },
            redo = { network.addNetworkModel(newNeuron, usePlacementManager = false, useAutoAssignedId = false) }
        )

        // Verify that the neuron was added
        assertTrue(network.flatNeuronList.contains(newNeuron), "The neuron should be added to the network")

        // Action 3: Add a text object to the network
        val textObject = NetworkTextObject("Test Text")
        network.addNetworkModelAsync(textObject)
        networkPanel.undoManager.addUndoableAction(
            description = "Add text object",
            undo = { textObject.delete() },
            redo = { network.addNetworkModel(textObject, usePlacementManager = false, useAutoAssignedId = false) }
        )

        // Verify that the text object was added
        assertTrue(network.getModels<NetworkTextObject>().contains(textObject), "The text object should be added to the network")

        repeat(2) {
            // Store the number of actions in the undo stack
            val undoStackSize = networkPanel.undoManager.undoStack.size

            // Undo all actions
            repeat(undoStackSize) {
                networkPanel.undoManager.undo()
            }

            // Verify that all actions were undone
            assertEquals(0, networkPanel.undoManager.undoStack.size, "The undo stack should be empty")
            assertEquals(undoStackSize, networkPanel.undoManager.redoStack.size, "The redo stack should contain all undone actions")

            // Verify the state after all undos
            assertEquals(initialSupervisedModelCount, network.getModels(SupervisedModel::class.java).size,
                "The supervised model should be removed after undoing all actions")
            assertFalse(
                network.getModels(SupervisedModel::class.java).any { it.id == addedModelId },
                "The added supervised model should not be in the network after undoing all actions"
            )
            assertFalse(network.flatNeuronList.contains(newNeuron), "The neuron should be removed after undoing all actions")
            assertFalse(network.getModels<NetworkTextObject>().contains(textObject), "The text object should be removed after undoing all actions")

            // Store the number of actions in the redo stack
            val redoStackSize = networkPanel.undoManager.redoStack.size

            // Redo all actions
            repeat(redoStackSize) {
                networkPanel.undoManager.redo()
            }

            // Verify that all actions were redone
            assertEquals(redoStackSize, networkPanel.undoManager.undoStack.size, "The undo stack should contain all redone actions")
            assertEquals(0, networkPanel.undoManager.redoStack.size, "The redo stack should be empty")

            // Verify the final state after all redos
            assertEquals(initialSupervisedModelCount + 1, network.getModels(SupervisedModel::class.java).size,
                "The supervised model should be added back after redoing all actions")
            val redoModel = network.getModels(SupervisedModel::class.java).last()
            assertEquals(addedModelId, redoModel.id,
                "A supervised model with the same ID should be in the network after redoing all actions")
            assertEquals(inputLayer, redoModel.inputLayer,
                "The model should have the correct input layer after redoing all actions")
            assertEquals(outputLayer, redoModel.outputLayer,
                "The model should have the correct output layer after redoing all actions")
            assertTrue(network.flatNeuronList.contains(newNeuron), "The neuron should be added back after redoing all actions")
            assertTrue(network.getModels<NetworkTextObject>().contains(textObject), "The text object should be added back after redoing all actions")
        }

    }

}
