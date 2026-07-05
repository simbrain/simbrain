package org.simbrain.network.compositor

import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.tensor.op.OpPlan
import org.simbrain.network.tensor.op.TensorOp

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

    fun buildScene(model: Lfm2Model, displaySeq: Int): CompositorScene {
        val config = model.config
        val plan = model.plan
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

        fun stackedHistory(
            id: String, layers: List<Int>, title: String, w: Double, h: Double,
            kind: TileKind = TileKind.ACTIVATION, portOf: (Int) -> String,
        ) {
            scene.addTile(VectorHistoryTile(
                ports = layers.map { plan.port(portOf(it)) },
                rows = displaySeq, title = title, kind = kind, id = id, stackLayers = layers,
            ).apply { width = w; height = h })
        }

        fun stackedWeight(id: String, layers: List<Int>, title: String, size: Double = WEIGHT_SIZE) {
            // Real-scale weight matrices have heavy outliers; quantile-normalize or they wash gray.
            scene.addTile(MatrixTile(
                id = id, title = title,
                tensors = layers.map { plan.port("model.layers.$it." + id.removePrefix("block.w.")).tensor },
                kind = TileKind.WEIGHT, quantileNorm = true, stackLayers = layers,
            ).apply { width = size; height = size })
        }

        stackedHistory("block.in", allLayers, "block in", SPINE_WIDTH, SPINE_HEIGHT, TileKind.RESIDUAL) { inputName(it) }
        stackedHistory("block.mixer_resid", allLayers, "+ mixer", SPINE_WIDTH, SPINE_HEIGHT, TileKind.RESIDUAL) { "layers.$it.mixer_resid" }
        stackedHistory("block.resid", allLayers, "+ mlp (block out)", SPINE_WIDTH, SPINE_HEIGHT, TileKind.RESIDUAL) { "layers.$it.resid" }

        stackedWeight("block.w.conv.in_proj.weight", convLayers, "in_proj")
        stackedWeight("block.w.conv.conv.weight", convLayers, "kernel", 60.0)
        stackedWeight("block.w.conv.out_proj.weight", convLayers, "out_proj", 60.0)
        stackedHistory("block.conv.bcx", convLayers, "B·C·x (in_proj)", ACTIVATION_WIDTH * 1.4, ACTIVATION_HEIGHT) { "layers.$it.conv.bcx" }
        stackedHistory("block.conv.bx", convLayers, "B ⊙ x", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.conv.bx" }
        scene.addTile(MatrixTile(
            id = "block.conv.cache", title = "conv window (k=${config.convKernel})",
            tensors = convLayers.map { plan.port("layers.$it.conv.cache").tensor },
            displayTransposed = true, stackLayers = convLayers,
        ).apply { width = ACTIVATION_WIDTH; height = CONV_WINDOW_HEIGHT })
        stackedHistory("block.conv.raw", convLayers, "conv", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.conv.raw" }
        stackedHistory("block.conv.gated", convLayers, "C ⊙ conv", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.conv.gated" }
        stackedHistory("block.conv.out", convLayers, "conv out", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.conv.out" }

        stackedWeight("block.w.self_attn.q_proj.weight", attnLayers, "Wq")
        stackedWeight("block.w.self_attn.k_proj.weight", attnLayers, "Wk")
        stackedWeight("block.w.self_attn.v_proj.weight", attnLayers, "Wv")
        stackedWeight("block.w.self_attn.out_proj.weight", attnLayers, "Wo", 60.0)
        scene.addTile(VectorHistoryTile(plan.port("rope.cos"), displaySeq, "rope cos", TileKind.ACTIVATION).apply {
            width = ROPE_WIDTH; height = ROPE_HEIGHT
        })
        scene.addTile(VectorHistoryTile(plan.port("rope.sin"), displaySeq, "rope sin", TileKind.ACTIVATION).apply {
            width = ROPE_WIDTH; height = ROPE_HEIGHT
        })
        stackedHistory("block.attn.q", attnLayers, "q (${config.numHeads} heads)", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.attn.q" }
        stackedHistory("block.attn.k", attnLayers, "k (${config.numKvHeads} kv heads)", ACTIVATION_WIDTH * 0.6, ACTIVATION_HEIGHT) { "layers.$it.attn.k" }
        stackedHistory("block.attn.v", attnLayers, "v (${config.numKvHeads} kv heads)", ACTIVATION_WIDTH * 0.6, ACTIVATION_HEIGHT) { "layers.$it.attn.v" }
        scene.addTile(DeckTile(
            id = "block.attn.k_cache", title = "k cache",
            tensors = attnLayers.map { plan.port("layers.$it.attn.k_cache").tensor },
            slices = config.numKvHeads, signedNorm = true, columnSlices = true, stackLayers = attnLayers,
        ).apply { width = DECK_SIZE; height = DECK_SIZE })
        scene.addTile(DeckTile(
            id = "block.attn.v_cache", title = "v cache",
            tensors = attnLayers.map { plan.port("layers.$it.attn.v_cache").tensor },
            slices = config.numKvHeads, signedNorm = true, columnSlices = true, stackLayers = attnLayers,
        ).apply { width = DECK_SIZE; height = DECK_SIZE })
        scene.addTile(AttentionTile(
            ports = attnLayers.map { plan.port("layers.$it.attn.weights") },
            numHeads = config.numHeads, seqLen = displaySeq,
            title = "attention", id = "block.attn.weights", stackLayers = attnLayers,
        ).apply { width = DECK_SIZE; height = DECK_SIZE })
        stackedHistory("block.attn.context", attnLayers, "context", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.attn.context" }
        stackedHistory("block.attn.out", attnLayers, "attn out", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.attn.out" }

        stackedWeight("block.w.feed_forward.w1.weight", allLayers, "W1 (gate)")
        stackedWeight("block.w.feed_forward.w3.weight", allLayers, "W3 (up)")
        stackedWeight("block.w.feed_forward.w2.weight", allLayers, "W2 (down)")
        stackedHistory("block.mlp.gate", allLayers, "gate", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.mlp.gate" }
        stackedHistory("block.mlp.up", allLayers, "up", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.mlp.up" }
        stackedHistory("block.mlp.act", allLayers, "silu(gate) ⊙ up", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.mlp.act" }
        stackedHistory("block.mlp.out", allLayers, "mlp out", ACTIVATION_WIDTH, ACTIVATION_HEIGHT) { "layers.$it.mlp.out" }

        scene.connectFromGraph()
        CompositorLayout().apply(scene)

        // The GQA story: wheel-flipping the attention deck flips the cache decks to the serving
        // KV group, and the cache arrows carry standing emphasis so the sharing path reads.
        val kCacheTile = scene.tile("block.attn.k_cache") as DeckTile
        val vCacheTile = scene.tile("block.attn.v_cache") as DeckTile
        val qPerKv = config.numHeads / config.numKvHeads
        fun servingLabel(name: String): (Int) -> String = { group ->
            "$name · kv head $group (serves q ${group * qPerKv}–${(group + 1) * qPerKv - 1})"
        }
        kCacheTile.sliceLabel = servingLabel("k cache")
        vCacheTile.sliceLabel = servingLabel("v cache")
        scene.onHeadSelected = { tile, head ->
            if (tile is AttentionTile) {
                val group = head / qPerKv
                kCacheTile.selectedSlice = group
                vCacheTile.selectedSlice = group
            }
        }
        scene.emphasizedEdges = scene.edges.filter { edge ->
            (edge.from as? TensorTile)?.id?.endsWith("_cache") == true
        }.toSet()

        // The depth strip: every residual checkpoint at mini scale with the logit lens, placed
        // left of the block after layout (it takes no part in edge derivation or ranking). Its
        // rows select the layer whose block the diagram shows.
        val residPorts = listOf(plan.port("embed")) + allLayers.map { plan.port("layers.$it.resid") }
        val blockLeft = scene.tiles.minOf { it.x }
        val blockTop = scene.tiles.minOf { it.y }
        val stripTiles = residPorts.mapIndexed { i, port ->
            val label = if (i == 0) "embed" else
                "layer ${i - 1} (${if (i - 1 in config.attentionLayers) "attn" else "conv"})"
            VectorHistoryTile(port, displaySeq, label).apply {
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

        scene.layerSelector = { raw ->
            val layer = raw.mod(config.numLayers)
            scene.selectedLayer = layer
            val attnActive = layer in config.attentionLayers
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
            scene.highlightedTiles = setOf(stripTiles[layer], stripTiles[layer + 1])
        }
        scene.layerSelector?.invoke(0)
        return scene
    }

    private const val SPINE_WIDTH = 190.0
    private const val SPINE_HEIGHT = 90.0
    private const val WEIGHT_SIZE = 70.0
    private const val ACTIVATION_WIDTH = 110.0
    private const val ACTIVATION_HEIGHT = 70.0
    private const val DECK_SIZE = 120.0
    private const val ROPE_WIDTH = 60.0
    private const val ROPE_HEIGHT = 50.0
    private const val CONV_WINDOW_HEIGHT = 26.0
    private const val STRIP_WIDTH = 170.0
    private const val STRIP_ROW_HEIGHT = 26.0
    private const val STRIP_ROW_GAP = 20.0
    private const val STRIP_CLEARANCE = 320.0
}
