package org.simbrain.network.compositor

import org.simbrain.util.NetworkTheme

class TileEdge(val from: TensorTile, val to: TensorTile)

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

    /** Derives tile-to-tile edges from op-graph reachability between the tiles' ports. */
    fun connectFromGraph() {
        val g = requireNotNull(graph) { "Scene has no plan graph to derive edges from" }
        val byId = _tiles.associateBy { it.id }
        edges = g.anchorEdges(byId.keys).map { (from, to) -> TileEdge(byId.getValue(from), byId.getValue(to)) }
    }

    /** Copies this token's values into every tile and refreshes the lens. Compute-thread side. */
    fun publish(tokenIndex: Int) {
        for (tile in _tiles) tile.publish(tokenIndex)
        lens?.refresh()
    }

    /** Clears every tile's published history for a fresh generation run. */
    fun reset() {
        for (tile in _tiles) tile.reset()
        lens?.reset()
    }

    /** Tier-2/3 pixel writes: shades whatever is dirty using the active theme palette. */
    fun shadeDirty() {
        val palette = NetworkTheme.current
        for (tile in _tiles) tile.shadeDirty(palette.coolNode, palette.neutralMidpoint, palette.hotNode)
    }

    /** Tier 3: re-runs the color mapping over every tile's value buffer (palette/theme change). */
    fun reshadeAll() {
        for (tile in _tiles) tile.markAllDirty()
        shadeDirty()
    }

    fun tileAt(sceneX: Double, sceneY: Double) = _tiles.lastOrNull { it.contains(sceneX, sceneY) }

    fun tilesIn(x: Double, y: Double, w: Double, h: Double) = _tiles.filter { it.intersects(x, y, w, h) }

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
