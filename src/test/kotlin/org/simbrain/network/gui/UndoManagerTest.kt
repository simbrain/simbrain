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
import org.simbrain.network.core.Network
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

        // Directly implement the functionality of newNeuronAction
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
}
