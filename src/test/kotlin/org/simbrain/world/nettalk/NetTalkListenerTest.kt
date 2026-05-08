package org.simbrain.world.nettalk

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.util.getSimbrainXStream

class NetTalkListenerTest {

    @Test
    fun `synth event listener is wired after deserialization`() = runBlocking {
        val original = NetTalk()
        val xml = getSimbrainXStream().toXML(original)
        val restored = getSimbrainXStream().fromXML(xml) as NetTalk
        // The listener wired in init/readResolve should propagate synth speakingChanged
        // events into NetTalk.notifyAudioStarted, which then fires audioSegmentChanged.
        var fired = false
        restored.events.audioSegmentChanged.on { fired = true }
        restored.synthesizer.events.speakingChanged.fire("test").await()
        // Give the event dispatcher a moment to deliver.
        kotlinx.coroutines.delay(100)
        assertEquals(true, fired, "NetTalk's wired listener should propagate synth events after deserialization")
    }
}
