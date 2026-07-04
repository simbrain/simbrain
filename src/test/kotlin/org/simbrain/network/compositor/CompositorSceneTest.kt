package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.LinearOp
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.TensorPort
import org.simbrain.util.toSimbrainColor
import java.awt.Color

class CompositorSceneTest {

    private val neg = Color.BLUE
    private val mid = Color.WHITE
    private val pos = Color.RED

    @Test
    fun `vector history tile appends the published row and shades only touched rows`() {
        val port = TensorPort("resid", FloatTensor.of(1, 3, floatArrayOf(0.5f, -1f, 0.25f)))
        val tile = VectorHistoryTile(port, rows = 4)
        tile.publish(0)
        tile.shadeDirty(neg, mid, pos)

        assertEquals(0.5f, tile.valueAt(0, 0))
        assertEquals(-1f, tile.valueAt(0, 1))
        assertEquals((0.5f).toSimbrainColor(neg, mid, pos), tile.image.getRGB(0, 0))
        assertEquals((-1f).toSimbrainColor(neg, mid, pos), tile.image.getRGB(1, 0))
        assertEquals(0f.toSimbrainColor(neg, mid, pos), tile.image.getRGB(0, 2), "untouched rows stay neutral")
    }

    @Test
    fun `publish is gated on the source tensor version`() {
        val port = TensorPort("resid", FloatTensor.of(1, 2, floatArrayOf(1f, 2f)))
        val tile = VectorHistoryTile(port, rows = 3)
        tile.publish(0)
        tile.publish(1)
        assertEquals(0f, tile.valueAt(1, 0), "unchanged tensor must not publish a second row")
        port.tensor.copyFrom(floatArrayOf(3f, 4f))
        tile.publish(1)
        assertEquals(3f, tile.valueAt(1, 0))
    }

    @Test
    fun `growing value scale reshades earlier rows against the new normalization`() {
        val port = TensorPort("resid", FloatTensor.of(1, 2, floatArrayOf(0.5f, 0.5f)))
        val tile = VectorHistoryTile(port, rows = 3)
        tile.publish(0)
        tile.shadeDirty(neg, mid, pos)
        val fullyHot = tile.image.getRGB(0, 0)
        assertEquals(1f.toSimbrainColor(neg, mid, pos), fullyHot, "0.5 is the max so far, so it shades saturated")

        port.tensor.copyFrom(floatArrayOf(2f, 2f))
        tile.publish(1)
        assertTrue(tile.isDirty)
        tile.shadeDirty(neg, mid, pos)
        assertEquals((0.25f).toSimbrainColor(neg, mid, pos), tile.image.getRGB(0, 0),
            "row 0 must reshade against the grown scale")
        assertEquals(1f.toSimbrainColor(neg, mid, pos), tile.image.getRGB(0, 1))
    }

    @Test
    fun `palette reshade rewrites pixels without touching values`() {
        val port = TensorPort("resid", FloatTensor.of(1, 2, floatArrayOf(1f, -1f)))
        val tile = VectorHistoryTile(port, rows = 2)
        tile.publish(0)
        tile.shadeDirty(neg, mid, pos)
        val before = tile.image.getRGB(0, 0)
        val valuesBefore = tile.values.copyOf()

        tile.markAllDirty()
        tile.shadeDirty(Color.GREEN, Color.BLACK, Color.YELLOW)
        assertNotEquals(before, tile.image.getRGB(0, 0))
        assertEquals(1f.toSimbrainColor(Color.GREEN, Color.BLACK, Color.YELLOW), tile.image.getRGB(0, 0))
        assertTrue(valuesBefore.contentEquals(tile.values), "tier 3 must not touch the value buffer")
    }

    @Test
    fun `attention tile retains every head and switches heads from history`() {
        val weights = FloatTensor(2, 4)
        val port = TensorPort("attn", weights)
        val tile = AttentionTile(port, numHeads = 2, seqLen = 3)

        weights.copyFrom(floatArrayOf(1f, 0f, 0f, 0f, 0.9f, 0f, 0f, 0f))
        tile.publish(0)
        weights.copyFrom(floatArrayOf(0.3f, 0.7f, 0f, 0f, 0.6f, 0.4f, 0f, 0f))
        tile.publish(1)

        assertEquals(0.7f, tile.valueAt(1, 1), "head 0 shown by default")
        tile.selectedHead = 1
        assertEquals(0.4f, tile.valueAt(1, 1), "head switch rebuilds values from retained history")
        assertEquals(0.9f, tile.valueAt(0, 0))
        assertTrue(tile.isDirty, "head switch must trigger a full reshade")
        tile.shadeDirty(neg, mid, pos)
        assertEquals((0.4f).toSimbrainColor(neg, mid, pos), tile.image.getRGB(1, 1))
    }

    @Test
    fun `matrix tile bulk-publishes the whole tensor gated on its version`() {
        val tensor = FloatTensor.of(2, 3, floatArrayOf(1f, -2f, 3f, -4f, 5f, -6f))
        val tile = MatrixTile("weights", "weights", tensor, kind = TileKind.WEIGHT)
        tile.publish(-1)
        tile.shadeDirty(neg, mid, pos)

        assertEquals(-2f, tile.valueAt(0, 1))
        assertEquals(5f, tile.valueAt(1, 1))
        assertEquals((-1f).toSimbrainColor(neg, mid, pos), tile.image.getRGB(2, 1), "-6 is the abs max")

        tensor.data.put(0, 99f)
        tile.publish(-1)
        assertEquals(1f, tile.valueAt(0, 0), "unbumped version must not republish")
        tensor.markMutated()
        tile.publish(-1)
        assertEquals(99f, tile.valueAt(0, 0), "version bump republishes the whole matrix")
    }

    @Test
    fun `transposed matrix tile renders the transpose for display`() {
        val tensor = FloatTensor.of(2, 3, floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f))
        val tile = MatrixTile("w", "w", tensor, displayTransposed = true)
        assertEquals(3, tile.rows)
        assertEquals(2, tile.cols)
        tile.publish(-1)
        assertEquals(4f, tile.valueAt(0, 1))
        assertEquals(3f, tile.valueAt(2, 0))
    }

    @Test
    fun `full-pass scene publish refreshes matrix tiles and leaves token-indexed tiles alone`() {
        val matrixTensor = FloatTensor.of(2, 2, floatArrayOf(1f, 2f, 3f, 4f))
        val historyPort = TensorPort("resid", FloatTensor.of(1, 2, floatArrayOf(7f, 8f)))
        val scene = CompositorScene().apply {
            addTile(MatrixTile("m", "m", matrixTensor))
            addTile(VectorHistoryTile(historyPort, rows = 3))
        }
        scene.publish()
        assertEquals(1f, scene.tile("m").valueAt(0, 0))
        assertEquals(0f, scene.tile("resid").valueAt(0, 0),
            "a token-history tile must ignore a full-pass publish")
    }

    @Test
    fun `hit testing maps scene points to tiles and cells`() {
        val port = TensorPort("resid", FloatTensor(1, 4))
        val tile = VectorHistoryTile(port, rows = 2).apply {
            x = 10.0; y = 20.0; width = 40.0; height = 20.0
        }
        val scene = CompositorScene().apply { addTile(tile) }

        assertEquals(tile, scene.tileAt(15.0, 25.0))
        assertNull(scene.tileAt(5.0, 5.0))
        assertEquals(1 to 3, tile.cellAt(49.0, 39.0))
        assertEquals(0 to 0, tile.cellAt(10.0, 20.0))
        assertEquals(listOf(tile), scene.tilesIn(0.0, 0.0, 15.0, 25.0))
        assertTrue(scene.tilesIn(0.0, 0.0, 5.0, 5.0).isEmpty())
    }

    @Test
    fun `selection model notifies on change and supports toggle and marquee set`() {
        val tiles = (0..2).map { VectorHistoryTile(TensorPort("t$it", FloatTensor(1, 2)), rows = 1) }
        val selection = TileSelectionModel()
        var changes = 0
        selection.onChange = { changes++ }

        selection.set(listOf(tiles[0]))
        assertTrue(tiles[0] in selection)
        selection.toggle(tiles[1])
        assertEquals(setOf(tiles[0], tiles[1]), selection.selected)
        selection.toggle(tiles[0])
        assertEquals(setOf(tiles[1]), selection.selected)
        selection.clear()
        assertTrue(selection.selected.isEmpty())
        assertEquals(4, changes)
        selection.clear()
        assertEquals(4, changes, "clearing an empty selection must not notify")
    }

    @Test
    fun `trace highlights the paths through the focus but not bypass edges`() {
        val x = TensorPort("x", FloatTensor(1, 4))
        val a = TensorPort("a", FloatTensor(1, 4))
        val b = TensorPort("b", FloatTensor(1, 4))
        val c = TensorPort("c", FloatTensor(1, 4))
        val d = TensorPort("d", FloatTensor(1, 4))
        val plan = OpPlan(listOf(
            LinearOp("l1", TensorPort("W1", FloatTensor(4, 4)), x, a),
            LinearOp("l2", TensorPort("W2", FloatTensor(4, 4)), a, b),
            AddOp("add", a, b, c),
            LinearOp("l3", TensorPort("W3", FloatTensor(4, 4)), c, d),
        ))
        val scene = CompositorScene(PlanGraph(plan))
        val tileA = VectorHistoryTile(a, rows = 1).also { scene.addTile(it) }
        val tileB = VectorHistoryTile(b, rows = 1).also { scene.addTile(it) }
        val tileD = VectorHistoryTile(d, rows = 1).also { scene.addTile(it) }
        scene.connectFromGraph()
        fun FlowEndpoint.key() = when (this) {
            is TensorTile -> id
            is OpVertex -> op.name
        }
        val endpoints = scene.edges.map { it.from.key() to it.to.key() }.toSet()
        assertEquals(
            setOf("a" to "b", "a" to "add", "b" to "add", "add" to "d"), endpoints,
            "the two-input add becomes a junction vertex both streams arrow into"
        )

        scene.setTrace(tileB)
        assertEquals(setOf(tileA, tileB, tileD), scene.tracedTiles)
        val traced = scene.tracedEdges.map { it.from.key() to it.to.key() }.toSet()
        assertEquals(
            setOf("a" to "b", "b" to "add", "add" to "d"), traced,
            "the a->add bypass arm is not on a path through b"
        )

        scene.setTrace(null)
        assertTrue(scene.tracedTiles.isEmpty())
        assertTrue(scene.tracedEdges.isEmpty())
        assertFalse(tileB in scene.selection)
    }
}
