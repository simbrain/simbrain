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
import org.simbrain.network.gui.UndoManager.UndoableAction
import javax.swing.JButton

class UndoManagerTest {

    @Test
    fun testUndoableAction() {
        // Test that the undoableAction function correctly creates an UndoableAction
        // with the expected behavior

        // Create a test context
        val testContext = "Initial Context"
        val testDescription = "Test Action"

        // Create variables to track if undo and redo were called
        var undoCalled = false
        var redoCalled = false
        var contextPassedToUndo: Any? = null
        var contextPassedToRedo: Any? = null

        // Create an UndoableAction using the undoableAction function
        val action = undoableAction(
            initialContext = testContext,
            description = testDescription,
            undo = { context ->
                undoCalled = true
                contextPassedToUndo = context
            },
            redo = { context ->
                redoCalled = true
                contextPassedToRedo = context
            }
        )

        // Verify that the context and description are correctly stored
        assertEquals(testContext, action.context)
        assertEquals(testDescription, action.description)

        // Test the undo function
        runBlocking {
            action.undo()
        }

        // Verify that undo was called with the correct context
        assertEquals(true, undoCalled)
        assertEquals(testContext, contextPassedToUndo)

        // Test the redo function
        runBlocking {
            action.redo()
        }

        // Verify that redo was called with the correct context
        assertEquals(true, redoCalled)
        assertEquals(testContext, contextPassedToRedo)
    }

    @Test
    fun testUndoableActionWithContextModification() {
        // Test that the context can be modified and the modified context is used
        // in subsequent calls to undo and redo

        // Create a test context
        val testContext = "Initial Context"
        val modifiedContext = "Modified Context"

        // Create variables to track the context passed to undo and redo
        var contextPassedToUndo: Any? = null
        var contextPassedToRedo: Any? = null

        // Create a mutable reference to hold the action
        var actionRef: UndoableAction? = null

        // Create an UndoableAction using the undoableAction function
        val action = undoableAction(
            initialContext = testContext,
            description = "Test Action with Context Modification",
            undo = { context ->
                contextPassedToUndo = context
                // Modify the context
                actionRef!!.context = modifiedContext
            },
            redo = { context ->
                contextPassedToRedo = context
            }
        )

        // Set the reference to the action
        actionRef = action

        // Verify that the initial context is correctly stored
        assertEquals(testContext, action.context)

        // Test the undo function
        runBlocking {
            action.undo()
        }

        // Verify that undo was called with the initial context
        assertEquals(testContext, contextPassedToUndo)

        // Verify that the context was modified
        assertEquals(modifiedContext, action.context)

        // Test the redo function
        runBlocking {
            action.redo()
        }

        // Verify that redo was called with the modified context
        assertEquals(modifiedContext, contextPassedToRedo)
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
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)

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
        network.addNetworkModel(textObject)
        networkPanel.undoManager.addUndoableAction(
            description = "Add text object",
            undo = { textObject.delete() },
            redo = { network.addNetworkModel(textObject, usePlacementManager = false, useAutoAssignedId = false)?.await() }
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
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)

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
        network.addNetworkModel(neuron1)
        network.addNetworkModel(neuron2)

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
}
