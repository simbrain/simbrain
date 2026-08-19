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

class TinyLanguageModelGenerationTest {

    private fun model() = TinyLanguageModel(TinyLmConfig(
        contextSize = 6, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1,
    )).apply {
        tokenLabels = arrayListOf("the", "cat", "sat", "on", "mat", "dog")
    }

    @Test
    fun `injected text walks in one word per step then the model feeds back its own samples`() {
        val languageModel = model()
        languageModel.injectText("the cat sat")
        assertTrue(languageModel.canAdvance)

        languageModel.step()
        assertEquals(1, languageModel.contextTokens.size)
        assertEquals("", languageModel.generatedToken, "walking the injected text is prefill")
        languageModel.step()
        assertEquals("", languageModel.generatedToken)
        languageModel.step()
        assertEquals(3, languageModel.contextTokens.size)
        assertTrue(languageModel.generatedToken.isNotEmpty(),
            "the last injected word's prediction is accepted")
        assertTrue(languageModel.text.startsWith("the cat sat"))
        assertEquals(4, languageModel.text.split(" ").size)

        languageModel.step()
        assertEquals(4, languageModel.contextTokens.size, "the accepted word slides into the context")
        assertEquals(5, languageModel.text.split(" ").size)
        assertEquals(8, languageModel.hiddenState.size)
    }

    @Test
    fun `the context slides at capacity while the text keeps full history`() {
        val languageModel = model()
        languageModel.injectText("the cat sat")
        repeat(12) { languageModel.step() }
        assertEquals(6, languageModel.contextTokens.size, "context is capped at contextSize")
        assertEquals(13, languageModel.text.split(" ").size, "text keeps the whole run")
    }

    @Test
    fun `words outside the vocabulary are dropped by the encoder`() {
        val languageModel = model()
        assertEquals(2, languageModel.encode("the ZORP cat").size)
        assertEquals("the cat", languageModel.decode(languageModel.encode("the ZORP cat")))
    }

    @Test
    fun `injected text enters the context before sampling resumes`() {
        val languageModel = model()
        languageModel.injectText("the cat")
        repeat(3) { languageModel.step() }

        languageModel.injectText("on the mat")
        assertTrue(languageModel.text.endsWith("on the mat"))

        repeat(3) {
            languageModel.step()
            assertEquals("", languageModel.generatedToken,
                "walking the unfed sample and injected words is prefill")
        }
        languageModel.step()
        assertTrue(languageModel.generatedToken.isNotEmpty(),
            "the last injected word's prediction resumes generation")
    }

    @Test
    fun `an empty context waits for input instead of writing`() {
        val languageModel = TinyLanguageModel(TinyLmConfig(
            contextSize = 6, embedDim = 8, numHeads = 2, hiddenDim = 8, vocabSize = 6, numLayers = 1,
        ))
        repeat(3) { languageModel.step() }
        assertFalse(languageModel.canAdvance, "nothing to walk and nothing to continue")
        assertTrue(languageModel.waitingForInput)
        assertEquals("", languageModel.generatedToken)
        assertEquals(0, languageModel.hiddenState.size)
    }

    @Test
    fun `context arriving through the document starts generation by itself`() {
        val languageModel = model()
        assertTrue(languageModel.waitingForInput)

        languageModel.contextWindow = "the cat"
        assertFalse(languageModel.waitingForInput)
        languageModel.step()
        assertTrue(languageModel.generatedToken.isNotEmpty(),
            "the delivered context generates on the next step")
        assertTrue(languageModel.text.startsWith("the cat"))
    }

    @Test
    fun `a typed prompt and play generate through the couplings with no arming`() {
        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val textWorldComponent = TextWorldComponent("text")
        workspace.addWorkspaceComponent(textWorldComponent)
        val world = textWorldComponent.world

        val languageModel = model()
        runBlocking { network.addNetworkModel(languageModel) }
        with(workspace.couplingManager) {
            createCoupling(
                world.getProducer("getText"),
                languageModel.getConsumer("setContextWindow"),
            )
            createCoupling(
                languageModel.getProducer("getContextWindow"),
                world.getConsumer("setTextIfChanged"),
            )
        }

        repeat(2) { workspace.simpleIterate() }
        assertTrue(languageModel.waitingForInput, "an empty document leaves the model idling")

        world.text = "the cat sat"
        repeat(4) { workspace.simpleIterate() }
        assertTrue(languageModel.text.split(" ").size > 3,
            "the typed prompt generates with no arming step, got: ${languageModel.text}")
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

        val languageModel = model()
        runBlocking { network.addNetworkModel(languageModel) }
        with(workspace.couplingManager) {
            createCoupling(
                world.getProducer("getText"),
                languageModel.getConsumer("setContextWindow"),
            )
            createCoupling(
                languageModel.getProducer("getContextWindow"),
                world.getConsumer("setTextIfChanged"),
            )
        }
        languageModel.injectText("the cat sat")

        repeat(5) { workspace.simpleIterate() }
        assertEquals(5, languageModel.contextTokens.size,
            "echoes must never read as edits (an edit would replace the context)")
        assertTrue(world.text.startsWith("the cat sat"))

        world.text = "the mat"
        workspace.simpleIterate()
        assertTrue(languageModel.text.startsWith("the mat"),
            "an edit replaces the context outright, got: ${languageModel.text}")
        assertTrue(world.text.startsWith("the mat"),
            "the producer publishes the rebuilt window, not the old one")

        repeat(3) { workspace.simpleIterate() }
        assertTrue(languageModel.text.split(" ").size > 2,
            "generation continues from the edited context")
    }

    @Test
    fun `clearing a coupled document clears the model context`() {
        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val textWorldComponent = TextWorldComponent("text")
        workspace.addWorkspaceComponent(textWorldComponent)
        val world = textWorldComponent.world

        val languageModel = model()
        runBlocking { network.addNetworkModel(languageModel) }
        with(workspace.couplingManager) {
            createCoupling(
                world.getProducer("getText"),
                languageModel.getConsumer("setContextWindow"),
            )
            createCoupling(
                languageModel.getProducer("getContextWindow"),
                world.getConsumer("setTextIfChanged"),
            )
        }

        world.text = "the cat sat"
        repeat(3) { workspace.simpleIterate() }
        assertTrue(languageModel.contextTokens.isNotEmpty())

        world.text = ""
        workspace.simpleIterate()

        assertTrue(languageModel.contextTokens.isEmpty())
        assertTrue(languageModel.waitingForInput)
        assertEquals("", world.text)
    }
}
