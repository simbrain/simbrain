package org.simbrain.network.compositor

import org.simbrain.network.tensor.op.HeadMixOp
import org.simbrain.network.tensor.op.HeadScoresOp
import org.simbrain.network.tensor.op.MergeHeadsOp
import org.simbrain.network.tensor.op.SplitHeadsOp
import org.simbrain.network.tensor.op.TensorOp
import java.awt.geom.Point2D

/** Something an edge can start or end at: a tensor tile, or a junction op vertex. */
sealed interface FlowEndpoint

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
    /** True when the value crossing this edge is head-stacked — rendered as a strand fan. */
    val stranded: Boolean = false,
) {
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
 * Self-contained selection over compositor tiles, shaped like the network canvas selection model
 * (add/remove/toggle/set/clear plus change notification) but deliberately separate from it —
 * interior objects aren't network models, so global selection actions don't apply to them.
 */
class TileSelectionModel {

    private val _selected = LinkedHashSet<TensorTile>()
    val selected: Set<TensorTile> get() = _selected

    var onChange: (() -> Unit)? = null

    operator fun contains(tile: TensorTile) = tile in _selected

    fun add(tiles: Collection<TensorTile>) = change { _selected.addAll(tiles) }

    fun remove(tile: TensorTile) = change { _selected.remove(tile) }

    fun toggle(tile: TensorTile) = change {
        if (!_selected.remove(tile)) _selected.add(tile) else true
    }

    fun set(tiles: Collection<TensorTile>) = change {
        if (_selected == tiles.toSet()) false else {
            _selected.clear()
            _selected.addAll(tiles)
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

    var edges: List<FlowEdge> = emptyList()
        private set

    var opVertices: List<OpVertex> = emptyList()
        private set

    var lens: LogitLens? = null

    /** Edges rendered with standing emphasis — e.g. the KV-cache arrows telling the GQA story. */
    var emphasizedEdges: Set<FlowEdge> = emptySet()

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

    /** Maps a tile to the model layer it selects when clicked or wheeled — the depth strip rows. */
    var layerOfTile: ((TensorTile) -> Int?)? = null

    /** Tiles rendered with a standing accent border — the depth strip rows the block spans. */
    var highlightedTiles: Set<TensorTile> = emptySet()

    val selection = TileSelectionModel()

    fun addTile(tile: TensorTile) {
        require(_tiles.none { it.id == tile.id }) { "Duplicate tile id ${tile.id}" }
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
                FlowEdge(from, endpoint(it.to), it.ops, stranded = strandedState(from, it.ops))
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

    /** Whether the value arriving at the end of an edge with these beads is head-stacked. */
    private fun strandedState(from: FlowEndpoint, beads: List<TensorOp>): Boolean {
        var stacked = when (from) {
            is DeckTile -> true
            is OpVertex -> from.op is SplitHeadsOp || from.op is HeadScoresOp || from.op is HeadMixOp
            else -> false
        }
        for (op in beads) {
            when (op) {
                is SplitHeadsOp -> stacked = true
                is MergeHeadsOp -> stacked = false
                else -> {}
            }
        }
        return stacked
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

    fun tileAt(sceneX: Double, sceneY: Double) = _tiles.lastOrNull { it.contains(sceneX, sceneY) }

    fun tilesIn(x: Double, y: Double, w: Double, h: Double) = _tiles.filter { it.intersects(x, y, w, h) }

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
