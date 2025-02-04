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

import kotlinx.coroutines.runBlocking
import org.simbrain.util.DependenciesInvalidatingCachedObject
import org.simbrain.util.SimpleTokenizer
import org.simbrain.util.TokenizerResult
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/**
 * TextWorld is an environment for modeling speech and reading and other linguistic phenomena and their interactions
 * with a neural network.
 *
 * A [TokenEmbedding] object associates words or other tokens with vectors and vice versa, using [Coupling]s.
 *
 * Text in the main window is parsed and highlighted, and if a corresponding entry is found in the token embedding, a
 * vector is sent to any coupled objects, for example the input layer of a neural network.
 *
 * Output from a neural network can also be sent to the world. The closest matching vector in the token embedding is
 * found and then the corresponding token in the embedding is printed to the main window.
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
    var tokenEmbedding = TokenEmbeddingBuilder(
        tokenizer = SimpleTokenizer(),
        embeddingType = EmbeddingType.OneHot()
    ).build("Dog cat Hello how are you")
        set(value) {
            field = value
            events.tokenVectorMapChanged.fire()
        }

    /**
     * Private backing for [text] field.
     */
    private var _text = ""
        set(value) {
            field = value
            position = min(position, value.length)
        }

    /**
     * The main "world text" associated with this world (which displays in the main window).
     */
    var text: String
        get() = _text
        set(value) {
            _text = value
            events.textChanged.fireAndBlock()
        }

    @UserParameter(
        label = "Tokenizer",
        description = "The tokenizer to use for parsing text.",
        order = 5
    )
    var tokenizer
        get() = tokenEmbedding.tokenizer
        set(value) {
            tokenEmbedding.trainingDocument?.let {
                tokenEmbedding = TokenEmbeddingBuilder().apply {
                    tokenizer = value
                }.build(it)
            } ?: throw UnsupportedOperationException("Cannot change tokenizer when training document is not set.")
        }

    @delegate:Transient
    var tokens by DependenciesInvalidatingCachedObject(::text, ::tokenEmbedding, ::tokenizer) {
        tokenizer.tokenize(text)
    }

    @Transient
    var currentTokenIndex = 0
        get() {
            field = field.coerceIn(0, max(tokens.lastIndex, 0))
            return field
        }
        set(value) {
            field = value
            events.currentTokenChanged.fireAndBlock(tokens[value])
        }

    @UserParameter(
        label = "Auto advance",
        description = "If true, automatically advance to the next token.",
        order = 2
    )
    var autoAdvance = true

    @UserParameter(
        label = "Highlight current token",
        description = "If true, highlight the current token.",
        order = 3
    )
    var highlightCurrentToken = true

    @UserParameter(
        label = "Show token boundaries",
        description = "If true, draw a rectangle around each token.",
        order = 4
    )
    var showTokenBoundaries = true

    /**
     * Set main text without firing an event.
     */
    fun setTextNoEvent(newText: String) {
        _text = newText
    }

    /**
     * What the current "cursor" position in the text is.
     */
    var position = 0

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
    private val currentTextItem: TokenizerResult get() = tokens[currentTokenIndex]

    @UserParameter(
        label = "Stop at end",
        description = "If true, the workspace will stop at the end of the text area.",
        order = 3
    )
    var stopAtEnd: Boolean = false

    @Transient
    var events = TextWorldEvents()

    /**
     * Returns the double array associated with the currently selected token
     * (character or word). The reader world can produce a vector at any moment
     * by calling this function. Called by reflection by ReaderComponent.
     *
     * @return the vector corresponding to the currently parsed token.
     */
    @get:Producible
    val currentVector: DoubleArray
        get() = tokens[currentTokenIndex].token.let { tokenEmbedding.get(it) }

    /**
     * Display the string associated with the closest matching vector in the embedding
     */
    @Consumable()
    fun displayClosestWord(key: DoubleArray) {
        // Using addTextAtCursor produces strange results. Must be better synced with cursor.
        addTextAtEnd(tokenEmbedding.getClosestWord(key))
    }

    /**
     * Advance the position in the text, and update the current item.
     */
    suspend fun update() {
        if (autoAdvance) {
            advance()
        }
    }

    fun advance() {
        if (currentTokenIndex < tokens.size - 1) {
            currentTokenIndex++
        } else {
            events.atEnd.fire()
            currentTokenIndex = 0
        }
        position = tokens[currentTokenIndex].end
    }

    /**
     * Add a text to the end of the world text.
     */
    @Consumable
    fun addTextAtCursor(newText: String) {
        text = StringBuilder(text).insert(position, " $newText ").toString()
        position += newText.length + 1
        events.textChanged.fireAndBlock()
    }

    /**
     * Add a text to the end of the world text.
     */
    @Consumable
    fun addTextAtEnd(newText: String, spacing: String = " ") {
        text += "$spacing$newText"
        runBlocking {
            position = text.length
            events.cursorPositionChanged.fire().await()
            events.textChanged.fire().await()
            events.currentTokenChanged.fire(tokens[tokens.lastIndex]).await()
        }
    }

    /**
     * Returns a standard java string containing the character or characters
     * selected by the reader world.
     *
     * @return the current string
     */
    @get:Producible
    val currentToken: String
        get() = tokens[currentTokenIndex].token

    fun setPosition(newPosition: Int, fireEvent: Boolean) {
        if (newPosition <= text.length) {
            lastPosition = position
            position = newPosition
            if (fireEvent) {
                events.cursorPositionChanged.fire()
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
        events = TextWorldEvents()
        return this
    }

    override val id = "Text World"

}




