package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.TensorPort
import java.awt.Color
import kotlin.math.ceil

class TensorTileDirtyRowsTest {

    private fun historyTile(rows: Int = 32, cols: Int = 8): Pair<VectorHistoryTile, TensorPort> {
        val port = TensorPort("resid", FloatTensor(1, cols))
        return VectorHistoryTile(port, rows) to port
    }

    @Test
    fun `token publish dirties only the written row`() {
        val (tile, port) = historyTile()
        port.tensor.copyFrom(FloatArray(8) { 0.5f })
        tile.publish(0)
        tile.consumeDirtyRows()

        port.tensor.copyFrom(FloatArray(8) { 0.4f })
        tile.publish(7)
        assertEquals(7..7, tile.consumeDirtyRows())
    }

    @Test
    fun `scale growth dirties the whole tile`() {
        val (tile, port) = historyTile()
        port.tensor.copyFrom(FloatArray(8) { 0.5f })
        tile.publish(0)
        tile.consumeDirtyRows()

        port.tensor.copyFrom(FloatArray(8) { 5f })
        tile.publish(1)
        assertNull(tile.consumeDirtyRows(), "a grown normalization scale restyles every shaded cell")
    }

    @Test
    fun `consume clears the accumulated range`() {
        val (tile, port) = historyTile()
        port.tensor.copyFrom(FloatArray(8) { 0.5f })
        tile.publish(0)
        tile.consumeDirtyRows()

        port.tensor.copyFrom(FloatArray(8) { 0.4f })
        tile.publish(3)
        port.tensor.copyFrom(FloatArray(8) { 0.3f })
        tile.publish(5)
        assertEquals(3..5, tile.consumeDirtyRows())
        assertNull(tile.consumeDirtyRows(), "nothing recorded since the last consume")
    }

    @Test
    fun `layer flip dirties the whole tile`() {
        val ports = listOf(TensorPort("a", FloatTensor(1, 8)), TensorPort("b", FloatTensor(1, 8)))
        val tile = VectorHistoryTile(ports, 32, stackLayers = listOf(0, 1))
        ports[0].tensor.copyFrom(FloatArray(8) { 0.5f })
        tile.publish(0)
        tile.consumeDirtyRows()

        tile.showLayer(1)
        assertNull(tile.consumeDirtyRows())
    }

    @Test
    fun `band reshade of the dirty rows matches a full reshade pixel for pixel`() {
        val rows = 64
        val cols = 16
        val (tile, port) = historyTile(rows, cols)
        val rng = java.util.Random(7)
        for (t in 0 until 40) {
            port.tensor.copyFrom(FloatArray(cols) { rng.nextFloat() * 2f - 1f })
            tile.publish(t)
        }
        tile.consumeDirtyRows()

        val patchW = 24
        val patchH = 20
        val neg = Color.BLUE
        val mid = Color.WHITE
        val pos = Color.RED
        val rowFrom = 0.0
        val rowTo = rows.toDouble()
        val colFrom = 0.0
        val colTo = cols.toDouble()

        val before = IntArray(patchW * patchH)
        tile.shadePatch(before, patchW, patchW, patchH, rowFrom, rowTo, colFrom, colTo, neg, mid, pos)

        // One more token, but with values inside the existing scale so the dirt stays one row.
        port.tensor.copyFrom(FloatArray(cols) { rng.nextFloat() * 0.5f - 0.25f })
        tile.publish(40)
        val dirty = tile.consumeDirtyRows()
        assertEquals(40..40, dirty)

        val full = IntArray(patchW * patchH)
        tile.shadePatch(full, patchW, patchW, patchH, rowFrom, rowTo, colFrom, colTo, neg, mid, pos)

        val banded = before.copyOf()
        val rowSpan = rowTo - rowFrom
        val y0 = (((dirty!!.first - rowFrom) / rowSpan * patchH).toInt() - 1).coerceIn(0, patchH)
        val y1 = (ceil((dirty.last + 1 - rowFrom) / rowSpan * patchH).toInt() + 1).coerceIn(y0, patchH)
        tile.shadePatch(
            banded, patchW, patchW, y1 - y0,
            rowFrom + rowSpan * y0 / patchH, rowFrom + rowSpan * y1 / patchH,
            colFrom, colTo, neg, mid, pos,
            destOffset = y0 * patchW,
        )

        assertArrayEquals(full, banded)
    }

    @Test
    fun `a stash captured before later tokens is dropped so the flip replays instead`() {
        val a = TensorPort("a", FloatTensor(1, 8))
        val b = TensorPort("b", FloatTensor(1, 8))
        val tile = VectorHistoryTile(listOf(a, b), rows = 8, stackLayers = listOf(0, 1), id = "stacked")
        a.tensor.copyFrom(FloatArray(8) { 0.5f })
        tile.showLayer(0)
        tile.publish(0)

        tile.showLayer(1)
        assertEquals(true, tile.hasHistoryFor(0), "the flipped-out layer starts stashed")

        b.tensor.copyFrom(FloatArray(8) { 0.4f })
        tile.publish(1)
        assertEquals(false, tile.hasHistoryFor(0),
            "a stash captured at token 0 can never gain token 1; it must drop")
        assertEquals(true, tile.hasHistoryFor(1), "the shown layer keeps recording")
    }

    @Test
    fun `ghosted view reshades the outgoing live row on always-recording tiles`() {
        val (tile, port) = historyTile()
        tile.alwaysRecords = true
        tile.historyView = HistoryView.GHOSTED
        port.tensor.copyFrom(FloatArray(8) { 0.5f })
        tile.publish(0)
        tile.consumeDirtyRows()

        tile.publish(1)
        val dirty = tile.consumeDirtyRows()
        assertEquals(true, dirty == null || 0 in dirty,
            "the outgoing live row must reshade to ghost strength, dirty was $dirty")
    }
}
