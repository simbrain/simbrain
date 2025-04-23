package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.gui.UndoManager.UndoableAction

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
}
