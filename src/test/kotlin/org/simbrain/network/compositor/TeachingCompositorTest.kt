package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.TeachingTransformerConfig
import org.simbrain.network.llm.TeachingTransformerModel
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.LayerNormOp
import org.simbrain.network.tensor.op.MatMulLinearOp
import kotlin.math.abs

class TeachingCompositorTest {

    private fun model() = TeachingTransformerModel(TeachingTransformerConfig(
        contextSize = 5, embedDim = 8, numHeads = 2, hiddenDim = 10, vocabSize = 7, numLayers = 1
    ))

    private fun FlowEndpoint.key() = when (this) {
        is TensorTile -> id
        is OpVertex -> op.name
    }

    @Test
    fun `spine checkpoints chain through junction adds and limb streams converge on their ops`() {
        val scene = TeachingCompositor.buildScene(model())
        val edges = scene.edges.associateBy { it.from.key() to it.to.key() }
        val junctions = scene.opVertices.associateBy { it.op.name }

        assertTrue(junctions.getValue("layers.0.attn_residual").op is AddOp,
            "the residual rejoin is a junction vertex")
        assertTrue(("resid0" to "layers.0.attn_residual") in edges, "the skip arm arrows into the ⊕")
        assertTrue(("layers.0.attn.out" to "layers.0.attn_residual") in edges, "the limb return arrows into the ⊕")
        assertTrue(("layers.0.attn_residual" to "layers.0.attn_resid") in edges, "the ⊕ writes the checkpoint")

        assertTrue("add_pos" in junctions, "embedding + positions join at the + itself")
        assertTrue(("embed.table" to "add_pos") in edges)
        assertTrue(("embed.pos" to "add_pos") in edges)
        assertTrue(("add_pos" to "resid0") in edges)

        val intoQ = edges.getValue("resid0" to "layers.0.attn.q")
        assertTrue(intoQ.ops.any { it is LayerNormOp }, "limb entry crosses the pre-norm")
        assertTrue(intoQ.ops.any { it is MatMulLinearOp })

        assertTrue(("layers.0.attn.q" to "layers.0.attn.score") in edges, "q arrows into the scores ×")
        assertTrue(("layers.0.attn.k" to "layers.0.attn.score") in edges, "k arrows into the scores ×")
        assertTrue(("layers.0.attn.score" to "layers.0.attn.weights") in edges)
        assertTrue(("layers.0.attn.weights" to "layers.0.attn.mix") in edges)
        assertTrue(("layers.0.attn.v" to "layers.0.attn.mix") in edges)
        assertTrue(("layers.0.attn.mix" to "layers.0.attn.out") in edges)
        assertTrue(("layers.0.resid" to "logits") in edges)
        assertTrue(("logits" to "probs") in edges)
    }

    @Test
    fun `head-stacked segments carry per-head strands and merge collapses the fan`() {
        val scene = TeachingCompositor.buildScene(model())
        val edges = scene.edges.associateBy { it.from.key() to it.to.key() }
        assertEquals(2, edges.getValue("layers.0.attn.q" to "layers.0.attn.score").strands,
            "after the split the flow is one strand per head")
        assertEquals(2, edges.getValue("layers.0.attn.score" to "layers.0.attn.weights").strands)
        assertEquals(2, edges.getValue("layers.0.attn.weights" to "layers.0.attn.mix").strands)
        assertEquals(2, edges.getValue("layers.0.attn.v" to "layers.0.attn.mix").strands)
        assertEquals(1, edges.getValue("layers.0.attn.mix" to "layers.0.attn.out").strands,
            "the merge bead collapses the strands")
        assertEquals(1, edges.getValue("resid0" to "layers.0.attn.q").strands)
    }

    @Test
    fun `weight and bias tiles ride the edges carrying their consuming ops`() {
        val scene = TeachingCompositor.buildScene(model())
        val satellites = scene.satellites.associateBy { it.tile.id }

        val wq = satellites.getValue("layers.0.attn.wq")
        assertEquals("resid0" to "layers.0.attn.q", wq.edge.from.key() to wq.edge.to.key(),
            "Wq rides the limb-entry edge with its projection op")
        assertTrue(wq.op is MatMulLinearOp)

        val b1 = satellites.getValue("layers.0.mlp.b1")
        assertEquals("layers.0.mlp.act", b1.edge.to.key(), "the bias strip rides the edge into the hidden tile")

        assertTrue("unembed.weight" in satellites)
        assertTrue("embed.table" !in satellites, "the embedding consumes above the first anchor")
        val edgeEndpoints = scene.edges.map { it.from.key() to it.to.key() }
        assertTrue(("embed.table" to "add_pos") in edgeEndpoints, "standalone parameters arrow into their junction")
        assertTrue(scene.edges.none { it.from.key() == "layers.0.attn.wq" || it.to.key() == "layers.0.attn.wq" },
            "satellite parameters are not edge anchors")
    }

    @Test
    fun `satellite op glyphs center above their parameter tiles`() {
        val scene = TeachingCompositor.buildScene(model())
        val node = CompositorNode(scene)

        for (satellite in scene.satellites) {
            val glyph = node.glyphFor(satellite.op) ?: continue
            assertEquals(satellite.tile.x + satellite.tile.width / 2, glyph.xOffset, 1e-9)
            assertTrue(glyph.yOffset < satellite.tile.y,
                "${satellite.tile.title}'s operation glyph sits above its parameter tile")
        }
    }

    @Test
    fun `forward pass fills spine deck and probability tiles through full-pass publish`() {
        val model = model()
        val scene = TeachingCompositor.buildScene(model)
        model.setSample(intArrayOf(1, 2, 3, 4, 0))
        model.forward()
        scene.publish()

        val spine = scene.tile("resid0")
        assertTrue((0 until spine.cols).any { spine.valueAt(0, it) != 0f }, "spine received the residual")

        val deck = scene.tile("layers.0.attn.weights") as DeckTile
        for (row in 0 until deck.rows) {
            var sum = 0f
            for (col in 0..row) sum += deck.valueAt(row, col)
            assertTrue(abs(sum - 1f) < 1e-5f, "head 0 row $row attention sums to $sum")
            for (col in row + 1 until deck.cols) {
                assertEquals(0f, deck.valueAt(row, col), 0f, "masked cell ($row,$col)")
            }
        }
        val head0 = deck.valueAt(1, 0)
        deck.selectedSlice = 1
        assertTrue(abs(deck.valueAt(1, 0) + deck.valueAt(1, 1) - 1f) < 1e-5f, "head 1 rows normalized too")
        deck.selectedSlice = 0
        assertEquals(head0, deck.valueAt(1, 0), 0f, "flipping back restores head 0 from the cube")

        val probsTile = scene.tile("probs")
        var probSum = 0f
        for (col in 0 until probsTile.cols) probSum += probsTile.valueAt(0, col)
        assertTrue(abs(probSum - 1f) < 1e-5f, "probability rows sum to 1")
    }

    @Test
    fun `lens reads the selected position through the model's own head`() {
        val model = model()
        val scene = TeachingCompositor.buildScene(model)
        model.setSample(intArrayOf(1, 2, 3))
        model.forward()
        val lens = scene.lens!!
        lens.sourceRow = 2
        scene.publish()

        val lastReading = lens.readings.last()
        val distribution = model.distributionAt(2)
        val expectedToken = distribution.indices.maxBy { distribution[it] }
        assertEquals(expectedToken, lastReading.tokenId,
            "the last checkpoint's lens is the model's own prediction")
        assertEquals(distribution[expectedToken], lastReading.prob, 1e-4f)
    }

    @Test
    fun `stale tiles shrink as a stepped pass advances and clear at the boundary`() {
        val model = model()
        val scene = TeachingCompositor.buildScene(model)
        assertTrue(scene.staleTiles(0).isEmpty(), "nothing is stale at a step boundary")

        model.beginSteppedTrainStep(intArrayOf(1, 2, 3, 4, 0), intArrayOf(2, 3, 4, 0, 1))
        model.stepOp()
        val afterEmbed = scene.staleTiles(model.plan.cursor)
        assertTrue(scene.tile("resid0") in afterEmbed, "resid0 is written by the second op")
        assertTrue(scene.tile("probs") in afterEmbed)
        assertTrue(scene.tile("embed.table") !in afterEmbed, "parameter tiles are never stale")

        var lastSize = afterEmbed.size
        while (model.stepPhase == TeachingTransformerModel.StepPhase.FORWARD) {
            model.stepOp()
            val stale = scene.staleTiles(model.plan.cursor)
            assertTrue(stale.size <= lastSize, "stale set only shrinks during the pass")
            lastSize = stale.size
        }
        assertTrue(scene.staleTiles(model.plan.cursor).isEmpty())
        while (model.stepPhase != TeachingTransformerModel.StepPhase.IDLE) model.stepOp()
    }

    @Test
    fun `gradient view swaps tiles to gradient buffers and back`() {
        val model = model()
        val scene = TeachingCompositor.buildScene(model)
        model.beginSteppedTrainStep(intArrayOf(1, 2, 3, 4, 0), intArrayOf(2, 3, 4, 0, 1))
        while (model.stepPhase != TeachingTransformerModel.StepPhase.IDLE) model.stepOp()

        scene.publish()
        val wq = scene.tile("layers.0.attn.wq") as MatrixTile
        val forwardValues = wq.values.copyOf()

        scene.setGradientView(true)
        scene.publish()
        val gradients = model.grads.of(model.params.getValue("layers.0.attn.wq").tensor)
        assertEquals(gradients.data.get(0), wq.valueAt(0, 0), 0f, "gradient view reads the gradient buffer")
        assertTrue((0 until wq.values.size).any { wq.values[it] != forwardValues[it] })

        scene.setGradientView(false)
        scene.publish()
        assertTrue(forwardValues.contentEquals(wq.values), "leaving gradient view restores forward values")
    }

    @Test
    fun `weight tiles refresh when a training step bumps parameter versions`() {
        val model = model()
        val scene = TeachingCompositor.buildScene(model)
        model.setSample(intArrayOf(1, 2, 3, 4, 0))
        model.forward()
        scene.publish()
        val wq = scene.tile("layers.0.attn.wq")
        val before = wq.values.copyOf()

        model.trainStep(intArrayOf(1, 2, 3, 4, 0), intArrayOf(2, 3, 4, 0, 1))
        scene.publish()
        assertTrue(!before.contentEquals(wq.values), "Adam's version bump must republish the weight tile")
    }
}
