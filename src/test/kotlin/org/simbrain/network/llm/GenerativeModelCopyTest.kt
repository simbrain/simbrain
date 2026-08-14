package org.simbrain.network.llm

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.compositor.DeckTile
import org.simbrain.network.compositor.HistoryView
import org.simbrain.network.trainers.SamplingStrategy

class GenerativeModelCopyTest {

    private fun trainedTransformer(): TeachingTransformer {
        val transformer = TeachingTransformer(TeachingTransformerConfig(
            contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1
        ))
        transformer.tokenLabels = ArrayList(listOf("a", "b", "c", "d", "e"))
        transformer.setCorpus(IntArray(40) { it % 5 }, IntArray(20) { it % 5 })
        val tokens = intArrayOf(0, 1, 2, 3, 4, 0)
        val targets = intArrayOf(1, 2, 3, 4, 0, 1)
        repeat(5) { transformer.model.trainStep(tokens, targets) }
        transformer.setContext(intArrayOf(2, 3, 4))
        transformer.learningRate = 0.005
        transformer.samplingTemperature = 0.5
        transformer.samplingStrategy = SamplingStrategy.TopK(3)
        transformer.scene.tiles.filterIsInstance<DeckTile>().first().selectedSlice = 2
        transformer.gradientView = true
        transformer.label = "Trained twin"
        return transformer
    }

    @Test
    fun `teaching transformer copy carries the trained parameters and full state`() {
        val original = trainedTransformer()
        val copy = original.copy()

        assertEquals(original.config, copy.config)
        assertEquals(original.model.params.keys, copy.model.params.keys)
        for ((name, port) in original.model.params) {
            assertArrayEquals(port.tensor.toFloatArray(), copy.model.params[name]!!.tensor.toFloatArray(),
                "trained parameter $name must copy exactly")
        }
        assertArrayEquals(original.contextTokens, copy.contextTokens)
        assertEquals(original.tokenLabels, copy.tokenLabels)
        assertArrayEquals(original.corpusTokenIds, copy.corpusTokenIds)
        assertEquals(original.trainer.trainingWindows.size, copy.trainer.trainingWindows.size,
            "the corpus must be re-applied to the copy's trainer")
        assertEquals(original.learningRate, copy.learningRate)
        assertEquals(original.samplingTemperature, copy.samplingTemperature)
        assertEquals(3, (copy.samplingStrategy as SamplingStrategy.TopK).k)
        assertNotSame(original.samplingStrategy, copy.samplingStrategy)
        assertEquals(2, copy.scene.tiles.filterIsInstance<DeckTile>().first().selectedSlice,
            "the deck's head slice rides the copy")
        assertFalse(copy.gradientView, "gradients don't ride the copy, so neither does the gradient view")
        assertFalse(copy.hasGradients)
        assertEquals(original.label, copy.label)
    }

    @Test
    fun `teaching transformer copy trains independently of the original`() {
        val original = trainedTransformer()
        val copy = original.copy()

        val before = original.model.params.getValue("embed.table").tensor.toFloatArray()
        repeat(3) {
            copy.model.trainStep(intArrayOf(0, 1, 2, 3, 4, 0), intArrayOf(1, 2, 3, 4, 0, 1))
        }
        assertArrayEquals(before, original.model.params.getValue("embed.table").tensor.toFloatArray(),
            "training the copy must not touch the original's parameters")
        assertFalse(before.contentEquals(copy.model.params.getValue("embed.table").tensor.toFloatArray()),
            "the copy's own training must move its parameters")
    }

    @Test
    fun `language model copy carries the recipe and settings without loading weights`() {
        val original = LanguageModel("/nowhere/weights", maxSeqLen = 128)
        original.label = "LFM2.5-230M"
        original.promptMode = PromptMode.CHAT
        original.tokensToGenerate = 7
        original.temperature = 0.7
        original.samplingStrategy = SamplingStrategy.TopP(0.9)
        original.stopAtEndOfText = false
        original.pauseWorkspaceAtEnd = false
        original.selectedLayer = 5
        original.selectedHead = 3
        original.lensEnabled = false
        original.historyView = HistoryView.GHOSTED
        original.hideInactiveLimb = true
        original.tileLayout = hashMapOf("embed" to doubleArrayOf(1.0, 2.0))
        original.initialText = "seed text"

        val copy = original.copy()

        assertFalse(copy.isLoaded)
        assertEquals("/nowhere/weights", copy.weightsDirectory)
        assertEquals(128, copy.maxSeqLen)
        assertEquals(original.label, copy.label)
        assertEquals(PromptMode.CHAT, copy.promptMode)
        assertEquals(7, copy.tokensToGenerate)
        assertEquals(0.7, copy.temperature)
        assertEquals(0.9, (copy.samplingStrategy as SamplingStrategy.TopP).p)
        assertNotSame(original.samplingStrategy, copy.samplingStrategy)
        assertFalse(copy.stopAtEndOfText)
        assertFalse(copy.pauseWorkspaceAtEnd)
        assertEquals(5, copy.selectedLayer)
        assertEquals(3, copy.selectedHead)
        assertFalse(copy.lensEnabled)
        assertEquals(HistoryView.GHOSTED, copy.historyView)
        assertTrue(copy.hideInactiveLimb)
        assertArrayEquals(doubleArrayOf(1.0, 2.0), copy.tileLayout!!["embed"])
        assertNotSame(original.tileLayout!!["embed"], copy.tileLayout!!["embed"])
        assertEquals("seed text", copy.initialText, "an unloaded original passes its pending seed along")
    }

    @Test
    fun `language model copy captures the committed window as its seed`() {
        val dir = Lfm2Weights.findWeightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not found in the Simbrain or HF cache")

        val original = LanguageModel(dir.toString(), maxSeqLen = 64)
        original.initialText = "The capital of France is"
        original.tokensToGenerate = 3
        original.stopAtEndOfText = false
        original.loadWeights()
        while (original.canAdvance) original.step()
        val window = original.contextWindow
        assertTrue(window.isNotBlank())

        val copy = original.copy()
        assertEquals(window, copy.initialText, "the copy's seed is the original's exact committed stream")

        // The captured stream starts with the BOS marker; reseeding with it must round-trip
        // exactly rather than doubling the marker.
        original.seedWindow(window)
        assertEquals(window, original.contextWindow, "a full-window seed must round-trip bit-exactly")
    }
}
