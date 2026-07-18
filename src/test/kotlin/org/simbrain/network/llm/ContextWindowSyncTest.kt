package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.util.HuggingFaceFileTokenizer
import org.simbrain.workspace.Workspace
import org.simbrain.world.textworld.TextWorld
import org.simbrain.world.textworld.TextWorldComponent
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

class ContextWindowSyncTest {

    private fun weightsDirectory(): Path? {
        val hub = Path.of(
            System.getProperty("user.home"), ".cache", "huggingface", "hub",
            "models--LiquidAI--LFM2.5-230M", "snapshots"
        )
        if (!hub.exists()) return null
        return hub.listDirectoryEntries().firstOrNull { Lfm2Weights.isValidWeightsDirectory(it) }
    }

    private class Rig(dir: Path) {
        val workspace = Workspace()
        val world: TextWorld
        val languageModel: LanguageModel

        init {
            val network = Network()
            workspace.addWorkspaceComponent(NetworkComponent("net", network))
            val textWorldComponent = TextWorldComponent("text")
            workspace.addWorkspaceComponent(textWorldComponent)
            world = textWorldComponent.world
            languageModel = LanguageModel(dir.toString(), maxSeqLen = 64)
            languageModel.prompt = "The capital of France is"
            languageModel.stopAtEndOfText = false
            runBlocking { network.addNetworkModel(languageModel) }
            languageModel.loadWeights()
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
        }
    }

    @Test
    fun `two-way document sync reaches a fixpoint without spurious rebuilds`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val rig = Rig(dir!!)
        val promptTokens = rig.languageModel.loaded!!.tokenizer.encode(rig.languageModel.prompt).size
        val iterations = promptTokens + 3
        repeat(iterations) { rig.workspace.simpleIterate() }

        assertEquals(iterations, rig.languageModel.loaded!!.model.position,
            "echoes must never read as edits (a rebuild would reset the position)")
        assertTrue(rig.world.text.startsWith("<|startoftext|>"),
            "the window is honest: scaffolding included, got: ${rig.world.text}")
        assertTrue(rig.world.text.contains(rig.languageModel.prompt))
    }

    @Test
    fun `a stopped run syncs its final window then goes quiet`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val rig = Rig(dir!!)
        val promptTokens = rig.languageModel.loaded!!.tokenizer.encode(rig.languageModel.prompt).size
        repeat(promptTokens + 2) { rig.workspace.simpleIterate() }
        rig.languageModel.stopGeneration()

        repeat(2) { rig.workspace.simpleIterate() }
        val synced = rig.world.text
        assertTrue(synced.contains(rig.languageModel.text),
            "the final window, last tokens included, reaches the document")

        repeat(2) { rig.workspace.simpleIterate() }
        assertEquals(synced, rig.world.text, "a quiet producer leaves the document alone")
    }

    @Test
    fun `an edited document rebuilds the context and replays it through prefill`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val rig = Rig(dir!!)
        val promptTokens = rig.languageModel.loaded!!.tokenizer.encode(rig.languageModel.prompt).size
        repeat(promptTokens + 2) { rig.workspace.simpleIterate() }
        rig.languageModel.stopGeneration()
        repeat(2) { rig.workspace.simpleIterate() }

        val edited = "<|startoftext|>The capital of Germany is"
        assertNotEquals(rig.world.text, edited)
        rig.world.text = edited
        rig.workspace.simpleIterate()

        assertEquals(0, rig.languageModel.loaded!!.model.position, "an edit resets the model for replay")
        assertFalse(rig.languageModel.isGenerating, "a stopped model stays stopped after an edit")
        assertEquals(edited, rig.world.text, "the quiet producer must not clobber the edit")
        assertTrue(rig.languageModel.text.contains("Germany"))
        assertFalse(rig.languageModel.text.contains("<|"), "the clean text strips scaffolding")

        rig.languageModel.resumeGeneration()
        val windowTokens = rig.languageModel.loaded!!.tokenizer
            .encode(edited, addSpecials = false).size
        repeat(windowTokens) { rig.workspace.simpleIterate() }
        assertEquals(windowTokens, rig.languageModel.loaded!!.model.position,
            "replay walks the edited window one token per iteration")

        repeat(6) { rig.workspace.simpleIterate() }
        assertTrue(rig.languageModel.text.contains("Berlin"),
            "the model continues from the edited context, got: ${rig.languageModel.text}")
    }

    @Test
    fun `the synced document adopts the model's tokenizer and boxes its real tokens`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val rig = Rig(dir!!)
        var waited = 0
        while (rig.world.displayTokenizer == null && waited++ < 100) {
            Thread.sleep(10)
        }
        assertTrue(rig.world.displayTokenizer != null,
            "creating the document coupling hands the model's tokenizer over")
        assertTrue(rig.world.displayTokenizer is HuggingFaceFileTokenizer)

        repeat(3) { rig.workspace.simpleIterate() }
        assertEquals("<|startoftext|>", rig.world.tokens.first().token,
            "token boxes follow the model's real BPE boundaries, scaffolding included")
    }

    @Test
    fun `an edit while generating rebuilds immediately and keeps generating`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")

        val rig = Rig(dir!!)
        repeat(3) { rig.workspace.simpleIterate() }

        rig.world.text = "<|startoftext|>Count: one two three"
        rig.workspace.simpleIterate()

        assertEquals(1, rig.languageModel.loaded!!.model.position,
            "the rebuild lands before the same iteration's step")
        assertTrue(rig.languageModel.isGenerating, "a generating model keeps generating through an edit")
        assertTrue(rig.world.text.startsWith("<|startoftext|>Count:"),
            "the document snaps to the rebuilt window, got: ${rig.world.text}")
    }
}
