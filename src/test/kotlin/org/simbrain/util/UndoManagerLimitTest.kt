/**
 * The memory limit and change notification of [UndoManager]. Its undo and redo behavior is covered through the network
 * panel in the network gui tests.
 */
package org.simbrain.util

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UndoManagerLimitTest {

    private fun actionOf(description: String, bytes: Long) = object : UndoManager.UndoableAction {
        override val description = description
        override val sizeBytes = bytes
        override suspend fun undo() {}
        override suspend fun redo() {}
    }

    @Test
    fun `the oldest actions are forgotten once the limit is passed`() {
        val manager = UndoManager(maxBytes = 100)
        manager.addUndoableAction(actionOf("first", 60))
        manager.addUndoableAction(actionOf("second", 30))
        manager.addUndoableAction(actionOf("third", 30))
        assertEquals(listOf("second", "third"), manager.undoStack.map { it.description })
    }

    @Test
    fun `an action larger than the limit is not kept`() {
        val manager = UndoManager(maxBytes = 100)
        manager.addUndoableAction(actionOf("small", 10))
        manager.addUndoableAction(actionOf("huge", 500))
        assertTrue(manager.undoStack.isEmpty())
    }

    @Test
    fun `actions with no size are never forgotten`() {
        val manager = UndoManager()
        repeat(1000) { manager.addUndoableAction(undoableAction("action", {}, {})) }
        assertEquals(1000, manager.undoStack.size)
    }

    @Test
    fun `every change to the stacks is announced`() = runBlocking {
        val manager = UndoManager()
        var changes = 0
        manager.onChanged = { changes++ }
        manager.addUndoableAction(undoableAction("action", {}, {}))
        manager.undo()
        manager.redo()
        manager.clear()
        assertEquals(4, changes)
    }
}
