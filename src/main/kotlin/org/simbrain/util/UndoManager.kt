/**
 * The undo and redo stacks shared by editors that support undo, such as the network panel and image world. Each editor
 * supplies its own [UndoManager.UndoableAction]s; this file knows nothing about what they change.
 */
package org.simbrain.util

import org.simbrain.util.UndoManager.UndoableAction
import java.util.*

/**
 * Manage undo / redo operations.
 *
 * Related code can be found in callers to [addUndoableAction].
 *
 * @param maxBytes the most memory the stacks may hold, by [UndoableAction.sizeBytes]. The oldest actions are forgotten
 * first, and an action larger than this on its own is not kept at all.
 */
class UndoManager(private val maxBytes: Long = Long.MAX_VALUE) {

    /**
     * All actions that can be undone are pushed to this stack.
     */
    val undoStack = Stack<UndoableAction>()

    /**
     * When an action is undone, it is popped off the undo stack and pushed on
     * to this stack.
     */
    val redoStack = Stack<UndoableAction>()

    /**
     * Called after either stack changes, on the thread that changed it.
     */
    var onChanged: () -> Unit = {}

    fun addUndoableAction(action: UndoableAction) {
        undoStack.push(action)
        redoStack.removeAllElements()
        while (undoStack.isNotEmpty() && undoStack.sumOf { it.sizeBytes } > maxBytes) {
            undoStack.removeAt(0)
        }
        onChanged()
    }

    fun addUndoableAction(
        description: String,
        undo: suspend () -> Unit,
        redo: suspend () -> Unit
    ) {
        addUndoableAction(undoableAction(description, undo, redo))
    }

    /**
     * Undo the last undoable action.
     */
    suspend fun undo() {
        if (!undoStack.isEmpty()) {
            val lastEvent = undoStack.pop()
            lastEvent.undo()
            redoStack.push(lastEvent)
            onChanged()
        }
    }

    /**
     * Redo the last undone action.
     */
    suspend fun redo() {
        if (!redoStack.isEmpty()) {
            val redoEvent = redoStack.pop()
            redoEvent.redo()
            undoStack.push(redoEvent)
            onChanged()
        }
    }

    /**
     * Forget every action.
     */
    fun clear() {
        undoStack.removeAllElements()
        redoStack.removeAllElements()
        onChanged()
    }

    interface UndoableAction {
        val description: String

        /**
         * Memory this action keeps alive, counted against [UndoManager]'s limit.
         */
        val sizeBytes: Long get() = 0
        suspend fun undo()
        suspend fun redo()
    }

}

fun undoableAction(
    description: String,
    undo: suspend () -> Unit,
    redo: suspend () -> Unit) =
    object : UndoableAction {

        override val description = description

        override suspend fun undo() {
            undo()
        }

        override suspend fun redo() {
            redo()
        }
    }
