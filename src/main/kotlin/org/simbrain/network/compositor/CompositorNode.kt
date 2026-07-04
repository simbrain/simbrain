package org.simbrain.network.compositor

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.tensor.op.AddOp
import org.simbrain.network.tensor.op.BiasOp
import org.simbrain.network.tensor.op.CausalMaskedRowSoftmaxOp
import org.simbrain.network.tensor.op.HeadMixOp
import org.simbrain.network.tensor.op.HeadScoresOp
import org.simbrain.network.tensor.op.LayerNormOp
import org.simbrain.network.tensor.op.LinearOp
import org.simbrain.network.tensor.op.MatMulLinearOp
import org.simbrain.network.tensor.op.MergeHeadsOp
import org.simbrain.network.tensor.op.ReLUOp
import org.simbrain.network.tensor.op.RmsNormOp
import org.simbrain.network.tensor.op.SeqEmbedOp
import org.simbrain.network.tensor.op.SeqSoftmaxCrossEntropyOp
import org.simbrain.network.tensor.op.SiluGateOp
import org.simbrain.network.tensor.op.SoftmaxCrossEntropyOp
import org.simbrain.network.tensor.op.SplitHeadsOp
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.util.*
import org.simbrain.util.piccolo.SimbrainImage
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.atan2

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

    /** Live op glyphs by op, rebuilt with the edges; micro-stepping highlights through this. */
    private val glyphsByOp = HashMap<TensorOp, OpGlyphNode>()

    /** The curve each edge currently renders along, rebuilt with the edges. */
    private val routesByEdge = HashMap<TileEdge, BezierRoute>()

    private val arrowTip = Path2D.Double().apply {
        moveTo(0.0, -TIP_LENGTH)
        lineTo(RIBBON_THICKNESS.toDouble(), 0.0)
        lineTo(-RIBBON_THICKNESS.toDouble(), 0.0)
        closePath()
    }

    private var currentStepOp: TensorOp? = null

    private var staleTiles: Set<TensorTile> = emptySet()

    private inner class TileNode(val tile: TensorTile) : PNode() {
        /** Dimmed offset cards behind a deck's live front slice — the 2.5D stack. */
        val backCards: List<PPath> = if (tile is DeckTile && tile.slices > 1) {
            (minOf(tile.slices - 1, MAX_BACK_CARDS) downTo 1).map { i ->
                PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
                    pickable = false
                    setOffset(-DECK_STEP * i, -DECK_STEP * i)
                }.also { addChild(it) }
            }
        } else emptyList()
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
            syncLabel()
        }

        fun syncLabel() {
            label.text = if (tile is DeckTile) "${tile.title} · head ${tile.selectedSlice}" else tile.title
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
            backCards.forEach {
                it.paint = palette.canvasBackground
                it.strokePaint = palette.imageBorder
                it.stroke = BasicStroke(1f)
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
            row.setOffset(tile.x - LENS_SPACE, tile.y + tile.height / 2 - 8.0)
        }
        val bounds = scene.tiles.fold(null as Rectangle2D?) { acc, tile ->
            val r = Rectangle2D.Double(tile.x, tile.y, tile.width, tile.height + 20)
            acc?.also { it.add(r) } ?: r
        } ?: Rectangle2D.Double()
        background.reset()
        background.append(
            Rectangle2D.Double(
                bounds.x - MARGIN - LENS_SPACE, bounds.y - MARGIN,
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

    /**
     * A small operation glyph strung on a data-flow edge: circled symbols for the arithmetic ops
     * (the old transformer node's junction/multiply decorations, now derived from the plan) and
     * abbreviations for the rest.
     */
    inner class OpGlyphNode(val op: TensorOp) : PNode() {
        private val symbol = opSymbol(op)
        private val circular = symbol.length == 1
        val shape: PPath = if (circular) {
            PPath.createEllipse(-GLYPH_RADIUS, -GLYPH_RADIUS, 2 * GLYPH_RADIUS, 2 * GLYPH_RADIUS)
        } else {
            PPath.createRoundRectangle(
                -GLYPH_RADIUS * 1.6, -GLYPH_RADIUS * 0.8,
                GLYPH_RADIUS * 3.2, GLYPH_RADIUS * 1.6, 4.0, 4.0
            )
        }.also { addChild(it) }
        private val text = PText(symbol).apply {
            font = Theme.tiny
        }.also { addChild(it) }

        var glowing = false
            set(value) {
                field = value
                syncTheme()
            }

        init {
            pickable = false
            syncTheme()
        }

        fun syncTheme() {
            val palette = NetworkTheme.current
            shape.paint = palette.canvasBackground
            shape.strokePaint = if (glowing) palette.sourceHandle else palette.connectionLine
            shape.stroke = BasicStroke(if (glowing) 2.5f else 1f)
            text.textPaint = palette.valueText
            text.setOffset(-text.width / 2, -text.height / 2)
        }

        fun containsScenePoint(sceneX: Double, sceneY: Double): Boolean {
            val reach = GLYPH_RADIUS * if (circular) 1.0 else 1.8
            return abs(sceneX - xOffset) <= reach && abs(sceneY - yOffset) <= reach
        }
    }

    private fun opSymbol(op: TensorOp): String = when (op) {
        is AddOp -> "+"
        is BiasOp -> "+"
        is LinearOp, is MatMulLinearOp, is HeadScoresOp, is HeadMixOp -> "×"
        is LayerNormOp, is RmsNormOp -> "LN"
        is CausalMaskedRowSoftmaxOp -> "σ"
        is SoftmaxCrossEntropyOp, is SeqSoftmaxCrossEntropyOp -> "CE"
        is ReLUOp -> "ReLU"
        is SiluGateOp -> "SiLU"
        is SplitHeadsOp -> "split"
        is MergeHeadsOp -> "merge"
        is SeqEmbedOp -> "emb"
        else -> op.name.substringAfterLast('.')
    }

    /**
     * Attach fractions along each tile side, spread so several curves sharing a side fan out
     * instead of stacking on the midpoint. Curves are ordered along the side by where their far
     * endpoint projects onto it, so the fan never crosses itself.
     */
    private fun attachFractions(
        sideOf: (TileEdge) -> Line2D,
        tileOf: (TileEdge) -> TensorTile,
        guideOf: (TileEdge) -> Point2D,
    ): Map<TileEdge, Double> {
        val fractions = HashMap<TileEdge, Double>()
        val bySide = scene.edges.groupBy { tileOf(it) to sideOf(it).midPoint.let { m -> m.x to m.y } }
        for ((_, group) in bySide) {
            val ordered = group.sortedBy { sideOf(it).projectionFraction(guideOf(it)) }
            ordered.forEachIndexed { j, edge -> fractions[edge] = (j + 1.0) / (ordered.size + 1.0) }
        }
        return fractions
    }

    /**
     * Slides a satellite along its curve away from the op's nominal slot until it stops
     * overlapping already-placed tiles.
     */
    private fun satelliteT(route: BezierRoute, tile: TensorTile, slotT: Double, obstacles: List<TensorTile>): Double {
        for (offset in SATELLITE_NUDGES) {
            val t = slotT + offset
            if (t < 0.1 || t > 0.9) continue
            val at = route.pointAt(t)
            val clear = obstacles.none {
                it.intersects(
                    at.x - tile.width / 2 - 12, at.y - tile.height / 2 - 12,
                    tile.width + 24, tile.height + 24
                )
            }
            if (clear) return t
        }
        return slotT
    }

    private fun rebuildEdges() {
        val palette = NetworkTheme.current
        edgeLayer.removeAllChildren()
        glyphsByOp.clear()
        routesByEdge.clear()
        val satellitesByEdge = scene.satellites.groupBy { it.edge }
        val satelliteTiles = scene.satellites.map { it.tile }.toSet()
        val placedObstacles = scene.tiles.filter { it !in satelliteTiles }.toMutableList()
        val tailSides = scene.edges.associateWith {
            it.from.bounds.facingSide(it.waypoints.firstOrNull() ?: it.to.bounds.center)
        }
        val headSides = scene.edges.associateWith {
            it.to.bounds.facingSide(it.waypoints.lastOrNull() ?: it.from.bounds.center)
        }
        val tailFractions = attachFractions(
            { tailSides.getValue(it) }, { it.from },
            { it.waypoints.firstOrNull() ?: it.to.bounds.center })
        val headFractions = attachFractions(
            { headSides.getValue(it) }, { it.to },
            { it.waypoints.lastOrNull() ?: it.from.bounds.center })
        for (edge in scene.edges) {
            val traced = edge in scene.tracedEdges
            val tailSide = tailSides.getValue(edge)
            val headSide = headSides.getValue(edge)
            val tail = tailSide.p(tailFractions.getValue(edge))
            val head = headSide.p(headFractions.getValue(edge)) + headSide.unitNormal * TIP_LENGTH
            val route = routeThrough(
                listOf(tail) + edge.waypoints + listOf(head),
                tailSide.unitNormal, headSide.unitNormal
            )
            routesByEdge[edge] = route
            val thickness = if (traced) RIBBON_THICKNESS + 2f else RIBBON_THICKNESS
            val ribbon = Area(
                BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(route.path)
            )
            val tangent = route.endTangent
            val tipTransform = AffineTransform().apply {
                translate(route.end.x, route.end.y)
                rotate(atan2(tangent.x, -tangent.y))
            }
            ribbon.add(Area(tipTransform.createTransformedShape(arrowTip)))
            PPath.Double(ribbon, null).apply {
                paint = if (traced) palette.receptiveFieldTrace else palette.connectionLine
                transparency = if (traced) 0.8f else 0.5f
                pickable = false
                edgeLayer.addChild(this)
            }
            val satellitesByOp = satellitesByEdge[edge]?.associateBy { it.op } ?: emptyMap()
            for ((i, op) in edge.ops.withIndex()) {
                val slotT = (i + 1).toDouble() / (edge.ops.size + 1)
                val satellite = satellitesByOp[op]
                val at = if (satellite != null) {
                    route.pointAt(satelliteT(route, satellite.tile, slotT, placedObstacles))
                } else {
                    route.pointAt(slotT)
                }
                if (satellite != null) {
                    satellite.tile.x = at.x - satellite.tile.width / 2
                    satellite.tile.y = at.y - satellite.tile.height / 2
                    placedObstacles.add(satellite.tile)
                    tileNodesById.getValue(satellite.tile.id).syncLayout()
                }
                if (op !in glyphsByOp) {
                    OpGlyphNode(op).apply {
                        if (satellite != null) {
                            setOffset(satellite.tile.x + satellite.tile.width, satellite.tile.y + satellite.tile.height)
                        } else {
                            setOffset(at.x, at.y)
                        }
                        glyphsByOp[op] = this
                        edgeLayer.addChild(this)
                    }
                }
            }
        }
        currentStepOp?.let { glyphsByOp[it]?.glowing = true }
    }

    /** The glyph rendered for [op], if any edge carries it. */
    fun glyphFor(op: TensorOp): OpGlyphNode? = glyphsByOp[op]

    /**
     * Micro-stepping render state: glows [currentOp]'s glyph and dims every tile in [stale]
     * (the not-yet-recomputed half of the pass). Dimming is pure transparency over the cached
     * rasters — no reshading. Pass (null, empty) at a step boundary to clear.
     */
    fun syncStepState(currentOp: TensorOp?, stale: Set<TensorTile>) {
        currentStepOp = currentOp
        staleTiles = stale
        glyphsByOp.values.forEach { it.glowing = it.op == currentOp }
        tileNodes.forEach {
            it.raster.transparency = if (it.tile in stale) STALE_TRANSPARENCY else 1f
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

        override fun mouseWheelRotated(event: PInputEvent) {
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val tile = scene.tileAt(point.x, point.y) as? DeckTile ?: return
            val next = (tile.selectedSlice + event.wheelRotation).mod(tile.slices)
            tile.selectedSlice = next
            tileNodesById.getValue(tile.id).syncLabel()
            refreshDirtyTiles()
            event.isHandled = true
        }

        override fun mouseMoved(event: PInputEvent) {
            val target = canvas ?: return
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val glyph = glyphsByOp.values.firstOrNull { it.containsScenePoint(point.x, point.y) }
            if (glyph != null) {
                target.toolTipText = glyph.op.toString()
                return
            }
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
        private const val DECK_STEP = 4.0
        private const val MAX_BACK_CARDS = 5
        private const val GLYPH_RADIUS = 7.0
        private const val STALE_TRANSPARENCY = 0.35f
        private const val RIBBON_THICKNESS = 5f
        private val TIP_LENGTH = RIBBON_THICKNESS * 2 * sin60deg
        private val SATELLITE_NUDGES = doubleArrayOf(0.0, -0.08, 0.08, -0.16, 0.16, -0.24, 0.24, -0.32, 0.32)
    }
}
