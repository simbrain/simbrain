package org.simbrain.world.textworld

import kotlinx.coroutines.runBlocking
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import java.awt.Color
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
    @get:Producible
    var text: String
        get() = _text
        set(value) {
            _text = value
            events.textChanged.fireAsync()
        }

    /**
     * Replaces the whole document, but only when [newText] actually differs — a per-iteration
     * coupling from a document producer syncs through this without endless change events. The
     * cursor follows [addTextAtEnd]'s convention: pinned to the end with the newest token
     * current, so the view tracks a growing document.
     */
    @Consumable
    fun setTextIfChanged(newText: String) {
        if (newText.isEmpty() || newText == _text) return
        text = newText
        position = _text.length
        events.cursorPositionChanged.fireAsync()
        currentTokenIndex = tokens.lastIndex
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
                    embeddingType = tokenEmbedding.embeddingType
                }.build(it)
            } ?: throw UnsupportedOperationException("Cannot change tokenizer when training document is not set.")
        }

    /**
     * When set, token boxes, highlighting, and [currentToken] follow this tokenizer instead of
     * the embedding's — so a document synced from a language model shows the model's true token
     * boundaries. Adopted automatically when a document coupling from a
     * [org.simbrain.util.ProvidesDisplayTokenizer] is created; embedding lookups are unaffected.
     */
    var displayTokenizer: Tokenizer<*>? = null
        set(value) {
            field = value
            events.textChanged.fireAsync()
        }

    /** Optional contextual guidance displayed below the document by the desktop view. */
    @Transient
    var statusMessageProvider: (() -> String?)? = null
        set(value) {
            field = value
            events.statusChanged.fire()
        }

    /**
     * When true, the text is read-only while the workspace is running, so a model streaming
     * into this document is never edited mid-iteration — pause the workspace to edit, and the
     * settled edit is consumed on the next Play or Step. Adopted automatically alongside
     * [displayTokenizer] when a document coupling from a generative model is created.
     */
    @UserParameter(
        label = "Read-only while running",
        description = "Lock the text while the workspace is running; pause to edit.",
        order = 6
    )
    var lockWhileRunning = false

    @delegate:Transient
    var tokens by DependenciesInvalidatingCachedObject(::text, ::tokenEmbedding, ::tokenizer, ::displayTokenizer) {
        (displayTokenizer ?: tokenizer).tokenize(text)
    }

    @Transient
    private var _currentTokenIndex = 0

    var currentTokenIndex: Int
        get() {
            val tokenList = tokens
            if (tokenList.isEmpty()) return 0
            _currentTokenIndex = _currentTokenIndex.coerceIn(0, tokenList.lastIndex)
            return _currentTokenIndex
        }
        set(value) {
            val tokenList = tokens
            if (tokenList.isEmpty()) return
            _currentTokenIndex = value.coerceIn(0, tokenList.lastIndex)
            events.currentTokenChanged.fireAsync(tokenList[_currentTokenIndex])
        }

    /**
     * Suspend version of setting currentTokenIndex that awaits the event to complete.
     * Use this in update actions to prevent backpressure when the simulation runs faster
     * than the UI can update.
     */
    suspend fun setCurrentTokenIndexSuspend(value: Int) {
        val tokenList = tokens
        if (tokenList.isEmpty()) return
        _currentTokenIndex = value.coerceIn(0, tokenList.lastIndex)
        events.currentTokenChanged.fire(tokenList[_currentTokenIndex])
    }

    @UserParameter(
        label = "Auto advance",
        description = "If true, automatically advance selected token on update.",
        order = 2
    )
    var autoAdvance = false

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
     * Sampling strategy for generating tokens from probability distributions.
     * Used primarily by language model simulations.
     */
    var samplingStrategy: SamplingStrategy = SamplingStrategy.TopP()

    /**
     * Set main text without firing an event.
     */
    fun setTextNoEvent(newText: String) {
        _text = newText
    }

    /**
     * Suspend version of setting text that awaits the textChanged event to complete.
     * Use this in update actions to prevent backpressure when the simulation runs faster
     * than the UI can update.
     */
    suspend fun setTextSuspend(newText: String) {
        _text = newText
        events.textChanged.fire()
    }

    /**
     * What the current "cursor" position in the text is.
     */
    var position = 0

    /**
     * Highlight color.
     */
    var highlightColor = Color.GRAY

    /**
     * The current text item.
     */
    private val currentTextItem: TokenizerResult get() = tokens[currentTokenIndex]

    /**
     * Find the token index that contains the given character position.
     * If no exact match, finds the closest token (preferring the one before the cursor).
     * Returns 0 if tokens are empty.
     */
    private fun findTokenIndexAtPosition(pos: Int): Int {
        if (tokens.isEmpty()) return 0
        
        // Try exact match first (cursor is within token bounds)
        val exactMatch = tokens.indexOfFirst { token -> 
            pos >= token.start && pos <= token.end
        }
        if (exactMatch >= 0) return exactMatch
        
        // If no exact match, find the last token whose end is before the cursor position
        // (this handles cursor in spaces between tokens or after the last character)
        val tokenBeforeCursor = tokens.indexOfLast { it.end < pos }
        if (tokenBeforeCursor >= 0) return tokenBeforeCursor
        
        // If position is before all tokens, return first token
        return 0
    }

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
        get() = tokens.getOrNull(currentTokenIndex)?.token?.let { 
            tokenEmbedding.get(it) 
        } ?: doubleArrayOf()

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

    suspend fun advance() {
        if (currentTokenIndex < tokens.size - 1) {
            setCurrentTokenIndexSuspend(currentTokenIndex + 1)
        } else {
            events.atEnd.fire()
            setCurrentTokenIndexSuspend(0)
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
     * Add a text to the end of the world text. Empty strings are ignored so per-iteration
     * couplings from sources that only sometimes produce a token add no stray spacing.
     */
    @Consumable
    fun addTextAtEnd(newText: String) {
        if (newText.isEmpty()) return
        addTextAtEnd(newText, " ")
    }

    fun addTextAtEnd(newText: String, spacing: String) {
        runBlocking {
            _text += "$spacing$newText"
            events.textChanged.fire()
            position = _text.length
            events.cursorPositionChanged.fire()
            currentTokenIndex = tokens.lastIndex
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
        get() = tokens.getOrNull(currentTokenIndex)?.token ?: ""

    fun setPosition(newPosition: Int, fireEvent: Boolean) {
        if (newPosition <= text.length) {
            position = newPosition
            if (tokens.isNotEmpty()) {
                currentTokenIndex = findTokenIndexAtPosition(newPosition)
            }
            if (fireEvent) {
                events.cursorPositionChanged.fireAsync()
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
     * Sample a token using various sampling strategies
     * @param probabilities Raw logits from the model
     * @param samplingStrategy The sampling strategy to use
     * @return The sampled token
     */
    fun sampleToken(probabilities: DoubleArray): String {
        val tokenIndex = samplingStrategy.sample(probabilities)
        return tokenEmbedding.tokens.getOrElse(tokenIndex) { "UNK" }
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



