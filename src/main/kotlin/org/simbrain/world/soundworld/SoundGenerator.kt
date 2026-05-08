package org.simbrain.world.soundworld

import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

abstract class SoundGenerator: AttributeContainer, EditableObject, AutoCloseable {

    open val sampleRate: Float get() = 44100f

    protected open val format: AudioFormat get() = AudioFormat(sampleRate, 16, 1, true, false)

    /**
     * Lazily-opened native audio line. Marked transient so XStream doesn't try to traverse
     * into JDK-internal `DirectAudioDevice` types (which fail under Java 17 modular access
     * rules). Re-opens automatically on first access after deserialization.
     */
    @Transient
    private var _line: SourceDataLine? = null

    protected val line: SourceDataLine
        get() = _line ?: openLine().also { _line = it }

    private fun openLine(): SourceDataLine {
        val info = DataLine.Info(SourceDataLine::class.java, format)
        return (AudioSystem.getLine(info) as SourceDataLine).also {
            it.open(format)
            it.start()
        }
    }

    override fun close() {
        _line?.let {
            try { it.drain() } catch (_: Exception) {}
            try { it.stop() } catch (_: Exception) {}
            try { it.close() } catch (_: Exception) {}
        }
        _line = null
    }

}