package org.simbrain.world.speechsynthesizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * Exercises the JNA bridge into `libespeak-ng`. Skipped if the dev build hasn't been run
 * yet (`./gradlew buildEspeakNg`), since CI on a fresh checkout won't have the lib.
 *
 * Tests share `EspeakRuntime`'s process-wide state (loaded library, last-applied
 * voice/rate/pitch/volume cache). Each test sets the params it cares about; the
 * [MethodOrderer.MethodName] annotation just keeps execution order deterministic for
 * easier debugging if a test does start to depend on order.
 */
@TestMethodOrder(MethodOrderer.MethodName::class)
class EspeakRuntimeTest {

    @Test
    fun `library loads and reports a positive sample rate`() {
        val available = EspeakRuntime.ensureInitialized()
        assumeTrue(available, "eSpeak-ng not built locally — run `./gradlew buildEspeakNg`")
        assertTrue(EspeakRuntime.sampleRate > 0)
    }

    @Test
    fun `synthesizing a phoneme string emits PCM samples via the callback`() {
        assumeTrue(EspeakRuntime.ensureInitialized(), "eSpeak-ng not built")
        EspeakRuntime.setVoice("en-us")
        EspeakRuntime.setRate(175)
        EspeakRuntime.setPitch(50)
        EspeakRuntime.setVolume(100)

        var totalSamples = 0
        var endMarkerSeen = false
        val ok = EspeakRuntime.synth(
            "[[h@l'oU]]",
            EspeakNgLibrary.ESPEAK_CHARS_UTF8 or EspeakNgLibrary.ESPEAK_PHONEMES
        ) { samples, count ->
            if (samples == null || count == 0) {
                endMarkerSeen = true
            } else {
                totalSamples += count
            }
            0
        }

        assertTrue(ok, "espeak_Synth should return EE_OK")
        assertTrue(totalSamples > 0, "Expected PCM samples, got $totalSamples")
        assertEquals(true, endMarkerSeen, "Expected a final end-marker callback")
    }

    @Test
    fun `cancellation aborts synthesis early`() {
        assumeTrue(EspeakRuntime.ensureInitialized(), "eSpeak-ng not built")
        EspeakRuntime.setVoice("en-us")
        var callbackCount = 0
        EspeakRuntime.synth("[[h@l'oU w3:ld f@`I]]") { _, _ ->
            callbackCount++
            1  // abort immediately
        }
        // Either we got exactly one callback (the first chunk) before abort, or zero
        // callbacks if synthesis decided not to start. Either way, we shouldn't see many.
        assertTrue(callbackCount <= 2, "Expected early abort, saw $callbackCount callbacks")
    }

    /** Sanity-check that rate changes produce different sample counts for the same input. */
    @Test
    fun `rate parameter changes synthesis duration`() {
        assumeTrue(EspeakRuntime.ensureInitialized(), "eSpeak-ng not built")
        EspeakRuntime.setVoice("en-us")
        val text = "hello world"

        fun synthesizeAndCount(rate: Int): Int {
            EspeakRuntime.setRate(rate)
            var samples = 0
            EspeakRuntime.synth(text) { wav, n ->
                if (wav != null) samples += n
                0
            }
            return samples
        }

        val slow = synthesizeAndCount(80)
        val fast = synthesizeAndCount(450)
        assertTrue(slow > fast, "Slow ($slow samples) should be longer than fast ($fast samples)")
    }
}
