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

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.Color
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * TextWorld is an environment for modeling speech and reading and other linguistic phenomena and their interactions
 * with a neural network.
 *
 * A dictionary object associates words or other tokens with vectors and vice versa, using [Coupling]s.
 *
 * Text in the main window is parsed and highlighted, and if a corresponding entry is found in the dictionary, a
 * vector is sent to any coupled objects, for example the input layer of a neural network.
 *
 * Output from a neural network can also be sent to the world. The closest matching vector in the dictionary is found
 * and then the corresponding token in the dictionary is printed to the main window.
 *
 * The dictionary can be generated in several ways, which correspond to methods of word embedding.
 *
 * @see https://en.wikipedia.org/wiki/Word_embedding
 * @author Jeff Yoshimi
 * @author Yulin Li
 *
 */
class TextWorld : AttributeContainer, EditableObject {

    /**
     * Associates string tokens with arrays of doubles and vice-versa
     */
    var tokenVectorMap = TokenVectorMap(
        tokens = listOf("Dog", "Cat", "Hello", "how", "are", "you"),
        tokenVectorMatrix = Matrix.eye(6)
    )
        set(value) {
            field = value
            events.fireTokenVectorMapChanged()
        }

    /**
     * The main displayed text to be parsed.
     */
    var text = ""
        set(value) {
            field = value
        }

    /**
     * The current item of text (letter, word, etc.)
     */
    var currentItem: TextItem? = null
        set(value) {
            field = value
            events.fireCurrentTokenChanged(value)
        }

    /**
     * What the current position in the text is.
     */
    var position = 0
        set(value) {
            field = value
        }

    /**
     * Last position in the text.
     */
    protected var lastPosition = 0

    /**
     * Highlight color.
     */
    var highlightColor = Color.GRAY

    /**
     * The current text item.
     */
    private var currentTextItem: TextItem? = null

    /**
     * List of parsing style.
     */
    enum class ParseStyle {
        CHARACTER, WORD
    }

    /**
     * The current parsing style.
     */
    @UserParameter(label = "Parse Style", description = "The current parsing style.", order = 1)
    var parseStyle = ParseStyle.WORD
    // TODO: Fire an event that the radio button listens to

    /**
     * Regular expression for matcher.
     */
    @UserParameter(label = "Regular Expression", description = "Regular expression for matcher.", order = 2)
    private var regularExpression = "(\\S+)"
        set(value) {
            field = value
            pattern = Pattern.compile(regularExpression)
            matcher = pattern.matcher(text)
            updateMatcher()
        }

    /**
     * Regular expression pattern. By default search for whole words
     */
    private var pattern: Pattern = Pattern.compile(regularExpression)
    // TODO: Document other good choices in the pref dialog. e.g. (\\w+)

    /**
     * Pattern matcher.
     */
    private var matcher: Matcher = pattern.matcher(text)

    @Transient
    var events = TextWorldEvents(this)

    /**
     * Returns the double array associated with the currently selected token
     * (character or word). The reader world can produce a vector at any moment
     * by calling this function. Called by reflection by ReaderComponent.
     *
     * @return the vector corresponding to the currently parsed token.
     */
    @get:Producible
    val currentVector: DoubleArray
        get() = currentItem.let {
            if (it == null) {
                DoubleArray(tokenVectorMap.size)
            } else {
                tokenVectorMap.get(it.text)
            }
        }

    /**
     * Display the string associated with the closest matching vector in the
     * dictionary.
     */
    @Consumable()
    fun displayClosestWord(key: DoubleArray) {
        addText(tokenVectorMap.getClosestWord(key))
    }

    /**
     * Advance the position in the text, and update the current item.
     */
    fun update() {
        if (parseStyle == ParseStyle.CHARACTER) {
            wrapText()
            val begin = position
            val end = position + 1
            currentItem = TextItem(begin, end, text.substring(begin, end))
            position = end
        } else if (parseStyle == ParseStyle.WORD) {
            wrapText()
            val matchFound = findNextToken()
            if (matchFound) {
                selectCurrentToken()
            } else {
                // No match found. Go back to the beginning of the text area
                // and select the first token found
                position = 0
                updateMatcher()
                // Having wrapped to the beginning select the next token, if
                // there is one.
                if (findNextToken()) {
                    selectCurrentToken()
                }
            }
        }
    }

    /**
     * Reset the parser and specify the region focused on by it, to go from the
     * current cursor position to the end of the text.
     */
    fun updateMatcher() {
        val begin = position
        val end = text.length
        // System.out.println(begin + "," + end);
        matcher.reset(text)
        matcher.region(begin, end)
    }

    /**
     * Find the next token in the text area.
     *
     * @return true if some token is found, false otherwise.
     */
    private fun findNextToken(): Boolean {
        val foundToken = matcher.find()
        currentTextItem = if (foundToken) {
            val begin = matcher.start()
            val end = matcher.end()
            val text = matcher.group()
            // System.out.println("[" + text + "](" + begin + "," + end + ")");
            TextItem(begin, end, text)
        } else {
            null
        }
        return foundToken
    }

    /**
     * Select the current token.
     */
    private fun selectCurrentToken() {
        currentItem = currentTextItem
        position = currentTextItem!!.endPosition
    }

    /**
     * If the position is at the end of the text area, "reset" the position to
     * 0.
     */
    private fun wrapText() {
        if (atEnd()) {
            position = 0
            updateMatcher()
        }
    }

    /**
     * @return true if the current position is past the end of the text area,
     * false otherwise.
     */
    private fun atEnd(): Boolean {
        return position >= text.length
    }

    /**
     * Utility method to "preview" the next token after the current one. Used in
     * some scripts.
     *
     * @return the next token in the text area.
     */
    fun previewNextToken(): String {
        matcher.find()
        val nextOne = matcher.group()
        updateMatcher() // Return matcher to its previous state
        return nextOne
    }

    /**
     * Add a text to the end of the underling text object.
     */
    @Consumable
    fun addText(newText: String) {
        position = text.length
        text += newText
        events.fireTextChanged()
    }

    /**
     * Returns a standard java string containing the character or characters
     * selected by the reader world.
     *
     * @return the current string
     */
    @get:Producible
    val currentToken: String
        get() = currentItem.let { it?.text ?: "" }

    // TODO: Remove
    fun setText(text: String, fireEvent: Boolean) {
        this.text = text
        if (fireEvent) {
            events.fireTextChanged()
        }
    }

    // TODO
    fun setPosition(newPosition: Int, fireEvent: Boolean) {
        if (newPosition <= text.length) {
            lastPosition = position
            position = newPosition
            if (fireEvent) {
                events.fireCursorPositionChanged()
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
    fun readResolve(): TextWorld {
        events = TextWorldEvents(this)
        return this
    }

    override val id = "Text World"

    /**
     * Represents the "current item" as String, and includes a representation of
     * the beginning and ending of the item in the main text.
     */
    inner class TextItem(

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