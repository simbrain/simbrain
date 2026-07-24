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

class ContextWindowSyncTest {

    private fun weightsDirectory(): Path? = Lfm2Weights.findWeightsDirectory()

    private class Rig(dir: Path, sealAtEndOfText: Boolean = false) {
        val promptText = "The capital of France is"
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
            languageModel.initialText = promptText
            languageModel.stopAtEndOfText = sealAtEndOfText
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
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val rig = Rig(dir!!)
        val promptTokens = rig.languageModel.loaded!!.tokenizer.encode(rig.promptText).size
        val iterations = promptTokens + 3
        repeat(iterations) { rig.workspace.simpleIterate() }

        assertEquals(iterations, rig.languageModel.loaded!!.model.position,
            "echoes must never read as edits (a rebuild would reset the position)")
        assertTrue(rig.world.text.startsWith("<|startoftext|>"),
            "the window is honest: scaffolding included, got: ${rig.world.text}")
        assertTrue(rig.world.text.contains(rig.promptText))
    }

    @Test
    fun `a sealed run syncs its final window then goes quiet`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val rig = Rig(dir!!, sealAtEndOfText = true)
        var guard = 0
        while (rig.languageModel.canAdvance && guard++ < 80) rig.workspace.simpleIterate()
        assertTrue(rig.languageModel.isSealed, "the run ends at its own end-of-text")

        repeat(2) { rig.workspace.simpleIterate() }
        val synced = rig.world.text
        assertTrue(synced.contains(rig.languageModel.text),
            "the final window, last tokens included, reaches the document")
        assertTrue(synced.endsWith("<|im_end|>"), "the seal is visible in the document")

        repeat(2) { rig.workspace.simpleIterate() }
        assertEquals(synced, rig.world.text, "a quiet producer leaves the document alone")
    }

    @Test
    fun `a finished run pauses the workspace after the end marker reaches the document`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val rig = Rig(dir!!, sealAtEndOfText = true)
        rig.workspace.run()
        var waited = 0
        while (waited++ < 3000 && !(rig.languageModel.isSealed && !rig.workspace.updater.isRunning)) {
            Thread.sleep(10)
        }
        assertTrue(rig.languageModel.isSealed, "the run seals on its own")
        assertFalse(rig.workspace.updater.isRunning, "the workspace pauses itself when the run ends")
        assertTrue(rig.world.text.endsWith("<|im_end|>"),
            "the end marker reached the document before the pause, got: ${rig.world.text}")
    }

    @Test
    fun `an edited document rebuilds the context and replays it through prefill`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val rig = Rig(dir!!)
        val promptTokens = rig.languageModel.loaded!!.tokenizer.encode(rig.promptText).size
        repeat(promptTokens + 2) { rig.workspace.simpleIterate() }

        val edited = "<|startoftext|>The capital of Germany is"
        assertNotEquals(rig.world.text, edited)
        rig.world.text = edited
        rig.workspace.simpleIterate()

        assertEquals(1, rig.languageModel.loaded!!.model.position,
            "an edit resets the model and the same iteration's step starts the replay")
        assertTrue(rig.languageModel.text.contains("Germany"))
        assertFalse(rig.languageModel.text.contains("<|"), "the clean text strips scaffolding")

        val windowTokens = rig.languageModel.loaded!!.tokenizer
            .encode(edited, addSpecials = false).size
        repeat(windowTokens - 1) { rig.workspace.simpleIterate() }
        assertEquals(windowTokens, rig.languageModel.loaded!!.model.position,
            "replay walks the edited window one token per iteration")

        repeat(6) { rig.workspace.simpleIterate() }
        assertTrue(rig.languageModel.text.contains("Berlin"),
            "the model continues from the edited context, got: ${rig.languageModel.text}")
    }

    @Test
    fun `the synced document adopts the model's tokenizer and boxes its real tokens`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

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
    fun `an edit that drops the BOS marker gets it restored and generation continues`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val rig = Rig(dir!!)
        repeat(3) { rig.workspace.simpleIterate() }

        rig.world.text = "The capital of Germany is"
        rig.workspace.simpleIterate()

        val windowTokens = rig.languageModel.loaded!!.tokenizer
            .encode("The capital of Germany is").size
        repeat(windowTokens + 6) { rig.workspace.simpleIterate() }
        assertTrue(rig.world.text.startsWith("<|startoftext|>"),
            "the restored marker republishes to the document, got: ${rig.world.text}")
        assertTrue(rig.languageModel.text.contains("Berlin"),
            "the model continues from the marker-less edit, got: ${rig.languageModel.text}")
    }

    @Test
    fun `an edit while generating rebuilds immediately and keeps generating`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val rig = Rig(dir!!)
        repeat(3) { rig.workspace.simpleIterate() }

        rig.world.text = "<|startoftext|>Count: one two three"
        rig.workspace.simpleIterate()

        assertEquals(1, rig.languageModel.loaded!!.model.position,
            "the rebuild lands before the same iteration's step")
        assertTrue(rig.languageModel.canAdvance, "an advancing model keeps advancing through an edit")
        assertTrue(rig.world.text.startsWith("<|startoftext|>Count:"),
            "the document snaps to the rebuilt window, got: ${rig.world.text}")
    }
}
