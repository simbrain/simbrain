package org.simbrain.network.compositor

import org.simbrain.network.tensor.op.MergeHeadsOp
import org.simbrain.network.tensor.op.SplitHeadsOp
import org.simbrain.network.tensor.op.TensorOp
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/** Something an edge can start or end at: a tensor tile, or a junction op vertex. */
sealed interface FlowEndpoint

/** Box size of a junction glyph on the diagram, per stage of its strip. */
const val JUNCTION_SIZE = 24.0

/** Vertical spacing between stacked return lanes. */
const val LANE_GAP = 26.0

/** The rect routing clears for an endpoint: a tile's rect, or the junction glyph's box. */
val FlowEndpoint.routeRect: Rectangle2D
    get() = when (this) {
        is TensorTile -> bounds
        is OpVertex -> {
            val stages = glyphStages(op)?.size ?: 1
            Rectangle2D.Double(
                x - JUNCTION_SIZE * stages / 2, y - JUNCTION_SIZE / 2,
                JUNCTION_SIZE * stages, JUNCTION_SIZE
            )
        }
    }

/**
 * Routing intent for a limb's return edge, recorded by the layout pass. Only the intent is
 * stored; the waypoint geometry derives from the CURRENT rects each time
 * [CompositorScene.deriveReturnWaypoints] runs, so lanes follow their tiles when dragged
 * instead of bending through where the layout once put them.
 */
class ReturnLaneRoute(
    /** Everything hanging in the edge's spine gap — the lane runs below all of it. */
    val clearItems: List<FlowEndpoint>,
    /** Lane ordinal below the group, 0 nearest the strips. */
    val lane: Int,
    /** True when the drop must also swing right of the whole group, not just its source. */
    val clearsGroupRight: Boolean,
)

/**
 * A multi-input op promoted to a diagram vertex: its input streams' arrows converge into the
 * glyph and one arrow leaves it — the residual ⊕, q x k, attention x values, embedding +
 * positions. [x]/[y] are the glyph center, assigned by layout ([placed]) or derived from
 * neighbors at render time.
 */
class OpVertex(val op: TensorOp) : FlowEndpoint {
    var x = 0.0
    var y = 0.0
    var placed = false

    /** True when this junction belongs to a limb the selected layer doesn't use. */
    var dimmed = false
}

class FlowEdge(
    val from: FlowEndpoint,
    val to: FlowEndpoint,
    val ops: List<TensorOp> = emptyList(),
    /**
     * How many independent parallel streams cross this edge — rendered as a strand fan of that
     * true width, so the 16-strand q pipe meets the 8-strand cache pipe at the scores deck.
     */
    val strands: Int = 1,
    /** The junction input port this edge arrives at, keying the target glyph's pin; see [DisplaySegment.toPort]. */
    val toPort: String? = null,
) {
    /**
     * Block indices of the source tile's ticked segments this edge reads, for slice-read edges
     * (a gate taking one chunk of a fused projection). Rendered as identity-colored strands
     * matching the segment bars; empty for whole-value edges.
     */
    var sliceBlocks: List<Int> = emptyList()
    /** Interior route knots in scene coordinates; set by layout to steer the curve around tiles. */
    var waypoints: List<Point2D> = emptyList()

    /** True when this edge belongs to a limb the selected layer doesn't use. */
    var dimmed = false
}

/**
 * A parameter tile that rides the data-flow edge carrying the op that consumes it — a weight
 * matrix sitting on the line with its multiply, a bias strip on the line with its add. Its
 * layout rect is derived from the edge's curve at render time, never placed independently.
 */
class TileSatellite(val tile: TensorTile, val edge: FlowEdge, val op: TensorOp)

/**
 * A movable compositor interior element that has bounds and persists with the scene's view, but
 * is not a tensor or data-flow endpoint. Probability cards use this status.
 */
class InteriorOverlay(
    val id: String,
    val width: Double,
    val height: Double,
    var x: Double = Double.NaN,
    var y: Double = Double.NaN,
)

/**
 * Self-contained selection over interior endpoints — tiles and op vertices — shaped like the
 * network canvas selection model (add/remove/toggle/set/clear plus change notification) but
 * deliberately separate from it — interior objects aren't network models, so global selection
 * actions don't apply to them.
 */
class InteriorSelectionModel {

    private val _selected = LinkedHashSet<FlowEndpoint>()
    val selected: Set<FlowEndpoint> get() = _selected

    var onChange: (() -> Unit)? = null

    operator fun contains(item: FlowEndpoint) = item in _selected

    fun add(items: Collection<FlowEndpoint>) = change { _selected.addAll(items) }

    fun remove(item: FlowEndpoint) = change { _selected.remove(item) }

    fun toggle(item: FlowEndpoint) = change {
        if (!_selected.remove(item)) _selected.add(item) else true
    }

    fun set(items: Collection<FlowEndpoint>) = change {
        if (_selected == items.toSet()) false else {
            _selected.clear()
            _selected.addAll(items)
            true
        }
    }

    fun clear() = change {
        if (_selected.isEmpty()) false else {
            _selected.clear()
            true
        }
    }

    private fun change(mutate: () -> Boolean) {
        if (mutate()) onChange?.invoke()
    }
}

/**
 * The compositor's retained scene: the tiles with their layout rects and value buffers, the
 * data-flow edges between them (derived from the op graph), the interior selection, and the
 * trace state. Pure model — no Piccolo — so publish and the invalidation tiers are testable
 * headless; [CompositorNode] renders it.
 *
 * [publish] runs on the compute thread at each token boundary (the copy into tile value buffers
 * is the compute/EDT synchronization point); shading happens at paint time on the EDT, straight
 * from the value buffers ([TilePatchNode]).
 */
class CompositorScene(val graph: PlanGraph? = null) {

    private val _tiles = mutableListOf<TensorTile>()
    val tiles: List<TensorTile> get() = _tiles

    private val _overlays = mutableListOf<InteriorOverlay>()
    val overlays: List<InteriorOverlay> get() = _overlays

    fun addOverlay(overlay: InteriorOverlay) {
        require(_overlays.none { it.id == overlay.id }) { "Duplicate overlay id ${overlay.id}" }
        _overlays += overlay
    }

    var edges: List<FlowEdge> = emptyList()
        private set

    var opVertices: List<OpVertex> = emptyList()
        private set

    var lens: LogitLens? = null

    /**
     * Edges carrying prior tokens' state into the current step — the KV-cache and conv-window
     * reads. Everything else in the diagram is this-token dataflow, so these render in their
     * own cross-time color.
     */
    var memoryEdges: Set<FlowEdge> = emptySet()

    /** Invoked when the user wheel-flips a deck or attention tile, e.g. to couple GQA decks. */
    var onHeadSelected: ((TensorTile, Int) -> Unit)? = null

    /**
     * Present on stacked-layer scenes: flips every [LayerStacked] tile (plus limb dimming and
     * the strip highlight) to one model layer, wrapping out-of-range values. Installed by the
     * scene's compositor; renderers and hosts invoke it and then refresh.
     */
    var layerSelector: ((Int) -> Unit)? = null

    /** The model layer a stacked scene currently shows; -1 for scenes without layer stacks. */
    var selectedLayer = -1

    /**
     * Total model layers behind a stacked scene, or 0 when the scene has no layer dimension.
     * Renderers use it to draw every layer deck on the same slot axis: one slot per model
     * layer, with gaps where a tile's stack skips layers.
     */
    var layerCount = 0

    /** Maps a tile to the model layer it selects when clicked or wheeled — the depth strip rows. */
    var layerOfTile: ((TensorTile) -> Int?)? = null

    /** Tiles rendered with a standing accent border — the depth strip rows the block spans. */
    var highlightedTiles: Set<TensorTile> = emptySet()

    /** Lane-routing intents for limb return edges, recorded by the layout pass. */
    var returnLanes: Map<FlowEdge, ReturnLaneRoute> = emptyMap()

    /**
     * Declarative grid templates for limb interiors, set by the scene's compositor. The layout
     * pass lays a limb out from the first template whose keys exactly cover its endpoints;
     * unmatched limbs keep the automatic rank-column layout.
     */
    var limbTemplates: List<LimbTemplate> = emptyList()

    /**
     * Re-derives return-edge waypoints from the current rects: each lane runs below everything
     * hanging in its gap, drops right of its source (or the whole group, for upper strips), and
     * re-enters toward its spine target. Runs on every relayout, so dragging a tile — or
     * applying a saved layout — pulls the lanes along with it.
     */
    fun deriveReturnWaypoints() {
        for ((edge, route) in returnLanes) {
            val laneY = route.clearItems.maxOf { it.routeRect.maxY } + LANE_GAP * (route.lane + 1)
            var dropX = edge.from.routeRect.maxX + 40.0
            if (route.clearsGroupRight) {
                dropX = maxOf(dropX, route.clearItems.maxOf { it.routeRect.maxX } + 40.0)
            }
            edge.waypoints = listOf(
                Point2D.Double(dropX, laneY),
                Point2D.Double(edge.to.routeRect.maxX + 60.0, laneY),
            )
        }
    }

    val selection = InteriorSelectionModel()

    /**
     * How token history renders across the scene — see [HistoryView]. Leaving
     * [HistoryView.OFF] re-derives the dropped history through [rebuildHistory].
     */
    var historyView = HistoryView.FULL
        set(value) {
            if (field == value) return
            val droppedHistory = field == HistoryView.OFF
            field = value
            for (tile in _tiles) tile.historyView = value
            if (droppedHistory) rebuildHistory?.invoke()
        }

    /** Scene-builder hook replaying the shown layers' history after [HistoryView.OFF] ends. */
    var rebuildHistory: (() -> Unit)? = null

    /**
     * When true, dimmed interior pieces — the limb the selected layer doesn't use — are hidden
     * entirely instead of ghosted. Hidden pieces don't render and don't hit-test.
     */
    var hideDimmed = false

    /** True when [item] should currently render and hit-test; false only for hidden dimmed pieces. */
    fun isShown(item: FlowEndpoint) = !hideDimmed || when (item) {
        is TensorTile -> !item.dimmed
        is OpVertex -> !item.dimmed
    }

    fun addTile(tile: TensorTile) {
        require(_tiles.none { it.id == tile.id }) { "Duplicate tile id ${tile.id}" }
        tile.historyView = historyView
        _tiles.add(tile)
    }

    fun tile(id: String) = _tiles.firstOrNull { it.id == id } ?: error("No tile with id $id")

    var satellites: List<TileSatellite> = emptyList()
        private set

    /**
     * Derives the display graph from op-graph reachability: edges between anchor tiles and
     * [OpVertex] junctions (multi-input ops rendered as convergence points), with single-input
     * ops riding the edges as glyph beads.
     *
     * Parameter tiles ([TileKind.WEIGHT]) are not edge anchors: each one attaches as a
     * [TileSatellite] to the edge carrying the op that reads it. A parameter whose consuming op
     * lies on no edge (e.g. the embedding table, consumed above the first anchor) falls back to
     * being a standalone anchor with its own edges.
     *
     * Pass [junctionVertices] = false for scenes that deliberately abstract the op flow (coarse
     * residual-history views): every op stays a bead and no vertices are created.
     */
    fun connectFromGraph(junctionVertices: Boolean = true) {
        val g = requireNotNull(graph) { "Scene has no plan graph to derive edges from" }
        val byId = _tiles.associateBy { it.id }
        val params = _tiles.filter { it.kind == TileKind.WEIGHT }
        val coreIds = _tiles.filter { it.kind != TileKind.WEIGHT }.map { it.id }
        val coreEdges = g.anchorEdges(coreIds)
        val attachable = params.filterTo(mutableListOf()) { p ->
            val readers = g.readers(p.id)
            coreEdges.any { edge -> edge.ops.any { it in readers } }
        }

        while (true) {
            val anchorIds = coreIds + (params - attachable.toSet()).map { it.id }
            val junctions = if (junctionVertices) g.junctionOps(anchorIds) else emptySet()
            val verticesByOp = junctions.associateWith { OpVertex(it) }
            fun endpoint(key: Any): FlowEndpoint = when (key) {
                is String -> byId.getValue(key)
                is TensorOp -> verticesByOp.getValue(key)
                else -> error("Unexpected endpoint $key")
            }
            val derived = g.displayEdges(anchorIds, junctions).map {
                val from = endpoint(it.from)
                FlowEdge(from, endpoint(it.to), it.ops, strands = strandCount(from, it.ops), toPort = it.toPort)
            }
            // A satellite needs its consuming op riding some edge as a bead; if the op was
            // promoted to a junction (or fell off the paths), the parameter goes standalone.
            val unhosted = attachable.filter { p ->
                val readers = g.readers(p.id).toSet()
                derived.none { edge -> edge.ops.any { it in readers } }
            }
            if (unhosted.isNotEmpty()) {
                attachable.removeAll(unhosted)
                continue
            }
            edges = derived
            opVertices = verticesByOp.values.toList()
            satellites = attachable.map { p ->
                val readers = g.readers(p.id).toSet()
                // Prefer the most direct hop carrying the consuming op — e.g. Wo rides the
                // attention-pattern edge into the output, not the longer value bypass.
                val host = edges.filter { edge -> edge.ops.any { it in readers } }.minBy { it.ops.size }
                TileSatellite(p, host, host.ops.first { it in readers })
            }
            return
        }
    }

    /**
     * The number of parallel streams arriving at the end of an edge: the source value's strand
     * count, split up by a head-split bead and collapsed back to one by a merge bead. Strands
     * are born where head-aware processing begins and die where the heads mix.
     */
    private fun strandCount(from: FlowEndpoint, beads: List<TensorOp>): Int {
        var count = when (from) {
            is TensorTile -> from.strands
            is OpVertex -> when (val op = from.op) {
                is SplitHeadsOp -> op.numHeads
                else -> opParallelism(op)
            }
        }
        for (op in beads) {
            count = when (op) {
                is SplitHeadsOp -> op.numHeads
                is MergeHeadsOp -> 1
                else -> count
            }
        }
        return count
    }

    /** Copies this token's values into every tile and refreshes the lens. Compute-thread side. */
    fun publish(tokenIndex: Int) {
        for (tile in _tiles) tile.publish(tokenIndex)
        lens?.refresh()
    }

    /**
     * Full-pass publish for scenes without a token cursor (the teaching model recomputes every
     * tile's tensor each forward): version-gated tiles refresh, token-indexed tiles no-op.
     */
    fun publish() = publish(-1)

    /** Clears every tile's published history for a fresh generation run. */
    fun reset() {
        for (tile in _tiles) tile.reset()
        lens?.reset()
    }

    fun tileAt(sceneX: Double, sceneY: Double) = _tiles.lastOrNull { isShown(it) && it.contains(sceneX, sceneY) }

    fun tilesIn(x: Double, y: Double, w: Double, h: Double) = _tiles.filter { isShown(it) && it.intersects(x, y, w, h) }

    /**
     * The tiles a mid-pass forward step has NOT yet reached: their writer op's schedule index is
     * at or past [cursor]. At a step boundary (cursor 0) nothing is stale. Tiles with no writer
     * op (parameters) are never stale.
     */
    fun staleTiles(cursor: Int): Set<TensorTile> {
        val g = graph ?: return emptySet()
        if (cursor == 0) return emptySet()
        return _tiles.filter { tile ->
            val writer = g.writerIndex(tile.id)
            writer != null && writer >= cursor
        }.toSet()
    }

    /**
     * Swaps every tile that has a gradient buffer between its forward values and its gradients
     * (the training-mode backward view); tiles without one keep showing forward values.
     */
    fun setGradientView(enabled: Boolean) {
        for (tile in _tiles) {
            (tile as? MatrixTile)?.let { if (it.gradientSource != null) it.showingGradient = enabled }
        }
    }

    var traceFocus: TensorTile? = null
        private set

    var tracedTiles: Set<TensorTile> = emptySet()
        private set

    var tracedEdges: Set<FlowEdge> = emptySet()
        private set

    /**
     * Sets (or clears, with null) the trace focus: highlights every tile on a data-flow path
     * into or out of [focus], and the edges along those paths — but not edges that bypass the
     * focus, like a residual edge skipping around a traced attention tile. Junction vertices
     * qualify through their output ports: an arm into a junction traces only when the junction's
     * result actually flows through (or is) the focus.
     */
    fun setTrace(focus: TensorTile?) {
        traceFocus = focus
        if (focus == null || graph == null) {
            tracedTiles = emptySet()
            tracedEdges = emptySet()
            return
        }
        val upstream = graph.upstreamPorts(focus.id)
        val downstream = graph.downstreamPorts(focus.id)
        tracedTiles = _tiles.filter { it.id in upstream || it.id in downstream }.toSet() + focus

        fun up(e: FlowEndpoint) = when (e) {
            is TensorTile -> e.id in upstream
            is OpVertex -> e.op.outputs.any { graph.alias(it.name).let { n -> n in upstream || n == focus.id } }
        }
        fun down(e: FlowEndpoint) = when (e) {
            is TensorTile -> e.id in downstream
            is OpVertex -> e.op.outputs.any { graph.alias(it.name) in downstream }
        }
        tracedEdges = edges.filter { edge ->
            (up(edge.from) && (up(edge.to) || edge.to == focus)) ||
                (down(edge.to) && (down(edge.from) || edge.from == focus))
        }.toSet()
    }
}
