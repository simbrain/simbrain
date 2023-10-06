package org.simbrain.world.soundworld

import org.simbrain.util.UserParameter
import org.simbrain.workspace.Consumable
import kotlin.math.sin


class Beeper: SoundGenerator() {

    @UserParameter(label = "Duration")
    var beepDuration = 0.01f

    @UserParameter(label = "Max Duration")
    var maxDuration = 0.1f

    private val numSamples get() = (sampleRate * beepDuration).toInt()

    @UserParameter(label = "Frequency")
    var frequency = 400.0

    @UserParameter(label = "Volume")
    var volume = 0.5f


    private fun generateBeep(): ByteArray {
        val sample = ByteArray(numSamples * 2)  // 2 bytes for each sample

        // Generate a sine wave with consistent frequency and adjust amplitude for volume
        for (i in 0 until numSamples) {
            val angle = (2.0 * Math.PI * i.toDouble() * frequency / sampleRate).toFloat()
            val amplitude = (Short.MAX_VALUE * volume * sin(angle.toDouble())).toInt().toShort()
            sample[i * 2] = amplitude.toByte()         // low byte
            sample[i * 2 + 1] = (amplitude.toInt() ushr 8).toByte()  // high byte
        }

        return sample
    }

    private fun playBeep() {
        val sample = generateBeep()
        line.write(sample, 0, sample.size)
    }

    @Consumable
    fun playBeepAtVolume(volume: Double) {
        this.volume = volume.toFloat()
        playBeep()
    }

    @Consumable
    fun playBeepAtFrequency(frequency: Double) {
        this.frequency = frequency
        playBeep()
    }

    @Consumable
    fun playBeepForDuration(duration: Double) {
        this.beepDuration = duration.toFloat()
        playBeep()
    }


    override val id: String = "Beeper"
}

