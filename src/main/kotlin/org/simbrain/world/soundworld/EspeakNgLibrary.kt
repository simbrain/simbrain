package org.simbrain.world.soundworld

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference

/**
 * Direct JNA binding to the eSpeak-ng C API. Only the surface used by [PhonemeSynthesizer]
 * is mapped — voice/parameter setup, synchronous synthesis with a sample-delivery callback,
 * and cancellation. See `speak_lib.h` in the eSpeak-ng source for the canonical reference.
 */
internal interface EspeakNgLibrary : Library {

    fun espeak_Initialize(output: Int, buflength: Int, path: String?, options: Int): Int

    fun espeak_SetVoiceByName(name: String): Int

    fun espeak_SetParameter(parameter: Int, value: Int, relative: Int): Int

    fun espeak_SetSynthCallback(callback: SynthCallback?)

    fun espeak_Synth(
        text: ByteArray,
        size: NativeLong,
        position: Int,
        positionType: Int,
        endPosition: Int,
        flags: Int,
        uniqueIdentifier: IntByReference?,
        userData: Pointer?
    ): Int

    fun espeak_Cancel(): Int

    fun espeak_Terminate(): Int

    interface SynthCallback : Callback {
        /**
         * Called as PCM samples are produced.
         *  - `wav` points to `numsamples` signed 16-bit little-endian samples, or null at end.
         *  - `events` is an array of synthesis events; we ignore it.
         * Return 0 to continue, non-zero to abort the in-progress synthesis.
         */
        fun invoke(wav: Pointer?, numsamples: Int, events: Pointer?): Int
    }

    companion object {
        const val AUDIO_OUTPUT_SYNCHRONOUS = 2

        const val ESPEAK_CHARS_UTF8 = 1

        const val ESPEAK_PHONEMES = 0x100

        const val POS_CHARACTER = 1

        const val PARAM_RATE = 1
        const val PARAM_VOLUME = 2
        const val PARAM_PITCH = 3

        const val EE_OK = 0
    }
}
