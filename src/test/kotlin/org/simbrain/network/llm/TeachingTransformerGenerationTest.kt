package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.workspace.Workspace
import org.simbrain.world.textworld.TextWorldComponent

class TeachingTransformerGenerationTest {

    private fun model() = TeachingTransformer(TeachingTransformerConfig(
        contextSize = 6, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1,
    )).apply {
        tokenLabels = arrayListOf("the", "cat", "sat", "on", "mat", "dog")
    }

    @Test
    fun `seeding from the prompt walks it then feeds back the model's own samples`() {
        val transformer = model()
        transformer.prompt = "the cat sat"
        transformer.seedFromPrompt()
        assertTrue(transformer.canAdvance)

        transformer.step()
        assertEquals(1, transformer.contextTokens.size)
        assertEquals("", transformer.generatedToken, "walking the seed is prefill")
        transformer.step()
        assertEquals("", transformer.generatedToken)
        transformer.step()
        assertEquals(3, transformer.contextTokens.size)
        assertTrue(transformer.generatedToken.isNotEmpty(),
            "the last seed word's prediction is accepted")
        assertTrue(transformer.text.startsWith("the cat sat"))
        assertEquals(4, transformer.text.split(" ").size)

        transformer.step()
        assertEquals(4, transformer.contextTokens.size, "the accepted word slides into the context")
        assertEquals(5, transformer.text.split(" ").size)
        assertEquals(8, transformer.hiddenState.size)
    }

    @Test
    fun `the context slides at capacity while the text keeps full history`() {
        val transformer = model()
        transformer.prompt = "the cat sat"
        transformer.seedFromPrompt()
        repeat(12) { transformer.step() }
        assertEquals(6, transformer.contextTokens.size, "context is capped at contextSize")
        assertEquals(13, transformer.text.split(" ").size, "text keeps the whole run")
    }

    @Test
    fun `words outside the vocabulary are dropped by the encoder`() {
        val transformer = model()
        assertEquals(2, transformer.encode("the ZORP cat").size)
        assertEquals("the cat", transformer.decode(transformer.encode("the ZORP cat")))
    }

    @Test
    fun `injected text enters the context before sampling resumes`() {
        val transformer = model()
        transformer.prompt = "the cat"
        transformer.seedFromPrompt()
        repeat(3) { transformer.step() }

        transformer.injectText("on the mat")
        assertTrue(transformer.text.endsWith("on the mat"))

        repeat(3) {
            transformer.step()
            assertEquals("", transformer.generatedToken,
                "walking the unfed sample and injected words is prefill")
        }
        transformer.step()
        assertTrue(transformer.generatedToken.isNotEmpty(),
            "the last injected word's prediction resumes generation")
    }

    @Test
    fun `an empty context waits for input instead of writing`() {
        val transformer = TeachingTransformer(TeachingTransformerConfig(
            contextSize = 6, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1,
        ))
        transformer.prompt = "anything"
        transformer.seedFromPrompt()
        repeat(3) { transformer.step() }
        assertFalse(transformer.canAdvance, "nothing to walk and nothing to continue")
        assertTrue(transformer.waitingForInput)
        assertEquals("", transformer.generatedToken)
        assertEquals(0, transformer.hiddenState.size)
    }

    @Test
    fun `context arriving through the document starts generation by itself`() {
        val transformer = model()
        assertTrue(transformer.waitingForInput)

        transformer.contextWindow = "the cat"
        assertFalse(transformer.waitingForInput)
        transformer.step()
        assertTrue(transformer.generatedToken.isNotEmpty(),
            "the delivered context generates on the next step")
        assertTrue(transformer.text.startsWith("the cat"))
    }

    @Test
    fun `a typed prompt and play generate through the couplings with no arming`() {
        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val textWorldComponent = TextWorldComponent("text")
        workspace.addWorkspaceComponent(textWorldComponent)
        val world = textWorldComponent.world

        val transformer = model()
        runBlocking { network.addNetworkModel(transformer) }
        with(workspace.couplingManager) {
            createCoupling(
                world.getProducer("getText"),
                transformer.getConsumer("setContextWindow"),
            )
            createCoupling(
                transformer.getProducer("getContextWindow"),
                world.getConsumer("setTextIfChanged"),
            )
        }

        repeat(2) { workspace.simpleIterate() }
        assertTrue(transformer.waitingForInput, "an empty document leaves the model idling")

        world.text = "the cat sat"
        repeat(4) { workspace.simpleIterate() }
        assertTrue(transformer.text.split(" ").size > 3,
            "the typed prompt generates with no arming step, got: ${transformer.text}")
        assertTrue(world.text.startsWith("the cat sat"))
    }

    @Test
    fun `the sliding window syncs with a text world and edits replace the context`() {
        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val textWorldComponent = TextWorldComponent("text")
        workspace.addWorkspaceComponent(textWorldComponent)
        val world = textWorldComponent.world

        val transformer = model()
        transformer.prompt = "the cat sat"
        runBlocking { network.addNetworkModel(transformer) }
        with(workspace.couplingManager) {
            createCoupling(
                world.getProducer("getText"),
                transformer.getConsumer("setContextWindow"),
            )
            createCoupling(
                transformer.getProducer("getContextWindow"),
                world.getConsumer("setTextIfChanged"),
            )
        }
        transformer.seedFromPrompt()

        repeat(5) { workspace.simpleIterate() }
        assertEquals(5, transformer.contextTokens.size,
            "echoes must never read as edits (an edit would replace the context)")
        assertTrue(world.text.startsWith("the cat sat"))

        world.text = "the mat"
        workspace.simpleIterate()
        assertTrue(transformer.text.startsWith("the mat"),
            "an edit replaces the context outright, got: ${transformer.text}")
        assertTrue(world.text.startsWith("the mat"),
            "the producer publishes the rebuilt window, not the old one")

        repeat(3) { workspace.simpleIterate() }
        assertTrue(transformer.text.split(" ").size > 2,
            "generation continues from the edited context")
    }
}
