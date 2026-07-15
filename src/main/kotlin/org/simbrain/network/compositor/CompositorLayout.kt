package org.simbrain.network.compositor

/**
 * Constrained layout for compositor scenes, replacing hand-placed coordinates. The residual
 * checkpoint spine — junction ⊕s included — plus everything downstream of the last checkpoint
 * runs top-down on a vertical axis. Off-spine structure is grouped by limb: the connected
 * components of the flow graph once the spine is removed, each one processing arm (attention,
 * gated conv, the MLP).
 *
 * Limbs flow HORIZONTALLY: each lays out as left-to-right columns of its local ranks, centered
 * on the checkpoint that feeds it — the classic block-diagram shape where arms run straight out
 * sideways and rejoin at the ⊕ below. A long arm costs width instead of height, so the diagram
 * stays near screen aspect. Limbs sharing a checkpoint stack as strips (more spine-facing
 * edges on top) centered as a group, and their return arrows route through reserved lanes at
 * the bottom of the gap — recorded as [ReturnLaneRoute] intents whose waypoint geometry
 * re-derives from the current rects on every relayout. Within a column, siblings order by the barycenter of their placed
 * inputs — the crossing-minimization pass — with schedule order breaking ties. Standalone
 * parameter tiles sit above the endpoint they feed. Satellite tiles are untouched — their rects
 * derive from edge curves at render time, with column gaps and spine gaps opened to fit them.
 */
class CompositorLayout(
    scale: Double = 1.0,
    private val spineAxisX: Double = 0.0,
) {

    // Gaps scale with the diagram but keep floors: labels, glyphs, and the lens strip stay at
    // fixed point sizes, so a shrunken diagram still needs room for them.
    private val rowGap = (90.0 * scale).coerceAtLeast(70.0)
    private val junctionGap = (64.0 * scale).coerceAtLeast(48.0)
    private val columnGap = (110.0 * scale).coerceAtLeast(90.0)
    private val stackGap = (70.0 * scale).coerceAtLeast(45.0)
    private val limbClearance = 220.0 + 20.0 * scale
    private val paramGap = (30.0 * scale).coerceAtLeast(20.0)
    private val interLimbGap = (60.0 * scale).coerceAtLeast(50.0)
    private val laneGap = LANE_GAP

    private fun width(e: FlowEndpoint) = when (e) {
        is TensorTile -> e.width
        is OpVertex -> JUNCTION_SIZE * (glyphStages(e.op)?.size ?: 1)
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
                e.x = x + width(e) / 2
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

        val spine = flow.filter(::onSpine)
        val spineIndex = spine.withIndex().associate { (i, item) -> item to i }
        val spineHalfWidth = spine.maxOfOrNull { width(it) / 2 } ?: 0.0

        // Satellites riding spine-to-limb entry edges perch in the clearance channel; widen it
        // so several of them (Wq/Wk/Wv fanning into one limb) have room to spread out.
        val entrySatelliteNeed = scene.satellites
            .filter { onSpine(it.edge.from) && it.edge.to in rank && !onSpine(it.edge.to) }
            .maxOfOrNull { it.tile.width + 100.0 } ?: 0.0
        val limbLeft = spineAxisX + spineHalfWidth + limbClearance + entrySatelliteNeed

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

        // Local ranks within each limb become its columns; flow order makes one sweep enough.
        val localRank = HashMap<FlowEndpoint, Int>()
        for (item in flow) {
            val limb = limbOf[item] ?: continue
            localRank[item] = edgesByTarget[item].orEmpty()
                .filter { limbOf[it.from] == limb }
                .mapNotNull { localRank[it.from]?.plus(1) }
                .maxOrNull() ?: 0
        }

        class LimbPlan(val id: Int) {
            val columns: List<List<FlowEndpoint>> = limbs[id]
                .groupBy { localRank.getValue(it) }
                .toSortedMap().values
                .map { column -> column.sortedWith(compareBy({ rank.getValue(it) }, { scheduleIndex(it) })) }
            val columnHeights = columns.map { column ->
                column.sumOf { height(it) } + stackGap * (column.size - 1)
            }
            val stripHeight = columnHeights.max()
            // Column boundaries crossed by a satellite-carrying edge open to fit the riding tile.
            val columnGaps = DoubleArray((columns.size - 1).coerceAtLeast(0)) { columnGap }.also { gaps ->
                for (satellite in scene.satellites) {
                    if (limbOf[satellite.edge.from] != id || limbOf[satellite.edge.to] != id) continue
                    val from = localRank.getValue(satellite.edge.from)
                    val to = localRank.getValue(satellite.edge.to)
                    for (boundary in from until to) {
                        gaps[boundary] = maxOf(gaps[boundary], satellite.tile.width + 100.0)
                    }
                }
            }
            var spineEdges = 0
            val returnEdges = scene.edges.filter { limbOf[it.from] == id && onSpine(it.to) }
            var top = 0.0
        }

        val plans = limbs.indices.map { LimbPlan(it) }
        for (edge in scene.edges) {
            if (edge.from !in rank || edge.to !in rank) continue
            if (onSpine(edge.from) && !onSpine(edge.to)) limbOf[edge.to]?.let { plans[it].spineEdges++ }
            if (!onSpine(edge.from) && onSpine(edge.to)) limbOf[edge.from]?.let { plans[it].spineEdges++ }
        }

        // Each limb hangs in the spine gap right below the last spine item feeding it.
        fun entryGap(plan: LimbPlan): Int {
            val fromSpine = limbs[plan.id].flatMap { item ->
                edgesByTarget[item].orEmpty().mapNotNull { spineIndex[it.from] }
            }
            val exit = plan.returnEdges.mapNotNull { spineIndex[it.to] }.minOrNull()
            return (fromSpine.maxOrNull() ?: exit?.minus(1) ?: 0).coerceIn(0, (spine.size - 2).coerceAtLeast(0))
        }

        val limbsAtGap = plans.groupBy(::entryGap).mapValues { (_, group) ->
            group.sortedWith(compareByDescending<LimbPlan> { it.spineEdges }.thenBy { it.id })
        }

        // Limb strips center as a group on the checkpoint feeding them, so the arm runs
        // straight out sideways instead of stepping down first. A gap then only pays for the
        // part of the strip stack extending below its checkpoint plus the return lanes; the
        // part reaching above is charged to the gap before it.
        fun stackHeight(group: List<LimbPlan>) =
            group.sumOf { it.stripHeight } + interLimbGap * (group.size - 1)

        fun overhang(i: Int) = limbsAtGap[i]
            ?.let { ((stackHeight(it) - height(spine[i])) / 2).coerceAtLeast(0.0) } ?: 0.0

        // Spine gaps: the base row gap (tight around junction-only neighbors), opened for
        // spine-riding satellites, the strip overhangs on either side, and the return lanes
        // along the bottom.
        val spineSatelliteNeed = HashMap<Int, Double>()
        for (satellite in scene.satellites) {
            val from = spineIndex[satellite.edge.from] ?: continue
            val to = spineIndex[satellite.edge.to] ?: continue
            for (boundary in from until to) {
                spineSatelliteNeed[boundary] =
                    maxOf(spineSatelliteNeed[boundary] ?: 0.0, satellite.tile.height + 100.0)
            }
        }
        val gapNeed = DoubleArray((spine.size - 1).coerceAtLeast(0)) { i ->
            val base = if (spine[i] is OpVertex || spine[i + 1] is OpVertex) junctionGap else rowGap
            var need = maxOf(base, spineSatelliteNeed[i] ?: 0.0)
            limbsAtGap[i]?.let { group ->
                val lanes = laneGap * (group.count { it.returnEdges.isNotEmpty() } + 1)
                need = maxOf(need, overhang(i) + maxOf(rowGap, lanes))
            }
            need
        }
        for (i in 1 until gapNeed.size) {
            gapNeed[i - 1] += overhang(i)
        }

        var y = 0.0
        for ((i, item) in spine.withIndex()) {
            place(item, spineAxisX - width(item) / 2, y)
            if (i < gapNeed.size) {
                limbsAtGap[i]?.let { group ->
                    var stripTop = y + height(item) / 2 - stackHeight(group) / 2
                    for (plan in group) {
                        plan.top = stripTop
                        stripTop += plan.stripHeight + interLimbGap
                    }
                }
                y += height(item) + gapNeed[i]
            }
        }

        for (plan in plans) {
            var x = limbLeft
            for ((c, column) in plan.columns.withIndex()) {
                // Columns center vertically in the strip; within one, siblings order by the
                // barycenter of their placed inputs — the crossing-minimization pass — with
                // schedule order breaking ties, and stack so fan-in curves arrive at distinct
                // heights.
                val ordered = column.sortedWith(compareBy(
                    { item ->
                        val centers = edgesByTarget[item].orEmpty()
                            .filter { it.from in rank && (limbOf[it.from] != plan.id || localRank.getValue(it.from) < c) }
                            .map { (it.from as? TensorTile)?.let { t -> t.y + t.height / 2 } ?: (it.from as OpVertex).y }
                        if (centers.isEmpty()) Double.MAX_VALUE else centers.average()
                    },
                    { scheduleIndex(it) },
                ))
                var itemY = plan.top + (plan.stripHeight - plan.columnHeights[c]) / 2
                for (item in ordered) {
                    place(item, x, itemY)
                    itemY += height(item) + stackGap
                }
                x += (column.maxOf { width(it) }) + (plan.columnGaps.getOrNull(c) ?: 0.0)
            }
        }

        // Return arrows travel back to the spine through lanes below the gap's limb strips, so
        // they never cut through a neighboring strip's columns. Only the routing intent is
        // recorded here; the waypoint geometry derives from current rects so lanes follow
        // their tiles when dragged.
        val lanes = HashMap<FlowEdge, ReturnLaneRoute>()
        for ((_, group) in limbsAtGap) {
            val clearItems = group.flatMap { limbs[it.id] }
            for ((lane, plan) in group.asReversed().withIndex()) {
                for (edge in plan.returnEdges) {
                    lanes[edge] = ReturnLaneRoute(clearItems, lane, clearsGroupRight = plan !== group.last())
                }
            }
        }
        scene.returnLanes = lanes
        scene.deriveReturnWaypoints()

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

}
