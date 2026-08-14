package org.simbrain.network.compositor

import org.piccolo2d.PCanvas
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.simbrain.network.gui.ArrowDirection
import org.simbrain.network.gui.createArrowButton
import org.simbrain.network.gui.isPanKeyDown
import org.simbrain.network.llm.HeadwiseNormRopeOp
import org.simbrain.network.tensor.op.*
import org.simbrain.util.*
import org.simbrain.util.piccolo.RasterCachedNode
import org.simbrain.util.piccolo.SvgIconNode
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.*
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
    private val probabilitySnapshot: () -> TokenProbabilitySnapshot? = { null },
    private val probabilityCardStyle: TokenProbabilityCardStyle = TokenProbabilityCardStyle(),
    private val probabilityCardPosition: ((CompositorScene, Rectangle2D, TokenProbabilityCardNode) -> Point2D)? = null,
    private val onProbabilityCardMoved: ((Double, Double) -> Unit)? = null,
    private val cellReadout: (TensorTile, Int, Int) -> String = { tile, row, col ->
        "Cell [$row, $col] = ${"%.4f".format(tile.valueAt(row, col))}"
    },
) : PNode() {

    private fun opTooltip(op: TensorOp): String {
        val (title, description) = op.displayTooltip().split("\n", limit = 2).let {
            it.first() to it.getOrElse(1) { "" }
        }
        val parallel = opParallelism(op)
        val parallelDescription = when {
            op is HeadwiseNormRopeOp -> "This happens independently in each of $parallel attention heads."
            parallel > 1 -> "This happens independently in $parallel attention heads."
            else -> ""
        }
        return "Op: $title\nShape: ${op.displayShape()}\n" +
            listOf(description, parallelDescription).filter(String::isNotEmpty).joinToString(" ")
    }

    private fun dataTooltipTitle(tile: TensorTile) = when (tile) {
        is AttentionTile -> "Data: attention weights"
        else -> "Data: ${tile.title}" + if (tile.kind == TileKind.WEIGHT) " matrix" else ""
    }

    private val background = PPath.createRectangle(0.0, 0.0, 1.0, 1.0).apply {
        paint = NetworkTheme.current.canvasBackground
        strokePaint = NetworkTheme.current.subnetOutline
    }.also { addChild(it) }

    /** Bounds of the compositor's background border in this node's parent coordinates. */
    fun outlineBoundsInParentCoordinates(): PBounds = PBounds(background.fullBoundsReference).also(::localToParent)

    /**
     * Fans and edges live in one raster-cached chrome layer: this vector work only changes on
     * relayout, flips, selection, and theme switches, so between changes each frame blits one
     * image instead of re-rasterizing every antialiased ribbon, strand, and card.
     */
    private val chromeLayer = RasterCachedNode().also { addChild(it) }

    private val probabilityCardOverlay = scene.overlays.firstOrNull { it.id == PROBABILITY_CARD_ID }
        ?: InteriorOverlay(PROBABILITY_CARD_ID, probabilityCardStyle.width, probabilityCardStyle.height).also(scene::addOverlay)

    private val probabilityCard = TokenProbabilityCardNode(tokenLabel, probabilityCardStyle).also {
        it.onMoved = {
            probabilityCardOverlay.x = it.offset.x
            probabilityCardOverlay.y = it.offset.y
            onProbabilityCardMoved?.invoke(it.offset.x, it.offset.y)
            relayout()
            onLayoutChanged?.invoke()
        }
        addChild(it)
    }

    /**
     * Tile card fans paint UNDER the edges, front tiles above them: a pipe runs over the card
     * ladder (strand i visibly on card i) and terminates at the open card's border, instead of
     * vanishing behind an opaque fan.
     */
    private val fanLayer = PNode().also { chromeLayer.addChild(it) }

    private val edgeLayer = PNode().also { chromeLayer.addChild(it) }

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

    /** Pager containers, so the interior handler leaves their clicks to the arrow buttons. */
    private val pagerNodes = mutableListOf<PNode>()

    /** A layer-flip arrow; the adjacent state indicator shows the currently selected layer. */
    private inner class PagerNode(direction: ArrowDirection, onClick: () -> Unit) : PNode() {
        private val arrow = createArrowButton(direction, PAGER_ARROW) { onClick() }.also { addChild(it) }

        init {
            pagerNodes.add(this)
        }

        val rowWidth: Double get() = arrow.fullBoundsReference.width
    }

    private inner class TileNode(val tile: TensorTile) : PNode() {
        /** This tile's card fan, a sibling in [fanLayer] so edges route over the cards. */
        val fan = PNode().also { fanLayer.addChild(it) }

        /**
         * Anonymous cards behind a head deck's front card, one per hidden sibling — the pages
         * of the open layer card: 15 behind the attention tile, 7 behind each KV cache, so the
         * GQA 2:1 stands in the depths.
         */
        val headCards: List<PPath> = run {
            val depth = when {
                tile is DeckTile && tile.slices > 1 -> tile.slices - 1
                tile is AttentionTile && tile.numHeads > 1 -> tile.numHeads - 1
                else -> 0
            }
            (depth downTo 1).map { i ->
                PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
                    pickable = false
                    setOffset(-HEAD_STEP * i, -HEAD_STEP * i)
                }.also { fan.addChild(it) }
            }
        }

        /**
         * Layer cards on the shared slot axis: every layer deck spans [CompositorScene.layerCount]
         * slots, with a card only at the layers this tile's stack owns — the skip-rhythm shows
         * WHICH layers have the piece, not just how many. The selected layer's card is accented,
         * the rest ghosted; head decks page their layer dimension through the pager instead.
         */
        val slotCards: List<Pair<Int, PPath>> = run {
            if (headCards.isNotEmpty()) return@run emptyList()
            val stack = (tile as? LayerStacked)?.stackLayers?.takeIf { it.size > 1 } ?: return@run emptyList()
            val span = maxOf(scene.layerCount, stack.max() + 1)
            stack.sorted().map { layer ->
                layer to PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
                    pickable = false
                    setOffset(-DECK_STEP * (span - layer), -DECK_STEP * (span - layer))
                }.also { fan.addChild(it) }
            }
        }
        val raster = TilePatchNode(tile).apply {
            setBounds(0.0, 0.0, tile.width, tile.height)
        }.also { addChild(it) }
        val border = PPath.createRectangle(0.0, 0.0, tile.width, tile.height).apply {
            paint = null
        }.also { addChild(it) }

        /** Boundary notches marking the substructure packed along the tile's axes (heads, chunks). */
        val ticks = PPath.Double(Path2D.Double(), null).apply {
            pickable = false
        }.also { addChild(it) }

        val blockTexts: List<PText> = tile.blockLabels.map { text ->
            PText(text).apply {
                font = Theme.tiny
                pickable = false
            }.also { addChild(it) }
        }

        /** Identity bars over the ticked segments — the chunk colors the slice edges wear. */
        val blockBars: List<PPath> = tile.blockLabels.indices.map {
            PPath.Double(Path2D.Double(), null).apply {
                pickable = false
            }.also { addChild(it) }
        }

        /** Caption line one: what the tile is, centered under it. */
        val label = PText(tile.title).apply {
            font = Theme.small
        }.also { addChild(it) }

        /** Layer pagers flanking the caption. */
        val layerPagers: Pair<PagerNode, PagerNode>? = (tile as? LayerStacked)
            ?.takeIf { it.stackLayers.size > 1 }?.let { stacked ->
                val left = PagerNode(ArrowDirection.LEFT) {
                    stacked.layerBefore(scene.selectedLayer)?.let { selectLayer(it) }
                }.also { addChild(it) }
                val right = PagerNode(ArrowDirection.RIGHT) {
                    stacked.layerAfter(scene.selectedLayer)?.let { selectLayer(it) }
                }.also { addChild(it) }
                left to right
            }

        /** Head pagers flanking the current head indicator. */
        val headPagers: Pair<PagerNode, PagerNode>? =
            if (tile is DeckTile && tile.slices > 1 || tile is AttentionTile && tile.numHeads > 1) {
                val left = PagerNode(ArrowDirection.LEFT) { stepHead(tile, -1) }.also { addChild(it) }
                val right = PagerNode(ArrowDirection.RIGHT) { stepHead(tile, 1) }.also { addChild(it) }
                left to right
            } else null

        /** Caption line two: where the tile is in its stacks, in a muted icon+number grammar. */
        val layerIcon: SvgIconNode? = layerPagers?.let {
            SvgIconNode("icons/stat-layers.svg", STATE_ICON).also { addChild(it) }
        }
        val layerText: PText? = layerPagers?.let {
            PText().apply {
                font = Theme.tiny
                pickable = false
            }.also { addChild(it) }
        }
        val headText: PText? = headPagers?.let {
            PText().apply {
                font = Theme.tiny
                pickable = false
            }.also { addChild(it) }
        }

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

        /** The activation op producing this tile, shown above the tile instead of on its edge. */
        val activationOp: TensorOp? = scene.graph?.writer(tile.id)
            ?.takeIf { it is ReLUOp }

        val badge: PPath? = activationOp?.let { op ->
            PPath.createEllipse(-BADGE_RADIUS, -BADGE_RADIUS, 2 * BADGE_RADIUS, 2 * BADGE_RADIUS).apply {
                pickable = false
                setOffset(tile.width / 2, -BADGE_RADIUS - OP_GLYPH_GAP)
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
            syncFanStyle()
            syncDim()
        }

        fun syncLayout() {
            setOffset(tile.x, tile.y)
            fan.setOffset(tile.x, tile.y)
            // Notches mark boundaries on both edges, dropping to one edge on tiles too thin for
            // opposing marks to stay visually separate.
            val marks = Path2D.Double()
            val columnTick = if (tile.fullHeightColumnTicks) tile.height else minOf(TICK_LENGTH, tile.height / 4)
            for (c in tile.columnTicks) {
                val tx = c.toDouble() / tile.cols * tile.width
                marks.moveTo(tx, 0.0)
                marks.lineTo(tx, columnTick)
                if (!tile.fullHeightColumnTicks && tile.height >= TICK_LENGTH * 4) {
                    marks.moveTo(tx, tile.height - columnTick)
                    marks.lineTo(tx, tile.height)
                }
            }
            val rowTick = minOf(TICK_LENGTH, tile.width / 4)
            for (r in tile.rowTicks) {
                val ty = r.toDouble() / tile.rows * tile.height
                marks.moveTo(0.0, ty)
                marks.lineTo(rowTick, ty)
                if (tile.width >= TICK_LENGTH * 4) {
                    marks.moveTo(tile.width - rowTick, ty)
                    marks.lineTo(tile.width, ty)
                }
            }
            ticks.reset()
            ticks.append(marks, false)
            if (blockTexts.isNotEmpty()) {
                val blockEdges = listOf(0) + tile.columnTicks + listOf(tile.cols)
                blockTexts.forEachIndexed { i, text ->
                    val cx = (blockEdges[i] + blockEdges[i + 1]) / 2.0 / tile.cols * tile.width
                    text.setOffset(cx - text.width / 2, -text.height - 5.0)
                }
                blockBars.forEachIndexed { i, bar ->
                    val x0 = blockEdges[i].toDouble() / tile.cols * tile.width
                    val x1 = blockEdges[i + 1].toDouble() / tile.cols * tile.width
                    bar.reset()
                    bar.append(Rectangle2D.Double(x0 + 1.0, -3.5, x1 - x0 - 2.0, 2.0), false)
                }
            }
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
            label.text = tile.title
            label.setOffset((tile.width - label.width) / 2, tile.height + 3.0)
            val muted = blend(NetworkTheme.current.valueText, NetworkTheme.current.canvasBackground, 0.65)
            layerText?.let {
                it.text = (tile as LayerStacked).shownLayer.takeIf { l -> l >= 0 }?.toString()
                    ?: scene.selectedLayer.toString()
                it.textPaint = muted
            }
            headText?.let {
                it.text = when (tile) {
                    is DeckTile -> "${tile.selectedSlice}/${tile.slices}"
                    is AttentionTile -> "${tile.selectedHead}/${tile.numHeads}"
                    else -> ""
                }
                it.textPaint = muted
            }
            val showLayerControls = scene.showLayerCards && layerPagers != null
            layerPagers?.let { (left, right) ->
                left.visible = showLayerControls
                right.visible = showLayerControls
            }
            layerIcon?.visible = showLayerControls
            layerText?.visible = showLayerControls

            // Stack layer and head controls when both are visible, keeping each selector readable.
            val rowTop = tile.height + 4.0 + label.height
            val gap = 3.0
            var stateX = 0.0
            var stateY = rowTop
            fun place(node: PNode, width: Double, height: Double) {
                node.setOffset(stateX, stateY + (PAGER_ROW_HEIGHT - height) / 2)
                stateX += width
            }
            if (showLayerControls) layerPagers?.let { (left, right) ->
                val rowWidth = left.rowWidth + gap + STATE_ICON + 1.0 + layerText!!.width + gap + right.rowWidth
                stateX = (tile.width - rowWidth) / 2
                place(left, left.rowWidth + gap, left.fullBoundsReference.height)
                place(layerIcon!!, STATE_ICON + 1.0, STATE_ICON)
                place(layerText!!, layerText.width + gap, layerText.height)
                place(right, right.rowWidth, right.fullBoundsReference.height)
                stateY += PAGER_ROW_HEIGHT
            }
            headPagers?.let { (left, right) ->
                val rowWidth = left.rowWidth + gap + headText!!.width + gap + right.rowWidth
                stateX = (tile.width - rowWidth) / 2
                place(left, left.rowWidth + gap, left.fullBoundsReference.height)
                place(headText!!, headText.width, headText.height)
                place(right, right.rowWidth, right.fullBoundsReference.height)
            }
        }

        fun headSelectorContains(sceneX: Double, sceneY: Double): Boolean {
            fun contains(node: PNode?) = node?.fullBoundsReference?.let { bounds ->
                sceneX in tile.x + bounds.minX..tile.x + bounds.maxX &&
                    sceneY in tile.y + bounds.minY..tile.y + bounds.maxY
            } ?: false
            return contains(headPagers?.first) || contains(headText) || contains(headPagers?.second)
        }

        fun headSelectorTooltip() = when (tile) {
            is DeckTile -> tile.sliceTooltip?.invoke(tile.selectedSlice)
                ?: "Current head: ${tile.selectedSlice}/${tile.slices}."
            is AttentionTile -> "Current query head: ${tile.selectedHead}/${tile.numHeads}."
            else -> null
        }

        fun syncDim() {
            val shown = scene.isShown(tile)
            visible = shown
            pickable = shown
            childrenPickable = shown
            fan.visible = shown
            transparency = if (tile.dimmed) DIM_TRANSPARENCY else 1f
            fan.transparency = if (tile.dimmed) DIM_TRANSPARENCY else 1f
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
            ticks.strokePaint = palette.imageBorder
            ticks.stroke = BasicStroke(1f)
            blockTexts.forEachIndexed { i, text ->
                text.textPaint = palette.chunkColors[i % palette.chunkColors.size]
            }
            blockBars.forEachIndexed { i, bar ->
                bar.paint = palette.chunkColors[i % palette.chunkColors.size]
                bar.strokePaint = null
            }
            badge?.let {
                it.paint = palette.canvasBackground
                it.strokePaint = if (badgeGlowing) palette.sourceHandle else palette.connectionLine
                it.stroke = BasicStroke(if (badgeGlowing) 2.5f else 1f)
            }
            // Two cursor meanings, two colors: the row in flight on history tiles wears the
            // accent; the write frontier on the caches wears the cross-time memory color.
            liveMarker.paint = if (tile.accumulatesHistory) palette.sourceHandle
                else blend(palette.coolNode, palette.connectionLine, 0.5)
            liveMarker.strokePaint = null
        }

        /**
         * Card-fan styling reads only the theme and the selected layer, never tile selection.
         * The cards live in the chrome raster, so this stays out of [syncHighlight] — selection
         * clicks must not invalidate that cache.
         */
        fun syncFanStyle() {
            val palette = NetworkTheme.current
            headCards.forEach {
                it.paint = palette.canvasBackground
                it.strokePaint = palette.imageBorder
                it.stroke = BasicStroke(1f)
            }
            for ((layer, card) in slotCards) {
                val selected = layer == scene.selectedLayer
                card.visible = scene.showLayerCards
                card.paint = palette.canvasBackground
                card.strokePaint = if (selected) palette.sourceHandle else palette.imageBorder
                card.stroke = BasicStroke(if (selected) 1.5f else 1f)
                card.transparency = if (selected) 1f else GHOST_CARD_TRANSPARENCY
            }
        }

        fun badgeContains(sceneX: Double, sceneY: Double) = activationOp != null &&
            abs(sceneX - (tile.x + tile.width / 2)) <= BADGE_RADIUS &&
            abs(sceneY - (tile.y - BADGE_RADIUS - OP_GLYPH_GAP)) <= BADGE_RADIUS
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

    /**
     * Selection rings around selected op glyphs. The glyphs themselves live in the chrome
     * raster, so restyling them per click would invalidate that cache; the rings are separate
     * nodes above it instead, like the tile borders.
     */
    private val opSelectionOverlay = PNode().apply { pickable = false }.also { addChild(it) }

    /** Step-walk readout pinned inside the outline's top-left corner; hidden with no status. */
    private val stepStatus = PText().apply {
        font = Theme.small
        textPaint = NetworkTheme.current.valueText
        pickable = false
        visible = false
    }.also { addChild(it) }

    private var stepStatusIsNotice = false

    private fun placeStepStatus() {
        val bounds = background.boundsReference
        stepStatus.setOffset(bounds.x + 14.0, bounds.y + 10.0)
    }

    private fun paintStepStatus() {
        val palette = NetworkTheme.current
        stepStatus.textPaint = if (stepStatusIsNotice) palette.spiking else palette.valueText
    }

    private var marquee: PPath? = null

    /** Invoked after every tier-1 relayout, e.g. so a host can persist tile positions. */
    var onLayoutChanged: (() -> Unit)? = null

    /** Invoked as the pointer moves across tiles (null between tiles); hosts hang previews on it. */
    var onTileHover: ((TensorTile?) -> Unit)? = null

    init {
        rebuildEdges()
        relayout()
        refreshDirtyTiles()
        probabilityCard.raiseToTop()
        addInputEventListener(InteriorInputHandler())
    }

    /** Repaints tiles whose content moved since their last shade; actual shading happens at paint. */
    fun refreshDirtyTiles() {
        tileNodes.forEach {
            it.raster.syncContent()
            it.syncLiveRow()
        }
        refreshLensRows()
        probabilityCard.refresh(probabilitySnapshot())
    }

    /** A disabled lens stops computing, so hide its rows rather than leaving stale readings up. */
    private fun refreshLensRows() {
        val lensOn = scene.lens?.enabled != false
        lensRows.forEach {
            it.visible = lensOn
            it.refresh()
        }
    }

    /** Re-derives node offsets, edges, return lanes, lens placement, and bounds from tile rects. */
    fun relayout() {
        tileNodes.forEach { it.syncLayout() }
        scene.deriveReturnWaypoints()
        rebuildEdges()
        for (row in lensRows) {
            val sourceId = scene.lens?.sources?.get(row.index)?.name ?: continue
            val tile = tileNodesById[sourceId]?.tile ?: continue
            row.setOffset(tile.x - LENS_SPACE, tile.y + tile.height / 2 - 8.0)
        }
        val bounds = scene.tiles.fold(null as Rectangle2D?) { acc, tile ->
            val r = Rectangle2D.Double(tile.x, tile.y, tile.width, tile.height + 34)
            acc?.also { it.add(r) } ?: r
        } ?: Rectangle2D.Double()
        // Edge geometry can reach past the tiles (waypoint lanes, arrowheads); keep it inside.
        val edgeBounds = edgeLayer.fullBoundsReference
        if (edgeBounds.width > 0 && edgeBounds.height > 0) {
            bounds.add(Rectangle2D.Double(edgeBounds.x, edgeBounds.y, edgeBounds.width, edgeBounds.height))
        }
        if (probabilityCardOverlay.x.isNaN() || probabilityCardOverlay.y.isNaN()) {
            val cardPosition = probabilityCardPosition?.invoke(scene, bounds, probabilityCard)
                ?: Point2D.Double(bounds.x - LENS_SPACE + MARGIN, bounds.maxY + 18.0)
            probabilityCardOverlay.x = cardPosition.x
            probabilityCardOverlay.y = cardPosition.y
        }
        probabilityCard.setOffset(probabilityCardOverlay.x, probabilityCardOverlay.y)
        val cardBounds = probabilityCard.fullBoundsReference
        bounds.add(Rectangle2D.Double(cardBounds.x, cardBounds.y, cardBounds.width, cardBounds.height))
        background.reset()
        background.append(
            Rectangle2D.Double(
                bounds.x - MARGIN - LENS_SPACE, bounds.y - MARGIN,
                bounds.width + 2 * MARGIN + LENS_SPACE, bounds.height + 2 * MARGIN
            ), false
        )
        placeStepStatus()
        syncOpSelectionOverlay()
        onLayoutChanged?.invoke()
    }

    /** Re-runs the palette over every tile — data untouched, geometry untouched. */
    fun refreshTheme() {
        val palette = NetworkTheme.current
        background.paint = palette.canvasBackground
        background.strokePaint = palette.subnetOutline
        paintStepStatus()
        tileNodes.forEach {
            it.raster.markStale()
            it.syncHighlight()
            it.syncFanStyle()
            it.syncLabel()
        }
        rebuildEdges()
        refreshLensRows()
        syncOpSelectionOverlay()
    }

    private fun syncHighlights() {
        tileNodes.forEach { it.syncHighlight() }
        rebuildEdges()
        syncOpSelectionOverlay()
    }

    /**
     * Selection only styles tile borders and the op rings — edge ribbons don't read it — so
     * click and marquee skip the edge rebuild and leave the chrome raster untouched.
     */
    private fun syncSelection() {
        tileNodes.forEach { it.syncHighlight() }
        syncOpSelectionOverlay()
    }

    private fun syncOpSelectionOverlay() {
        opSelectionOverlay.removeAllChildren()
        val palette = NetworkTheme.current
        for (item in scene.selection.selected) {
            val vertex = item as? OpVertex ?: continue
            if (!scene.isShown(vertex)) continue
            val b = vertex.endpointBounds
            opSelectionOverlay.addChild(
                PPath.createRoundRectangle(
                    b.x - OP_RING_PAD, b.y - OP_RING_PAD,
                    b.width + 2 * OP_RING_PAD, b.height + 2 * OP_RING_PAD,
                    2 * (GLYPH_RADIUS + OP_RING_PAD), 2 * (GLYPH_RADIUS + OP_RING_PAD)
                ).apply {
                    paint = null
                    strokePaint = palette.selectionHandle
                    stroke = BasicStroke(2f)
                    pickable = false
                }
            )
        }
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

        private fun stripShape(): PPath = when {
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
        }

        /**
         * One card per parallel per-head pass behind the glyph — the scores deck runs 16 deep,
         * the key-side norm+rope 8. Flat ops (projections, cache writes, gates) wear none, so
         * the fan's absence marks exactly which ops never see heads.
         */
        private val parallelCards: List<PPath> = (opParallelism(op) - 1 downTo 1).map { i ->
            stripShape().apply {
                pickable = false
                setOffset(-HEAD_STEP * i, -HEAD_STEP * i)
            }.also { addChild(it) }
        }

        val shape: PPath = stripShape().also { addChild(it) }
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
            parallelCards.forEach {
                it.paint = palette.canvasBackground
                it.strokePaint = palette.connectionLine
                it.stroke = BasicStroke(1f)
            }
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

    /** True when strands at this endpoint should register on its head-card ladder. */
    private fun fansIntoCards(e: FlowEndpoint): Boolean = when (e) {
        is DeckTile -> e.slices > 1
        is AttentionTile -> e.numHeads > 1
        is TensorTile -> false
        is OpVertex -> opParallelism(e.op) > 1
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
            if (!scene.isShown(vertex)) continue
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
            if (edge.dimmed && scene.hideDimmed) continue
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
            val ribbonColor = when {
                edge.dimmed -> palette.connectionLine
                traced -> palette.receptiveFieldTrace
                else -> palette.connectionLine
            }
            val thickness = if (traced) RIBBON_THICKNESS + 2f else RIBBON_THICKNESS
            val stroke = BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
            val tailDir = (tailSide.p2 - tailSide.p1).norm
            val headDir = (headSide.p2 - headSide.p1).norm
            val visibleOps = edge.ops.filter { it !in badgedOps }
            fun nominalT(op: TensorOp) = (visibleOps.indexOf(op) + 1).toDouble() / (visibleOps.size + 1)
            if (edge.strands > 1) {
                // One ghost strand per real stream, each interpolating between what its two
                // endpoints offer: a head deck's card ladder (strand i registers on card i) or
                // a flat tensor's attach side (strands converge onto the ribbon) — a pipe reads
                // as one vector fanning out into per-head cards, never as a band floating over
                // a flat tile. Strands span only where the parallelism exists: born at a split
                // bead, dying at the first bead that mixes the heads back (the output
                // projection), converging into the responsible glyph.
                val splitAt = visibleOps.firstOrNull { it is SplitHeadsOp }
                val mergeAt = visibleOps.firstOrNull { it is MergeHeadsOp || it is LinearOp || it is MatMulLinearOp }
                val tStart = splitAt?.let(::nominalT) ?: 0.0
                val tEnd = mergeAt?.let(::nominalT) ?: 1.0
                val tailFans = splitAt == null && fansIntoCards(edge.from)
                val headFans = mergeAt == null && fansIntoCards(edge.to)
                for (i in (edge.strands - 1) downTo 1) {
                    val spread = (i - (edge.strands - 1) / 2.0) * FLAT_STRAND_SPREAD
                    val tailOff = when {
                        splitAt != null -> Point2D.Double(0.0, 0.0)
                        tailFans -> Point2D.Double(-HEAD_STEP * i, -HEAD_STEP * i)
                        else -> tailDir * spread
                    }
                    val headOff = when {
                        mergeAt != null -> Point2D.Double(0.0, 0.0)
                        headFans -> Point2D.Double(-HEAD_STEP * i, -HEAD_STEP * i)
                        else -> headDir * spread
                    }
                    val strandPath = Path2D.Double()
                    for (sample in 0..STRAND_SAMPLES) {
                        val u = sample.toDouble() / STRAND_SAMPLES
                        val at = route.pointAt(tStart + (tEnd - tStart) * u)
                        val x = at.x + tailOff.x * (1 - u) + headOff.x * u
                        val y = at.y + tailOff.y * (1 - u) + headOff.y * u
                        if (sample == 0) strandPath.moveTo(x, y) else strandPath.lineTo(x, y)
                    }
                    PPath.Double(stroke.createStrokedShape(strandPath), null).apply {
                        paint = ribbonColor
                        transparency = if (edge.dimmed) 0.03f else 0.1f
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
                    else -> 0.5f
                }
                pickable = false
                edgeLayer.addChild(this)
            }
            if (edge.sliceBlocks.isNotEmpty()) {
                // A slice-read wears the identity colors of the chunks it carries — one thin
                // strand per chunk, matching the segment bars on the ticked source tile.
                val chunkStroke = BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
                for ((j, block) in edge.sliceBlocks.withIndex()) {
                    val spread = (j - (edge.sliceBlocks.size - 1) / 2.0) * 4.0
                    val strandPath = Path2D.Double()
                    for (sample in 0..STRAND_SAMPLES) {
                        val u = sample.toDouble() / STRAND_SAMPLES
                        val at = route.pointAt(u)
                        val off = tailDir * (spread * (1 - u)) + headDir * (spread * u)
                        if (sample == 0) strandPath.moveTo(at.x + off.x, at.y + off.y)
                        else strandPath.lineTo(at.x + off.x, at.y + off.y)
                    }
                    PPath.Double(chunkStroke.createStrokedShape(strandPath), null).apply {
                        paint = palette.chunkColors[block % palette.chunkColors.size]
                        transparency = if (edge.dimmed) 0.12f else 0.7f
                        pickable = false
                        edgeLayer.addChild(this)
                    }
                }
            }
            val satellitesByOp = satellitesByEdge[edge]?.associateBy { it.op } ?: emptyMap()
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
                            setOffset(
                                satellite.tile.x + satellite.tile.width / 2,
                                satellite.tile.y - GLYPH_RADIUS - OP_GLYPH_GAP,
                            )
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
            it.syncFanStyle()
        }
        rebuildEdges()
        refreshDirtyTiles()
    }

    private fun selectLayer(layer: Int) {
        val selector = scene.layerSelector ?: return
        selector(layer)
        refreshStackState()
    }

    /** Flips a deck tile's head, coupling GQA partners and refreshing labels — wheel and pager. */
    private fun stepHead(tile: TensorTile, delta: Int) {
        when (tile) {
            is DeckTile -> {
                tile.selectedSlice = (tile.selectedSlice + delta).mod(tile.slices)
                scene.onHeadSelected?.invoke(tile, tile.selectedSlice)
            }
            is AttentionTile -> {
                tile.selectedHead = (tile.selectedHead + delta).mod(tile.numHeads)
                scene.onHeadSelected?.invoke(tile, tile.selectedHead)
            }
            else -> return
        }
        tileNodes.forEach { it.syncLabel() }
        refreshDirtyTiles()
    }

    /**
     * Micro-stepping render state: glows [currentOp]'s glyph, dims every tile in [stale]
     * (the not-yet-recomputed half of the pass), and shows [status] — the walk's data source and
     * progress — in the outline's top-left corner. A [notice] status is an attention flash (why a
     * step was refused) and paints in the warning color. Dimming is pure transparency over the
     * cached rasters — no reshading. Pass (null, empty, null) at a step boundary to clear.
     */
    fun syncStepState(currentOp: TensorOp?, stale: Set<TensorTile>, status: String? = null, notice: Boolean = false) {
        currentStepOp = currentOp
        staleTiles = stale
        glyphsByOp.values.forEach { it.glowing = it.op == currentOp }
        tileNodes.forEach {
            it.raster.transparency = if (it.tile in stale) STALE_TRANSPARENCY else 1f
            it.badgeGlowing = currentOp != null && it.activationOp == currentOp
        }
        stepStatusIsNotice = notice
        stepStatus.text = status ?: ""
        stepStatus.visible = status != null
        paintStepStatus()
        if (status != null) placeStepStatus()
    }

    private inner class InteriorInputHandler : PBasicInputEventHandler() {

        private var mode = Mode.NONE
        private var pressPoint: Point2D? = null
        private var marqueeAdditive = false

        override fun mousePressed(event: PInputEvent) {
            if (!event.isLeftMouseButton) return
            // With the pan key held the canvas handler owns the gesture; leave it unhandled.
            if (event.isPanKeyDown) return
            // Pager clicks belong to their arrow buttons; don't start a marquee under them.
            var picked: PNode? = event.pickedNode
            while (picked != null) {
                if (picked in pagerNodes) return
                picked = picked.parent
            }
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
            val hit: FlowEndpoint? = vertex ?: tile
            if (hit != null) {
                if (event.isShiftDown) {
                    scene.selection.toggle(hit)
                } else if (hit !in scene.selection) {
                    scene.selection.set(listOf(hit))
                }
                (hit as? TensorTile)?.let { t -> scene.layerOfTile?.invoke(t)?.let { selectLayer(it) } }
                mode = Mode.MOVE
            } else {
                marqueeAdditive = event.isShiftDown
                if (!marqueeAdditive) scene.selection.clear()
                mode = Mode.MARQUEE
            }
            pressPoint = point
            syncSelection()
            event.isHandled = true
        }

        /**
         * Dragged tiles track the pointer every event, but the full edge relayout (routing,
         * satellites, glyphs) is throttled: edges trail the tile by at most one throttle window,
         * and release runs an exact relayout.
         */
        private var lastDragRelayout = 0L

        private fun relayoutThrottled() {
            val now = System.currentTimeMillis()
            if (now - lastDragRelayout >= DRAG_RELAYOUT_MS) {
                lastDragRelayout = now
                relayout()
            }
        }

        override fun mouseDragged(event: PInputEvent) {
            val start = pressPoint ?: return
            when (mode) {
                Mode.MOVE -> {
                    val delta = event.getDeltaRelativeTo(this@CompositorNode)
                    for (item in scene.selection.selected) {
                        when (item) {
                            is TensorTile -> {
                                item.x += delta.width
                                item.y += delta.height
                                tileNodesById[item.id]?.syncLayout()
                            }
                            is OpVertex -> {
                                item.x += delta.width
                                item.y += delta.height
                                item.placed = true
                            }
                        }
                    }
                    relayoutThrottled()
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
                    val hit = scene.tilesIn(rect.x, rect.y, rect.width, rect.height) +
                        scene.opVertices.filter { scene.isShown(it) && it.endpointBounds.intersects(rect) }
                    if (marqueeAdditive) scene.selection.add(hit) else scene.selection.set(hit)
                    syncSelection()
                }
                event.isHandled = true
            }
            if (mode == Mode.MOVE) {
                relayout()
            }
            mode = Mode.NONE
            pressPoint = null
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
            if (tile !is DeckTile && tile !is AttentionTile) return
            stepHead(tile, event.wheelRotation)
            event.isHandled = true
        }

        override fun mouseMoved(event: PInputEvent) {
            val point = event.getPositionRelativeTo(this@CompositorNode)
            val tile = scene.tileAt(point.x, point.y)
            onTileHover?.invoke(tile)
            val target = canvas ?: return
            val glyph = glyphsByOp.values.firstOrNull { it.containsScenePoint(point.x, point.y) }
            if (glyph != null) {
                target.toolTipText = opTooltip(glyph.op)
                return
            }
            val badged = tileNodes.firstOrNull { it.badgeContains(point.x, point.y) }
            if (badged != null) {
                target.toolTipText = badged.activationOp?.let(::opTooltip)
                return
            }
            val headSelector = tileNodes.firstOrNull { it.headSelectorContains(point.x, point.y) }
            if (headSelector != null) {
                target.toolTipText = headSelector.headSelectorTooltip()
                return
            }
            val cell = tile?.cellAt(point.x, point.y)
            target.toolTipText = if (tile != null && cell != null) {
                "${dataTooltipTitle(tile)}\nShape: ${tile.tooltipShape}\n${cellReadout(tile, cell.first, cell.second)}"
            } else null
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

    private enum class Mode { NONE, MOVE, MARQUEE }

    companion object {
        private const val PROBABILITY_CARD_ID = "probability-card"
        private const val MARGIN = 40.0
        private const val DRAG_RELAYOUT_MS = 33L
        private const val LENS_SPACE = 220.0
        private const val DECK_STEP = 2.0

        /** One diagonal step for everything head-indexed — deck cards, op-deck cards, edge
         *  strands — so strand i lines up with card i at both ends of a head-parallel pipe. */
        private const val HEAD_STEP = 1.5

        /** Per-strand spacing across the attach side where a pipe meets a flat tensor. */
        private const val FLAT_STRAND_SPREAD = 1.2
        private const val STRAND_SAMPLES = 24
        private const val STATE_ICON = 10.0
        private const val PAGER_ROW_HEIGHT = 13.0
        private const val GHOST_CARD_TRANSPARENCY = 0.35f
        private const val PAGER_ARROW = 7.0
        private const val TICK_LENGTH = 4.0
        private const val GLYPH_RADIUS = 9.0
        private const val OP_GLYPH_GAP = 3.0
        private const val OP_RING_PAD = 3.0
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
