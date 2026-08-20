/**
 * Covers the edge-bead collision nudge in [CompositorNode]: op glyphs strung on data-flow edges
 * slide along their curve out from under tiles instead of rendering hidden behind them, and stay
 * at their nominal slots when nothing is in the way.
 */
package org.simbrain.network.compositor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.network.llm.TinyLmModel

class CompositorNodeGlyphNudgeTest {

    private fun scene() = TinyLmCompositor.buildScene(TinyLmModel(TinyLmConfig(
        contextSize = 5, embedDim = 8, numHeads = 2, hiddenDim = 10, vocabSize = 7, numLayers = 1
    )))

    /** The first edge bead that renders as a free glyph (not pinned above a satellite tile). */
    private fun freeBead(scene: CompositorScene, node: CompositorNode) =
        scene.edges.asSequence().flatMap { edge -> edge.ops.map { edge to it } }
            .first { (_, op) ->
                op !in scene.satellites.map { it.op } && node.glyphFor(op) != null
            }

    @Test
    fun `a glyph slides along its edge out from under a tile dragged over it`() {
        val scene = scene()
        val node = CompositorNode(scene)
        val (edge, op) = freeBead(scene, node)
        val glyph = node.glyphFor(op)!!
        val nominal = glyph.xOffset to glyph.yOffset

        val obstacle = scene.tiles.first {
            it.kind != TileKind.WEIGHT && it !== edge.from && it !== edge.to
        }
        obstacle.x = nominal.first - obstacle.width / 2
        obstacle.y = nominal.second - obstacle.height / 2
        node.relayout()

        val nudged = node.glyphFor(op)!!
        assertFalse(nudged.fullBoundsReference.intersects(obstacle.bounds),
            "glyph must slide clear of the tile covering its slot")
        assertTrue(nominal != nudged.xOffset to nudged.yOffset, "glyph must leave its covered slot")
    }

    @Test
    fun `an unobstructed glyph keeps its nominal slot across relayouts`() {
        val scene = scene()
        val node = CompositorNode(scene)
        val (_, op) = freeBead(scene, node)
        val glyph = node.glyphFor(op)!!
        val nominal = glyph.xOffset to glyph.yOffset

        node.relayout()

        val after = node.glyphFor(op)!!
        assertEquals(nominal, after.xOffset to after.yOffset,
            "with nothing in the way the nudge must be a no-op")
    }
}
