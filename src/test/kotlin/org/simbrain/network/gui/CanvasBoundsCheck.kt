/**
 * Test support for checking that a network canvas has finished its Piccolo bounds bookkeeping: every node's cached
 * full bounds match its content, and no node is still marked for a validation pass that nothing has scheduled.
 *
 * Piccolo validates bounds in a pass from the root and clears each node's marks as it goes. A node marked outside
 * that pass schedules the next one only if the mark reaches the root, and a mark stops climbing at the first
 * ancestor that is already marked. Marks left behind once the event thread is idle therefore keep later changes under
 * them from scheduling a pass, until something else on the canvas happens to schedule one.
 */
package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertTrue
import org.piccolo2d.PNode
import org.simbrain.util.UiWork
import org.simbrain.util.onEdt
import java.awt.geom.Rectangle2D
import javax.swing.SwingUtilities
import kotlin.math.abs

/** A node whose cached full bounds were not what its content gives. */
internal class StaleBounds(val node: PNode, val cached: Rectangle2D, val fresh: Rectangle2D)

/**
 * What [canvasBoundsReport] found.
 *
 * @param marked nodes still marked as needing validation
 * @param stale nodes whose cached full bounds were out of date
 */
internal class CanvasBoundsReport(val marked: List<PNode>, val stale: List<StaleBounds>) {

    val settled get() = marked.isEmpty() && stale.isEmpty()

    override fun toString() = buildString {
        append("${marked.size} nodes marked for validation, ${stale.size} with stale full bounds")
        marked.groupBy { it.pathFromRoot() }.forEach { (path, nodes) -> append("\n  marked x${nodes.size}: $path") }
        stale.groupBy { it.node.pathFromRoot() }.forEach { (path, entries) ->
            val first = entries.first()
            append("\n  stale x${entries.size}: $path cached ${first.cached.describe()} fresh ${first.fresh.describe()}")
        }
    }
}

private fun PNode.pathFromRoot() =
    generateSequence(this) { it.parent }.toList().reversed().joinToString(" > ") { it.javaClass.simpleName }

private fun Rectangle2D.describe() = "[%.1f, %.1f, %.1f x %.1f]".format(x, y, width, height)

private fun Rectangle2D.differsFrom(other: Rectangle2D) =
    !(isEmpty && other.isEmpty) && (
        abs(x - other.x) > BOUNDS_TOLERANCE || abs(y - other.y) > BOUNDS_TOLERANCE ||
            abs(width - other.width) > BOUNDS_TOLERANCE || abs(height - other.height) > BOUNDS_TOLERANCE
        )

private const val BOUNDS_TOLERANCE = 1e-6

private val fullBoundsInvalid = PNode::class.java.getDeclaredField("fullBoundsInvalid").apply { isAccessible = true }
private val childBoundsInvalid = PNode::class.java.getDeclaredField("childBoundsInvalid").apply { isAccessible = true }
private val fullBoundsCache = PNode::class.java.getDeclaredField("fullBoundsCache").apply { isAccessible = true }

private val PNode.isMarked get() = fullBoundsInvalid.getBoolean(this) || childBoundsInvalid.getBoolean(this)

/**
 * Waits for the event thread to finish the work queued on it and the work that queues in turn, without asking any
 * node for its bounds, which would validate them. Must not be called on the event thread.
 */
internal fun NetworkPanel.awaitCanvasIdle() {
    // Model events reach the canvas through coroutines, which take a moment to hand their work to the event thread
    Thread.sleep(150)
    UiWork.awaitIdle()
    repeat(5) { SwingUtilities.invokeAndWait { } }
}

/**
 * Inspects the bounds bookkeeping of every node on the canvas, from the root down. Reading the fresh bounds validates
 * the canvas, so a second report straight after a first is always settled.
 */
internal fun NetworkPanel.canvasBoundsReport(): CanvasBoundsReport = onEdt {
    val nodes = canvas.root.allNodes.filterIsInstance<PNode>()
    val marked = nodes.filter { it.isMarked }
    val cached = nodes.associateWith { Rectangle2D.Double().apply { setRect(fullBoundsCache.get(it) as Rectangle2D) } }
    // Asking a node for its bounds validates it, which can mark its parent, so repeat until nothing is marked
    var rounds = 0
    do {
        nodes.forEach { it.fullBoundsReference }
    } while (nodes.any { it.isMarked } && ++rounds < 10)
    val stale = nodes.mapNotNull { node ->
        val fresh = node.computeFullBounds(null)
        val old = cached.getValue(node)
        if (old.differsFrom(fresh)) StaleBounds(node, old, fresh) else null
    }
    CanvasBoundsReport(marked, stale)
}

/**
 * Validates the whole canvas once it has gone idle, as its first paint does. A panel built off the event thread, as
 * tests build them, is marked from the root down without a pass being scheduled, so nothing is validated until this
 * or a paint asks.
 */
internal fun NetworkPanel.validateCanvas() {
    awaitCanvasIdle()
    canvasBoundsReport()
    awaitCanvasIdle()
}

/** Waits for the canvas to go idle, then fails if its bounds bookkeeping is unfinished. [after] names what was just done. */
internal fun NetworkPanel.assertCanvasBoundsSettled(after: String) {
    awaitCanvasIdle()
    val report = canvasBoundsReport()
    assertTrue(report.settled) { "Canvas bounds not settled after $after: $report" }
}
