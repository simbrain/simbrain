package org.simbrain.network.gui

import org.simbrain.network.gui.UndoManager.UndoableAction
import java.util.*

/**
 * Manage undo / redo operations in the network panel.
 */
class UndoManager {
    // Todo: implement a cap on max-undo.
    /**
     * All actions that can be undone are pushed to this stack.
     */
    private val undoStack = Stack<UndoableAction>()

    /**
     * When an action is undone, it is popped off the undo stack and pushed on
     * to this stack.
     */
    private val redoStack = Stack<UndoableAction>()

    fun addUndoableAction(action: UndoableAction) {
        undoStack.push(action)
        redoStack.removeAllElements()
    }

    fun addUndoableAction(initialContext: Any? = null, undo: suspend (context: Any?) -> Unit, redo: suspend (context: Any?) -> Unit) {
        addUndoableAction(undoableAction(initialContext, undo, redo))
    }

    /**
     * Undo the last undoable action.
     */
    suspend fun undo() {
        if (!undoStack.isEmpty()) {
            val lastEvent = undoStack.pop()
            lastEvent.undo()
            redoStack.push(lastEvent)
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
        }
    }

    interface UndoableAction {

        var context: Any?

        suspend fun undo()

        suspend fun redo()
    }
}

fun undoableAction(initialContext: Any?, undo: suspend (context: Any?) -> Unit, redo: suspend (context: Any?) -> Unit) = object : UndoableAction {

    override var context: Any? = initialContext

    override suspend fun undo() {
        undo(context)
    }

    override suspend fun redo() {
        redo(context)
    }
}
