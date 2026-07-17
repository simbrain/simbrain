package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.workspace.Workspace
import org.simbrain.world.textworld.TextWorldComponent
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

class LanguageModelTest {

    private fun weightsDirectory(): Path? {
        val hub = Path.of(
            System.getProperty("user.home"), ".cache", "huggingface", "hub",
            "models--LiquidAI--LFM2.5-230M", "snapshots"
        )
        if (!hub.exists()) return null
        return hub.listDirectoryEntries().firstOrNull { Lfm2Weights.isValidWeightsDirectory(it) }
    }

    @Test
    fun `one update generates one token and stops at the token budget`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.prompt = "The capital of France is"
        languageModel.tokensToGenerate = 3
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()
        assertTrue(languageModel.isLoaded)

        languageModel.startGeneration()
        assertTrue(languageModel.isGenerating)
        val promptTokens = languageModel.loaded!!.tokenizer.encode(languageModel.prompt).size

        var steps = 0
        while (languageModel.isGenerating) {
            languageModel.step()
            steps++
            assertEquals(steps, languageModel.loaded!!.model.position, "one step is one forward pass")
        }

        assertEquals(promptTokens + 2, steps, "prompt feeding plus continuation sampled from the last prompt token on")
        assertTrue(languageModel.text.startsWith(languageModel.prompt))
        assertTrue(languageModel.text.contains("Paris"), "greedy continuation should name Paris, got: ${languageModel.text}")
        val embedTile = languageModel.loaded!!.scene.tile("embed")
        assertTrue((0 until embedTile.cols).any { embedTile.valueAt(0, it) != 0f }, "scene received published rows")
    }

    @Test
    fun `loading arms generation and network iterations drive it one token per update`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val net = Network()
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.loadWeights()
        runBlocking { net.addNetworkModel(languageModel) }

        assertTrue(languageModel.isGenerating, "a loaded model is armed without an explicit start")
        assertEquals(0, languageModel.tokensToGenerate, "no token cap by default")

        net.update()
        net.update()
        assertEquals(2, languageModel.loaded!!.model.position)

        languageModel.stopGeneration()
        net.update()
        assertEquals(2, languageModel.loaded!!.model.position, "a stopped model ignores network updates")
    }

    @Test
    fun `generation stops at the end-of-text token instead of feeding it back`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.prompt = "The capital of France is"
        languageModel.tokensToGenerate = 40
        languageModel.loadWeights()

        languageModel.startGeneration()
        while (languageModel.isGenerating) {
            languageModel.step()
        }

        assertTrue(languageModel.text.contains("Paris"), "got: ${languageModel.text}")
        assertFalse(languageModel.text.contains("<|"), "special tokens must not be appended: ${languageModel.text}")
        assertTrue(languageModel.loaded!!.model.position < 64, "the run should stop at EOS well before the cache fills")
    }

    @Test
    fun `steps are no-ops while stopped and resume continues the run`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.loadWeights()

        languageModel.step()
        languageModel.stopGeneration()
        val positionAtStop = languageModel.loaded!!.model.position
        assertEquals(1, positionAtStop)
        languageModel.step()
        assertEquals(positionAtStop, languageModel.loaded!!.model.position)
        assertFalse(languageModel.isGenerating)

        languageModel.resumeGeneration()
        assertTrue(languageModel.isGenerating)
        languageModel.step()
        assertEquals(positionAtStop + 1, languageModel.loaded!!.model.position)
    }

    @Test
    fun `chat mode answers the prompt and stops at the end of the assistant turn`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 128)
        languageModel.prompt = "What is the capital of France?"
        languageModel.promptMode = PromptMode.CHAT
        languageModel.loadWeights()

        while (languageModel.isGenerating) {
            languageModel.step()
        }

        assertTrue(languageModel.text.contains("Paris"), "got: ${languageModel.text}")
        assertFalse(languageModel.text.contains("<|"), "chat scaffolding must not leak into text")
        assertTrue(languageModel.loaded!!.model.position < 128,
            "the run stops at im_end before the window fills")
    }

    @Test
    fun `coupling attributes are safe while weights are not loaded`() {
        val languageModel = LanguageModel("/no/such/dir", maxSeqLen = 64)
        assertEquals("", languageModel.generatedToken)
        assertEquals(0, languageModel.hiddenState.size)
        languageModel.injectText("hello")
        assertEquals("", languageModel.text)
    }

    @Test
    fun `generated tokens flow through a workspace coupling and prefill produces none`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val textWorldComponent = TextWorldComponent("text")
        workspace.addWorkspaceComponent(textWorldComponent)

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.prompt = "The capital of France is"
        languageModel.stopAtEndOfText = false
        runBlocking { network.addNetworkModel(languageModel) }
        languageModel.loadWeights()

        with(workspace.couplingManager) {
            createCoupling(
                languageModel.getProducer("getGeneratedToken"),
                textWorldComponent.world.getConsumer("addTextAtEnd"),
            )
        }

        val promptTokens = languageModel.loaded!!.tokenizer.encode(languageModel.prompt).size
        repeat(promptTokens - 1) { workspace.simpleIterate() }
        assertEquals("", languageModel.generatedToken, "prefill produces no token")
        assertTrue(textWorldComponent.world.text.isEmpty(), "empty productions must not accumulate")

        repeat(3) { workspace.simpleIterate() }
        assertTrue(languageModel.generatedToken.isNotEmpty())
        assertTrue(languageModel.text.endsWith(languageModel.generatedToken))
        assertTrue(textWorldComponent.world.text.isNotBlank(),
            "generated tokens arrive in the text world (couplings run before components, so it lags one token)")
    }

    @Test
    fun `hidden state produces the selected layer's residual once tokens flow`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.selectedLayer = 4
        languageModel.loadWeights()

        languageModel.step()
        val state = languageModel.hiddenState
        assertEquals(1024, state.size)
        assertTrue(state.any { it != 0.0 })
        assertTrue(languageModel.hiddenStateDescription().contains("layer 4"))
    }

    @Test
    fun `injected text extends prefill before the model resumes its own continuation`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.prompt = "The capital of France is"
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()

        val promptTokens = languageModel.loaded!!.tokenizer.encode(languageModel.prompt).size
        repeat(promptTokens + 2) { languageModel.step() }
        languageModel.stopGeneration()

        val injected = " The capital of Germany is"
        val injectedIds = languageModel.loaded!!.tokenizer.encode(injected, addSpecials = false).size
        languageModel.injectText(injected)
        assertFalse(languageModel.isGenerating, "injection does not arm a run")
        assertTrue(languageModel.text.endsWith(injected))

        languageModel.resumeGeneration()
        val positionBefore = languageModel.loaded!!.model.position
        repeat(injectedIds) {
            languageModel.step()
            assertEquals("", languageModel.generatedToken,
                "walking the unfed sample and injected tokens is prefill")
        }
        languageModel.step()
        assertTrue(languageModel.generatedToken.isNotEmpty(),
            "the last injected token's prediction resumes generation")
        assertEquals(positionBefore + injectedIds + 1, languageModel.loaded!!.model.position,
            "the pre-injection sample is fed first, then the injected tokens")
    }

    @Test
    fun `network round trip preserves configuration and view state without weights`() {
        val net = Network()
        val languageModel = LanguageModel("/no/such/dir", maxSeqLen = 128)
        languageModel.label = "LM"
        languageModel.prompt = "Hello"
        languageModel.promptMode = PromptMode.CHAT
        languageModel.systemPrompt = "Be brief."
        languageModel.tokensToGenerate = 7
        languageModel.temperature = 0.7
        languageModel.selectedLayer = 4
        languageModel.selectedHead = 3
        languageModel.lensEnabled = false
        languageModel.tileLayout = hashMapOf("embed" to doubleArrayOf(11.0, 22.0))
        runBlocking { net.addNetworkModel(languageModel) }

        val xml = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xml) as Network
        val restored = fromXml.getModels<LanguageModel>().first()

        assertEquals("/no/such/dir", restored.weightsDirectory)
        assertEquals(128, restored.maxSeqLen)
        assertEquals("Hello", restored.prompt)
        assertEquals(PromptMode.CHAT, restored.promptMode)
        assertEquals("Be brief.", restored.systemPrompt)
        assertEquals(7, restored.tokensToGenerate)
        assertEquals(0.7, restored.temperature)
        assertEquals(4, restored.selectedLayer)
        assertEquals(3, restored.selectedHead)
        assertFalse(restored.lensEnabled)
        assertEquals(11.0, restored.tileLayout?.get("embed")?.get(0))
        assertFalse(restored.isLoaded, "weights are never serialized")
        assertFalse(restored.isGenerating)
        assertNotNull(restored.events, "transient events must be rebuilt")
    }

    @Test
    fun `loading a round-tripped model applies its saved view state to the scene`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val net = Network()
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        runBlocking { net.addNetworkModel(languageModel) }
        languageModel.selectedHead = 5
        languageModel.tileLayout = hashMapOf("embed" to doubleArrayOf(123.0, 456.0))

        val xml = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xml) as Network
        val restored = fromXml.getModels<LanguageModel>().first()
        restored.loadWeights()

        val scene = restored.loaded!!.scene
        assertEquals(123.0, scene.tile("embed").x)
        assertEquals(456.0, scene.tile("embed").y)
    }
}
