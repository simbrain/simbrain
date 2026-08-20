package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.trainers.SamplingStrategy
import org.simbrain.workspace.Workspace
import org.simbrain.world.textworld.TextWorldComponent
import java.nio.file.Path

class LanguageModelTest {


    @Test
    fun `language model defaults match the LFM2 generation configuration`() {
        val languageModel = LanguageModel()

        assertEquals(0.1, languageModel.temperature)
        assertEquals(50, (languageModel.samplingStrategy as SamplingStrategy.TopK).k)
    }

    @Test
    fun `one update generates one token and stops at the token budget`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        val seedText = "The capital of France is"
        languageModel.initialText = seedText
        languageModel.tokensToGenerate = 3
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()
        assertTrue(languageModel.isLoaded)

        assertTrue(languageModel.canAdvance)
        val promptTokens = languageModel.loaded!!.tokenizer.encode(seedText).size

        var steps = 0
        while (languageModel.canAdvance) {
            languageModel.step()
            steps++
            assertEquals(steps, languageModel.loaded!!.model.position, "one step is one forward pass")
        }

        assertEquals(promptTokens + 2, steps, "seed feeding plus continuation sampled from the last seed token on")
        assertTrue(languageModel.text.startsWith(seedText))
        assertTrue(languageModel.text.contains("Paris"), "greedy continuation should name Paris, got: ${languageModel.text}")
        val embedTile = languageModel.loaded!!.scene.tile("embed")
        assertTrue((0 until embedTile.cols).any { embedTile.valueAt(0, it) != 0f }, "scene received published rows")
    }

    @Test
    fun `injecting into an empty window prepends BOS to the window and the feed queue`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 32)
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()
        val injected = languageModel.loaded!!.tokenizer.encode("Hello world", addSpecials = false).size
        languageModel.injectText("Hello world")

        assertTrue(languageModel.contextWindow.startsWith(Lfm2ChatFormat.BOS),
            "the committed stream regains its leading BOS marker")
        val textBefore = languageModel.text
        repeat(injected) { languageModel.step() }
        assertEquals(injected, languageModel.loaded!!.model.position)
        assertEquals(textBefore, languageModel.text,
            "with BOS prepended, one queued token still remains, so nothing sampled yet")
        languageModel.step()
        assertEquals(injected + 1, languageModel.loaded!!.model.position,
            "BOS fed as the first token, so the queue held one more than the injected ids")
    }

    @Test
    fun `a pure append skips rewind and feeds only the new tokens`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.initialText = "The capital of France is"
        languageModel.tokensToGenerate = 1
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()
        while (languageModel.canAdvance) languageModel.step()
        val positionBefore = languageModel.loaded!!.model.position
        val embedTile = languageModel.loaded!!.scene.tile("embed")
        assertTrue((0 until embedTile.cols).any { embedTile.valueAt(0, it) != 0f })

        languageModel.contextWindow = languageModel.contextWindow + " Paris"
        assertEquals(positionBefore, languageModel.loaded!!.model.position,
            "a pure append must not rewind or reset the caches")
        assertTrue((0 until embedTile.cols).any { embedTile.valueAt(0, it) != 0f },
            "scene rows survive the append untruncated")
        assertTrue(languageModel.canAdvance, "the appended tokens queue for feeding")
        languageModel.step()
        assertEquals(positionBefore + 1, languageModel.loaded!!.model.position)
    }

    @Test
    fun `head selection round-trips between the pager, the serialized state, and the kv caches`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 32)
        languageModel.selectedHead = 5
        languageModel.loadWeights()

        val scene = languageModel.loaded!!.scene
        val config = languageModel.loaded!!.model.config
        val qPerKv = config.numHeads / config.numKvHeads
        val attention = scene.tile("block.attn.weights") as org.simbrain.network.compositor.AttentionTile
        val kCache = scene.tile("block.attn.k_cache") as org.simbrain.network.compositor.DeckTile
        assertEquals(5, attention.selectedHead, "the restored head lands on the attention tile")
        assertEquals(5 / qPerKv, kCache.selectedSlice, "the restored head places the kv caches on its group")

        attention.selectedHead = 9
        scene.onHeadSelected!!.invoke(attention, 9)
        assertEquals(9, languageModel.selectedHead, "a pager flip lands in the serialized view state")
        assertEquals(9 / qPerKv, kCache.selectedSlice, "the pager flip still moves the kv caches")
    }

    @Test
    fun `loading seeds the window and network iterations drive it one token per update`() {
        val dir = assumeOrRequireWeights()

        val net = Network()
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.initialText = "The capital of France is"
        languageModel.loadWeights()
        runBlocking { net.addNetworkModel(languageModel) }

        assertTrue(languageModel.canAdvance, "the one-shot initial text readies a fresh model")
        assertEquals(0, languageModel.tokensToGenerate, "no token cap by default")

        net.update()
        net.update()
        assertEquals(2, languageModel.loaded!!.model.position)
    }

    @Test
    fun `generation stops at the end-of-text token instead of feeding it back`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.initialText = "The capital of France is"
        languageModel.tokensToGenerate = 40
        languageModel.loadWeights()

        while (languageModel.canAdvance) {
            languageModel.step()
        }

        assertTrue(languageModel.isSealed, "the emitted end-of-text seals the stream")
        assertTrue(languageModel.text.contains("Paris"), "got: ${languageModel.text}")
        assertFalse(languageModel.text.contains("<|"), "special tokens must not be appended: ${languageModel.text}")
        assertTrue(languageModel.loaded!!.model.position < 64, "the run should stop at EOS well before the cache fills")
    }

    @Test
    fun `steps are no-ops once the stream seals and an edit moves it again`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.initialText = "The capital of France is"
        languageModel.loadWeights()

        var guard = 0
        while (languageModel.canAdvance && guard++ < 64) languageModel.step()
        assertTrue(languageModel.isSealed)
        val positionAtSeal = languageModel.loaded!!.model.position
        languageModel.step()
        assertEquals(positionAtSeal, languageModel.loaded!!.model.position,
            "a sealed stream ignores steps")

        languageModel.contextWindow = "The capital of Germany is"
        assertFalse(languageModel.isSealed)
        languageModel.step()
        assertEquals(1, languageModel.loaded!!.model.position, "the edit resets and replays")
    }

    @Test
    fun `chat mode answers the prompt and stops at the end of the assistant turn`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 128)
        languageModel.promptMode = PromptMode.CHAT
        languageModel.loadWeights()
        assertFalse(languageModel.canAdvance, "a chat model starts empty, waiting for a message")
        languageModel.sendUserMessage("What is the capital of France?")

        while (languageModel.canAdvance) {
            languageModel.step()
        }

        assertTrue(languageModel.text.contains("Paris"), "got: ${languageModel.text}")
        assertFalse(languageModel.text.contains("<|"), "chat scaffolding must not leak into text")
        assertTrue(languageModel.loaded!!.model.position < 128,
            "the run stops at im_end before the window fills")
    }

    @Test
    fun `a sent message reopens a sealed chat and the model answers the follow-up`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 256)
        languageModel.promptMode = PromptMode.CHAT
        languageModel.loadWeights()
        languageModel.sendUserMessage("What is the capital of France?")

        var guard = 0
        while (languageModel.canAdvance && guard++ < 256) languageModel.step()
        assertTrue(languageModel.isSealed)
        assertTrue(languageModel.text.contains("Paris"), "got: ${languageModel.text}")

        languageModel.sendUserMessage("What is the capital of Germany?")
        assertFalse(languageModel.isSealed, "the queued turn moves the stream past the end marker")
        assertTrue(languageModel.canAdvance)
        assertTrue(languageModel.contextWindow.contains(
            "<|im_start|>user\nWhat is the capital of Germany?<|im_end|>"),
            "the turn is templated into the window")

        guard = 0
        while (languageModel.canAdvance && guard++ < 256) languageModel.step()
        assertTrue(languageModel.isSealed, "the follow-up answer seals again")
        assertTrue(languageModel.text.substringAfterLast("Germany").contains("Berlin"),
            "the model answers the follow-up, got: ${languageModel.text}")
    }

    @Test
    fun `coupling attributes are safe while weights are not loaded`() {
        val languageModel = LanguageModel("/no/such/dir", maxSeqLen = 64)
        assertEquals("", languageModel.generatedToken)
        assertEquals(0, languageModel.hiddenState.size)
        assertEquals("", languageModel.contextWindow)
        languageModel.injectText("hello")
        languageModel.contextWindow = "hello"
        languageModel.sendUserMessage("hello")
        assertEquals("", languageModel.text)
    }

    @Test
    fun `generated tokens flow through a workspace coupling and prefill produces none`() {
        val dir = assumeOrRequireWeights()

        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("net", network))
        val textWorldComponent = TextWorldComponent("text")
        workspace.addWorkspaceComponent(textWorldComponent)

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        val seedText = "The capital of France is"
        languageModel.initialText = seedText
        languageModel.stopAtEndOfText = false
        runBlocking { network.addNetworkModel(languageModel) }
        languageModel.loadWeights()

        with(workspace.couplingManager) {
            createCoupling(
                languageModel.getProducer("getGeneratedToken"),
                textWorldComponent.world.getConsumer("addTextAtEnd"),
            )
        }

        val promptTokens = languageModel.loaded!!.tokenizer.encode(seedText).size
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
    fun `the current token span tracks reading during prefill and the fresh token during generation`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        val seedText = "The capital of France is"
        languageModel.initialText = seedText
        languageModel.stopAtEndOfText = false
        languageModel.tokensToGenerate = 4
        languageModel.loadWeights()

        assertEquals(0, languageModel.currentTokenSpan.size, "no span before the first step")

        val promptTokens = languageModel.loaded!!.tokenizer.encode(seedText).size
        var previousEnd = 0
        repeat(promptTokens) {
            languageModel.step()
            val span = languageModel.currentTokenSpan
            assertEquals(previousEnd, span[0], "prefill spans tile the window in reading order")
            assertTrue(span[1] > span[0])
            previousEnd = span[1]
        }
        assertEquals("<|startoftext|>$seedText".length, previousEnd,
            "the sweep covered exactly the seeded window")

        languageModel.step()
        val genSpan = languageModel.currentTokenSpan
        val window = languageModel.contextWindow
        assertEquals(window.length, genSpan[1], "generation highlights the appended token")
        assertEquals(languageModel.generatedToken, window.substring(genSpan[0], genSpan[1]))

        languageModel.contextWindow = "<|startoftext|>Numbers: one two"
        assertEquals(0, languageModel.currentTokenSpan.size, "an edit clears the span")
    }

    @Test
    fun `hidden state produces the selected layer's residual once tokens flow`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.selectedLayer = 4
        languageModel.initialText = "The capital of France is"
        languageModel.loadWeights()

        languageModel.step()
        val state = languageModel.hiddenState
        assertEquals(1024, state.size)
        assertTrue(state.any { it != 0.0 })
        assertTrue(languageModel.hiddenStateDescription().contains("layer 4"))
    }

    @Test
    fun `injected text extends prefill before the model resumes its own continuation`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        val seedText = "The capital of France is"
        languageModel.initialText = seedText
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()

        val promptTokens = languageModel.loaded!!.tokenizer.encode(seedText).size
        repeat(promptTokens + 2) { languageModel.step() }

        val injected = " The capital of Germany is"
        val injectedIds = languageModel.loaded!!.tokenizer.encode(injected, addSpecials = false).size
        languageModel.injectText(injected)
        assertTrue(languageModel.text.endsWith(injected))
        assertTrue(languageModel.canAdvance, "a fed queue is ready to walk")

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
    fun `an edit near the end of a long window rewinds to a checkpoint and continues exactly`() {
        val dir = assumeOrRequireWeights()

        val seed = "One two three four five six seven eight nine ten eleven twelve thirteen " +
            "fourteen fifteen sixteen seventeen eighteen nineteen twenty twenty-one twenty-two " +
            "twenty-three twenty-four twenty-five twenty-six twenty-seven twenty-eight twenty-nine " +
            "thirty thirty-one thirty-two thirty-three thirty-four thirty-five"
        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 128)
        languageModel.samplingStrategy = SamplingStrategy.Greedy
        languageModel.stopAtEndOfText = false
        languageModel.initialText = seed
        languageModel.loadWeights()

        val promptTokens = languageModel.loaded!!.tokenizer.encode(seed).size
        assertTrue(promptTokens > 40, "the seed must reach past the first checkpoint, got $promptTokens")
        repeat(promptTokens) { languageModel.step() }

        val editedWindow = languageModel.contextWindow.replace("thirty-five", "forty")
        languageModel.contextWindow = editedWindow
        val positionAfterEdit = languageModel.loaded!!.model.position
        assertTrue(positionAfterEdit >= 32,
            "the edit rewinds to a conv checkpoint, not to zero (position $positionAfterEdit)")
        assertTrue(positionAfterEdit < promptTokens, "the edited tail itself is requeued")

        var guard = 0
        while (languageModel.isPromptProcessing && guard++ < 256) languageModel.step()
        repeat(4) { languageModel.step() }

        val fresh = LanguageModel(dir.toString(), maxSeqLen = 128)
        fresh.samplingStrategy = SamplingStrategy.Greedy
        fresh.stopAtEndOfText = false
        fresh.loadWeights()
        fresh.seedWindow(editedWindow)
        guard = 0
        while (fresh.isPromptProcessing && guard++ < 256) fresh.step()
        repeat(4) { fresh.step() }

        assertEquals(fresh.contextWindow, languageModel.contextWindow,
            "the checkpoint-rewound continuation must match a from-scratch replay exactly")
    }

    @Test
    fun `network round trip preserves configuration and view state without weights`() {
        val net = Network()
        val languageModel = LanguageModel("/no/such/dir", maxSeqLen = 128)
        languageModel.label = "LM"
        languageModel.promptMode = PromptMode.CHAT
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
        assertEquals(PromptMode.CHAT, restored.promptMode)
        assertEquals(7, restored.tokensToGenerate)
        assertEquals(0.7, restored.temperature)
        assertEquals(4, restored.selectedLayer)
        assertEquals(3, restored.selectedHead)
        assertFalse(restored.lensEnabled)
        assertEquals(11.0, restored.tileLayout?.get("embed")?.get(0))
        assertFalse(restored.isLoaded, "weights are never serialized")
        assertFalse(restored.canAdvance, "an unloaded model cannot advance")
        assertNotNull(restored.events, "transient events must be rebuilt")
    }

    @Test
    fun `loading a round-tripped model applies its saved view state to the scene`() {
        val dir = assumeOrRequireWeights()

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

    @Test
    fun `streaming decode withholds incomplete multi-byte characters until they complete`() {
        val dir = assumeOrRequireWeights()

        val languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
        languageModel.initialText = "The capital"
        languageModel.stopAtEndOfText = false
        languageModel.loadWeights()
        val emojiIds = languageModel.loaded!!.tokenizer.encode("😀", addSpecials = false)
        assertTrue(emojiIds.size > 1, "byte-level BPE splits an emoji across tokens")

        // The step that drains the feed queue commits its own sample, so the override scripts
        // the emoji's byte tokens starting from that step.
        var scripted = 0
        languageModel.sampleOverride = { emojiIds[scripted.coerceAtMost(emojiIds.lastIndex)] }
        while (languageModel.isPromptProcessing) languageModel.step()
        scripted++
        val windowAfterFirst = languageModel.contextWindow

        assertEquals("", languageModel.generatedToken, "mid-character token emits nothing")
        assertFalse(windowAfterFirst.contains('\uFFFD'), "no replacement characters rendered")

        while (scripted < emojiIds.size - 1) {
            languageModel.step()
            scripted++
            assertEquals("", languageModel.generatedToken)
            assertEquals(windowAfterFirst, languageModel.contextWindow, "window unchanged mid-character")
            assertTrue(languageModel.currentTokenSpan.isEmpty(), "nothing rendered to highlight yet")
        }

        languageModel.step()
        assertEquals("😀", languageModel.generatedToken, "completed character emitted whole")
        assertTrue(languageModel.contextWindow.endsWith("😀"))
        assertFalse(languageModel.contextWindow.contains('\uFFFD'))
        assertTrue(languageModel.text.endsWith("😀"))
        val span = languageModel.currentTokenSpan
        assertEquals(2, span[1] - span[0], "span covers the whole surrogate pair")
        assertEquals(languageModel.contextWindow.length, span[1])
    }
}
