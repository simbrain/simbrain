package org.simbrain.network.compositor

import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.tensor.op.OpPlan

/**
 * One LFM2 layer's full anatomy as a compositor scene, in the teaching-transformer style: a
 * local residual spine (the layer's input checkpoint → mixer rejoin → layer output), the mixer
 * limb — gated short convolution with its live k-tap sliding window, or GQA attention with a
 * per-Q-head attention deck and column-sliced KV-cache decks — and the SwiGLU limb, with weight
 * tiles riding their edges and multi-input ops as junction vertices. Everything derives from the
 * plan graph.
 *
 * Activation tiles accumulate one history row per generated token from the moment the scene is
 * built (the ports hold only the current token), so structure emerges as generation runs.
 */
object Lfm2LayerCompositor {

    fun buildScene(model: Lfm2Model, layer: Int, displaySeq: Int): CompositorScene {
        val config = model.config
        val plan = model.plan
        require(layer in 0 until config.numLayers) { "Layer $layer out of range" }
        val prefix = "layers.$layer"
        val weightPrefix = "model.layers.$layer"
        val attention = layer in config.attentionLayers

        // Scope the graph to this layer's ops: the layer input, rope angles, and conv window
        // become pure sources, and walks can't wander through the other thirteen layers.
        val layerPlan = OpPlan(plan.ops.filter { it.name.startsWith("$prefix.") })
        val scene = CompositorScene(PlanGraph(layerPlan))

        fun history(portName: String, title: String, w: Double, h: Double, kind: TileKind = TileKind.ACTIVATION) {
            scene.addTile(VectorHistoryTile(plan.port(portName), displaySeq, title, kind).apply {
                width = w; height = h
            })
        }

        fun weight(name: String, title: String, size: Double = WEIGHT_SIZE) {
            // Real-scale weight matrices have heavy outliers; quantile-normalize or they wash gray.
            scene.addTile(MatrixTile(plan.port(name), kind = TileKind.WEIGHT, title = title, quantileNorm = true).apply {
                width = size; height = size
            })
        }

        val inputPort = if (layer == 0) "embed" else "layers.${layer - 1}.resid"
        history(inputPort, if (layer == 0) "embed" else "layer ${layer - 1} out", SPINE_WIDTH, SPINE_HEIGHT, TileKind.RESIDUAL)
        history("$prefix.mixer_resid", "+ ${if (attention) "attention" else "conv"}", SPINE_WIDTH, SPINE_HEIGHT, TileKind.RESIDUAL)
        history("$prefix.resid", "+ mlp (layer $layer out)", SPINE_WIDTH, SPINE_HEIGHT, TileKind.RESIDUAL)

        if (attention) {
            weight("$weightPrefix.self_attn.q_proj.weight", "Wq")
            weight("$weightPrefix.self_attn.k_proj.weight", "Wk")
            weight("$weightPrefix.self_attn.v_proj.weight", "Wv")
            weight("$weightPrefix.self_attn.out_proj.weight", "Wo", 60.0)
            history("rope.cos", "rope cos", ROPE_WIDTH, ROPE_HEIGHT)
            history("rope.sin", "rope sin", ROPE_WIDTH, ROPE_HEIGHT)
            history("$prefix.attn.q", "q (${config.numHeads} heads)", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
            history("$prefix.attn.k", "k (${config.numKvHeads} kv heads)", ACTIVATION_WIDTH * 0.6, ACTIVATION_HEIGHT)
            history("$prefix.attn.v", "v (${config.numKvHeads} kv heads)", ACTIVATION_WIDTH * 0.6, ACTIVATION_HEIGHT)
            scene.addTile(DeckTile(
                plan.port("$prefix.attn.k_cache"), slices = config.numKvHeads,
                title = "k cache", signedNorm = true, columnSlices = true,
            ).apply { width = DECK_SIZE; height = DECK_SIZE })
            scene.addTile(DeckTile(
                plan.port("$prefix.attn.v_cache"), slices = config.numKvHeads,
                title = "v cache", signedNorm = true, columnSlices = true,
            ).apply { width = DECK_SIZE; height = DECK_SIZE })
            scene.addTile(AttentionTile(
                plan.port("$prefix.attn.weights"), config.numHeads, displaySeq, "attention",
            ).apply { width = DECK_SIZE; height = DECK_SIZE })
            history("$prefix.attn.context", "context", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
            history("$prefix.attn.out", "attn out", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
        } else {
            weight("$weightPrefix.conv.in_proj.weight", "in_proj")
            weight("$weightPrefix.conv.conv.weight", "kernel", 60.0)
            weight("$weightPrefix.conv.out_proj.weight", "out_proj", 60.0)
            history("$prefix.conv.bcx", "B·C·x (in_proj)", ACTIVATION_WIDTH * 1.4, ACTIVATION_HEIGHT)
            history("$prefix.conv.bx", "B ⊙ x", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
            scene.addTile(MatrixTile(
                plan.port("$prefix.conv.cache"),
                title = "conv window (k=${config.convKernel})",
                displayTransposed = true,
            ).apply { width = ACTIVATION_WIDTH; height = CONV_WINDOW_HEIGHT })
            history("$prefix.conv.raw", "conv", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
            history("$prefix.conv.gated", "C ⊙ conv", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
            history("$prefix.conv.out", "conv out", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
        }

        weight("$weightPrefix.feed_forward.w1.weight", "W1 (gate)")
        weight("$weightPrefix.feed_forward.w3.weight", "W3 (up)")
        weight("$weightPrefix.feed_forward.w2.weight", "W2 (down)")
        history("$prefix.mlp.gate", "gate", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
        history("$prefix.mlp.up", "up", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
        history("$prefix.mlp.act", "silu(gate) ⊙ up", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)
        history("$prefix.mlp.out", "mlp out", ACTIVATION_WIDTH, ACTIVATION_HEIGHT)

        scene.connectFromGraph()
        CompositorLayout().apply(scene)
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
}
