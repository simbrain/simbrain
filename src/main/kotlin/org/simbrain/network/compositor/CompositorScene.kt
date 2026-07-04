package org.simbrain.network.compositor

import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.util.NetworkTheme
import java.awt.geom.Point2D

class TileEdge(val from: TensorTile, val to: TensorTile, val ops: List<TensorOp> = emptyList()) {
    /** Interior route knots in scene coordinates; set by layout to steer the curve around tiles. */
    var waypoints: List<Point2D> = emptyList()
}

/**
 * A parameter tile that rides the data-flow edge carrying the op that consumes it — a weight
 * matrix sitting on the line with its multiply, a bias strip on the line with its add. Its
 * layout rect is derived from the edge's curve at render time, never placed independently.
 */
class TileSatellite(val tile: TensorTile, val edge: TileEdge, val op: TensorOp)

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
 * is the compute/EDT synchronization point); [shadeDirty] runs on the EDT before painting.
 */
class CompositorScene(val graph: PlanGraph? = null) {

    private val _tiles = mutableListOf<TensorTile>()
    val tiles: List<TensorTile> get() = _tiles

    var edges: List<TileEdge> = emptyList()
        private set

    var lens: LogitLens? = null

    val selection = TileSelectionModel()

    fun addTile(tile: TensorTile) {
        require(_tiles.none { it.id == tile.id }) { "Duplicate tile id ${tile.id}" }
        _tiles.add(tile)
    }

    fun tile(id: String) = _tiles.firstOrNull { it.id == id } ?: error("No tile with id $id")

    var satellites: List<TileSatellite> = emptyList()
        private set

    /**
     * Derives tile-to-tile edges (and their op decorations) from op-graph reachability.
     *
     * Parameter tiles ([TileKind.WEIGHT]) are not edge anchors: each one attaches as a
     * [TileSatellite] to the edge carrying the op that reads it. A parameter whose consuming op
     * lies on no edge (e.g. the embedding table, consumed above the first anchor) falls back to
     * being a standalone anchor with its own edges.
     */
    fun connectFromGraph() {
        val g = requireNotNull(graph) { "Scene has no plan graph to derive edges from" }
        val byId = _tiles.associateBy { it.id }
        val params = _tiles.filter { it.kind == TileKind.WEIGHT }
        val coreIds = _tiles.filter { it.kind != TileKind.WEIGHT }.map { it.id }
        val coreEdges = g.anchorEdges(coreIds)
        val attachable = params.filter { p ->
            val readers = g.readers(p.id)
            coreEdges.any { edge -> edge.ops.any { it in readers } }
        }
        val standaloneIds = (params - attachable.toSet()).map { it.id }
        edges = g.anchorEdges(coreIds + standaloneIds).map {
            TileEdge(byId.getValue(it.from), byId.getValue(it.to), it.ops)
        }
        satellites = attachable.map { p ->
            val readers = g.readers(p.id).toSet()
            edges.firstNotNullOf { edge ->
                edge.ops.firstOrNull { it in readers }?.let { TileSatellite(p, edge, it) }
            }
        }
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

    /** Tier-2/3 pixel writes: shades whatever is dirty, choosing the palette by tile kind. */
    fun shadeDirty() {
        val palette = NetworkTheme.current
        for (tile in _tiles) {
            when (tile.kind) {
                TileKind.WEIGHT -> tile.shadeDirty(palette.inhibitorySynapse, palette.zeroWeight, palette.excitatorySynapse)
                else -> tile.shadeDirty(palette.coolNode, palette.neutralMidpoint, palette.hotNode)
            }
        }
    }

    /** Tier 3: re-runs the color mapping over every tile's value buffer (palette/theme change). */
    fun reshadeAll() {
        for (tile in _tiles) tile.markAllDirty()
        shadeDirty()
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

    var tracedEdges: Set<TileEdge> = emptySet()
        private set

    /**
     * Sets (or clears, with null) the trace focus: highlights every tile on a data-flow path
     * into or out of [focus], and the edges along those paths — but not edges that bypass the
     * focus, like a residual edge skipping around a traced attention tile.
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
        val upTiles = _tiles.filter { it.id in upstream }.toSet()
        val downTiles = _tiles.filter { it.id in downstream }.toSet()
        tracedTiles = upTiles + downTiles + focus
        tracedEdges = edges.filter { edge ->
            (edge.from in upTiles && (edge.to in upTiles || edge.to == focus)) ||
                (edge.to in downTiles && (edge.from in downTiles || edge.from == focus))
        }.toSet()
    }
}
