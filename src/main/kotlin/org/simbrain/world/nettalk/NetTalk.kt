package org.simbrain.world.nettalk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.util.UserParameter
import org.simbrain.util.nettalk.NettalkEncoder
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.world.soundworld.PhonemeSynthesizer

/**
 * NETtalk reading component. Maintains a fixed text and a reading position; produces a
 * sliding 7-letter window for an external network's input layer; consumes the network's
 * 26-D output, decodes a phoneme + stress, accumulates per-word phoneme buffers, and
 * publishes phoneme strings for an external phoneme synthesizer to speak.
 *
 * Network and synthesizer are wired via couplings — this object owns no network of its
 * own. See `nettalkComponentSim.kt` for typical wiring.
 */
class NetTalk : AttributeContainer, EditableObject {

    /**
     * Text the network is reading. Reset to a default sentence with a wide letter
     * coverage; users can edit via the panel or by setting via a coupling.
     */
    @set:Consumable(description = "Replace the text being read.")
    var text: String = DEFAULT_TEXT
        set(value) {
            field = value
            if (value.isEmpty()) {
                position = 0
            } else {
                position = position.coerceIn(0, value.length - 1)
            }
            outputCorrespondsToPosition = -1
            lastDecodedPosition = -1
            wordBuffer.clear()
            wordLetters.clear()
            wordStartIndex = -1
            transcription.clear()
            posToTranscriptionRange.clear()
            pendingAudioSegments.clear()
            currentlyPlayingSegment = null
            events.textChanged.fire()
            events.audioSegmentChanged.fire()
        }

    /** 0-indexed character position in [text]; the "current letter". */
    @set:Consumable(description = "Jump the reading position to the given character index.")
    var position: Int = 0
        set(value) {
            val clamped = if (text.isEmpty()) 0 else value.coerceIn(0, text.length - 1)
            if (field != clamped) {
                field = clamped
                events.positionChanged.fire()
            }
        }

    @UserParameter(label = "Audio mode", description = "Whether to speak after each letter or accumulate per word.", order = 10)
    var audioMode: AudioMode = AudioMode.PER_WORD
        set(value) {
            field = value
            wordBuffer.clear()
            events.audioModeChanged.fire()
        }

    @UserParameter(label = "Auto advance", description = "If true, the position advances by one each time the component updates.", order = 20)
    var autoAdvance: Boolean = true

    @UserParameter(label = "Window size", description = "Letter window width centered on the current letter.", order = 30, minimumValue = 1.0)
    var windowSize: Int = 7
        set(value) {
            require(value >= 1 && value % 2 == 1) { "windowSize must be a positive odd integer" }
            field = value
            encoderCache = NettalkEncoder(value)
        }

    @Transient
    private var encoderCache: NettalkEncoder = NettalkEncoder(windowSize)

    val encoder: NettalkEncoder get() = encoderCache

    @Transient
    private var lastDecoded: Char = '-'

    /**
     * The text position whose phoneme is currently in [lastDecoded] / [lastFeatures].
     * Distinct from the live [position] field, which is two ticks ahead by the time the
     * panel repaints (couplings run before component updates, and `update()` advances
     * `position` after recording the input it sent). Used by the panel so the centered
     * letter in the window display matches the phoneme/IPA shown beneath it.
     * `-1` means no phoneme has been decoded yet.
     */
    @Transient
    var lastDecodedPosition: Int = -1
        private set

    @Transient
    private var lastFeatures: DoubleArray = DoubleArray(NettalkPhonology.outputDimension)

    @Transient
    private val wordBuffer: StringBuilder = StringBuilder()

    @Transient
    private val wordLetters: StringBuilder = StringBuilder()

    /**
     * Position whose phoneme the next [setNetworkOutput] call should be attributed to.
     *
     * The default workspace updater runs couplings before component updates, so when
     * `setNetworkOutput` reads the network's output, that output is from the previous
     * tick's forward pass — not the input that was just set this tick. This field tracks
     * which letter actually corresponds to the output being received, set in [update]
     * to the position whose window will be forwarded later in the same tick.
     *
     * `-1` means we haven't sent any input yet (first tick); skip processing.
     */
    @Transient
    private var outputCorrespondsToPosition: Int = -1

    /** Pending eSpeak phoneme string to be picked up by [phonemesToSpeak] this tick. */
    @Transient
    private var pendingSpeak: String = ""

    /** Letter index where the in-progress word started, or -1 if no word is in progress. */
    @Transient
    private var wordStartIndex: Int = -1

    /**
     * Queue of (text-range, phoneme-string) tuples that have been sent to a downstream
     * synthesizer but may not yet have started playing. As the synthesizer reports it has
     * begun playing each item, we pop the front and update [currentlyPlayingSegment].
     */
    @Transient
    private val pendingAudioSegments: ArrayDeque<AudioSegment> = ArrayDeque()

    /**
     * The audio segment currently being played by the connected synthesizer, or null when
     * idle. Updated by [notifyAudioStarted] / [notifyAudioFinished] which the panel wires
     * up to a `PhonemeSynthesizer.events.speakingChanged` event.
     */
    @Transient
    var currentlyPlayingSegment: AudioSegment? = null
        private set

    /** Accumulated transcription of everything decoded so far (NETtalk phoneme letters). */
    @Transient
    val transcription: StringBuilder = StringBuilder()

    /**
     * Map from text position to the transcription character range that was emitted for
     * that position. Used by the panel to mirror the text area's yellow/green highlights
     * onto the transcription. Non-letter characters that don't append phonemes are absent.
     */
    @Transient
    private val posToTranscriptionRange: HashMap<Int, IntRange> = HashMap()

    /** Transcription range for the most recently decoded letter, or null if none yet. */
    val lastDecodedTranscriptionRange: IntRange?
        get() = if (outputCorrespondsToPosition >= 0) posToTranscriptionRange[outputCorrespondsToPosition] else null

    /** Transcription range covering all letters in [textRange], or null if none mapped. */
    fun transcriptionRangeForTextRange(textRange: IntRange): IntRange? {
        var minStart = Int.MAX_VALUE
        var maxEnd = Int.MIN_VALUE
        for (p in textRange) {
            val r = posToTranscriptionRange[p] ?: continue
            if (r.first < minStart) minStart = r.first
            if (r.last > maxEnd) maxEnd = r.last
        }
        return if (minStart == Int.MAX_VALUE) null else minStart..maxEnd
    }

    /** Decoded stress symbol from the most recent network output (`>`, `<`, `0`, `1`, `2`). */
    @Transient
    var lastStress: Char = '>'
        private set

    @Transient
    var events: NetTalkEvents = NetTalkEvents()
        private set

    /**
     * Phoneme synthesizer this NetTalk speaks through. Owned by the component, so the
     * panel can edit its voice/speed/pitch settings without an extra `SoundWorld`
     * component. Still exposed publicly so simulations can couple to it independently
     * if desired.
     */
    val synthesizer: PhonemeSynthesizer = PhonemeSynthesizer()

    init {
        wireSynthesizerListener()
    }

    private fun wireSynthesizerListener() {
        synthesizer.events.speakingChanged.on(Dispatchers.Swing) { phonemes ->
            if (phonemes.isEmpty()) notifyAudioFinished() else notifyAudioStarted(phonemes)
        }
    }

    @get:Producible(description = "${'$'}{windowSize} × 29 one-hot encoding of the current letter window.")
    val currentWindow: DoubleArray
        get() = encoderCache.encodeWindow(text, position)

    @get:Producible(description = "The current letter at the reading position.")
    val currentLetter: String
        get() = text.getOrNull(position)?.toString() ?: ""

    @get:Producible(description = "The most recently decoded NETtalk phoneme symbol.")
    val predictedPhoneme: String
        get() = lastDecoded.toString()

    @get:Producible(description = "The most recently received 26-D articulatory feature + stress vector.")
    val articulatoryFeatures: DoubleArray
        get() = lastFeatures.copyOf()

    @get:Producible(description = "Phoneme string ready to speak this tick — empty when nothing should be spoken.")
    val phonemesToSpeak: String
        get() = pendingSpeak

    /** Accumulated phonemes for the word currently being read (cleared on word boundary). */
    @get:Producible(description = "Buffered phonemes for the in-progress word.")
    val currentWordPhonemes: String
        get() = wordBuffer.toString()

    /**
     * Receive a network output activation vector and decode a phoneme. In `PER_LETTER` mode,
     * sets [phonemesToSpeak] to the decoded phoneme's eSpeak form (or empty for silent).
     * In `PER_WORD` mode, accumulates phonemes until a non-letter character is reached,
     * then publishes the whole word for speaking.
     */
    @Consumable(description = "Network's 26-D output activation; decoded into a phoneme + stress.")
    fun setNetworkOutput(activations: DoubleArray) {
        if (activations.size != NettalkPhonology.outputDimension) return
        val (p, s) = NettalkPhonology.decodeOutput(activations)
        lastDecoded = p
        lastStress = s
        lastFeatures = activations.copyOf()

        // The output we just read corresponds to a window that was forwarded last tick.
        // Skip until [update] has had a chance to record which position that was.
        if (outputCorrespondsToPosition < 0 || text.isEmpty()) {
            pendingSpeak = ""
            events.decoded.fire()
            return
        }
        lastDecodedPosition = outputCorrespondsToPosition
        val centerChar = text.getOrNull(outputCorrespondsToPosition)?.lowercaseChar() ?: ' '

        pendingSpeak = when (audioMode) {
            AudioMode.PER_LETTER -> {
                if (centerChar in 'a'..'z') {
                    val tStart = transcription.length
                    transcription.append(p)
                    posToTranscriptionRange[outputCorrespondsToPosition] = tStart until transcription.length
                    events.transcriptionChanged.fire()
                }
                if (p == '-') {
                    ""
                } else {
                    val esp = NettalkPhonology.toEspeak[p] ?: ""
                    if (esp.isNotEmpty()) {
                        pendingAudioSegments.addLast(
                            AudioSegment(outputCorrespondsToPosition..outputCorrespondsToPosition, esp)
                        )
                    }
                    esp
                }
            }
            AudioMode.PER_WORD -> {
                if (centerChar in 'a'..'z') {
                    if (wordBuffer.isEmpty()) wordStartIndex = outputCorrespondsToPosition
                    wordLetters.append(centerChar)
                    wordBuffer.append(p)
                    val tStart = transcription.length
                    transcription.append(p)
                    posToTranscriptionRange[outputCorrespondsToPosition] = tStart until transcription.length
                    events.transcriptionChanged.fire()
                    ""
                } else if (wordBuffer.isNotEmpty()) {
                    val toSpeak = NettalkPhonology.nettalkToEspeak(wordBuffer.toString())
                    val range = wordStartIndex..(outputCorrespondsToPosition - 1).coerceAtLeast(wordStartIndex)
                    if (toSpeak.isNotEmpty()) {
                        pendingAudioSegments.addLast(AudioSegment(range, toSpeak))
                    }
                    wordBuffer.clear()
                    wordLetters.clear()
                    wordStartIndex = -1
                    transcription.append(' ')
                    events.transcriptionChanged.fire()
                    toSpeak
                } else {
                    ""
                }
            }
        }
        if (pendingSpeak.isNotEmpty()) {
            synthesizer.speakPhonemes(pendingSpeak)
        }
        events.decoded.fire()
    }

    /**
     * Called when the connected synthesizer reports it has started playing. Pops the next
     * queued segment and surfaces it as [currentlyPlayingSegment]. If the queue is empty
     * (e.g., a stray event after reset/flush), the previously-playing segment is left in
     * place so the highlight doesn't disappear unexpectedly.
     */
    fun notifyAudioStarted(phonemes: String) {
        pendingAudioSegments.removeFirstOrNull()?.let { currentlyPlayingSegment = it }
        events.audioSegmentChanged.fire()
    }

    /**
     * Called by the panel when the connected synthesizer reports it has finished playing.
     * Intentionally a no-op for the highlight: we leave [currentlyPlayingSegment] sticky so
     * the green marker stays put on the last-played segment during the gap before the next
     * one starts (eliminates flicker between segments). It moves only when a new one begins.
     */
    fun notifyAudioFinished() {
    }

    /** Reset to position 0 and clear all in-flight buffers (including queued audio). */
    fun reset() {
        position = 0
        outputCorrespondsToPosition = -1
        wordBuffer.clear()
        wordLetters.clear()
        wordStartIndex = -1
        transcription.clear()
        posToTranscriptionRange.clear()
        pendingSpeak = ""
        pendingAudioSegments.clear()
        currentlyPlayingSegment = null
        synthesizer.flush()
        wireSynthesizerListener()
        events.transcriptionChanged.fire()
        events.audioSegmentChanged.fire()
    }

    /**
     * Called once per workspace update. Records that the network is about to forward our
     * latest input (window at the current [position]) — so the next tick's
     * [setNetworkOutput] knows which letter the output it sees corresponds to. Then
     * advances [position] by one if [autoAdvance] is true.
     */
    suspend fun update() {
        if (text.isEmpty()) return
        outputCorrespondsToPosition = position
        if (autoAdvance) {
            position = (position + 1) % text.length
        }
    }

    fun readResolve(): NetTalk {
        events = NetTalkEvents()
        encoderCache = NettalkEncoder(windowSize)
        wireSynthesizerListener()
        return this
    }

    override val id: String = "NETtalk"

    enum class AudioMode { PER_WORD, PER_LETTER }

    /** A range of letters in [text] paired with the eSpeak phoneme string sent for them. */
    data class AudioSegment(val range: IntRange, val phonemes: String)

    companion object {
        const val DEFAULT_TEXT: String =
            "the quick brown fox jumps over the lazy dog. " +
            "she sells sea shells by the sea shore. " +
            "peter piper picked a peck of pickled peppers."
    }
}
