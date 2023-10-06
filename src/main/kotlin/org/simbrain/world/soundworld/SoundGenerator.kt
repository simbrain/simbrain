package org.simbrain.world.soundworld

import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

abstract class SoundGenerator: AttributeContainer, EditableObject, AutoCloseable {

    val sampleRate get() = 44100f

    protected val format = AudioFormat(sampleRate, 16, 1, true, false)
    private val lineInfo = DataLine.Info(SourceDataLine::class.java, format)
    protected val line = (AudioSystem.getLine(lineInfo) as SourceDataLine).also {
        it.open(format)
        it.start()
    }


    override fun close() {
        line.drain() // Ensure all data is played
        line.stop()
        line.close()
    }

}