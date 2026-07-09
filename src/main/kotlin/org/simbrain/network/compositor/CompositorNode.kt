package org.simbrain.network.compositor

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.tensor.op.ReLUOp
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.util.*
import org.simbrain.util.piccolo.SvgIconNode
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
 * Renders a [CompositorScene] as one Piccolo node: tile rasters ([TilePatchNode] children that
 * shade values straight to screen resolution at paint time), data-flow edges, the logit-lens
 * strip, and the interior interactions — click and marquee selection, drag-move, double-click
 * trace, and hover value tooltips.
 *
 * Call [refreshDirtyTiles] on the EDT after the compute thread publishes a token; [relayout]
 * after geometry changes; [refreshTheme] on palette/theme switches.
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
    private val routesByEdge = HashMap<FlowEdge, BezierRoute>()

    private val arrowTip = Path2D.Double().apply {
        moveTo(0.0, -TIP_LENGTH)
        lineTo(RIBBON_THICKNESS.toDouble(), 0.0)
        lineTo(-RIBBON_THICKNESS.toDouble(), 0.0)
        closePath()
    }

    private var currentStepOp: TensorOp? = null

    private var staleTiles: Set<TensorTile> = emptySet()

    private inner class TileNode(val tile: TensorTile) : PNode() {
        /** Dimmed offset cards behind the live front card — a head deck's slices or a layer stack. */
        val backCards: List<PPath> = run {
            val depth = when {
                tile is DeckTile && tile.slices > 1 -> tile.slices - 1
                tile is LayerStacked && tile.stackLayers.size > 1 -> tile.stackLayers.size - 1
                else -> 0
            }
            (minOf(depth, MAX_BACK_CARDS) downTo 1).map { i ->
                PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
                    pickable = false
                    setOffset(-DECK_STEP * i, -DECK_STEP * i)
                }.also { addChild(it) }
            }
        }
        val raster = TilePatchNode(tile).apply {
            setBounds(0.0, 0.0, tile.width, tile.height)
        }.also { addChild(it) }
        val border = PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
            paint = null
        }.also { addChild(it) }
        val label = PText(tile.title).apply {
            font = Theme.tiny
        }.also { addChild(it) }

        /** Cursor at the tile's live row: the current token's row, or a cache's write frontier. */
        val liveMarker = PPath.Double(Path2D.Double().apply {
            moveTo(-LIVE_MARKER_SIZE, -LIVE_MARKER_SIZE * 0.7)
            lineTo(0.0, 0.0)
            lineTo(-LIVE_MARKER_SIZE, LIVE_MARKER_SIZE * 0.7)
            closePath()
        }, null).apply {
            pickable = false
            visible = false
        }.also { addChild(it) }

        /** The activation op producing this tile, shown as a corner badge instead of an edge glyph. */
        val activationOp: TensorOp? = scene.graph?.writer(tile.id)
            ?.takeIf { it is ReLUOp }

        val badge: PPath? = activationOp?.let { op ->
            PPath.createEllipse(-BADGE_RADIUS, -BADGE_RADIUS, 2 * BADGE_RADIUS, 2 * BADGE_RADIUS).apply {
                pickable = false
                setOffset(tile.width, 0.0)
                addChild(SvgIconNode(opIcon(op)!!, BADGE_ICON).apply {
                    setOffset(-BADGE_ICON / 2, -BADGE_ICON / 2)
                })
            }.also { addChild(it) }
        }

        var badgeGlowing = false
            set(value) {
                if (field == value) return
                field = value
                syncHighlight()
            }

        init {
            syncLayout()
            syncHighlight()
            syncDim()
        }

        fun syncLayout() {
            setOffset(tile.x, tile.y)
            label.setOffset(0.0, tile.height + 3.0)
            syncLabel()
            syncLiveRow()
        }

        fun syncLiveRow() {
            val row = tile.liveRow
            liveMarker.visible = row in 0 until tile.rows
            if (liveMarker.visible) {
                liveMarker.setOffset(0.0, (row + 0.5) / tile.rows * tile.height)
            }
        }

        fun syncLabel() {
            val base = when (tile) {
                is DeckTile -> tile.sliceLabel?.invoke(tile.selectedSlice)
                    ?: "${tile.title} · head ${tile.selectedSlice}"
                is AttentionTile -> "${tile.title} · head ${tile.selectedHead}"
                else -> tile.title
            }
            val layer = (tile as? LayerStacked)?.takeIf { it.stackLayers.size > 1 }?.shownLayer
            label.text = if (layer != null && layer >= 0) "$base · layer $layer" else base
        }

        fun syncDim() {
            transparency = if (tile.dimmed) DIM_TRANSPARENCY else 1f
        }

        fun syncHighlight() {
            val palette = NetworkTheme.current
            label.textPaint = palette.valueText
            val highlighted = tile == scene.traceFocus || tile in scene.tracedTiles ||
                tile in scene.selection || tile in scene.highlightedTiles
            val thickness = if (highlighted || tile.kind == TileKind.WEIGHT) 2f else 1f
            border.stroke = if (tile.magnified && !highlighted) {
                BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(3f, 3f), 0f)
            } else {
                BasicStroke(thickness)
            }
            border.strokePaint = when {
                tile == scene.traceFocus -> palette.sourceHandle
                tile in scene.highlightedTiles -> palette.sourceHandle
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
            badge?.let {
                it.paint = palette.canvasBackground
                it.strokePaint = if (badgeGlowing) palette.sourceHandle else palette.connectionLine
                it.stroke = BasicStroke(if (badgeGlowing) 2.5f else 1f)
            }
            liveMarker.paint = palette.sourceHandle
            liveMarker.strokePaint = null
        }

        fun badgeContains(sceneX: Double, sceneY: Double) = activationOp != null &&
            abs(sceneX - (tile.x + tile.width)) <= BADGE_RADIUS && abs(sceneY - tile.y) <= BADGE_RADIUS
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

    /** Invoked as the pointer moves across tiles (null between tiles); hosts hang previews on it. */
    var onTileHover: ((TensorTile?) -> Unit)? = null

    init {
        rebuildEdges()
        relayout()
        refreshDirtyTiles()
        addInputEventListener(InteriorInputHandler())
    }

    /** Repaints tiles whose content moved since their last shade; actual shading happens at paint. */
    fun refreshDirtyTiles() {
        tileNodes.forEach {
            it.raster.syncContent()
            it.syncLiveRow()
        }
        lensRows.forEach { it.refresh() }
    }

    /** Re-derives node offsets, edges, lens placement, and bounds from tile rects. */
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
        // Edge geometry can reach past the tiles (waypoint lanes, arrowheads); keep it inside.
        val edgeBounds = edgeLayer.fullBoundsReference
        if (edgeBounds.width > 0 && edgeBounds.height > 0) {
            bounds.add(Rectangle2D.Double(edgeBounds.x, edgeBounds.y, edgeBounds.width, edgeBounds.height))
        }
        background.reset()
        background.append(
            Rectangle2D.Double(
                bounds.x - MARGIN - LENS_SPACE, bounds.y - MARGIN,
                bounds.width + 2 * MARGIN + LENS_SPACE, bounds.height + 2 * MARGIN
            ), false
        )
        onLayoutChanged?.invoke()
    }

    /** Re-runs the palette over every tile — data untouched, geometry untouched. */
    fun refreshTheme() {
        val palette = NetworkTheme.current
        background.paint = palette.canvasBackground
        background.strokePaint = palette.subnetOutline
        tileNodes.forEach {
            it.raster.markStale()
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
     * A small operation glyph strung on a data-flow edge: a circled SVG symbol in the app icon
     * style for the known ops (derived from the plan, never hand-wired), a text pill for
     * anything without an icon yet. Fused ops render as a stage strip — one icon per applied
     * transform, in order, separated by dividers ([glyphStages]) — and each incoming edge
     * attaches at the stage whose pin consumes it.
     */
    inner class OpGlyphNode(val op: TensorOp) : PNode() {
        private val stages = glyphStages(op)
        val stageCount = stages?.size ?: 1
        val shape: PPath = when {
            stages == null -> PPath.createRoundRectangle(
                -GLYPH_RADIUS * 1.6, -GLYPH_RADIUS * 0.8,
                GLYPH_RADIUS * 3.2, GLYPH_RADIUS * 1.6, 4.0, 4.0
            )
            stageCount == 1 -> PPath.createEllipse(-GLYPH_RADIUS, -GLYPH_RADIUS, 2 * GLYPH_RADIUS, 2 * GLYPH_RADIUS)
            else -> PPath.createRoundRectangle(
                -GLYPH_RADIUS * stageCount, -GLYPH_RADIUS,
                2 * GLYPH_RADIUS * stageCount, 2 * GLYPH_RADIUS,
                2 * GLYPH_RADIUS, 2 * GLYPH_RADIUS
            )
        }.also { addChild(it) }
        private val dividers: List<PPath> = (1 until stageCount).map { i ->
            val x = (2 * i - stageCount) * GLYPH_RADIUS
            PPath.createLine(x, -GLYPH_RADIUS, x, GLYPH_RADIUS).also { addChild(it) }
        }
        private val text = if (stages == null) PText(op.name.substringAfterLast('.')).apply {
            font = Theme.tiny
        }.also { addChild(it) } else null

        init {
            stages?.forEachIndexed { i, stage ->
                addChild(SvgIconNode(stage.icon, GLYPH_ICON).apply {
                    setOffset(stageCenterX(i) - GLYPH_ICON / 2, -GLYPH_ICON / 2)
                })
            }
        }

        /** Center of stage [i] relative to the glyph's own center. */
        fun stageCenterX(i: Int) = (2 * i + 1 - stageCount) * GLYPH_RADIUS

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
            dividers.forEach {
                it.strokePaint = palette.connectionLine
                it.stroke = BasicStroke(1f)
            }
            text?.let {
                it.textPaint = palette.valueText
                it.setOffset(-it.width / 2, -it.height / 2)
            }
        }

        fun containsScenePoint(sceneX: Double, sceneY: Double): Boolean {
            val reachX = GLYPH_RADIUS * if (stages != null) stageCount.toDouble() else 1.8
            val reachY = GLYPH_RADIUS * if (stages != null) 1.0 else 1.8
            return abs(sceneX - xOffset) <= reachX && abs(sceneY - yOffset) <= reachY
        }
    }

    /** The rectangle an edge attaches to: a tile's rect, or a box around a junction's glyph strip. */
    private val FlowEndpoint.endpointBounds: Rectangle2D
        get() = when (this) {
            is TensorTile -> bounds
            is OpVertex -> {
                val n = glyphStages(op)?.size ?: 1
                Rectangle2D.Double(x - GLYPH_RADIUS * n, y - GLYPH_RADIUS, 2 * GLYPH_RADIUS * n, 2 * GLYPH_RADIUS)
            }
        }

    /**
     * Where an edge meets an endpoint: the attach rectangle, and — for a pin on one stage of a
     * glyph strip — which of its vertical sides are interior (shared with a neighboring stage)
     * and so closed to arrows; an arrow through a divider would read as piercing the other stage.
     */
    private class AttachRect(val rect: Rectangle2D, val interiorLeft: Boolean = false, val interiorRight: Boolean = false) {
        fun facingSide(guide: Point2D): Line2D = rect.outlines.toList().filter { side ->
            val n = side.unitNormal
            !(n.x < -0.5 && interiorLeft) && !(n.x > 0.5 && interiorRight)
        }.maxBy { it.unitNormal dot (guide - it.midPoint).norm }
    }

    /** The pin of stage [stage] on a junction's glyph strip. */
    private fun stagePin(vertex: OpVertex, stage: Int): AttachRect {
        val n = glyphStages(vertex.op)?.size ?: 1
        val cx = vertex.x + (2 * stage + 1 - n) * GLYPH_RADIUS
        return AttachRect(
            Rectangle2D.Double(cx - GLYPH_RADIUS, vertex.y - GLYPH_RADIUS, 2 * GLYPH_RADIUS, 2 * GLYPH_RADIUS),
            interiorLeft = stage > 0, interiorRight = stage < n - 1,
        )
    }

    /**
     * Where [edge] enters its target: the pin of the glyph stage consuming the arrival port for
     * junction targets — q and k land on the multiply, cos/sin on the rotation — falling back to
     * the whole endpoint when the pin is unknown or ambiguous.
     */
    private fun headAttach(edge: FlowEdge): AttachRect {
        val vertex = edge.to as? OpVertex ?: return AttachRect(edge.to.endpointBounds)
        val port = edge.toPort ?: return AttachRect(vertex.endpointBounds)
        val graph = scene.graph ?: return AttachRect(vertex.endpointBounds)
        val stage = stageForInput(vertex.op, port) { graph.alias(it) } ?: return AttachRect(vertex.endpointBounds)
        return stagePin(vertex, stage)
    }

    /** Where [edge] leaves its source: a fused op's result exits from its last stage. */
    private fun tailAttach(edge: FlowEdge): AttachRect {
        val vertex = edge.from as? OpVertex ?: return AttachRect(edge.from.endpointBounds)
        val n = glyphStages(vertex.op)?.size ?: 1
        return if (n > 1) stagePin(vertex, n - 1) else AttachRect(vertex.endpointBounds)
    }

    /**
     * Junction vertices the layout didn't position (scenes without a layout pass) settle at the
     * centroid of their neighbors, so they always render somewhere sensible.
     */
    private fun placeLooseVertices() {
        repeat(2) {
            for (vertex in scene.opVertices) {
                if (vertex.placed) continue
                val neighbors = scene.edges.mapNotNull { edge ->
                    when {
                        edge.from === vertex -> edge.to
                        edge.to === vertex -> edge.from
                        else -> null
                    }
                }.filter { it is TensorTile || (it is OpVertex && it.placed) }
                if (neighbors.isEmpty()) continue
                vertex.x = neighbors.map { it.endpointBounds.centerX }.average()
                vertex.y = neighbors.map { it.endpointBounds.centerY }.average()
            }
        }
    }

    /**
     * Attach fractions along each endpoint side, spread so several curves sharing a side fan out
     * instead of stacking on the midpoint. Curves are ordered along the side by where their far
     * endpoint projects onto it, so the fan never crosses itself.
     */
    private fun attachFractions(
        sideOf: (FlowEdge) -> Line2D,
        endpointOf: (FlowEdge) -> FlowEndpoint,
        guideOf: (FlowEdge) -> Point2D,
    ): Map<FlowEdge, Double> {
        val fractions = HashMap<FlowEdge, Double>()
        val bySide = scene.edges.groupBy { endpointOf(it) to sideOf(it).midPoint.let { m -> m.x to m.y } }
        for ((_, group) in bySide) {
            val ordered = group.sortedBy { sideOf(it).projectionFraction(guideOf(it)) }
            ordered.forEachIndexed { j, edge -> fractions[edge] = (j + 1.0) / (ordered.size + 1.0) }
        }
        return fractions
    }

    /**
     * Slides a satellite along its curve away from the op's nominal slot until it stops
     * overlapping already-placed tiles; when every pocket is blocked, settles for the candidate
     * with the least overlap.
     */
    private fun satelliteT(route: BezierRoute, tile: TensorTile, slotT: Double, obstacles: List<TensorTile>): Double {
        var bestT = slotT
        var bestOverlap = Double.MAX_VALUE
        for (offset in SATELLITE_NUDGES) {
            val t = slotT + offset
            if (t < 0.1 || t > 0.9) continue
            val at = route.pointAt(t)
            val box = Rectangle2D.Double(
                at.x - tile.width / 2 - 12, at.y - tile.height / 2 - 12,
                tile.width + 24, tile.height + 24
            )
            val overlap = obstacles.sumOf { o ->
                val w = minOf(box.maxX, o.x + o.width) - maxOf(box.x, o.x)
                val h = minOf(box.maxY, o.y + o.height) - maxOf(box.y, o.y)
                if (w > 0 && h > 0) w * h else 0.0
            }
            if (overlap == 0.0) return t
            if (overlap < bestOverlap) {
                bestOverlap = overlap
                bestT = t
            }
        }
        return bestT
    }

    private fun rebuildEdges() {
        val palette = NetworkTheme.current
        edgeLayer.removeAllChildren()
        glyphsByOp.clear()
        routesByEdge.clear()
        placeLooseVertices()
        val satellitesByEdge = scene.satellites.groupBy { it.edge }
        val satelliteTiles = scene.satellites.map { it.tile }.toSet()
        val placedObstacles = scene.tiles.filter { it !in satelliteTiles }.toMutableList()
        val badgedOps = tileNodes.mapNotNull { it.activationOp }.toSet()
        for (vertex in scene.opVertices) {
            OpGlyphNode(vertex.op).apply {
                setOffset(vertex.x, vertex.y)
                if (vertex.dimmed) transparency = DIM_TRANSPARENCY
                glyphsByOp[vertex.op] = this
                edgeLayer.addChild(this)
            }
        }
        val tailRects = scene.edges.associateWith { tailAttach(it) }
        val headRects = scene.edges.associateWith { headAttach(it) }
        val tailSides = scene.edges.associateWith {
            tailRects.getValue(it).facingSide(it.waypoints.firstOrNull() ?: headRects.getValue(it).rect.center)
        }
        val headSides = scene.edges.associateWith {
            headRects.getValue(it).facingSide(it.waypoints.lastOrNull() ?: tailRects.getValue(it).rect.center)
        }
        val tailFractions = attachFractions(
            { tailSides.getValue(it) }, { it.from },
            { it.waypoints.firstOrNull() ?: headRects.getValue(it).rect.center })
        val headFractions = attachFractions(
            { headSides.getValue(it) }, { it.to },
            { it.waypoints.lastOrNull() ?: tailRects.getValue(it).rect.center })
        // Undimmed edges render (and claim shared bead glyphs) first, so an op both limbs share
        // shows at full strength on the active limb's edge.
        for (edge in scene.edges.sortedBy { it.dimmed }) {
            val traced = edge in scene.tracedEdges
            val emphasized = edge in scene.emphasizedEdges
            val tailSide = tailSides.getValue(edge)
            val headSide = headSides.getValue(edge)
            val tail = tailSide.p(tailFractions.getValue(edge))
            val head = headSide.p(headFractions.getValue(edge)) + headSide.unitNormal * TIP_LENGTH
            val route = routeThrough(
                listOf(tail) + edge.waypoints + listOf(head),
                tailSide.unitNormal, headSide.unitNormal
            )
            routesByEdge[edge] = route
            val ribbonColor = when {
                edge.dimmed -> palette.connectionLine
                traced -> palette.receptiveFieldTrace
                emphasized -> palette.sourceHandle
                else -> palette.connectionLine
            }
            val thickness = if (traced || emphasized) RIBBON_THICKNESS + 2f else RIBBON_THICKNESS
            val stroke = BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
            if (edge.stranded) {
                // Head-stacked flow renders as a strand fan echoing the deck's back cards.
                for (i in 2 downTo 1) {
                    val strandOffset = AffineTransform.getTranslateInstance(-DECK_STEP * i, -DECK_STEP * i)
                    PPath.Double(strandOffset.createTransformedShape(stroke.createStrokedShape(route.path)), null).apply {
                        paint = ribbonColor
                        transparency = if (edge.dimmed) 0.06f else 0.2f
                        pickable = false
                        edgeLayer.addChild(this)
                    }
                }
            }
            val ribbon = Area(stroke.createStrokedShape(route.path))
            val tangent = route.endTangent
            val tipTransform = AffineTransform().apply {
                translate(route.end.x, route.end.y)
                rotate(atan2(tangent.x, -tangent.y))
            }
            ribbon.add(Area(tipTransform.createTransformedShape(arrowTip)))
            PPath.Double(ribbon, null).apply {
                paint = ribbonColor
                transparency = when {
                    edge.dimmed -> 0.15f
                    traced -> 0.8f
                    emphasized -> 0.65f
                    else -> 0.5f
                }
                pickable = false
                edgeLayer.addChild(this)
            }
            val satellitesByOp = satellitesByEdge[edge]?.associateBy { it.op } ?: emptyMap()
            val visibleOps = edge.ops.filter { it !in badgedOps }
            // Short curves can't fit every bead without overlap: sample evenly, keep the rest
            // reachable through the shown ones' tooltips. Satellite ops always render.
            val fit = (route.length / (GLYPH_RADIUS * 3.0)).toInt().coerceAtLeast(1)
            val shownIndices = if (visibleOps.size <= fit) visibleOps.indices.toSet() else {
                (0 until fit).map { it * (visibleOps.size - 1) / (fit - 1).coerceAtLeast(1) }.toSet()
            }
            for ((i, op) in visibleOps.withIndex()) {
                val slotT = (i + 1).toDouble() / (visibleOps.size + 1)
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
                if (op !in glyphsByOp && (satellite != null || i in shownIndices)) {
                    OpGlyphNode(op).apply {
                        if (satellite != null) {
                            setOffset(satellite.tile.x + satellite.tile.width, satellite.tile.y + satellite.tile.height)
                        } else {
                            setOffset(at.x, at.y)
                        }
                        if (edge.dimmed) transparency = DIM_TRANSPARENCY
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
     * Re-renders layer-stack state after the scene's layer selector ran: limb dimming, per-layer
     * labels, the depth strip's highlighted rows, and freshly flipped tile data.
     */
    fun refreshStackState() {
        tileNodes.forEach {
            it.syncDim()
            it.syncLabel()
            it.syncHighlight()
        }
        rebuildEdges()
        refreshDirtyTiles()
    }

    private fun selectLayer(layer: Int) {
        val selector = scene.layerSelector ?: return
        selector(layer)
        refreshStackState()
    }

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
            it.badgeGlowing = currentOp != null && it.activationOp == currentOp
        }
    }

    private inner class InteriorInputHandler : PBasicInputEventHandler() {

        private var mode = Mode.NONE
        private var pressPoint: Point2D? = null
        private var marqueeAdditive = false
        private var draggedVertex: OpVertex? = null

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
            val vertex = scene.opVertices.firstOrNull {
                glyphsByOp[it.op]?.containsScenePoint(point.x, point.y) == true
            }
            if (vertex != null) {
                draggedVertex = vertex
                mode = Mode.MOVE_VERTEX
                pressPoint = point
                event.isHandled = true
                return
            }
            if (tile != null) {
                if (event.isShiftDown) {
                    scene.selection.toggle(tile)
                } else if (tile !in scene.selection) {
                    scene.selection.set(listOf(tile))
                }
                scene.layerOfTile?.invoke(tile)?.let { selectLayer(it) }
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
                Mode.MOVE_VERTEX -> {
                    val vertex = draggedVertex ?: return
                    val delta = event.getDeltaRelativeTo(this@CompositorNode)
                    vertex.x += delta.width
                    vertex.y += delta.height
                    vertex.placed = true
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
            draggedVertex = null
        }

        override fun mouseWheelRotated(event: PInputEvent) {
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val tile = scene.tileAt(point.x, point.y) ?: return
            if (scene.layerOfTile?.invoke(tile) != null) {
                // Wheeling over the depth strip walks the selected layer.
                selectLayer(scene.selectedLayer + event.wheelRotation)
                event.isHandled = true
                return
            }
            when (tile) {
                is DeckTile -> {
                    tile.selectedSlice = (tile.selectedSlice + event.wheelRotation).mod(tile.slices)
                    scene.onHeadSelected?.invoke(tile, tile.selectedSlice)
                }
                is AttentionTile -> {
                    tile.selectedHead = (tile.selectedHead + event.wheelRotation).mod(tile.numHeads)
                    scene.onHeadSelected?.invoke(tile, tile.selectedHead)
                }
                else -> return
            }
            tileNodes.forEach { it.syncLabel() }
            refreshDirtyTiles()
            event.isHandled = true
        }

        override fun mouseMoved(event: PInputEvent) {
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val tile = scene.tileAt(point.x, point.y)
            onTileHover?.invoke(tile)
            val target = canvas ?: return
            val glyph = glyphsByOp.values.firstOrNull { it.containsScenePoint(point.x, point.y) }
            if (glyph != null) {
                target.toolTipText = glyph.op.toString()
                return
            }
            val badged = tileNodes.firstOrNull { it.badgeContains(point.x, point.y) }
            if (badged != null) {
                target.toolTipText = badged.activationOp.toString()
                return
            }
            val cell = tile?.cellAt(point.x, point.y)
            target.toolTipText = if (tile != null && cell != null) cellReadout(tile, cell.first, cell.second) else null
        }

        override fun mouseExited(event: PInputEvent) {
            onTileHover?.invoke(null)
            canvas?.toolTipText = null
        }

        private fun rectBetween(a: Point2D, b: Point2D) = Rectangle2D.Double(
            minOf(a.x, b.x), minOf(a.y, b.y),
            kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.y - b.y)
        )
    }

    private enum class Mode { NONE, MOVE, MOVE_VERTEX, MARQUEE }

    companion object {
        private const val MARGIN = 40.0
        private const val LENS_SPACE = 220.0
        private const val DECK_STEP = 4.0
        private const val MAX_BACK_CARDS = 5
        private const val GLYPH_RADIUS = 9.0
        private const val GLYPH_ICON = 12.0
        private const val BADGE_RADIUS = 10.0
        private const val BADGE_ICON = 13.0
        private const val STALE_TRANSPARENCY = 0.35f
        private const val DIM_TRANSPARENCY = 0.3f
        private const val LIVE_MARKER_SIZE = 6.0
        private const val RIBBON_THICKNESS = 5f
        private val TIP_LENGTH = RIBBON_THICKNESS * 2 * sin60deg
        private val SATELLITE_NUDGES = doubleArrayOf(0.0) +
            (1..7).flatMap { listOf(-it * 0.06, it * 0.06) }.toDoubleArray()
    }
}

