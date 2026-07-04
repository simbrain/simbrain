package org.simbrain.network.llm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getNetworkXStream

class TeachingTransformerCanvasTest {

    private fun canvasModel() = TeachingTransformer(TeachingTransformerConfig(
        contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1
    ))

    @Test
    fun `network updates run the forward pass on the current context`() {
        val net = Network()
        val teaching = canvasModel()
        runBlocking { net.addNetworkModel(teaching) }

        net.update()
        assertEquals(0f, teaching.model.probs.tensor[0, 0], 0f, "no context, no forward")

        teaching.setContext(intArrayOf(1, 2, 3))
        net.update()
        val distribution = teaching.nextTokenDistribution()
        assertEquals(5, distribution.size)
        assertTrue(distribution.sum() > 0.99 && distribution.sum() < 1.01)
        assertTrue(teaching.scene.tile("resid0").values.any { it != 0f }, "scene published")
    }

    @Test
    fun `trained weights survive a network round trip through xstream`() {
        val net = Network()
        val teaching = canvasModel()
        teaching.label = "TT"
        teaching.tokenLabels = arrayListOf("a", "b", "c", "d", "e")
        teaching.setCorpus(IntArray(30) { it % 5 }, IntArray(12) { it % 5 })
        teaching.tileLayout = hashMapOf("resid0" to doubleArrayOf(12.0, 34.0))
        teaching.junctionLayout = hashMapOf("layers.0.attn_residual" to doubleArrayOf(56.0, 78.0))
        teaching.selectedHead = 2
        teaching.lensEnabled = false
        teaching.learningRate = 0.005
        teaching.setContext(intArrayOf(1, 2, 3, 4))
        runBlocking { net.addNetworkModel(teaching) }

        repeat(5) { teaching.model.trainStep(intArrayOf(0, 1, 2, 3, 4, 0), intArrayOf(1, 2, 3, 4, 0, 1)) }
        val trainedWq = teaching.model.params.getValue("layers.0.attn.wq").tensor.toFloatArray()
        val trainedEmbed = teaching.model.params.getValue("embed.table").tensor.toFloatArray()

        val xml = getNetworkXStream().toXML(net)
        val restored = (getNetworkXStream().fromXML(xml) as Network).getModels<TeachingTransformer>().first()

        assertEquals("TT", restored.label)
        assertEquals(6, restored.config.contextSize)
        assertEquals(3, restored.config.numHeads)
        assertEquals(listOf("a", "b", "c", "d", "e"), restored.tokenLabels)
        assertEquals(0.005, restored.learningRate)
        assertEquals(2, restored.selectedHead)
        assertEquals(false, restored.lensEnabled)
        assertArrayEquals(intArrayOf(1, 2, 3, 4), restored.contextTokens)
        assertNotNull(restored.events, "transient events must be rebuilt")

        assertArrayEquals(trainedWq, restored.model.params.getValue("layers.0.attn.wq").tensor.toFloatArray(), 0f)
        assertArrayEquals(trainedEmbed, restored.model.params.getValue("embed.table").tensor.toFloatArray(), 0f)

        assertEquals(12.0, restored.scene.tile("resid0").x, 0.0, "saved tile layout applied to the rebuilt scene")
        val rejoin = restored.scene.opVertices.first { it.op.name == "layers.0.attn_residual" }
        assertEquals(56.0, rejoin.x, 0.0, "saved junction layout applied to the rebuilt scene")
        assertEquals(78.0, rejoin.y, 0.0)
        assertEquals(24, restored.trainer.trainingWindows.size, "corpus windows rebuilt from the restored corpus")
        assertEquals(6, restored.trainer.testingWindows.size)
        assertEquals(0.005f, restored.trainer.learningRate)

        restored.forwardContext()
        val original = teaching.also { it.forwardContext() }.nextTokenDistribution()
        assertArrayEquals(original, restored.nextTokenDistribution(), 1e-6,
            "the restored model must compute exactly what the saved one did")
    }

    @Test
    fun `micro-step driver walks a training window and returns to idle`() {
        val teaching = canvasModel()
        teaching.setCorpus(IntArray(20) { it % 5 })
        var steps = 0
        assertNotNull(teaching.stepTrainingOp())
        steps++
        while (teaching.model.stepPhase != TeachingTransformerModel.StepPhase.IDLE) {
            teaching.stepTrainingOp()
            steps++
        }
        assertEquals(2 * teaching.model.plan.ops.size, steps, "forward plus backward, one op each")
        assertTrue(teaching.trainer.trainingWindows.isNotEmpty())
    }
}
