package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.network.llm.TinyLmModel

class CompositorLayoutTest {

    private fun scene(layers: Int = 1) = TinyLmCompositor.buildScene(TinyLmModel(
        TinyLmConfig(
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
        assertEquals(ys.sortedDescending(), ys, "spine flows strictly bottom-up")
        assertTrue(ys.zipWithNext().all { (a, b) -> b < a })
    }

    @Test
    fun `limbs flow horizontally clear of the lens strip centered on the checkpoint feeding them`() {
        val scene = scene()
        val spineRight = scene.tile("resid0").let { it.x + it.width }
        for (id in listOf("layers.0.attn.q", "layers.0.attn.weights", "layers.0.attn.out", "layers.0.mlp.act")) {
            assertTrue(scene.tile(id).x >= spineRight + 220.0, "$id clears the lens strip")
        }
        val q = scene.tile("layers.0.attn.q")
        val k = scene.tile("layers.0.attn.k")
        val v = scene.tile("layers.0.attn.v")
        assertEquals(q.x, k.x, 1e-9, "q, k, and v share the limb's first column")
        assertEquals(k.x, v.x, 1e-9)
        assertTrue(q.y >= k.y + k.height && k.y >= v.y + v.height,
            "column siblings stack upward in declaration order so fan-in curves clear each other")

        val branch = scene.tile("resid0")
        val rejoin = scene.tile("layers.0.attn_resid")
        val deck = scene.tile("layers.0.attn.weights")
        val out = scene.tile("layers.0.attn.out")
        assertTrue(deck.x > q.x && out.x > deck.x, "the limb flows left to right through its columns")
        // q/k/v is the limb's tallest column, so its extent is the strip's extent.
        val stripCenter = (q.y + v.y + v.height) / 2
        assertEquals(branch.y + branch.height / 2, stripCenter, 1e-6,
            "the limb strip centers on the checkpoint that feeds it")
        assertTrue(rejoin.y + rejoin.height < v.y, "the rejoin checkpoint sits above the strip")
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
        assertTrue(table.y > junction.y, "embedding sits below the +")
        assertTrue(pos.y > junction.y, "positions sit below the +")
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
        assertTrue(rejoin.y < scene.tile("layers.0.attn.out").y, "the ⊕ ranks after the limb output")
        assertTrue(rejoin.y > scene.tile("layers.0.attn_resid").y, "the ⊕ ranks before the checkpoint it writes")
        val scores = scene.opVertices.first { it.op.name == "layers.0.attn.score" }
        assertTrue(scores.x > scene.tile("resid0").x + scene.tile("resid0").width, "limb junctions stay in the limb")
    }

    @Test
    fun `diagram scale shrinks tiles but keeps the fixed-size lens and label room`() {
        val model = TinyLmModel(TinyLmConfig(
            contextSize = 5, embedDim = 8, numHeads = 2, hiddenDim = 10, vocabSize = 7, numLayers = 1
        ))
        val scene = TinyLmCompositor.buildScene(model, scale = 0.5)
        val resid0 = scene.tile("resid0")
        assertEquals(85.0, resid0.width, 1e-9, "tile geometry scales")
        assertEquals(60.0, resid0.height, 1e-9)
        val q = scene.tile("layers.0.attn.q")
        val logits = scene.tile("logits")
        val probs = scene.tile("probs")
        assertTrue(logits.y >= probs.y + probs.height + 54.0, "row gap keeps its label-room floor")
        assertTrue(q.x >= resid0.x + resid0.width + 220.0, "the fixed-size lens strip keeps its clearance")
    }

    @Test
    fun `return lanes re-derive from current rects so they follow dragged limbs`() {
        val scene = scene()
        val edge = scene.edges.first {
            (it.from as? TensorTile)?.id == "layers.0.attn.out" && it.to is OpVertex
        }
        assertTrue(edge in scene.returnLanes, "the limb-to-spine edge routes through a lane")
        assertEquals(2, edge.waypoints.size)
        val route = scene.returnLanes.getValue(edge)
        val laneBefore = edge.waypoints.first().y
        assertTrue(laneBefore < route.clearItems.minOf { it.routeRect.minY },
            "the lane runs above everything hanging in the gap")

        val highest = route.clearItems.filterIsInstance<TensorTile>().minBy { it.y }
        highest.y -= 300.0
        scene.deriveReturnWaypoints()
        assertEquals(laneBefore - 300.0, edge.waypoints.first().y, 1e-9,
            "the lane follows the dragged strip up")
    }

    @Test
    fun `return drops rise from the source center when the path to the lane is clear`() {
        val scene = scene()
        val edge = scene.edges.first {
            (it.from as? TensorTile)?.id == "layers.0.attn.out" && it.to is OpVertex
        }
        val out = scene.tile("layers.0.attn.out")
        assertEquals(out.x + out.width / 2, edge.waypoints.first().x, 1e-9,
            "an unobstructed return rises straight from its source")

        val blocker = scene.returnLanes.getValue(edge).clearItems
            .filterIsInstance<TensorTile>().first { it !== out }
        blocker.x = out.x
        blocker.y = out.y - blocker.height - 10.0
        scene.deriveReturnWaypoints()
        assertTrue(edge.waypoints.first().x >= out.x + out.width,
            "a blocked return drops beside its source instead")
    }

    @Test
    fun `layout is deterministic across repeated application`() {
        val scene = scene(layers = 2)
        val before = scene.tiles.associate { it.id to (it.x to it.y) }
        CompositorLayout(
            verticalFlow = VerticalFlow.BOTTOM_TO_TOP,
            density = LayoutDensity.COMPACT,
        ).apply(scene)
        val after = scene.tiles.associate { it.id to (it.x to it.y) }
        assertEquals(before, after)
    }
}
