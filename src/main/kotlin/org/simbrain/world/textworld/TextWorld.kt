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
package org.simbrain.world.textworld

import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import smile.math.matrix.Matrix
import java.awt.Color
import javax.swing.SwingUtilities

/**
 * TextWorld is an environment for modeling speech and reading and other
 * linguistic phenomena. It is the superclass for readerworld, where text is
 * converted or "read" and used to produce activation in neural nets (reader
 * world), and display world, where activations from neural nets can be used to
 * display text (e.g. modeled speech).
 */
abstract class TextWorld: AttributeContainer, EditableObject {

    /**
     * A "dictionary" which associates string tokens with arrays of doubles and vice-versa
     */
    var tokenToVectorDict = TokenVectorDictionary(
        // TODO: This is temporary
        tokens = listOf("Dog", "Cat", "Hello", "how", "are", "you"),
        tokenVectorMatrix = Matrix.eye(6)
    )
        set(value) {
            field = value
            fireDictionaryChangedEvent()
        }

    /**
     * The main displayed text to be parsed.
     */
    var text = ""
        set(value) {
            field = value
            // fireTextChangedEvent()
        }

    /**
     * The current item of text (letter, word, etc.)
     */
     protected var currentItem: TextItem? = null
        set(value) {
            field = value
            fireCurrentItemChanged(value)
        }

    /**
     * What the current position in the text is.
     */
    protected var position = 0
        set(value) {
            field = value
        }

    /**
     * Last position in the text.
     */
    protected var lastPosition = 0

    /**
     * List of listeners on this world.
     */
    @Transient
    private var listenerList: MutableList<TextListener> = ArrayList()

    /**
     * Highlight color.
     */
    var highlightColor = Color.GRAY

    /**
     * Advance the position in the text, and update the current item.
     */
    abstract fun update()

    /**
     * Add a text to the end of the underling text object.
     */
    @Consumable
    fun addText(newText: String) {
        position = text.length
        text += newText
        fireTextChangedEvent()
    }

    fun addListener(listener: TextListener) {
        listenerList.add(listener)
    }

    fun removeListener(listener: TextListener) {
        listenerList.remove(listener)
    }

    /**
     * Notify listeners that the text has changed.
     */
    fun fireTextChangedEvent() {
        SwingUtilities.invokeLater{
            for (listener in listenerList) {
                listener.textChanged()
            }
        }
    }

    /**
     * Notify listeners that the dictionary has changed.
     */
    fun fireDictionaryChangedEvent() {
        for (listener in listenerList) {
            listener.dictionaryChanged()
        }
    }

    /**
     * Notify listeners that preferences have changed.
     */
    fun firePrefsChangedEvent() {
        for (listener in listenerList) {
            listener.preferencesChanged()
        }
    }

    /**
     * Notify listeners that the caret position has changed.
     */
    fun firePositionChangedEvent() {
        for (listener in listenerList) {
            listener.positionChanged()
        }
    }

    /**
     * Notify listeners that the caret position has changed.
     *
     * @param newItem
     */
    fun fireCurrentItemChanged(newItem: TextItem?) {
        for (listener in listenerList) {
            listener.currentItemChanged(newItem)
        }
    }

    // TODO: Remove
    fun setText(text: String, fireEvent: Boolean) {
        this.text = text
        // if (fireEvent) {
        //     fireTextChangedEvent()
        // }
    }

    // TODO
    fun setPosition(newPosition: Int, fireEvent: Boolean) {
        if (newPosition <= text.length) {
            lastPosition = position
            position = newPosition
            if (fireEvent) {
                firePositionChangedEvent()
            }
        } else {
            System.err.println("Invalid position:$newPosition")
        }
    }

    /**
     * Returns a "preview" of the next character in the world. Used in some
     * scripts.
     *
     * @return the next character.
     */
    fun previewNextChar(): String {
        if (position < text.length) {
            return text.substring(position, position + 1)
        } else if (position == text.length) {
            return text.substring(0, 1)
        }
        return ""
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    protected open fun readResolve(): Any? {
        listenerList = ArrayList()
        return this
    }

    /**
     * Represents the "current item" as String, and includes a representation of
     * the beginning and ending of the item in the main text.
     */
    inner class TextItem (

        /**
         * Initial position in main text.
         */
        val beginPosition: Int,
        /**
         * Final position in main text.
         */
        val endPosition: Int,
        /**
         * The item text.
         */
        val text: String
    ) {

        override fun toString(): String {
            return "($beginPosition,$endPosition) $text"
        }
    }
}