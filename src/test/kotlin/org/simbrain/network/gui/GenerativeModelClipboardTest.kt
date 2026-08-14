package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.PromptMode
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.network.llm.TeachingTransformerConfig

class GenerativeModelClipboardTest : NetworkPanelDeleteUndoTestBase() {

    private fun addTransformer(): TeachingTransformer {
        val transformer = TeachingTransformer(TeachingTransformerConfig(
            contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1
        ))
        transformer.model.trainStep(intArrayOf(0, 1, 2, 3, 4, 0), intArrayOf(1, 2, 3, 4, 0, 1))
        runBlocking { network.addNetworkModel(transformer) }
        return transformer
    }

    @Test
    fun `pasting a teaching transformer creates an independent trained twin and undo removes it`() = runBlocking {
        val original = addTransformer()

        Clipboard.add(listOf(original))
        Clipboard.paste(panel)

        val transformers = network.getModels<TeachingTransformer>()
        assertEquals(2, transformers.size)
        val copy = transformers.first { it !== original }
        assertNotSame(original.model, copy.model)
        assertArrayEquals(
            original.model.params.getValue("embed.table").tensor.toFloatArray(),
            copy.model.params.getValue("embed.table").tensor.toFloatArray(),
            "the pasted twin carries the trained parameters",
        )

        panel.undoManager.undo()
        assertEquals(1, network.getModels<TeachingTransformer>().size, "undo removes the pasted copy")
        panel.undoManager.redo()
        assertEquals(2, network.getModels<TeachingTransformer>().size, "redo restores it")
    }

    @Test
    fun `pasting an unloaded language model copies the recipe`() = runBlocking {
        val original = LanguageModel("", maxSeqLen = 128)
        original.promptMode = PromptMode.CHAT
        original.temperature = 0.7
        network.addNetworkModel(original)

        Clipboard.add(listOf(original))
        Clipboard.paste(panel)

        val models = network.getModels<LanguageModel>()
        assertEquals(2, models.size)
        val copy = models.first { it !== original }
        assertEquals(PromptMode.CHAT, copy.promptMode)
        assertEquals(0.7, copy.temperature)
        assertEquals(128, copy.maxSeqLen)
    }

    @Test
    fun `deleting a selected teaching transformer is undoable`() = runBlocking {
        val transformer = addTransformer()
        val trained = transformer.model.params.getValue("embed.table").tensor.toFloatArray()

        selectOnly(transformer)
        panel.deleteSelectedObjects()
        assertTrue(network.getModels<TeachingTransformer>().isEmpty())

        panel.undoManager.undo()
        val restored = network.getModels<TeachingTransformer>()
        assertEquals(1, restored.size, "undo restores the deleted model")
        assertArrayEquals(trained, restored.first().model.params.getValue("embed.table").tensor.toFloatArray(),
            "the restored model keeps its trained parameters (same instance, not a rebuild)")
    }
}
