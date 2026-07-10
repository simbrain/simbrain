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
        val scene = Lfm2StackCompositor.buildScene(syntheticModel())

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
    fun `edges arrive at the pin of the glyph stage that consumes them`() {
        val scene = Lfm2StackCompositor.buildScene(syntheticModel())
        val alias = { name: String -> scene.graph!!.alias(name) }

        // q and k enter the scores glyph at the multiply stage; the softmax stage takes no pins.
        val scores = scene.opVertices.first { alias(it.op.name) == "block.attn.scores" }
        val intoScores = scene.edges.filter { it.to === scores }
        assertEquals(setOf("block.attn.q", "block.attn.k_cache"), intoScores.map { it.toPort }.toSet())
        intoScores.forEach { assertEquals(0, stageForInput(scores.op, it.toPort!!, alias)) }

        // The projected q enters norm+rope at the norm stage, the rope angles at the rotation.
        val qRope = scene.opVertices.first { alias(it.op.name) == "block.attn.q_norm_rope" }
        val portsIn = scene.edges.filter { it.to === qRope }.map { it.toPort!! }.toSet()
        assertEquals(setOf("block.attn.q_raw", "rope.cos", "rope.sin"), portsIn)
        assertEquals(0, stageForInput(qRope.op, "block.attn.q_raw", alias))
        assertEquals(1, stageForInput(qRope.op, "rope.cos", alias))
        assertEquals(1, stageForInput(qRope.op, "rope.sin", alias))

        // The gate passes through silu before the product; up enters at the product stage.
        val silu = scene.opVertices.first { alias(it.op.name) == "block.mlp.silu_gate" }
        assertEquals(0, stageForInput(silu.op, "block.mlp.gate", alias))
        assertEquals(1, stageForInput(silu.op, "block.mlp.up", alias))

        // The B gate reads bcx twice at one pin-ambiguous port: still a single arrow, no pin.
        val bGate = scene.opVertices.first { alias(it.op.name) == "block.conv.b_gate" }
        val intoBGate = scene.edges.filter { it.to === bGate }
        assertEquals(1, intoBGate.size)
        assertEquals("block.conv.bcx", intoBGate.single().toPort)
    }

    @Test
    fun `packed axes carry their substructure boundaries`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config))

        assertEquals(listOf(4, 8, 12), scene.tile("block.attn.q").columnTicks)
        assertEquals(listOf(4, 8, 12), scene.tile("block.attn.context").columnTicks)
        assertEquals(listOf(4), scene.tile("block.attn.k").columnTicks)
        assertEquals(listOf(4), scene.tile("block.attn.v").columnTicks)
        assertEquals(listOf(16, 32), scene.tile("block.conv.bcx").columnTicks)
        assertEquals(listOf("B", "C", "x"), scene.tile("block.conv.bcx").blockLabels)
        assertEquals(listOf(16, 32), scene.tile("block.w.conv.in_proj.weight").rowTicks)
    }

    @Test
    fun `strands trace head parallelism from norm-rope to the output projection`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config))
        val edges = scene.edges.associateBy { it.from.key(scene) to it.to.key(scene) }

        assertEquals(1, edges.getValue("block.in" to "block.attn.q_norm_rope").strands,
            "flat until the first head-aware op")
        assertEquals(4, edges.getValue("block.attn.q_norm_rope" to "block.attn.q").strands)
        assertEquals(4, edges.getValue("block.attn.q" to "block.attn.scores").strands)
        assertEquals(2, edges.getValue("block.attn.k_norm_rope" to "block.attn.k").strands,
            "the key side runs at the GQA head count")
        assertEquals(2, edges.getValue("block.attn.k" to "block.attn.k_cache").strands,
            "the cache write preserves head structure")
        assertEquals(2, edges.getValue("block.attn.k_cache" to "block.attn.scores").strands)
        assertEquals(4, edges.getValue("block.attn.scores" to "block.attn.weights").strands)
        assertEquals(4, edges.getValue("block.attn.context" to "block.attn.out").strands,
            "strands run under the output projection satellite")
        assertEquals(1, edges.getValue("block.attn.out" to "block.mixer_residual").strands,
            "heads exist nowhere past the output projection")
        assertEquals(1, edges.getValue("block.conv.bcx" to "block.conv.b_gate").strands)

        val scores = scene.opVertices.first { scene.graph!!.alias(it.op.name) == "block.attn.scores" }
        val kRope = scene.opVertices.first { scene.graph!!.alias(it.op.name) == "block.attn.k_norm_rope" }
        assertEquals(4, opParallelism(scores.op))
        assertEquals(2, opParallelism(kRope.op))
    }

    @Test
    fun `layer paging steps within a tile's own stack`() {
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(tinyConfig().copy(
            numLayers = 6, attentionLayers = setOf(2, 4),
        )))

        val attnTile = scene.tile("block.attn.q") as LayerStacked
        assertEquals(4, attnTile.layerAfter(2))
        assertEquals(2, attnTile.layerAfter(4), "wraps past the last attention layer")
        assertEquals(2, attnTile.layerBefore(4))
        assertEquals(4, attnTile.layerBefore(2), "wraps back to the last attention layer")
        assertEquals(4, attnTile.layerAfter(3), "steps from a layer outside the stack")

        val convTile = scene.tile("block.conv.bx") as LayerStacked
        assertEquals(3, convTile.layerAfter(2))
        assertEquals(1, convTile.layerBefore(2))

        assertEquals(6, scene.layerCount)
    }

    @Test
    fun `every anatomy tile stacks its layer subset`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config))

        assertEquals((0 until config.numLayers).toList(), (scene.tile("block.resid") as LayerStacked).stackLayers)
        assertEquals(listOf(0, 1, 3), (scene.tile("block.conv.bx") as LayerStacked).stackLayers)
        assertEquals(listOf(2), (scene.tile("block.attn.weights") as LayerStacked).stackLayers)
        assertEquals(listOf(0, 1, 3), (scene.tile("block.w.conv.in_proj.weight") as LayerStacked).stackLayers)
        assertEquals((0 until config.numLayers).toList(), (scene.tile("block.mlp.gate") as LayerStacked).stackLayers)
    }

    @Test
    fun `flipping the layer flips stacked tiles together and dims the unused limb`() {
        val model = syntheticModel()
        val scene = Lfm2StackCompositor.buildScene(model)
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
    fun `the spine backfills an unwatched layer's history from the depth strip on a flip`() {
        val model = syntheticModel()
        val scene = Lfm2StackCompositor.buildScene(model)
        repeat(3) { model.forwardToken(it + 1); scene.publish(it) }

        val blockOut = scene.tile("block.resid") as VectorHistoryTile
        scene.layerSelector!!.invoke(3)
        val strip = scene.tile("layers.3.resid")
        val row = (0 until blockOut.cols).map { blockOut.valueAt(2, it) }
        assertTrue(row.any { it != 0f }, "the flipped-to layer's checkpoint history was backfilled")
        assertEquals((0 until strip.cols).map { strip.valueAt(2, it) }, row,
            "the backfill copies the strip checkpoint exactly")
    }

    @Test
    fun `a flip to an unwatched attention layer re-derives exactly what live recording stored`() {
        val config = tinyConfig().copy(numLayers = 6, attentionLayers = setOf(2, 4))
        val model = syntheticModel(config)
        val recorded = Lfm2StackCompositor.buildScene(model)
        val derived = Lfm2StackCompositor.buildScene(model)
        recorded.layerSelector!!.invoke(4)
        repeat(5) {
            model.forwardToken(it + 1)
            recorded.publish(it)
            derived.publish(it)
        }

        derived.layerSelector!!.invoke(4)
        for (id in listOf("block.attn.weights", "block.attn.context", "block.attn.out")) {
            assertTrue(recorded.tile(id).values.any { it != 0f }, "$id recorded something to compare")
            assertTrue(recorded.tile(id).values.contentEquals(derived.tile(id).values),
                "$id must re-derive bit-exactly from q and the caches")
        }
    }

    @Test
    fun `recently watched layers restore from the stash and older ones are evicted`() {
        val config = tinyConfig().copy(numLayers = 6, attentionLayers = setOf(2, 4))
        val model = syntheticModel(config)
        val scene = Lfm2StackCompositor.buildScene(model)
        repeat(3) { model.forwardToken(it + 1); scene.publish(it) }

        val bx = scene.tile("block.conv.bx") as VectorHistoryTile
        val watched = bx.values.copyOf()
        assertTrue(watched.any { it != 0f })

        scene.layerSelector!!.invoke(1)
        assertTrue(bx.values.all { it == 0f }, "a never-watched conv layer starts blank")
        scene.layerSelector!!.invoke(0)
        assertTrue(bx.values.contentEquals(watched), "flip-back restores the stashed history")

        scene.layerSelector!!.invoke(1)
        scene.layerSelector!!.invoke(3)
        scene.layerSelector!!.invoke(5)
        scene.layerSelector!!.invoke(0)
        assertTrue(bx.values.all { it == 0f }, "history beyond the stash is gone")
    }

    @Test
    fun `the depth strip selects layers and highlights the block's span`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config))

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
    fun `tile geometry follows the shared feature, token, and weight scales`() {
        val config = tinyConfig()
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config))

        val q = scene.tile("block.attn.q")
        val k = scene.tile("block.attn.k")
        val gate = scene.tile("block.mlp.gate")
        val bcx = scene.tile("block.conv.bcx")
        assertEquals(q.width / 2, k.width, 1e-6, "k is half of q — the GQA ratio is geometric")
        assertEquals(q.width * config.intermediateSize / config.hiddenSize, gate.width, 1e-6)
        assertEquals(q.width * 3, bcx.width, 1e-6, "bcx is the three-chunk in_proj output")
        assertEquals(q.height, gate.height, 1e-6, "one token axis everywhere")
        assertEquals(q.height, scene.tile("block.attn.weights").width, 1e-6,
            "the attention triangle's columns are the same token axis as its rows")
        assertEquals(q.width / q.cols, q.height / q.rows, 1e-6,
            "token and feature axes share one px-per-cell scale — tile aspect is literal")

        val wq = scene.tile("block.w.self_attn.q_proj.weight")
        val wk = scene.tile("block.w.self_attn.k_proj.weight")
        assertEquals(wq.width, wk.width, 1e-6)
        assertEquals(wq.height / 2, wk.height, 1e-6, "Wk emits half of Wq's output dims")

        val window = scene.tile("block.conv.cache")
        val kernel = scene.tile("block.w.conv.conv.weight")
        assertEquals(window.width, kernel.width, 1e-6, "kernel and window share the channel axis")
        assertEquals(window.rows, kernel.rows, "both render taps as rows")
        assertTrue(kernel.magnified && window.magnified, "tap strips are floored, marked as insets")
        assertFalse(scene.tile("block.attn.weights").magnified, "the triangle is at true scale")
    }

    @Test
    fun `the live row cursor tracks the current token and the cache write frontier`() {
        val model = syntheticModel()
        val scene = Lfm2StackCompositor.buildScene(model)

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
        val scene = Lfm2StackCompositor.buildScene(syntheticModel(config))

        val attention = scene.tile("block.attn.weights") as AttentionTile
        val kCache = scene.tile("block.attn.k_cache") as DeckTile
        val vCache = scene.tile("block.attn.v_cache") as DeckTile
        val qPerKv = config.numHeads / config.numKvHeads

        attention.selectedHead = 3
        scene.onHeadSelected?.invoke(attention, 3)
        assertEquals(3 / qPerKv, kCache.selectedSlice)
        assertEquals(3 / qPerKv, vCache.selectedSlice)
        assertEquals("1/2 \u2192 q 2\u20133", kCache.sliceLabel!!.invoke(kCache.selectedSlice))
        assertTrue(scene.memoryEdges.any { (it.from as? TensorTile)?.id == "block.attn.k_cache" })
        assertTrue(scene.memoryEdges.any { (it.from as? TensorTile)?.id == "block.conv.cache" },
            "the conv window read is cross-time flow too")
    }
}
