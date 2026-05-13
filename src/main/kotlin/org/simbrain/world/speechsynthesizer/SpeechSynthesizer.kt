package org.simbrain.world.speechsynthesizer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.simbrain.util.UserParameter
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import org.simbrain.world.soundworld.SoundGenerator
import javax.sound.sampled.AudioFormat

class SpeechSynthesizer : SoundGenerator() {

    @UserParameter(label = "Voice", description = "Speaking voice / accent", order = 10)
    var voice: Voice = Voice.EN_US

    @UserParameter(label = "Input mode", description = "Primary input form shown in this synthesizer.", order = 20)
    var inputMode: InputMode = InputMode.TEXT
        set(value) {
            field = value
            events.inputModeChanged.fire()
        }

    @UserParameter(label = "Feature decoder", description = "How feature vectors are decoded to phonemes.", order = 30)
    var codecType: PhonemeCodecType = PhonemeCodecType.NETTALK
        set(value) {
            field = value
            events.codecChanged.fire()
        }

    val codec: PhonemeCodec
        get() = codecType.codec

    @UserParameter(
        label = "Speed (80-450 wpm)",
        description = "Speaking speed in words per minute.",
        minimumValue = 80.0,
        maximumValue = 450.0,
        increment = 10.0,
        order = 40
    )
    var speed: Int = 175

    @UserParameter(
        label = "Pitch (0-99)",
        description = "Voice pitch.",
        minimumValue = 0.0,
        maximumValue = 99.0,
        increment = 5.0,
        order = 50
    )
    var pitch: Int = 50

    @UserParameter(
        label = "Amplitude (0-200)",
        description = "Voice volume.",
        minimumValue = 0.0,
        maximumValue = 200.0,
        increment = 10.0,
        order = 60
    )
    var amplitude: Int = 100

    override val sampleRate: Float
        get() = if (EspeakRuntime.isAvailable) EspeakRuntime.sampleRate.toFloat() else 22050f

    override val format: AudioFormat get() = AudioFormat(sampleRate, 16, 1, true, false)

    @Transient
    var events: SpeechSynthesizerEvents = SpeechSynthesizerEvents()
        private set

    @Transient
    @Volatile
    private var scope: CoroutineScope = newScope()

    @Transient
    @Volatile
    private var speechChannel: Channel<Job> = Channel(capacity = Channel.RENDEZVOUS)

    @Transient
    @Volatile
    private var cancelRequested: Boolean = false

    @Transient
    private val transcriptionBuffer = StringBuilder()

    @Transient
    private var lastFeatureVector: DoubleArray = DoubleArray(codec.inputDimension)

    @Transient
    var currentlySpeaking: String = ""
        private set

    @get:Producible(description = "Current utterance being played, or empty when idle.")
    val currentUtterance: String
        get() = currentlySpeaking

    @get:Producible(description = "Accumulated text or phoneme history spoken by this synthesizer.")
    val transcription: String
        get() = transcriptionBuffer.toString()

    @get:Producible(description = "Most recent feature vector received by the synthesizer.")
    val featureVector: DoubleArray
        get() = lastFeatureVector.copyOf()

    init {
        startWorker()
    }

    @Consumable(description = "Speak ordinary text.")
    fun speakText(text: String) {
        if (text.isBlank()) return
        appendTranscription(text.trim(), separator = " ")
        enqueue(Job(text, asPhonemes = false, label = text))
    }

    @Consumable(description = "Speak an eSpeak-ng phoneme string.")
    fun speakPhonemes(phonemes: String) {
        if (phonemes.isBlank()) return
        appendTranscription(phonemes.trim(), separator = " ")
        enqueue(Job(phonemes, asPhonemes = true, label = phonemes))
    }

    @Consumable(description = "Decode a feature vector to a phoneme with the selected decoder and speak it.")
    fun speakFeatureVector(vector: DoubleArray) {
        if (vector.size != codec.inputDimension) return
        lastFeatureVector = vector.copyOf()
        val decoded = codec.decodeFeatures(vector)
        appendTranscription(decoded.symbol.toString(), separator = "")
        val espeak = codec.symbolsToEspeak(decoded.symbol.toString())
        if (espeak.isNotBlank()) {
            enqueue(Job(espeak, asPhonemes = true, label = espeak))
        }
    }

    fun restoreDefaults() {
        voice = Voice.EN_US
        inputMode = InputMode.TEXT
        codecType = PhonemeCodecType.NETTALK
        speed = 175
        pitch = 50
        amplitude = 100
    }

    fun clearTranscription() {
        transcriptionBuffer.clear()
        events.transcriptionChanged.fire()
    }

    fun flush() {
        cancelRequested = true
        scope.cancel()
        speechChannel.close()
        line.flush()
        scope = newScope()
        speechChannel = Channel(capacity = Channel.RENDEZVOUS)
        cancelRequested = false
        currentlySpeaking = ""
        events.speakingChanged.fire("")
        startWorker()
    }

    private fun enqueue(job: Job) {
        if (!EspeakRuntime.ensureInitialized()) return
        runBlocking { speechChannel.send(job) }
    }

    private fun appendTranscription(value: String, separator: String) {
        if (value.isBlank()) return
        if (separator.isNotEmpty() && transcriptionBuffer.isNotEmpty()) {
            transcriptionBuffer.append(separator)
        }
        transcriptionBuffer.append(value)
        events.transcriptionChanged.fire()
    }

    private fun newScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun startWorker() {
        scope.launch {
            for (job in speechChannel) {
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
            System.err.println("SpeechSynthesizer playback failed: ${e.message}")
        }
        setSpeaking("")
    }

    private fun setSpeaking(value: String) {
        currentlySpeaking = value
        events.speakingChanged.fire(value)
    }

    override fun close() {
        scope.cancel()
        speechChannel.close()
        super.close()
    }

    fun readResolve(): Any {
        events = SpeechSynthesizerEvents()
        scope = newScope()
        speechChannel = Channel(capacity = Channel.RENDEZVOUS)
        cancelRequested = false
        currentlySpeaking = ""
        startWorker()
        return this
    }

    override val id: String = "Speech Synthesizer"

    private data class Job(val text: String, val asPhonemes: Boolean, val label: String)

    enum class InputMode(private val displayName: String) {
        TEXT("Text"),
        PHONEMES("Phonemes"),
        ARTICULATORY_FEATURES("Articulatory features");

        override fun toString(): String = displayName
    }

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
