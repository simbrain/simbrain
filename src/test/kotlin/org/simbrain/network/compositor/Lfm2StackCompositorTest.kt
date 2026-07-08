package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.Lfm2Config
import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.tensor.FloatTensor
import java.util.Random

class Lfm2StackCompositorTest {

    private fun tinyConfig() = Lfm2Config(
        hiddenSize = 16,
        numLayers = 4,
        attentionLayers = setOf(2),
        numHeads = 4,
        numKvHeads = 2,
        headDim = 4,
        intermediateSize = 24,
        vocabSize = 32,
        maxSeqLen = 16,
    )

    private fun syntheticModel(config: Lfm2Config = tinyConfig()): Lfm2Model {
        val random = Random(42L)
        fun t(rows: Int, cols: Int) = FloatTensor(rows, cols).apply {
            for (i in 0 until size) data.put(i, (random.nextFloat() - 0.5f) * 0.4f)
        }
        val params = HashMap<String, FloatTensor>()
        params["model.embed_tokens.weight"] = t(config.vocabSize, config.hiddenSize)
        params["model.embedding_norm.weight"] = t(1, config.hiddenSize)
        for (i in 0 until config.numLayers) {
            val p = "model.layers.$i"
            params["$p.operator_norm.weight"] = t(1, config.hiddenSize)
            params["$p.ffn_norm.weight"] = t(1, config.hiddenSize)
            params["$p.feed_forward.w1.weight"] = t(config.intermediateSize, config.hiddenSize)
            params["$p.feed_forward.w3.weight"] = t(config.intermediateSize, config.hiddenSize)
            params["$p.feed_forward.w2.weight"] = t(config.hiddenSize, config.intermediateSize)
            if (i in config.attentionLayers) {
                params["$p.self_attn.q_proj.weight"] = t(config.numHeads * config.headDim, config.hiddenSize)
                params["$p.self_attn.k_proj.weight"] = t(config.kvDim, config.hiddenSize)
                params["$p.self_attn.v_proj.weight"] = t(config.kvDim, config.hiddenSize)
                params["$p.self_attn.out_proj.weight"] = t(config.hiddenSize, config.numHeads * config.headDim)
                params["$p.self_attn.q_layernorm.weight"] = t(1, config.headDim)
                params["$p.self_attn.k_layernorm.weight"] = t(1, config.headDim)
            } else {
                params["$p.conv.in_proj.weight"] = t(3 * config.hiddenSize, config.hiddenSize)
                params["$p.conv.out_proj.weight"] = t(config.hiddenSize, config.hiddenSize)
                params["$p.conv.conv.weight"] = t(config.hiddenSize, config.convKernel)
            }
        }
        return Lfm2Model(config, params)
    }

    private fun FlowEndpoint.key(scene: CompositorScene) = when (this) {
        is TensorTile -> id
        is OpVertex -> scene.graph!!.alias(op.name)
    }

    @Test
    fun `both mixer limbs converge on one merged residual junction`() {
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(), displaySeq = 8)

        val junctions = scene.opVertices.map { scene.graph!!.alias(it.op.name) }
        assertEquals(junctions.toSet().size, junctions.size, "no duplicate canonical junctions")
        assertTrue("block.mixer_residual" in junctions)
        assertTrue("block.conv.b_gate" in junctions)
        assertTrue("block.attn.scores" in junctions)
        assertTrue("block.mlp.silu_gate" in junctions)

        val edges = scene.edges.map { it.from.key(scene) to it.to.key(scene) }.toSet()
        assertTrue(("block.in" to "block.mixer_residual") in edges, "the residual bypass")
        assertTrue(("block.conv.out" to "block.mixer_residual") in edges, "the conv limb rejoins")
        assertTrue(("block.attn.out" to "block.mixer_residual") in edges, "the attention limb rejoins")
        assertTrue(("block.in" to "block.conv.bcx") in edges, "the shared norm feeds the conv limb")
        assertTrue(("block.attn.k_cache" to "block.attn.scores") in edges)
    }

    @Test
    fun `every anatomy tile stacks its layer subset`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config), displaySeq = 8)

        assertEquals((0 until config.numLayers).toList(), (scene.tile("block.resid") as LayerStacked).stackLayers)
        assertEquals(listOf(0, 1, 3), (scene.tile("block.conv.bx") as LayerStacked).stackLayers)
        assertEquals(listOf(2), (scene.tile("block.attn.weights") as LayerStacked).stackLayers)
        assertEquals(listOf(0, 1, 3), (scene.tile("block.w.conv.in_proj.weight") as LayerStacked).stackLayers)
        assertEquals((0 until config.numLayers).toList(), (scene.tile("block.mlp.gate") as LayerStacked).stackLayers)
    }

    @Test
    fun `flipping the layer flips stacked tiles together and dims the unused limb`() {
        val model = syntheticModel()
        val scene = Lfm2StackCompositor.buildScene(model, displaySeq = 8)
        repeat(4) { model.forwardToken(it + 1); scene.publish(it) }

        scene.layerSelector!!.invoke(2)
        assertEquals(2, scene.selectedLayer)
        assertEquals(2, (scene.tile("block.resid") as LayerStacked).shownLayer)
        assertEquals(2, (scene.tile("block.attn.q") as LayerStacked).shownLayer)
        assertTrue(scene.tile("block.conv.bx").dimmed, "conv limb dims on an attention layer")
        assertTrue(scene.tile("block.w.conv.conv.weight").dimmed)
        assertFalse(scene.tile("block.attn.k_cache").dimmed)
        assertFalse(scene.tile("block.mlp.gate").dimmed, "the shared SwiGLU limb never dims")
        assertTrue(
            scene.opVertices.first { scene.graph!!.alias(it.op.name) == "block.conv.b_gate" }.dimmed
        )
        assertFalse(
            scene.opVertices.first { scene.graph!!.alias(it.op.name) == "block.mixer_residual" }.dimmed
        )
        assertTrue(scene.edges.first { it.to.key(scene) == "block.conv.bcx" }.dimmed)
        assertFalse(scene.edges.first {
            it.from.key(scene) == "block.in" && it.to.key(scene) == "block.mixer_residual"
        }.dimmed, "the residual bypass stays live")

        scene.layerSelector!!.invoke(3)
        assertEquals(3, (scene.tile("block.conv.bx") as LayerStacked).shownLayer)
        assertFalse(scene.tile("block.conv.bx").dimmed)
        assertTrue(scene.tile("block.attn.k_cache").dimmed, "the attention limb dims on a conv layer")
        assertEquals(2, (scene.tile("block.attn.weights") as LayerStacked).shownLayer, "keeps its last layer")
    }

    @Test
    fun `stacked history tiles retain every layer's rows through a flip`() {
        val model = syntheticModel()
        val scene = Lfm2StackCompositor.buildScene(model, displaySeq = 8)
        repeat(3) { model.forwardToken(it + 1); scene.publish(it) }

        val resid = scene.tile("block.resid") as VectorHistoryTile
        scene.layerSelector!!.invoke(1)
        val layer1Row = (0 until resid.cols).map { resid.valueAt(2, it) }
        scene.layerSelector!!.invoke(3)
        val layer3Row = (0 until resid.cols).map { resid.valueAt(2, it) }
        assertTrue(layer1Row.any { it != 0f }, "history was retained for layer 1")
        assertTrue(layer3Row.any { it != 0f }, "history was retained for layer 3")
        assertFalse(layer1Row == layer3Row, "the flip really changed the displayed layer")
    }

    @Test
    fun `the depth strip selects layers and highlights the block's span`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config), displaySeq = 8)

        val strip = scene.tiles.filter { scene.layerOfTile!!.invoke(it) != null }
        assertEquals(config.numLayers + 1, strip.size, "embed plus one row per layer")
        assertEquals(0, scene.layerOfTile!!.invoke(scene.tile("embed")))
        assertEquals(2, scene.layerOfTile!!.invoke(scene.tile("layers.2.resid")))

        scene.layerSelector!!.invoke(2)
        assertEquals(
            setOf(scene.tile("layers.1.resid"), scene.tile("layers.2.resid")),
            scene.highlightedTiles,
            "the strip highlights the block's input and output checkpoints"
        )
        assertEquals(config.numLayers + 1, scene.lens?.sources?.size)

        val blockLeft = scene.tiles.filter { it.id.startsWith("block.") }.minOf { it.x }
        assertTrue(strip.all { it.x + it.width < blockLeft }, "the strip sits left of the block")
    }

    @Test
    fun `the live row cursor tracks the current token and the cache write frontier`() {
        val model = syntheticModel()
        val scene = Lfm2StackCompositor.buildScene(model, displaySeq = 8)

        val q = scene.tile("block.attn.q")
        val kCache = scene.tile("block.attn.k_cache")
        val attention = scene.tile("block.attn.weights")
        val kernel = scene.tile("block.w.conv.conv.weight")
        assertEquals(-1, q.liveRow, "no cursor before any token runs")

        repeat(3) { model.forwardToken(it + 1); scene.publish(it) }
        assertEquals(2, q.liveRow, "history tiles mark the current token's row")
        assertEquals(2, attention.liveRow)
        assertEquals(2, kCache.liveRow, "the cache cursor sits at the write frontier")
        assertEquals(-1, kernel.liveRow, "weights have no token axis")

        scene.reset()
        assertEquals(-1, q.liveRow)
        assertEquals(-1, kCache.liveRow)
    }

    @Test
    fun `flipping the attention deck flips the kv cache decks to the serving group`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config), displaySeq = 8)

        val attention = scene.tile("block.attn.weights") as AttentionTile
        val kCache = scene.tile("block.attn.k_cache") as DeckTile
        val vCache = scene.tile("block.attn.v_cache") as DeckTile
        val qPerKv = config.numHeads / config.numKvHeads

        attention.selectedHead = 3
        scene.onHeadSelected?.invoke(attention, 3)
        assertEquals(3 / qPerKv, kCache.selectedSlice)
        assertEquals(3 / qPerKv, vCache.selectedSlice)
        assertTrue(kCache.sliceLabel!!.invoke(kCache.selectedSlice).contains("serves q"))
        assertTrue(scene.emphasizedEdges.isNotEmpty())
    }
}
