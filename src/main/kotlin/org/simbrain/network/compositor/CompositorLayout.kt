package org.simbrain.network.compositor

/**
 * Constrained layered layout for compositor scenes, replacing hand-placed coordinates: anchor
 * tiles AND junction op vertices are ranked by longest path over the derived edges (the plan's
 * schedule order makes this a single relaxation sweep — the graph is already topologically
 * sorted), the residual checkpoint spine — junction ⊕s included — and everything downstream of
 * the last checkpoint is pinned to a vertical axis, and each limb row hangs to the right, past
 * the lens strip. Standalone parameter tiles sit above the endpoint they feed. Satellite tiles
 * are untouched — their rects derive from edge curves at render time.
 */
class CompositorLayout(
    scale: Double = 1.0,
    private val spineAxisX: Double = 0.0,
) {

    // Gaps scale with the diagram but keep floors: labels, glyphs, and the lens strip stay at
    // fixed point sizes, so a shrunken diagram still needs room for them.
    private val rowGap = (110.0 * scale).coerceAtLeast(70.0)
    private val junctionGap = (64.0 * scale).coerceAtLeast(48.0)
    private val stackStepX = 60.0 * scale
    private val stackGap = (70.0 * scale).coerceAtLeast(45.0)
    private val limbClearance = 220.0 + 20.0 * scale
    private val paramGap = (30.0 * scale).coerceAtLeast(20.0)

    private fun width(e: FlowEndpoint) = when (e) {
        is TensorTile -> e.width
        is OpVertex -> JUNCTION_SIZE
    }

    private fun height(e: FlowEndpoint) = when (e) {
        is TensorTile -> e.height
        is OpVertex -> JUNCTION_SIZE
    }

    private fun place(e: FlowEndpoint, x: Double, y: Double) {
        when (e) {
            is TensorTile -> {
                e.x = x
                e.y = y
            }
            is OpVertex -> {
                e.x = x + JUNCTION_SIZE / 2
                e.y = y + JUNCTION_SIZE / 2
                e.placed = true
            }
        }
    }

    fun apply(scene: CompositorScene) {
        val graph = scene.graph ?: return
        val satelliteTiles = scene.satellites.map { it.tile }.toSet()
        val anchors = scene.tiles.filter { it !in satelliteTiles }
        val (params, flowTiles) = anchors.partition { it.kind == TileKind.WEIGHT }
        if (flowTiles.isEmpty()) return

        fun scheduleIndex(e: FlowEndpoint) = when (e) {
            is TensorTile -> graph.writerIndex(e.id) ?: -1
            is OpVertex -> graph.scheduleIndex(e.op) ?: -1
        }

        // A junction and the tile it writes share a schedule index; the junction comes first.
        val flow: List<FlowEndpoint> = (flowTiles + scene.opVertices)
            .sortedWith(compareBy({ scheduleIndex(it) }, { it is TensorTile }))

        val edgesByTarget = scene.edges.groupBy { it.to }
        val rank = HashMap<FlowEndpoint, Int>()
        for (item in flow) {
            rank[item] = edgesByTarget[item].orEmpty()
                .mapNotNull { rank[it.from]?.plus(1) }
                .maxOrNull() ?: 0
        }

        val lastCheckpointRank = flowTiles.filter { it.kind == TileKind.RESIDUAL }
            .maxOfOrNull { rank.getValue(it) } ?: -1
        val spineTileIds = flowTiles.filter {
            it.kind == TileKind.RESIDUAL || rank.getValue(it) > lastCheckpointRank
        }.map { it.id }.toSet()

        fun onSpine(e: FlowEndpoint) = when (e) {
            is TensorTile -> e.id in spineTileIds
            is OpVertex -> e.op.outputs.any { it.name in spineTileIds }
        }

        val spineHalfWidth = flowTiles.filter(::onSpine).maxOfOrNull { it.width / 2 } ?: 0.0
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

        fun centerX(e: FlowEndpoint) = when (e) {
            is TensorTile -> e.x + e.width / 2
            is OpVertex -> e.x
        }

        var y = 0.0
        for (r in 0..rank.values.max()) {
            val row = flow.filter { rank[it] == r }
            if (row.isEmpty()) continue
            val limbItems = row.filter { !onSpine(it) }
            // Limb rows start under the barycenter of their already-placed inputs, so the limb
            // snakes with its data flow instead of hugging one column.
            var x = limbLeft
            if (limbItems.isNotEmpty()) {
                val inputCenters = limbItems.flatMap { item ->
                    edgesByTarget[item].orEmpty().mapNotNull { edge ->
                        if (edge.from in rank) centerX(edge.from) else null
                    }
                }
                if (inputCenters.isNotEmpty()) {
                    x = (inputCenters.average() - width(limbItems.first()) / 2).coerceAtLeast(limbLeft)
                }
            }
            var rowHeight = 0.0
            var stagger = 0.0
            for (item in row) {
                if (onSpine(item)) {
                    place(item, spineAxisX - width(item) / 2, y)
                    rowHeight = maxOf(rowHeight, height(item))
                } else {
                    // Siblings cascade steeply down with a small x-step: their fan-in curves
                    // arrive at distinct heights through the clear zone left of the limb, so
                    // neither the curves nor the weights riding them cross an earlier sibling.
                    place(item, x, y + stagger)
                    rowHeight = maxOf(rowHeight, stagger + height(item))
                    x += stackStepX
                    stagger += height(item) + stackGap
                }
            }
            val gap = if (row.all { it is OpVertex }) junctionGap else rowGap
            y += rowHeight + gap + (extraBelow[r] ?: 0.0)
        }

        for ((target, group) in params.groupBy { p -> scene.edges.firstOrNull { it.from == p }?.to }) {
            when (target) {
                is OpVertex -> {
                    // Feeding a junction: a side-by-side row above it, centered on the glyph.
                    val totalWidth = group.sumOf { it.width } + paramGap * (group.size - 1)
                    var x = target.x - totalWidth / 2
                    val bottom = target.y - JUNCTION_SIZE / 2 - paramGap * 2
                    for (param in group) {
                        param.x = x
                        param.y = bottom - param.height
                        x += param.width + paramGap
                    }
                }
                is TensorTile -> {
                    var bottom = target.y - paramGap
                    for (param in group.asReversed()) {
                        param.x = target.x - paramGap - param.width
                        param.y = bottom - param.height
                        bottom = param.y - paramGap
                    }
                }
                null -> {}
            }
        }
    }

    companion object {
        private const val JUNCTION_SIZE = 24.0
    }
}
