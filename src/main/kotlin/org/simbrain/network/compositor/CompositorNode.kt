package org.simbrain.network.compositor

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.toSimbrainColor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/**
 * Renders a [CompositorScene] as one Piccolo node: tile rasters ([SimbrainImage] children, whose
 * clip-region caches make zoom/pan a pure rescale of already-shaded pixels — invalidation tier 4),
 * data-flow edges, the logit-lens strip, and the interior interactions — click and marquee
 * selection, drag-move (tier 1), double-click trace, and hover value tooltips.
 *
 * Call [refreshDirtyTiles] on the EDT after the compute thread publishes a token; [relayout]
 * after geometry changes; [refreshTheme] on palette/theme switches (tier 3).
 */
class CompositorNode(
    val scene: CompositorScene,
    private val canvas: PCanvas? = null,
    private val tokenLabel: (Int) -> String = { "#$it" },
    private val cellReadout: (TensorTile, Int, Int) -> String = { tile, row, col ->
        "${tile.title} [$row, $col] = ${"%.4f".format(tile.valueAt(row, col))}"
    },
) : PNode() {

    private val background = PPath.createRectangle(0.0, 0.0, 1.0, 1.0).apply {
        paint = NetworkTheme.current.canvasBackground
        strokePaint = NetworkTheme.current.subnetOutline
    }.also { addChild(it) }

    private val edgeLayer = PNode().also { addChild(it) }

    private inner class TileNode(val tile: TensorTile) : PNode() {
        val raster = SimbrainImage(tile.image).apply {
            setBounds(0.0, 0.0, tile.width, tile.height)
        }.also { addChild(it) }
        val border = PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
            paint = null
        }.also { addChild(it) }
        val label = PText(tile.title).apply {
            font = Theme.tiny
        }.also { addChild(it) }

        init {
            syncLayout()
            syncHighlight()
        }

        fun syncLayout() {
            setOffset(tile.x, tile.y)
            label.setOffset(0.0, tile.height + 3.0)
        }

        fun syncHighlight() {
            val palette = NetworkTheme.current
            label.textPaint = palette.valueText
            val highlighted = tile == scene.traceFocus || tile in scene.tracedTiles || tile in scene.selection
            border.stroke = BasicStroke(if (highlighted || tile.kind == TileKind.WEIGHT) 2f else 1f)
            border.strokePaint = when {
                tile == scene.traceFocus -> palette.sourceHandle
                tile in scene.selection -> palette.selectionHandle
                tile in scene.tracedTiles -> palette.receptiveFieldTrace
                tile.kind == TileKind.WEIGHT -> palette.weightMatrixBoundary
                else -> palette.imageBorder
            }
        }
    }

    private val tileNodes = scene.tiles.map { TileNode(it).also { node -> addChild(node) } }
    private val tileNodesById = tileNodes.associateBy { it.tile.id }

    private inner class LensRowNode(val index: Int) : PNode() {
        val swatch = PPath.createRectangle(0.0, 0.0, 14.0, 14.0).apply {
            strokePaint = NetworkTheme.current.imageBorder
        }.also { addChild(it) }
        val text = PText().apply {
            font = Theme.small
            setOffset(20.0, 1.0)
        }.also { addChild(it) }

        fun refresh() {
            val lens = scene.lens ?: return
            val reading = lens.readings[index]
            val palette = NetworkTheme.current
            swatch.paint = Color(reading.prob.toSimbrainColor(palette.coolNode, palette.neutralMidpoint, palette.hotNode))
            text.textPaint = palette.valueText
            text.text = "${tokenLabel(reading.tokenId)}  ${"%.2f".format(reading.prob)}"
        }
    }

    private val lensRows = scene.lens?.sources?.indices?.map { LensRowNode(it).also { node -> addChild(node) } }
        ?: emptyList()

    private var marquee: PPath? = null

    /** Invoked after every tier-1 relayout, e.g. so a host can persist tile positions. */
    var onLayoutChanged: (() -> Unit)? = null

    init {
        rebuildEdges()
        relayout()
        refreshDirtyTiles()
        addInputEventListener(InteriorInputHandler())
    }

    /** Tier 2 (and 3, after a reshade request): shade dirty rows and repaint only touched tiles. */
    fun refreshDirtyTiles() {
        val dirty = scene.tiles.filter { it.isDirty }
        scene.shadeDirty()
        for (tile in dirty) tileNodesById.getValue(tile.id).raster.invalidatePaint()
        lensRows.forEach { it.refresh() }
    }

    /** Tier 1: re-derives node offsets, edges, lens placement, and bounds from tile rects. */
    fun relayout() {
        tileNodes.forEach { it.syncLayout() }
        rebuildEdges()
        for (row in lensRows) {
            val sourceId = scene.lens?.sources?.get(row.index)?.name ?: continue
            val tile = tileNodesById[sourceId]?.tile ?: continue
            row.setOffset(tile.x + tile.width + 14.0, tile.y + tile.height / 2 - 8.0)
        }
        val bounds = scene.tiles.fold(null as Rectangle2D?) { acc, tile ->
            val r = Rectangle2D.Double(tile.x, tile.y, tile.width, tile.height + 20)
            acc?.also { it.add(r) } ?: r
        } ?: Rectangle2D.Double()
        background.reset()
        background.append(
            Rectangle2D.Double(
                bounds.x - MARGIN, bounds.y - MARGIN,
                bounds.width + 2 * MARGIN + LENS_SPACE, bounds.height + 2 * MARGIN
            ), false
        )
        onLayoutChanged?.invoke()
    }

    /** Tier 3: re-run the palette over every value buffer — data untouched, geometry untouched. */
    fun refreshTheme() {
        val palette = NetworkTheme.current
        background.paint = palette.canvasBackground
        background.strokePaint = palette.subnetOutline
        scene.reshadeAll()
        tileNodes.forEach {
            it.raster.invalidatePaint()
            it.syncHighlight()
        }
        rebuildEdges()
        lensRows.forEach { it.refresh() }
    }

    private fun syncHighlights() {
        tileNodes.forEach { it.syncHighlight() }
        rebuildEdges()
    }

    private fun rebuildEdges() {
        val palette = NetworkTheme.current
        edgeLayer.removeAllChildren()
        for (edge in scene.edges) {
            val traced = edge in scene.tracedEdges
            PPath.createLine(
                edge.from.x + edge.from.width / 2, edge.from.y + edge.from.height / 2,
                edge.to.x + edge.to.width / 2, edge.to.y + edge.to.height / 2
            ).apply {
                stroke = BasicStroke(if (traced) 3f else 1.5f)
                strokePaint = if (traced) palette.receptiveFieldTrace else palette.connectionLine
                pickable = false
                edgeLayer.addChild(this)
            }
        }
    }

    private inner class InteriorInputHandler : PBasicInputEventHandler() {

        private var mode = Mode.NONE
        private var pressPoint: Point2D? = null
        private var marqueeAdditive = false

        override fun mousePressed(event: PInputEvent) {
            if (!event.isLeftMouseButton) return
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val tile = scene.tileAt(point.x, point.y)
            if (event.clickCount == 2) {
                scene.setTrace(if (tile == null || scene.traceFocus == tile) null else tile)
                syncHighlights()
                event.isHandled = true
                return
            }
            if (tile != null) {
                if (event.isShiftDown) {
                    scene.selection.toggle(tile)
                } else if (tile !in scene.selection) {
                    scene.selection.set(listOf(tile))
                }
                mode = Mode.MOVE
            } else {
                marqueeAdditive = event.isShiftDown
                if (!marqueeAdditive) scene.selection.clear()
                mode = Mode.MARQUEE
            }
            pressPoint = point
            syncHighlights()
            event.isHandled = true
        }

        override fun mouseDragged(event: PInputEvent) {
            val start = pressPoint ?: return
            when (mode) {
                Mode.MOVE -> {
                    val delta = event.getDeltaRelativeTo(this@CompositorNode)
                    for (tile in scene.selection.selected) {
                        tile.x += delta.width
                        tile.y += delta.height
                    }
                    relayout()
                }
                Mode.MARQUEE -> {
                    val point = event.getPositionRelativeTo(this@CompositorNode)
                    val rect = rectBetween(start, point)
                    val path = marquee ?: PPath.createRectangle(rect.x, rect.y, rect.width, rect.height).apply {
                        paint = null
                        stroke = BasicStroke(1f)
                        strokePaint = NetworkTheme.current.marquee
                        pickable = false
                        marquee = this
                        this@CompositorNode.addChild(this)
                    }
                    path.reset()
                    path.append(rect, false)
                }
                Mode.NONE -> return
            }
            event.isHandled = true
        }

        override fun mouseReleased(event: PInputEvent) {
            if (mode == Mode.MARQUEE) {
                val start = pressPoint
                marquee?.removeFromParent()
                marquee = null
                if (start != null) {
                    val point = event.getPositionRelativeTo(this@CompositorNode)
                    val rect = rectBetween(start, point)
                    val hit = scene.tilesIn(rect.x, rect.y, rect.width, rect.height)
                    if (marqueeAdditive) scene.selection.add(hit) else scene.selection.set(hit)
                    syncHighlights()
                }
                event.isHandled = true
            }
            mode = Mode.NONE
            pressPoint = null
        }

        override fun mouseMoved(event: PInputEvent) {
            val target = canvas ?: return
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val tile = scene.tileAt(point.x, point.y)
            val cell = tile?.cellAt(point.x, point.y)
            target.toolTipText = if (tile != null && cell != null) cellReadout(tile, cell.first, cell.second) else null
        }

        override fun mouseExited(event: PInputEvent) {
            canvas?.toolTipText = null
        }

        private fun rectBetween(a: Point2D, b: Point2D) = Rectangle2D.Double(
            minOf(a.x, b.x), minOf(a.y, b.y),
            kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.y - b.y)
        )
    }

    private enum class Mode { NONE, MOVE, MARQUEE }

    companion object {
        private const val MARGIN = 40.0
        private const val LENS_SPACE = 220.0
    }
}
