package org.simbrain.network.compositor

/**
 * Constrained layered layout for compositor scenes, replacing hand-placed coordinates: anchor
 * tiles are ranked by longest path over the derived edges (the plan's schedule order makes this
 * a single relaxation sweep — the graph is already topologically sorted), the residual checkpoint
 * spine and everything downstream of the last checkpoint is pinned to a vertical axis, and each
 * limb row hangs to the right, past the lens strip. Standalone parameter tiles sit above-left of
 * the anchor they feed. Satellite tiles are untouched — their rects derive from edge curves at
 * render time.
 */
class CompositorLayout(
    private val spineAxisX: Double = 0.0,
    private val rowGap: Double = 110.0,
    private val stackStepX: Double = 30.0,
    private val stackGap: Double = 50.0,
    private val limbClearance: Double = 240.0,
    private val paramGap: Double = 30.0,
) {

    fun apply(scene: CompositorScene) {
        val graph = scene.graph ?: return
        val satelliteTiles = scene.satellites.map { it.tile }.toSet()
        val anchors = scene.tiles.filter { it !in satelliteTiles }
        val (params, flow) = anchors.partition { it.kind == TileKind.WEIGHT }
        if (flow.isEmpty()) return

        val edgesByTarget = scene.edges.groupBy { it.to }
        val rank = HashMap<TensorTile, Int>()
        for (tile in flow.sortedBy { graph.writerIndex(it.id) ?: -1 }) {
            rank[tile] = edgesByTarget[tile].orEmpty()
                .mapNotNull { rank[it.from]?.plus(1) }
                .maxOrNull() ?: 0
        }

        val lastCheckpointRank = flow.filter { it.kind == TileKind.RESIDUAL }
            .maxOfOrNull { rank.getValue(it) } ?: -1
        fun onSpine(tile: TensorTile) = tile.kind == TileKind.RESIDUAL || rank.getValue(tile) > lastCheckpointRank

        val spineHalfWidth = flow.filter(::onSpine).maxOfOrNull { it.width / 2 } ?: 0.0
        val limbLeft = spineAxisX + spineHalfWidth + limbClearance

        // Rank boundaries crossed by a satellite-carrying edge open up so the riding tile fits.
        val extraBelow = HashMap<Int, Double>()
        for (satellite in scene.satellites) {
            val from = rank[satellite.edge.from] ?: continue
            val to = rank[satellite.edge.to] ?: continue
            for (boundary in from until to) {
                extraBelow[boundary] = maxOf(extraBelow[boundary] ?: 0.0, satellite.tile.height + 50.0)
            }
        }

        var y = 0.0
        for (r in 0..rank.values.max()) {
            val row = flow.filter { rank[it] == r }
            if (row.isEmpty()) continue
            var rowHeight = 0.0
            var x = limbLeft
            var stagger = 0.0
            for (tile in row) {
                if (onSpine(tile)) {
                    tile.x = spineAxisX - tile.width / 2
                    tile.y = y
                    rowHeight = maxOf(rowHeight, tile.height)
                } else {
                    // Siblings cascade steeply down with a small x-step: their fan-in curves
                    // arrive at distinct heights through the clear zone left of the limb, so
                    // neither the curves nor the weights riding them cross an earlier sibling.
                    tile.x = x
                    tile.y = y + stagger
                    rowHeight = maxOf(rowHeight, stagger + tile.height)
                    x += stackStepX
                    stagger += tile.height + stackGap
                }
            }
            y += rowHeight + rowGap + (extraBelow[r] ?: 0.0)
        }

        for ((target, group) in params.groupBy { p -> scene.edges.firstOrNull { it.from == p }?.to }) {
            if (target == null) continue
            var bottom = target.y - paramGap
            for (param in group.asReversed()) {
                param.x = target.x - paramGap - param.width
                param.y = bottom - param.height
                bottom = param.y - paramGap
            }
        }
    }
}
