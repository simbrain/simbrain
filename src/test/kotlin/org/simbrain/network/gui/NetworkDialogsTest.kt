package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron

class NetworkDialogsTest {

    @Test
    fun testUndoRedoFunctionality() = runBlocking {
        // Create a network, network component, and network panel
        val network = Network()
        val networkComponent = NetworkComponent("Test", network)
        val networkPanel = NetworkPanel(networkComponent)

        // Add some actions to the undo stack
        val neuron1 = Neuron()
        val neuron2 = Neuron()

        // Add first neuron with an undoable action
        network.addNetworkModelAsync(neuron1)
        networkPanel.undoManager.addUndoableAction(
            description = "Add neuron 1",
            undo = { neuron1.delete() },
            redo = { network.addNetworkModel(neuron1, usePlacementManager = false, useAutoAssignedId = false) }
        )

        // Add second neuron with an undoable action
        network.addNetworkModelAsync(neuron2)
        networkPanel.undoManager.addUndoableAction(
            description = "Add neuron 2",
            undo = { neuron2.delete() },
            redo = { network.addNetworkModel(neuron2, usePlacementManager = false, useAutoAssignedId = false) }
        )

        // Verify that there are 2 actions in the undo stack
        assertEquals(2, networkPanel.undoManager.undoStack.size, "There should be 2 actions in the undo stack")
        assertEquals(0, networkPanel.undoManager.redoStack.size, "The redo stack should be empty")

        // Verify the descriptions of the actions in the undo stack
        assertEquals("Add neuron 2", networkPanel.undoManager.undoStack.peek().description, "The top action in the undo stack should be 'Add neuron 2'")
        assertEquals("Add neuron 1", networkPanel.undoManager.undoStack[0].description, "The bottom action in the undo stack should be 'Add neuron 1'")

        // Perform an undo operation to move an item from the undo stack to the redo stack
        networkPanel.undoManager.undo()

        // Verify that one action was moved from the undo stack to the redo stack
        assertEquals(1, networkPanel.undoManager.undoStack.size, "There should be 1 action in the undo stack after undo")
        assertEquals(1, networkPanel.undoManager.redoStack.size, "There should be 1 action in the redo stack after undo")

        // Verify the descriptions of the actions in the stacks
        assertEquals("Add neuron 1", networkPanel.undoManager.undoStack.peek().description, "The action in the undo stack should be 'Add neuron 1'")
        assertEquals("Add neuron 2", networkPanel.undoManager.redoStack.peek().description, "The action in the redo stack should be 'Add neuron 2'")

        // Verify that the neuron was actually removed from the network
        assertEquals(1, network.flatNeuronList.size, "There should be 1 neuron in the network after undo")
        assertFalse(network.flatNeuronList.contains(neuron2), "Neuron 2 should not be in the network after undo")

        // Perform another undo operation
        networkPanel.undoManager.undo()

        // Verify that another action was moved from the undo stack to the redo stack
        assertEquals(0, networkPanel.undoManager.undoStack.size, "The undo stack should be empty after two undos")
        assertEquals(2, networkPanel.undoManager.redoStack.size, "There should be 2 actions in the redo stack after two undos")

        // Verify that the neuron was actually removed from the network
        assertEquals(0, network.flatNeuronList.size, "There should be no neurons in the network after two undos")

        // Perform a redo operation
        networkPanel.undoManager.redo()

        // Verify that an action was moved from the redo stack to the undo stack
        assertEquals(1, networkPanel.undoManager.undoStack.size, "There should be 1 action in the undo stack after redo")
        assertEquals(1, networkPanel.undoManager.redoStack.size, "There should be 1 action in the redo stack after redo")

        // Verify that the neuron was actually added back to the network
        assertEquals(1, network.flatNeuronList.size, "There should be 1 neuron in the network after redo")
        assertTrue(network.flatNeuronList.contains(neuron1), "Neuron 1 should be in the network after redo")

        // Perform another redo operation
        networkPanel.undoManager.redo()

        // Verify that another action was moved from the redo stack to the undo stack
        assertEquals(2, networkPanel.undoManager.undoStack.size, "There should be 2 actions in the undo stack after two redos")
        assertEquals(0, networkPanel.undoManager.redoStack.size, "The redo stack should be empty after two redos")

        // Verify that the neuron was actually added back to the network
        assertEquals(2, network.flatNeuronList.size, "There should be 2 neurons in the network after two redos")
        assertTrue(network.flatNeuronList.contains(neuron2), "Neuron 2 should be in the network after two redos")

        // This test verifies that the UndoManager correctly maintains the undo and redo stacks
        // and that the undo and redo operations work as expected, which is what the
        // showUndoHistoryDialog's "Go To Selected Point" button would use
    }
}
