package org.simbrain.network.compositor

/**
 * Constrained layered layout for compositor scenes, replacing hand-placed coordinates: anchor
 * tiles AND junction op vertices are ranked by longest path over the derived edges (the plan's
 * schedule order makes this a single relaxation sweep — the graph is already topologically
 * sorted), and the residual checkpoint spine — junction ⊕s included — plus everything downstream
 * of the last checkpoint is pinned to a vertical axis.
 *
 * Off-spine structure is grouped by limb: the connected components of the flow graph once the
 * spine is removed, each one processing arm (attention, gated conv, the MLP). Limbs whose rank
 * spans overlap get separate column bands so their arrows don't weave through each other; limbs
 * with disjoint spans pack into the band nearest the spine, and limbs with more spine-facing
 * edges take nearer bands so fewer arrows arc across a band. Within a rank, siblings order by
 * the barycenter of their already-placed inputs — the crossing-minimization pass — and cascade
 * down inside their band so fan-in curves clear earlier siblings. Standalone parameter tiles sit
 * above the endpoint they feed. Satellite tiles are untouched — their rects derive from edge
 * curves at render time.
 */
class CompositorLayout(
    scale: Double = 1.0,
    private val spineAxisX: Double = 0.0,
) {

    // Gaps scale with the diagram but keep floors: labels, glyphs, and the lens strip stay at
    // fixed point sizes, so a shrunken diagram still needs room for them.
    private val rowGap = (90.0 * scale).coerceAtLeast(70.0)
    private val junctionGap = (64.0 * scale).coerceAtLeast(48.0)
    private val stackStepX = 60.0 * scale
    private val stackGap = (70.0 * scale).coerceAtLeast(45.0)
    private val limbClearance = 220.0 + 20.0 * scale
    private val paramGap = (30.0 * scale).coerceAtLeast(20.0)
    private val bandGap = (100.0 * scale).coerceAtLeast(80.0)

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
            is OpVertex -> e.op.outputs.any { graph.alias(it.name) in spineTileIds }
        }

        // Limbs: connected components of the flow graph once the spine is removed.
        val neighbors = HashMap<FlowEndpoint, MutableList<FlowEndpoint>>()
        for (edge in scene.edges) {
            if (edge.from in rank && edge.to in rank && !onSpine(edge.from) && !onSpine(edge.to)) {
                neighbors.getOrPut(edge.from) { mutableListOf() }.add(edge.to)
                neighbors.getOrPut(edge.to) { mutableListOf() }.add(edge.from)
            }
        }
        val limbOf = HashMap<FlowEndpoint, Int>()
        val limbs = ArrayList<MutableList<FlowEndpoint>>()
        for (item in flow) {
            if (onSpine(item) || item in limbOf) continue
            val id = limbs.size
            limbs.add(mutableListOf())
            val queue = ArrayDeque<FlowEndpoint>().apply { add(item) }
            while (queue.isNotEmpty()) {
                val current = queue.removeLast()
                if (limbOf.putIfAbsent(current, id) != null) continue
                limbs[id].add(current)
                neighbors[current]?.forEach { if (it !in limbOf) queue.add(it) }
            }
        }

        fun limbExtent(members: List<FlowEndpoint>): Double =
            members.groupBy { rank.getValue(it) }.values.maxOf { row ->
                (row.size - 1) * stackStepX + row.maxOf { width(it) }
            }

        // Band assignment: greedy interval coloring over rank spans, nearest band first.
        val spineEdgeCount = IntArray(limbs.size)
        for (edge in scene.edges) {
            if (edge.from !in rank || edge.to !in rank) continue
            if (onSpine(edge.from) && !onSpine(edge.to)) limbOf[edge.to]?.let { spineEdgeCount[it]++ }
            if (!onSpine(edge.from) && onSpine(edge.to)) limbOf[edge.from]?.let { spineEdgeCount[it]++ }
        }
        val bandOfLimb = IntArray(limbs.size)
        val bandSpans = ArrayList<MutableList<IntRange>>()
        val bandWidths = ArrayList<Double>()
        for (id in limbs.indices.sortedWith(compareByDescending<Int> { spineEdgeCount[it] }.thenBy { it })) {
            val span = limbs[id].minOf { rank.getValue(it) }..limbs[id].maxOf { rank.getValue(it) }
            var band = bandSpans.indexOfFirst { spans ->
                spans.none { it.first <= span.last && span.first <= it.last }
            }
            if (band < 0) {
                band = bandSpans.size
                bandSpans.add(mutableListOf())
                bandWidths.add(0.0)
            }
            bandSpans[band].add(span)
            bandWidths[band] = maxOf(bandWidths[band], limbExtent(limbs[id]))
            bandOfLimb[id] = band
        }
        val spineHalfWidth = flowTiles.filter(::onSpine).maxOfOrNull { it.width / 2 } ?: 0.0
        val bandLefts = DoubleArray(bandWidths.size)
        var bandX = spineAxisX + spineHalfWidth + limbClearance
        for (band in bandLefts.indices) {
            bandLefts[band] = bandX
            bandX += bandWidths[band] + bandGap
        }

        // Rank boundaries crossed by a satellite-carrying edge open to fit the riding tile —
        // its height plus label and curve clearance — replacing the normal gap, not adding to it.
        val satelliteNeed = HashMap<Int, Double>()
        for (satellite in scene.satellites) {
            val from = rank[satellite.edge.from] ?: continue
            val to = rank[satellite.edge.to] ?: continue
            for (boundary in from until to) {
                satelliteNeed[boundary] = maxOf(satelliteNeed[boundary] ?: 0.0, satellite.tile.height + 100.0)
            }
        }

        fun centerX(e: FlowEndpoint) = when (e) {
            is TensorTile -> e.x + e.width / 2
            is OpVertex -> e.x
        }

        fun placedInputCenters(item: FlowEndpoint, r: Int) = edgesByTarget[item].orEmpty()
            .filter { (rank[it.from] ?: r) < r }
            .map { centerX(it.from) }

        val rowsByRank = flow.groupBy { rank.getValue(it) }
        fun junctionsOnly(r: Int) = rowsByRank[r]?.all { it is OpVertex } == true

        var y = 0.0
        for (r in 0..rank.values.max()) {
            val row = rowsByRank[r] ?: continue
            var rowHeight = 0.0
            for (item in row.filter(::onSpine)) {
                place(item, spineAxisX - width(item) / 2, y)
                rowHeight = maxOf(rowHeight, height(item))
            }
            for ((limbId, members) in row.filterNot(::onSpine).groupBy { limbOf.getValue(it) }) {
                // Barycenter order over placed inputs is the crossing-minimization pass; schedule
                // order breaks ties so equal-input siblings keep declaration order.
                val ordered = members.sortedWith(compareBy(
                    { item -> placedInputCenters(item, r).ifEmpty { null }?.average() ?: Double.MAX_VALUE },
                    { scheduleIndex(it) },
                ))
                val bandLeft = bandLefts[bandOfLimb[limbId]]
                val bandRight = bandLeft + bandWidths[bandOfLimb[limbId]]
                val extent = ordered.mapIndexed { i, item -> i * stackStepX + width(item) }.max()
                // The row starts under the barycenter of its placed inputs, clamped to its band,
                // so the limb snakes with its data flow without invading a neighboring band.
                val inputCenters = ordered.flatMap { placedInputCenters(it, r) }
                var x = if (inputCenters.isEmpty()) bandLeft else {
                    (inputCenters.average() - width(ordered.first()) / 2)
                        .coerceAtMost(bandRight - extent)
                        .coerceAtLeast(bandLeft)
                }
                var stagger = 0.0
                for (item in ordered) {
                    // Siblings cascade steeply down with a small x-step: their fan-in curves
                    // arrive at distinct heights, so neither the curves nor the weights riding
                    // them cross an earlier sibling.
                    place(item, x, y + stagger)
                    rowHeight = maxOf(rowHeight, stagger + height(item))
                    x += stackStepX
                    stagger += height(item) + stackGap
                }
            }
            // Boundaries touching a junction-only row tighten on both sides: the glyphs are
            // small, so the full row gap around them reads as dead space.
            val next = (r + 1..rank.values.max()).firstOrNull { rowsByRank.containsKey(it) }
            val gap = if (junctionsOnly(r) || (next != null && junctionsOnly(next))) junctionGap else rowGap
            y += rowHeight + maxOf(gap, satelliteNeed[r] ?: 0.0)
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
