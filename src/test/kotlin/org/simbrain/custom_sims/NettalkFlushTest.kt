/**
 * Tests the NETtalk word-boundary flush wiring: the synthesizer's feature buffer must flush at
 * non-letter characters and at the end of the text (so a single word with no trailing punctuation
 * still speaks once per reading cycle), and editing the text must discard any partial word.
 */
package org.simbrain.custom_sims

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.nettalk.NettalkReader
import org.simbrain.custom_sims.simulations.nettalk.wireNetTalk
import org.simbrain.network.NetworkComponent
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.nettalk.NettalkEncoder
import org.simbrain.workspace.Workspace
import org.simbrain.world.speechsynthesizer.SpeechSynthesizer
import org.simbrain.world.speechsynthesizer.SpeechSynthesizerComponent

class NettalkFlushTest {

    private class Fixture(val workspace: Workspace, val reader: NettalkReader, val synthesizer: SpeechSynthesizer)

    private suspend fun wiredWorkspace(text: String): Fixture {
        val scope = SimulationScope()
        val workspace = scope.workspace
        val networkComponent = NetworkComponent("Network")
        workspace.addWorkspaceComponent(networkComponent)
        val encoder = NettalkEncoder(7)
        val bp = BackpropNetwork(intArrayOf(encoder.inputSize, 80, encoder.outputSize))
        networkComponent.network.addNetworkModels(bp)
        bp.initBiases()
        bp.initWeights()
        val synthesizer = SpeechSynthesizer()
        workspace.addWorkspaceComponent(SpeechSynthesizerComponent("Speech Synthesizer", synthesizer))
        val reader = NettalkReader()
        with(scope) { wireNetTalk(workspace, reader) }
        reader.text = text
        return Fixture(workspace, reader, synthesizer)
    }

    private suspend fun bufferLengthsPerIteration(fixture: Fixture, iterations: Int): List<Int> =
        (1..iterations).map {
            fixture.workspace.iterateSuspend(1)
            fixture.synthesizer.featureBufferLength
        }

    @Test
    fun `single word with no trailing space flushes at end of text`() {
        runBlocking {
            val fixture = wiredWorkspace("hello")
            val lengths = bufferLengthsPerIteration(fixture, 10)
            assertEquals(listOf(1, 2, 3, 4, 0, 1, 2, 3, 4, 0), lengths)
        }
    }

    @Test
    fun `word boundary inside the text flushes the buffer`() {
        runBlocking {
            val fixture = wiredWorkspace("hello ")
            val lengths = bufferLengthsPerIteration(fixture, 12)
            assertEquals(listOf(1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0), lengths)
        }
    }

    @Test
    fun `editing the reader text clears buffered phonemes`() {
        runBlocking {
            val fixture = wiredWorkspace("hello")
            fixture.workspace.iterateSuspend(3)
            assertEquals(3, fixture.synthesizer.featureBufferLength)
            fixture.reader.text = "world"
            val deadline = System.currentTimeMillis() + 5000
            while (fixture.synthesizer.featureBufferLength != 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10)
            }
            assertTrue(fixture.synthesizer.featureBufferLength == 0) {
                "buffer should clear after a text edit, was ${fixture.synthesizer.featureBufferLength}"
            }
        }
    }
}
