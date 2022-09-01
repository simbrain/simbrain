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

import com.thoughtworks.xstream.XStream
import org.simbrain.util.UserParameter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.Producible
import org.simbrain.world.textworld.TextListener.TextAdapter
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * ReaderWorld intuitively models "reading". Text in the main display is
 * parsed by letter or word (where a "word" is the determined by a regular
 * expression that can be customized), and highlighted. This item is converted
 * in to scalar or vector values and sent to consumers (mainly neurons and
 * neuron groups) via couplings.
 *
 * When the reader world is updated, the current character or word is
 * highlighted. A dictionary is consulted, and if a match is found, any
 * correspond couplings produce values:
 *
 *  Vector: When the character or word is highlighted, send a vector to all
 * associated consumers. These vectors are specified in the "vector dictionary".
 */
class ReaderWorld private constructor() : TextWorld() {

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
    var theParseStyle = ParseStyle.WORD
        private set

    /**
     * Regular expression pattern. By default search for whole words
     */
    private var pattern: Pattern
    // TODO: Document other good choices in the pref dialog. e.g. (\\w+)

    /**
     * Regular expression for matcher.
     */
    @UserParameter(label = "Regular Expression", description = "Regular expression for matcher.", order = 2)
    private var regularExpression = "(\\S+)"

    /**
     * Pattern matcher.
     */
    private var matcher: Matcher?

    /**
     * Constructs an instance of TextWorld.
     */
    init {
        pattern = Pattern.compile(regularExpression)
        matcher = pattern.matcher(text)
    }

    /**
     * Returns 1 if the current item is this character, or 0 otherwise. Used for
     * localist representations of letters.
     *
     * @param token the letter to search for
     * @return 1 if the letter is contained, 0 otherwise.
     */
    fun getMatchingScalar(token: String?): Int {
        if (currentItem == null) {
            return 0
        }
        return if (currentItem?.text.equals(token, ignoreCase = true)) {
            1
        } else {
            0
        }
    }

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
                DoubleArray(tokenToVectorDict.size)
            } else {
                tokenToVectorDict.get(it.text)
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
        get() = currentItem.let { it?.text ?: "" }

    /**
     * Advance the position in the text, and update the current item.
     */
    override fun update() {
        if (theParseStyle == ParseStyle.CHARACTER) {
            wrapText()
            val begin = position
            val end = position + 1
            currentItem = TextItem(begin, end, text.substring(begin, end))
            position = end
        } else if (theParseStyle == ParseStyle.WORD) {
            if (matcher == null) {
                return
            }
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
        matcher!!.reset(text)
        matcher!!.region(begin, end)
    }

    /**
     * Find the next token in the text area.
     *
     * @return true if some token is found, false otherwise.
     */
    private fun findNextToken(): Boolean {
        val foundToken = matcher!!.find()
        currentTextItem = if (foundToken) {
            val begin = matcher!!.start()
            val end = matcher!!.end()
            val text = matcher!!.group()
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
        matcher!!.find()
        val nextOne = matcher!!.group()
        updateMatcher() // Return matcher to its previous state
        return nextOne
    }

    /**
     * @param parseStyle the parseStyle to set
     */
    fun setParseStyle(parseStyle: ParseStyle) {
        theParseStyle = parseStyle
        // TODO: Fire an event that the radio button listens to
    }

    /**
     * @return the regularExpression
     */
    fun getRegularExpression(): String {
        return regularExpression
    }

    /**
     * @param regularExpression the regularExpression to set
     */
    fun setRegularExpression(regularExpression: String) {
        this.regularExpression = regularExpression
        pattern = Pattern.compile(regularExpression)
        matcher = pattern.matcher(text)
        updateMatcher()
    }

    companion object {
        /**
         * Factory method for Reader world.
         *
         * @return the constructed world.
         */
        @JvmStatic
        fun createReaderWorld(): ReaderWorld {
            val r = ReaderWorld()
            r.addListener(object : TextAdapter() {
                override fun textChanged() {
                    r.updateMatcher()
                }

                override fun positionChanged() {
                    r.updateMatcher()
                }
            })
            return r
        }

        /**
         * Returns a properly initialized xstream object.
         *
         * @return the XStream object
         */
        @JvmStatic
        val xStream: XStream
            get() = getSimbrainXStream()
    }
}