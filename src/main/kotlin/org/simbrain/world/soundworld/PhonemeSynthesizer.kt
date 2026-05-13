package org.simbrain.world.soundworld

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.util.UserParameter
import org.simbrain.util.showCopyableWarningDialog
import org.simbrain.workspace.Consumable
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.swing.SwingUtilities

/**
 * Trigger eSpeak initialization and, on failure, surface a one-time warning dialog so the
 * user understands why audio is silent (rather than only seeing a stderr line). Safe to call
 * from any thread; the dialog is dispatched to the EDT. Subsequent calls within the same JVM
 * are no-ops once the dialog has been shown.
 */
private val espeakWarningShown = AtomicBoolean(false)

fun warnIfEspeakUnavailable() {
    if (EspeakRuntime.ensureInitialized()) return
    if (!espeakWarningShown.compareAndSet(false, true)) return
    val message = EspeakRuntime.errorMessage ?: "Phoneme audio is not available."
    SwingUtilities.invokeLater { showCopyableWarningDialog(message) }
}

/**
 * Speaks phonemes or words using the bundled `libespeak-ng` library, called directly via
 * JNA through [EspeakRuntime].
 *
 * Each call to [speakPhonemes] or [speakWord] enqueues a synthesis job on the audio
 * coroutine. Jobs run sequentially: the coroutine pulls a job, hands the text to eSpeak,
 * and writes PCM samples to the underlying [SourceDataLine][javax.sound.sampled.SourceDataLine]
 * as eSpeak produces them via its synth callback. The phoneme channel uses rendezvous
 * capacity so callers (couplings, click handlers) block when audio is in flight.
 *
 * Phoneme strings are passed in eSpeak-ng's Kirshenbaum notation, wrapped in `[[ ]]`
 * automatically (e.g. `h@l'oU` for "hello"). Plain text is also supported via [speakWord].
 *
 * If the bundled library can't be loaded, calls become no-ops and a one-time warning is
 * printed.
 */
class PhonemeSynthesizer : SoundGenerator() {

    @UserParameter(label = "Voice", description = "Speaking voice / accent", order = 10)
    var voice: Voice = Voice.EN_US

    @UserParameter(
        label = "Speed (80-450 wpm)",
        description = "Speaking speed in words per minute.",
        minimumValue = 80.0,
        maximumValue = 450.0,
        increment = 10.0,
        order = 20
    )
    var speed: Int = 175

    @UserParameter(
        label = "Pitch (0-99)",
        description = "Voice pitch.",
        minimumValue = 0.0,
        maximumValue = 99.0,
        increment = 5.0,
        order = 30
    )
    var pitch: Int = 50

    @UserParameter(
        label = "Amplitude (0-200)",
        description = "Voice volume.",
        minimumValue = 0.0,
        maximumValue = 200.0,
        increment = 10.0,
        order = 40
    )
    var amplitude: Int = 100

    override val sampleRate: Float
        get() = if (EspeakRuntime.isAvailable) EspeakRuntime.sampleRate.toFloat() else 22050f

    override val format: AudioFormat get() = AudioFormat(sampleRate, 16, 1, true, false)

    @Transient
    var events: PhonemeSynthesizerEvents = PhonemeSynthesizerEvents()
        private set

    @Transient
    @Volatile
    private var scope: CoroutineScope = newScope()

    @Transient
    @Volatile
    private var phonemeChannel: Channel<Job> = Channel(capacity = Channel.RENDEZVOUS)

    @Transient
    @Volatile
    private var cancelRequested: Boolean = false

    /** The phoneme string currently being played, or empty when idle. */
    @Transient
    @Volatile
    var currentlySpeaking: String = ""
        private set

    init {
        startWorker()
    }

    @Consumable
    fun speakPhonemes(phonemes: String) {
        if (phonemes.isBlank()) return
        if (!EspeakRuntime.ensureInitialized()) return
        runBlocking { phonemeChannel.send(Job(phonemes, asPhonemes = true, label = phonemes)) }
    }

    @Consumable
    fun speakWord(word: String) {
        if (word.isBlank()) return
        if (!EspeakRuntime.ensureInitialized()) return
        runBlocking { phonemeChannel.send(Job(word, asPhonemes = false, label = word)) }
    }

    fun restoreDefaults() {
        voice = Voice.EN_US
        speed = 175
        pitch = 50
        amplitude = 100
    }

    /** Drop any queued audio and stop playback at the next sample boundary. */
    fun flush() {
        cancelRequested = true
        scope.cancel()
        phonemeChannel.close()
        line.flush()
        scope = newScope()
        phonemeChannel = Channel(capacity = Channel.RENDEZVOUS)
        cancelRequested = false
        currentlySpeaking = ""
        events.speakingChanged.fire("")
        startWorker()
    }

    private fun newScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun startWorker() {
        scope.launch {
            for (job in phonemeChannel) {
                runOne(job)
            }
        }
    }

    private fun runOne(job: Job) {
        EspeakRuntime.setVoice(voice.id)
        EspeakRuntime.setRate(speed)
        EspeakRuntime.setPitch(pitch)
        EspeakRuntime.setVolume(amplitude)

        setSpeaking(job.label)
        try {
            val text = if (job.asPhonemes) "[[${job.text}]]" else job.text
            val flags = if (job.asPhonemes) {
                EspeakNgLibrary.ESPEAK_CHARS_UTF8 or EspeakNgLibrary.ESPEAK_PHONEMES
            } else {
                EspeakNgLibrary.ESPEAK_CHARS_UTF8
            }
            EspeakRuntime.synth(text, flags) { samples, count ->
                if (cancelRequested) return@synth 1
                if (samples == null || count == 0) return@synth 0
                val byteCount = count * 2
                val bytes = samples.getByteArray(0, byteCount)
                line.write(bytes, 0, byteCount)
                0
            }
            line.drain()
        } catch (e: Exception) {
            System.err.println("PhonemeSynthesizer playback failed: ${e.message}")
        }
        setSpeaking("")
    }

    private fun setSpeaking(value: String) {
        currentlySpeaking = value
        events.speakingChanged.fire(value)
    }

    override fun close() {
        scope.cancel()
        phonemeChannel.close()
        super.close()
    }

    fun readResolve(): Any {
        events = PhonemeSynthesizerEvents()
        scope = newScope()
        phonemeChannel = Channel(capacity = Channel.RENDEZVOUS)
        cancelRequested = false
        currentlySpeaking = ""
        startWorker()
        return this
    }

    override val id: String = "Phoneme Synthesizer"

    private data class Job(val text: String, val asPhonemes: Boolean, val label: String)

    /**
     * Bundled English voices. The string [id] is what eSpeak's `espeak_SetVoiceByName`
     * expects; [toString] is the dropdown label shown to the user.
     */
    enum class Voice(val id: String, private val displayName: String) {
        EN_US("en-us", "American (en-us)"),
        EN_GB_RP("en-gb-x-rp", "Received Pronunciation (en-gb-x-rp)"),
        EN_GB_SCOTLAND("en-gb-scotland", "Scottish (en-gb-scotland)"),
        EN_GB_LANCASTER("en-gb-x-gbclan", "Lancastrian (en-gb-x-gbclan)"),
        EN_GB_W_MIDLANDS("en-gb-x-gbcwmd", "West Midlands (en-gb-x-gbcwmd)"),
        EN_029("en-029", "Caribbean (en-029)"),
        EN_US_NYC("en-us-nyc", "New York City (en-us-nyc)"),
        EN_SHAW("en-shaw", "Shavian (en-shaw)");

        override fun toString(): String = displayName
    }
}
