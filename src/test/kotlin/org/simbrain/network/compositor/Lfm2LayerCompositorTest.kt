package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.Lfm2Config
import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.llm.Lfm2Weights
import org.simbrain.network.llm.Safetensors
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

class Lfm2LayerCompositorTest {

    private fun weightsDirectory(): Path? {
        val hub = Path.of(
            System.getProperty("user.home"), ".cache", "huggingface", "hub",
            "models--LiquidAI--LFM2.5-230M", "snapshots"
        )
        if (!hub.exists()) return null
        return hub.listDirectoryEntries().firstOrNull { Lfm2Weights.isValidWeightsDirectory(it) }
    }

    private fun model(dir: Path) =
        Lfm2Model(Lfm2Config(maxSeqLen = 32), Safetensors.load(dir.resolve("model.safetensors")))

    private fun FlowEndpoint.key() = when (this) {
        is TensorTile -> id
        is OpVertex -> op.name
    }

    @Test
    fun `conv layer anatomy derives the window the gates and the swiglu junctions`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")
        val scene = Lfm2LayerCompositor.buildScene(model(dir!!), layer = 0, displaySeq = 8)

        val junctions = scene.opVertices.map { it.op.name }.toSet()
        assertTrue("layers.0.conv.b_gate" in junctions, "the B gate joins two slices of the in_proj")
        assertTrue("layers.0.conv.c_gate" in junctions, "the C gate joins conv output and in_proj")
        assertTrue("layers.0.mlp.silu_gate" in junctions)
        assertTrue("layers.0.mixer_residual" in junctions)
        assertTrue("layers.0.residual" in junctions)

        val satellites = scene.satellites.map { it.tile.id }.toSet()
        assertTrue("model.layers.0.conv.in_proj.weight" in satellites)
        assertTrue("model.layers.0.conv.conv.weight" in satellites, "the conv kernel rides the conv edge")
        assertTrue("model.layers.0.feed_forward.w2.weight" in satellites)

        val edges = scene.edges.map { it.from.key() to it.to.key() }.toSet()
        assertTrue(("layers.0.conv.bx" to "layers.0.conv.cache") in edges, "the sliding window is written from B ⊙ x")
        assertTrue(("layers.0.conv.bx" to "layers.0.conv.raw") in edges)
        assertTrue(
            scene.edges.none { it.from.key().contains("layers.1") || it.to.key().contains("layers.1") },
            "the scoped graph never leaks into other layers"
        )

        val axis = scene.tile("embed").let { it.x + it.width / 2 }
        for (id in listOf("layers.0.mixer_resid", "layers.0.resid")) {
            assertEquals(axis, scene.tile(id).let { it.x + it.width / 2 }, 1e-9, "$id sits on the spine")
        }
    }

    @Test
    fun `attention layer anatomy shows gqa decks rope arms and the score-mix junctions`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")
        val m = model(dir!!)
        val layer = m.config.attentionLayers.first()
        val scene = Lfm2LayerCompositor.buildScene(m, layer, displaySeq = 8)

        val junctions = scene.opVertices.map { it.op.name }.toSet()
        assertTrue("layers.$layer.attn.scores" in junctions, "q and the k cache converge on the scores op")
        assertTrue("layers.$layer.attn.mix" in junctions)
        assertTrue("layers.$layer.attn.q_norm_rope" in junctions, "rope angles arrow into the per-head norm")

        val edges = scene.edges.map { it.from.key() to it.to.key() }.toSet()
        assertTrue(("rope.cos" to "layers.$layer.attn.q_norm_rope") in edges)
        assertTrue(("layers.$layer.attn.q" to "layers.$layer.attn.scores") in edges)
        assertTrue(("layers.$layer.attn.k_cache" to "layers.$layer.attn.scores") in edges)
        assertTrue(("layers.$layer.attn.v_cache" to "layers.$layer.attn.mix") in edges)
        assertTrue(("layers.$layer.attn.k" to "layers.$layer.attn.k_cache") in edges, "the cache write is on the k arm")

        val kCache = scene.tile("layers.$layer.attn.k_cache") as DeckTile
        assertEquals(m.config.numKvHeads, kCache.slices)
        assertEquals(m.config.headDim, kCache.cols, "column slicing shows one KV head at a time")
        assertEquals(m.config.maxSeqLen, kCache.rows)

        val satellites = scene.satellites.map { it.tile.id }.toSet()
        assertTrue("model.layers.$layer.self_attn.q_proj.weight" in satellites)
        assertTrue("model.layers.$layer.self_attn.out_proj.weight" in satellites)
    }

    @Test
    fun `flipping the attention deck flips the kv cache decks to the serving group`() {
        val dir = weightsDirectory()
        assumeTrue(dir != null, "LFM2 weights not present in the HF cache")
        val m = model(dir!!)
        val layer = m.config.attentionLayers.first()
        val scene = Lfm2LayerCompositor.buildScene(m, layer, displaySeq = 8)

        val attention = scene.tile("layers.$layer.attn.weights") as AttentionTile
        val kCache = scene.tile("layers.$layer.attn.k_cache") as DeckTile
        val vCache = scene.tile("layers.$layer.attn.v_cache") as DeckTile
        val qPerKv = m.config.numHeads / m.config.numKvHeads

        attention.selectedHead = 5
        scene.onHeadSelected?.invoke(attention, 5)
        assertEquals(5 / qPerKv, kCache.selectedSlice, "the k deck flips to the serving kv group")
        assertEquals(5 / qPerKv, vCache.selectedSlice)
        assertTrue(kCache.sliceLabel!!.invoke(kCache.selectedSlice).contains("serves q"))

        assertTrue(scene.emphasizedEdges.isNotEmpty(), "the cache arrows carry standing emphasis")
        assertTrue(scene.emphasizedEdges.all { edge ->
            (edge.from as TensorTile).id.endsWith("_cache")
        })
    }
}
