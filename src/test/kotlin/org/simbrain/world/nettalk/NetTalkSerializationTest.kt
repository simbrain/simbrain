package org.simbrain.world.nettalk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.simbrain.util.getSimbrainXStream
import org.simbrain.world.soundworld.PhonemeSynthesizer

class NetTalkSerializationTest {

    @Test
    fun `NetTalk round-trips through XStream`() {
        val original = NetTalk().apply {
            text = "hello world"
            position = 4
            audioMode = NetTalk.AudioMode.PER_LETTER
            synthesizer.voice = PhonemeSynthesizer.Voice.EN_GB
            synthesizer.speed = 200
        }
        val xml = getSimbrainXStream().toXML(original)
        val restored = getSimbrainXStream().fromXML(xml) as NetTalk
        assertEquals("hello world", restored.text)
        assertEquals(4, restored.position)
        assertEquals(NetTalk.AudioMode.PER_LETTER, restored.audioMode)
        assertEquals(PhonemeSynthesizer.Voice.EN_GB, restored.synthesizer.voice)
        assertEquals(200, restored.synthesizer.speed)
        assertNotNull(restored.synthesizer)
    }

    @Test
    fun `NetTalk round-trips after triggering an audio call`() {
        val original = NetTalk()
        // Touch the synthesizer's `line` field by attempting playback (in headless test envs the
        // espeak subprocess + line.write may fail, but the lazy line field gets initialized).
        original.synthesizer.speakPhonemes("h@l'oU")
        // No assertion on the speak itself; we only care that the post-speak NetTalk still
        // serializes cleanly (this used to throw on `DirectAudioDevice$DirectSDL`).
        val xml = getSimbrainXStream().toXML(original)
        val restored = getSimbrainXStream().fromXML(xml) as NetTalk
        assertNotNull(restored.synthesizer)
        // After deserialization, channels/scope are recreated by the synth's constructor.
        assertEquals("", restored.synthesizer.currentlySpeaking)
    }
}
