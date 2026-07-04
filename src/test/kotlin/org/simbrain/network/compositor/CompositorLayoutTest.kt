package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.TeachingTransformerConfig
import org.simbrain.network.llm.TeachingTransformerModel

class CompositorLayoutTest {

    private fun scene(layers: Int = 1) = TeachingCompositor.buildScene(TeachingTransformerModel(
        TeachingTransformerConfig(
            contextSize = 5, embedDim = 8, numHeads = 2, hiddenDim = 10, vocabSize = 7, numLayers = layers
        )
    ))

    private fun CompositorScene.centerX(id: String) = tile(id).let { it.x + it.width / 2 }

    @Test
    fun `spine checkpoints and the head share one vertical axis in flow order`() {
        val scene = scene(layers = 2)
        val spineIds = listOf(
            "resid0",
            "layers.0.attn_resid", "layers.0.resid",
            "layers.1.attn_resid", "layers.1.resid",
            "logits", "probs",
        )
        val axis = scene.centerX("resid0")
        for (id in spineIds) {
            assertEquals(axis, scene.centerX(id), 1e-9, "$id sits on the spine axis")
        }
        val ys = spineIds.map { scene.tile(it).y }
        assertEquals(ys.sorted(), ys, "spine flows strictly top-down")
        assertTrue(ys.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `limb rows sit clear of the lens strip and rank between their checkpoints`() {
        val scene = scene()
        val spineRight = scene.tile("resid0").let { it.x + it.width }
        for (id in listOf("layers.0.attn.q", "layers.0.attn.weights", "layers.0.attn.out", "layers.0.mlp.act")) {
            assertTrue(scene.tile(id).x >= spineRight + 220.0, "$id clears the lens strip")
        }
        val q = scene.tile("layers.0.attn.q")
        val k = scene.tile("layers.0.attn.k")
        val v = scene.tile("layers.0.attn.v")
        assertTrue(q.x < k.x && k.x < v.x, "rank row keeps declaration order left to right")
        assertTrue(q.y + q.height <= k.y && k.y + k.height <= v.y,
            "same-rank siblings cascade down so fan-in curves clear earlier siblings")

        val branch = scene.tile("resid0")
        val rejoin = scene.tile("layers.0.attn_resid")
        val deck = scene.tile("layers.0.attn.weights")
        assertTrue(q.y > branch.y && deck.y > q.y && rejoin.y > scene.tile("layers.0.attn.out").y,
            "the attention limb ranks strictly between its checkpoints")
    }

    @Test
    fun `anchor tiles never overlap`() {
        val scene = scene(layers = 2)
        val satellites = scene.satellites.map { it.tile }.toSet()
        val anchors = scene.tiles.filter { it !in satellites }
        for (i in anchors.indices) {
            for (j in i + 1 until anchors.size) {
                val a = anchors[i]
                val b = anchors[j]
                assertTrue(!a.intersects(b.x, b.y, b.width, b.height), "${a.id} overlaps ${b.id}")
            }
        }
    }

    @Test
    fun `standalone parameters sit side by side above the junction that joins them`() {
        val scene = scene()
        val junction = scene.opVertices.first { it.op.name == "add_pos" }
        val table = scene.tile("embed.table")
        val pos = scene.tile("embed.pos")
        assertTrue(table.y + table.height < junction.y, "embedding sits above the +")
        assertTrue(pos.y + pos.height < junction.y, "positions sit above the +")
        assertTrue(table.x + table.width <= pos.x, "the group lays out left to right")
        val groupCenter = (table.x + (pos.x + pos.width)) / 2
        assertEquals(junction.x, groupCenter, 1e-9, "the parameter row centers on the junction")
    }

    @Test
    fun `junction adds pin to the spine axis between their checkpoints`() {
        val scene = scene()
        val axis = scene.centerX("resid0")
        val rejoin = scene.opVertices.first { it.op.name == "layers.0.attn_residual" }
        assertTrue(rejoin.placed)
        assertEquals(axis, rejoin.x, 1e-9, "the residual ⊕ sits on the trunk")
        assertTrue(rejoin.y > scene.tile("layers.0.attn.out").y, "the ⊕ ranks after the limb output")
        assertTrue(rejoin.y < scene.tile("layers.0.attn_resid").y, "the ⊕ ranks before the checkpoint it writes")
        val scores = scene.opVertices.first { it.op.name == "layers.0.attn.score" }
        assertTrue(scores.x > scene.tile("resid0").x + scene.tile("resid0").width, "limb junctions stay in the limb")
    }

    @Test
    fun `diagram scale shrinks tiles but keeps the fixed-size lens and label room`() {
        val model = TeachingTransformerModel(TeachingTransformerConfig(
            contextSize = 5, embedDim = 8, numHeads = 2, hiddenDim = 10, vocabSize = 7, numLayers = 1
        ))
        val scene = TeachingCompositor.buildScene(model, scale = 0.5)
        val resid0 = scene.tile("resid0")
        assertEquals(85.0, resid0.width, 1e-9, "tile geometry scales")
        assertEquals(60.0, resid0.height, 1e-9)
        val q = scene.tile("layers.0.attn.q")
        assertTrue(q.y >= resid0.y + resid0.height + 70.0, "row gap keeps its label-room floor")
        assertTrue(q.x >= resid0.x + resid0.width + 220.0, "the fixed-size lens strip keeps its clearance")
    }

    @Test
    fun `layout is deterministic across repeated application`() {
        val scene = scene(layers = 2)
        val before = scene.tiles.associate { it.id to (it.x to it.y) }
        CompositorLayout().apply(scene)
        val after = scene.tiles.associate { it.id to (it.x to it.y) }
        assertEquals(before, after)
    }
}
