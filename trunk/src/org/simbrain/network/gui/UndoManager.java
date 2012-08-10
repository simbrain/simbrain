/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui;

import java.util.Stack;

/**
 * Manage undo / redo operations in the network panel.
 *
 * @author jeffyoshimi
 * @see NetworkPanel.
 */
public class UndoManager {

    // Todo: implement a cap on max-undo.
    // Todo: prevent or constraint very large memory undaoble actions?

    /** All actions that can be undone are pushed to this stack. */
    private final Stack<UndoableAction> undoStack = new Stack<UndoableAction>();

    /**
     * When an action is undone, it is popped off the undo stack and pushed on
     * to this stack.
     */
    private final Stack<UndoableAction> redoStack = new Stack<UndoableAction>();

    /**
     * Add a new undoable action.
     *
     * @param action the action to add.
     */
    public void addUndoableAction(UndoableAction action) {
        undoStack.push(action);
        // When clicking undo then there is no longer anything to redo.
        redoStack.removeAllElements();
        //System.out.println("Add action");
        //printUndoRedoStats();
    }

    /**
     * Undo the last undoable action.
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableAction lastEvent = undoStack.pop();
            lastEvent.undo();
            redoStack.push(lastEvent);
            //System.out.println("Undo");
            //printUndoRedoStats();
        }
    }

    /**
     * Redo the last undone action.
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableAction redoEvent = redoStack.pop();
            redoEvent.redo();
            undoStack.push(redoEvent);
            //System.out.println("Redo");
            //printUndoRedoStats();
        }
    }

    /**
     * Print debug info on undo and redo stack.
     */
    private void printUndoRedoStats() {
        System.out.println("UndoStack: " + undoStack.size());
        System.out.println("RedoStack: " + redoStack.size());
    }

    /**
     * Interface for actions that can be undone and redone. Usually
     * implementations of this interface are anonymous classes, created where
     * the undoable action is needed. Example, when adding a neuron an undoable
     * action is created where undo removes that neuron and redo re-adds it.
     */
    public interface UndoableAction {

        /** Undo this action. */
        void undo();

        /** Redo this action. */
        void redo();

    }

}
