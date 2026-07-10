package org.simbrain.network.compositor

import org.simbrain.network.llm.AttendMixOp
import org.simbrain.network.llm.AttendScoresOp
import org.simbrain.network.llm.Lfm2DecodeState
import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.llm.OffsetGateOp
import org.simbrain.network.tensor.FloatTensor
import org.simbrain.network.tensor.op.LinearOp
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.TensorOp
import org.simbrain.network.tensor.op.TensorPort

/**
 * The structure-first LFM2 scene: one layer-block anatomy is the diagram, and the layer
 * dimension collapses into card stacks. Every tile is a [LayerStacked] deck — the residual
 * spine and SwiGLU limb stack all layers, the gated short-conv limb stacks the conv layers,
 * the GQA attention limb stacks the attention layers — and flipping a layer flips the whole
 * scene together, dimming the limb that layer doesn't use.
 *
 * The display graph projects two representative layers (the first conv layer and the first
 * attention layer's mixer arm) onto a canonical `block.*` namespace via [PlanGraph]'s alias:
 * their `operator_norm` and `mixer_residual` ops merge into single nodes, so the shared
 * pre-mixer norm feeds both limbs and both mixer outputs converge on one residual ⊕ junction.
 *
 * A depth strip — every residual checkpoint at mini scale, with the logit lens — sits to the
 * left as orientation and doubles as the layer selector; the rows the selected block spans
 * carry a standing highlight.
 */
object Lfm2StackCompositor {

    fun buildScene(model: Lfm2Model): CompositorScene {
        val config = model.config
        val plan = model.plan
        // One sequence axis: every token-axis tile spans the model's full context window, so
        // nothing silently freezes past a display cutoff and the caches share the scale.
        val window = config.maxSeqLen
        val allLayers = (0 until config.numLayers).toList()
        val convLayers = allLayers.filter { it !in config.attentionLayers }
        val attnLayers = config.attentionLayers.sorted()
        val convRep = convLayers.first()
        val attnRep = attnLayers.first()
        require(attnRep != convRep + 1) {
            "Representative layers must not be adjacent, or their spine ports collide in the alias"
        }

        fun inputName(layer: Int) = if (layer == 0) "embed" else "layers.${layer - 1}.resid"

        val alias = fun(name: String): String = when {
            name == inputName(convRep) || name == inputName(attnRep) -> "block.in"
            name.startsWith("layers.$convRep.") -> "block." + name.removePrefix("layers.$convRep.")
            name.startsWith("layers.$attnRep.") -> "block." + name.removePrefix("layers.$attnRep.")
            name.startsWith("model.layers.$convRep.") -> "block.w." + name.removePrefix("model.layers.$convRep.")
            name.startsWith("model.layers.$attnRep.") -> "block.w." + name.removePrefix("model.layers.$attnRep.")
            else -> name
        }

        // The attention arm: ops computing layers.attnRep.attn.out from the block input and the
        // rope angles, found by walking writers backwards. Everything below the mixer (SwiGLU,
        // residuals) comes from the conv representative; the arm's mixer_residual merges with it.
        val writerByName = plan.ops.flatMap { op -> op.outputs.map { it.name to op } }.toMap()
        val attnArm = HashSet<TensorOp>()
        val armStack = ArrayDeque<String>().apply { add("layers.$attnRep.attn.out") }
        val armSeen = HashSet<String>()
        while (armStack.isNotEmpty()) {
            val port = armStack.removeLast()
            if (!armSeen.add(port)) continue
            if (port == inputName(attnRep) || port.startsWith("rope.")) continue
            val op = writerByName[port] ?: continue
            if (attnArm.add(op)) op.inputs.forEach { armStack.add(it.name) }
        }
        val displayPlan = OpPlan(plan.ops.filter {
            it.name.startsWith("layers.$convRep.") || it in attnArm || it.name == "layers.$attnRep.mixer_residual"
        })
        val scene = CompositorScene(PlanGraph(displayPlan, alias))
        scene.layerCount = config.numLayers

        // One shared scale per axis, so relative tile sizes are facts about the model: the
        // feature axis is anchored at hidden size (k/v strips come out exactly half of q, the
        // MLP inner tiles 2.5x wide, bcx 3x); the token axis is shared by every history row and
        // both sides of the attention triangle. Weights use their own quarter scale — true
        // within the weight class, small enough to ride edges as satellites — and tiles too
        // small to draw at scale are floored and marked [TensorTile.magnified] (dashed border)
        // so the break is explicit.
        val pxPerDim = ACTIVATION_WIDTH / config.hiddenSize
        val weightPxPerDim = pxPerDim / 4
        fun featureWidth(dims: Int) = dims * pxPerDim
        // The token axis uses the SAME px-per-cell scale as the feature axis, so cells are
        // square and every tile's aspect ratio is literal (q at window 512 really is twice as
        // wide as tall). Tiny windows floor the axis and wear the magnified marker.
        val tokenExtent = maxOf(window * pxPerDim, TOKEN_AXIS_MIN)
        val tokenAxisFloored = window * pxPerDim < TOKEN_AXIS_MIN

        fun stackedHistory(
            id: String, layers: List<Int>, title: String, w: Double,
            kind: TileKind = TileKind.ACTIVATION, portOf: (Int) -> String,
        ) {
            scene.addTile(VectorHistoryTile(
                ports = layers.map { plan.port(portOf(it)) },
                rows = window, title = title, kind = kind, id = id, stackLayers = layers,
            ).apply { width = w; height = tokenExtent; magnified = tokenAxisFloored })
        }

        fun stackedWeight(id: String, layers: List<Int>, title: String) {
            // Real-scale weight matrices have heavy outliers; quantile-normalize or they wash gray.
            val shape = plan.port("model.layers.${layers.first()}." + id.removePrefix("block.w.")).tensor
            scene.addTile(MatrixTile(
                id = id, title = title,
                tensors = layers.map { plan.port("model.layers.$it." + id.removePrefix("block.w.")).tensor },
                kind = TileKind.WEIGHT, quantileNorm = true, stackLayers = layers,
            ).apply { width = shape.cols * weightPxPerDim; height = shape.rows * weightPxPerDim })
        }

        stackedHistory("block.in", allLayers, "block in", featureWidth(config.hiddenSize), TileKind.RESIDUAL) { inputName(it) }
        stackedHistory("block.mixer_resid", allLayers, "+ mixer", featureWidth(config.hiddenSize), TileKind.RESIDUAL) { "layers.$it.mixer_resid" }
        stackedHistory("block.resid", allLayers, "+ mlp (block out)", featureWidth(config.hiddenSize), TileKind.RESIDUAL) { "layers.$it.resid" }

        stackedWeight("block.w.conv.in_proj.weight", convLayers, "in_proj")
        stackedWeight("block.w.conv.out_proj.weight", convLayers, "out_proj")
        stackedHistory("block.conv.bcx", convLayers, "B·C·x (in_proj)", featureWidth(3 * config.hiddenSize)) { "layers.$it.conv.bcx" }
        stackedHistory("block.conv.bx", convLayers, "B ⊙ x", featureWidth(config.hiddenSize)) { "layers.$it.conv.bx" }
        // The window and the kernel are dotted per channel, so they render in the same
        // orientation and width: taps as rows (time downward, newest last), channels across.
        scene.addTile(MatrixTile(
            id = "block.conv.cache", title = "conv window (last ${config.convKernel} tokens)",
            tensors = convLayers.map { plan.port("layers.$it.conv.cache").tensor },
            displayTransposed = true, stackLayers = convLayers,
        ).apply { width = featureWidth(config.hiddenSize); height = TAP_STRIP_HEIGHT; magnified = true })
        scene.addTile(MatrixTile(
            id = "block.w.conv.conv.weight", title = "kernel (${config.convKernel} taps)",
            tensors = convLayers.map { plan.port("model.layers.$it.conv.conv.weight").tensor },
            kind = TileKind.WEIGHT, quantileNorm = true, displayTransposed = true, stackLayers = convLayers,
        ).apply { width = featureWidth(config.hiddenSize); height = TAP_STRIP_HEIGHT; magnified = true })
        stackedHistory("block.conv.raw", convLayers, "conv", featureWidth(config.hiddenSize)) { "layers.$it.conv.raw" }
        stackedHistory("block.conv.gated", convLayers, "C ⊙ conv", featureWidth(config.hiddenSize)) { "layers.$it.conv.gated" }
        stackedHistory("block.conv.out", convLayers, "conv out", featureWidth(config.hiddenSize)) { "layers.$it.conv.out" }

        stackedWeight("block.w.self_attn.q_proj.weight", attnLayers, "Wq")
        stackedWeight("block.w.self_attn.k_proj.weight", attnLayers, "Wk")
        stackedWeight("block.w.self_attn.v_proj.weight", attnLayers, "Wv")
        stackedWeight("block.w.self_attn.out_proj.weight", attnLayers, "Wo")
        for (angle in listOf("cos", "sin")) {
            scene.addTile(VectorHistoryTile(plan.port("rope.$angle"), window, "rope $angle", TileKind.ACTIVATION).apply {
                width = maxOf(featureWidth(config.headDim / 2), SLIVER_MIN_WIDTH)
                height = tokenExtent
                magnified = tokenAxisFloored || featureWidth(config.headDim / 2) < SLIVER_MIN_WIDTH
            })
        }
        // q is the one trajectory no cache holds, so it is retained for EVERY attention layer:
        // it is the sufficient statistic from which a flip re-derives the whole attention limb.
        scene.addTile(VectorHistoryTile(
            ports = attnLayers.map { plan.port("layers.$it.attn.q") },
            rows = window, title = "q (${config.numHeads} heads)", kind = TileKind.ACTIVATION,
            id = "block.attn.q", stackLayers = attnLayers, retainAllLayers = true,
        ).apply {
            width = featureWidth(config.numHeads * config.headDim)
            height = tokenExtent
            magnified = tokenAxisFloored
        })
        // k and v are one row in flight: their history IS the cache, so drawing it here too
        // would duplicate the cache tiles. q keeps its history — it has no cache anywhere.
        fun tokenVector(id: String, title: String) {
            scene.addTile(MatrixTile(
                id = id, title = title,
                tensors = attnLayers.map { plan.port("layers.$it.${id.removePrefix("block.")}").tensor },
                stackLayers = attnLayers,
            ).apply { width = featureWidth(config.kvDim); height = TOKEN_ROW_HEIGHT; magnified = true })
        }
        tokenVector("block.attn.k", "k (new cache row)")
        tokenVector("block.attn.v", "v (new cache row)")
        scene.addTile(DeckTile(
            id = "block.attn.k_cache", title = "k cache",
            tensors = attnLayers.map { plan.port("layers.$it.attn.k_cache").tensor },
            slices = config.numKvHeads, signedNorm = true, columnSlices = true, stackLayers = attnLayers,
        ).apply {
            width = maxOf(featureWidth(config.headDim), SLIVER_MIN_WIDTH)
            height = tokenExtent
            magnified = tokenAxisFloored || featureWidth(config.headDim) < SLIVER_MIN_WIDTH
        })
        scene.addTile(DeckTile(
            id = "block.attn.v_cache", title = "v cache",
            tensors = attnLayers.map { plan.port("layers.$it.attn.v_cache").tensor },
            slices = config.numKvHeads, signedNorm = true, columnSlices = true, stackLayers = attnLayers,
        ).apply {
            width = maxOf(featureWidth(config.headDim), SLIVER_MIN_WIDTH)
            height = tokenExtent
            magnified = tokenAxisFloored || featureWidth(config.headDim) < SLIVER_MIN_WIDTH
        })
        scene.addTile(AttentionTile(
            ports = attnLayers.map { plan.port("layers.$it.attn.weights") },
            numHeads = config.numHeads, seqLen = window,
            title = "attention", id = "block.attn.weights", stackLayers = attnLayers,
        ).apply { width = tokenExtent; height = tokenExtent; magnified = tokenAxisFloored })
        stackedHistory("block.attn.context", attnLayers, "context", featureWidth(config.numHeads * config.headDim)) { "layers.$it.attn.context" }
        stackedHistory("block.attn.out", attnLayers, "attn out", featureWidth(config.hiddenSize)) { "layers.$it.attn.out" }

        stackedWeight("block.w.feed_forward.w1.weight", allLayers, "W1 (gate)")
        stackedWeight("block.w.feed_forward.w3.weight", allLayers, "W3 (up)")
        stackedWeight("block.w.feed_forward.w2.weight", allLayers, "W2 (down)")
        stackedHistory("block.mlp.gate", allLayers, "gate", featureWidth(config.intermediateSize)) { "layers.$it.mlp.gate" }
        stackedHistory("block.mlp.up", allLayers, "up", featureWidth(config.intermediateSize)) { "layers.$it.mlp.up" }
        stackedHistory("block.mlp.act", allLayers, "silu(gate) ⊙ up", featureWidth(config.intermediateSize)) { "layers.$it.mlp.act" }
        stackedHistory("block.mlp.out", allLayers, "mlp out", featureWidth(config.hiddenSize)) { "layers.$it.mlp.out" }

        // Substructure marks: head boundaries on the head-packed vectors, and the B|C|x chunk
        // boundaries on the fused conv projection (labels) with matching row blocks on in_proj.
        // Strand counts trace head parallelism through the limb: born at norm+rope, 16 on the
        // query side against 8 on the key/value side, dying at the output projection.
        val headTicks = (1 until config.numHeads).map { it * config.headDim }
        val kvHeadTicks = (1 until config.numKvHeads).map { it * config.headDim }
        scene.tile("block.attn.q").apply { columnTicks = headTicks; strands = config.numHeads }
        scene.tile("block.attn.context").apply { columnTicks = headTicks; strands = config.numHeads }
        scene.tile("block.attn.k").apply { columnTicks = kvHeadTicks; strands = config.numKvHeads }
        scene.tile("block.attn.v").apply { columnTicks = kvHeadTicks; strands = config.numKvHeads }
        scene.tile("block.conv.bcx").apply {
            columnTicks = listOf(config.hiddenSize, 2 * config.hiddenSize)
            blockLabels = listOf("B", "C", "x")
        }
        scene.tile("block.w.conv.in_proj.weight").rowTicks = listOf(config.hiddenSize, 2 * config.hiddenSize)

        scene.connectFromGraph()
        CompositorLayout().apply(scene)

        // The GQA story: wheel-flipping the attention deck flips the cache decks to the serving
        // KV group, and the cache arrows carry standing emphasis so the sharing path reads.
        val kCacheTile = scene.tile("block.attn.k_cache") as DeckTile
        val vCacheTile = scene.tile("block.attn.v_cache") as DeckTile
        val qPerKv = config.numHeads / config.numKvHeads
        val servingLabel: (Int) -> String = { group ->
            "$group/${config.numKvHeads} → q ${group * qPerKv}–${(group + 1) * qPerKv - 1}"
        }
        kCacheTile.sliceLabel = servingLabel
        vCacheTile.sliceLabel = servingLabel
        scene.onHeadSelected = { tile, head ->
            if (tile is AttentionTile) {
                val group = head / qPerKv
                kCacheTile.selectedSlice = group
                vCacheTile.selectedSlice = group
            }
        }
        // Cross-time reads: the KV caches and the conv window both feed PAST tokens' state into
        // the current step — the only edges in the diagram that aren't this-token dataflow.
        scene.memoryEdges = scene.edges.filter { edge ->
            (edge.from as? TensorTile)?.id?.let { it.endsWith("_cache") || it == "block.conv.cache" } == true
        }.toSet()

        // Slice identity: the conv gates read chunks of the ticked bcx projection. Each edge
        // carries the chunk indices it reads — the gate's offsets located against the tile's
        // tick boundaries — rendered as identity-colored strands matching the segment bars.
        val bcxTile = scene.tile("block.conv.bcx")
        val chunkStarts = listOf(0) + bcxTile.columnTicks
        for (edge in scene.edges) {
            if ((edge.from as? TensorTile)?.id != bcxTile.id) continue
            val gate = (edge.to as? OpVertex)?.op as? OffsetGateOp ?: continue
            edge.sliceBlocks = buildList {
                if (alias(gate.a.name) == bcxTile.id) add(chunkStarts.indexOfLast { it <= gate.aOffset })
                if (alias(gate.b.name) == bcxTile.id) add(chunkStarts.indexOfLast { it <= gate.bOffset })
            }.sorted()
        }

        // The depth strip: every residual checkpoint at mini scale with the logit lens, placed
        // left of the block after layout (it takes no part in edge derivation or ranking). Its
        // rows select the layer whose block the diagram shows.
        val residPorts = listOf(plan.port("embed")) + allLayers.map { plan.port("layers.$it.resid") }
        val blockLeft = scene.tiles.minOf { it.x }
        val blockTop = scene.tiles.minOf { it.y }
        val stripTiles = residPorts.mapIndexed { i, port ->
            val label = if (i == 0) "embed" else
                "layer ${i - 1} (${if (i - 1 in config.attentionLayers) "attn" else "conv"})"
            VectorHistoryTile(port, window, label).apply {
                x = blockLeft - STRIP_CLEARANCE - STRIP_WIDTH
                y = blockTop + i * (STRIP_ROW_HEIGHT + STRIP_ROW_GAP)
                width = STRIP_WIDTH
                height = STRIP_ROW_HEIGHT
            }.also { scene.addTile(it) }
        }
        scene.lens = LogitLens(
            embedWeight = plan.port("model.embed_tokens.weight").tensor,
            normWeight = plan.port("model.embedding_norm.weight").tensor,
            eps = config.normEps,
            sources = residPorts,
        )
        scene.layerOfTile = { tile -> stripTiles.indexOf(tile).takeIf { it >= 0 }?.let { (it - 1).coerceAtLeast(0) } }

        fun convSide(key: String) = key.startsWith("block.conv.") || key.startsWith("block.w.conv.")
        fun attnSide(key: String) =
            key.startsWith("block.attn.") || key.startsWith("block.w.self_attn.") || key.startsWith("rope.")

        fun endpointKey(e: FlowEndpoint) = when (e) {
            is TensorTile -> e.id
            is OpVertex -> alias(e.op.name)
        }

        // Flip backfills: watched-layer retention means a flip to a layer outside the stash
        // starts blank, then history is re-derived where the model's real state allows it —
        // the attention limb from retained q against the live KV caches (through the real ops,
        // so a derived row is exactly what live recording would have stored), and the spine
        // checkpoints by copying the depth strip. Conv/mlp internals have no such source.
        val qTile = scene.tile("block.attn.q") as VectorHistoryTile
        val weightsTile = scene.tile("block.attn.weights") as AttentionTile
        val contextTile = scene.tile("block.attn.context") as VectorHistoryTile
        val attnOutTile = scene.tile("block.attn.out") as VectorHistoryTile
        val blockInTile = scene.tile("block.in") as VectorHistoryTile
        val blockOutTile = scene.tile("block.resid") as VectorHistoryTile
        val backfillState = Lfm2DecodeState()
        val qScratch = TensorPort("backfill.q", FloatTensor(1, config.numHeads * config.headDim))
        val weightsScratch = TensorPort("backfill.weights", FloatTensor(config.numHeads, config.maxSeqLen))
        val contextScratch = TensorPort("backfill.context", FloatTensor(1, config.numHeads * config.headDim))
        val attnOutScratch = TensorPort("backfill.out", FloatTensor(1, config.hiddenSize))
        val qRow = FloatArray(config.numHeads * config.headDim)
        val contextRow = FloatArray(config.numHeads * config.headDim)
        val attnOutRow = FloatArray(config.hiddenSize)

        fun backfillAttentionLimb(layer: Int) {
            val stackIndex = attnLayers.indexOf(layer)
            if (stackIndex < 0) return
            val scores = AttendScoresOp("backfill.scores", qScratch, plan.port("layers.$layer.attn.k_cache"),
                weightsScratch, backfillState, config.numHeads, config.numKvHeads, config.headDim)
            val mix = AttendMixOp("backfill.mix", weightsScratch, plan.port("layers.$layer.attn.v_cache"),
                contextScratch, backfillState, config.numHeads, config.numKvHeads, config.headDim)
            val outProj = LinearOp("backfill.out_proj",
                plan.port("model.layers.$layer.self_attn.out_proj.weight"), contextScratch, attnOutScratch)
            for (t in 0 until minOf(model.position, window)) {
                qTile.copyHistoryRow(stackIndex, t, qRow)
                qScratch.tensor.copyFrom(qRow)
                backfillState.position = t
                scores.forward()
                mix.forward()
                outProj.forward()
                weightsTile.backfillRow(t, weightsScratch.tensor)
                contextScratch.tensor.data.duplicate().get(contextRow)
                contextTile.backfillRow(t, contextRow)
                attnOutScratch.tensor.data.duplicate().get(attnOutRow)
                attnOutTile.backfillRow(t, attnOutRow)
            }
        }

        fun backfillFromStrip(target: VectorHistoryTile, source: VectorHistoryTile) {
            for (t in 0 until minOf(model.position, window)) {
                target.backfillRow(t, source.values, t * source.cols)
            }
        }

        scene.layerSelector = { raw ->
            val layer = raw.mod(config.numLayers)
            scene.selectedLayer = layer
            val attnActive = layer in config.attentionLayers
            val deriveAttention = attnActive && !weightsTile.hasHistoryFor(layer)
            val copyBlockIn = !blockInTile.hasHistoryFor(layer)
            val copyBlockOut = !blockOutTile.hasHistoryFor(layer)
            fun inactive(key: String) = (convSide(key) && attnActive) || (attnSide(key) && !attnActive)
            for (tile in scene.tiles) {
                (tile as? LayerStacked)?.takeIf { it.stackLayers.isNotEmpty() }?.showLayer(layer)
                tile.dimmed = inactive(tile.id)
            }
            for (vertex in scene.opVertices) vertex.dimmed = inactive(alias(vertex.op.name))
            for (edge in scene.edges) {
                val keys = listOf(endpointKey(edge.from), endpointKey(edge.to)) + edge.ops.map { alias(it.name) }
                edge.dimmed = keys.any(::inactive)
            }
            if (deriveAttention) backfillAttentionLimb(layer)
            if (copyBlockIn) backfillFromStrip(blockInTile, stripTiles[layer])
            if (copyBlockOut) backfillFromStrip(blockOutTile, stripTiles[layer + 1])
            scene.highlightedTiles = setOf(stripTiles[layer], stripTiles[layer + 1])
        }
        scene.layerSelector?.invoke(0)
        return scene
    }

    /** The feature-axis anchor: hidden-size vectors render this wide; everything else scales. */
    private const val ACTIVATION_WIDTH = 110.0

    /** Legibility floor for the token axis on tiny context windows; floored tiles are marked. */
    private const val TOKEN_AXIS_MIN = 24.0

    /** Floor for a single-token row strip (true height would be one token — a few px). */
    private const val TOKEN_ROW_HEIGHT = 12.0

    /** Floor for the conv window / kernel tap strips (true height would be k tokens). */
    private const val TAP_STRIP_HEIGHT = 20.0

    /** Floor for sliver tiles a few dims wide at feature scale (rope angles, cache head slices). */
    private const val SLIVER_MIN_WIDTH = 24.0

    private const val STRIP_WIDTH = 170.0
    private const val STRIP_ROW_HEIGHT = 26.0
    private const val STRIP_ROW_GAP = 20.0
    private const val STRIP_CLEARANCE = 320.0
}
