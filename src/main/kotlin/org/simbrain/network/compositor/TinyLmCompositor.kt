package org.simbrain.network.compositor

import org.simbrain.network.llm.TinyLmModel

/**
 * Assembles the compositor scene for a [TinyLmModel]: the residual stream as a
 * vertical spine of real seq x dim checkpoint tiles (the trunk between checkpoints IS the skip
 * connection), with the attention and MLP limbs branching right and rejoining the spine at
 * add-glyph junctions, weight and bias tiles riding the edges that carry their ops, the
 * multi-head attention deck, and a logit lens docked beside every checkpoint. Edges, their op
 * glyphs, and all tile positions are derived from the plan graph — never hand-wired.
 *
 * Data flows bottom-up: embeddings at the bottom, logits and the prediction at the top.
 */
object TinyLmCompositor {

    fun buildScene(
        model: TinyLmModel,
        weightsTransposed: Boolean = false,
        scale: Double = 1.0,
    ): CompositorScene {
        val config = model.config
        val scene = CompositorScene(PlanGraph(model.plan))

        fun s(value: Double) = value * scale

        fun weightTile(name: String, title: String, size: Double = WEIGHT_SIZE) {
            scene.addTile(MatrixTile(
                model.params.getValue(name),
                kind = TileKind.WEIGHT,
                title = if (weightsTransposed) "${title}ᵀ" else title,
                displayTransposed = weightsTransposed,
            ).apply {
                width = s(size)
                height = s(size)
            })
        }

        fun biasTile(name: String, title: String) {
            scene.addTile(MatrixTile(
                model.params.getValue(name),
                kind = TileKind.WEIGHT,
                title = title,
            ).apply {
                width = s(BIAS_WIDTH)
                height = s(BIAS_HEIGHT).coerceAtLeast(6.0)
            })
        }

        fun activationTile(portName: String, title: String, w: Double = ACTIVATION_WIDTH, h: Double = ACTIVATION_HEIGHT) {
            scene.addTile(MatrixTile(model.plan.port(portName), title = title).apply {
                width = s(w)
                height = s(h)
                tracksLiveRow = true
            })
        }

        fun spineTile(portName: String, title: String) {
            scene.addTile(MatrixTile(
                model.plan.port(portName),
                kind = TileKind.RESIDUAL,
                title = title,
                quantileNorm = true,
            ).apply {
                width = s(SPINE_WIDTH)
                height = s(SPINE_HEIGHT)
                tracksLiveRow = true
            })
        }

        weightTile("embed.table", "embedding")
        weightTile("embed.pos", "positions")
        spineTile("resid0", "residual in")

        for (l in 0 until config.numLayers) {
            val prefix = "layers.$l"
            weightTile("$prefix.attn.wq", "Wq")
            weightTile("$prefix.attn.wk", "Wk")
            weightTile("$prefix.attn.wv", "Wv")
            activationTile("$prefix.attn.q", "q")
            activationTile("$prefix.attn.k", "k")
            activationTile("$prefix.attn.v", "v")
            scene.addTile(DeckTile(
                model.plan.port("$prefix.attn.weights"),
                slices = config.numHeads,
                title = "attention",
            ).apply {
                width = s(DECK_SIZE)
                height = s(DECK_SIZE)
            })
            weightTile("$prefix.attn.wo", "Wo", 60.0)
            activationTile("$prefix.attn.out", "attn out")
            spineTile("$prefix.attn_resid", "residual + attn")

            weightTile("$prefix.mlp.w1", "W1")
            biasTile("$prefix.mlp.b1", "b1")
            activationTile("$prefix.mlp.act", "hidden")
            weightTile("$prefix.mlp.w2", "W2")
            biasTile("$prefix.mlp.b2", "b2")
            activationTile("$prefix.mlp.out", "mlp out")
            spineTile("$prefix.resid", "residual + mlp")
        }

        weightTile("unembed.weight", "unembedding")
        activationTile("logits", "logits", SPINE_WIDTH, HEAD_HEIGHT)
        scene.addTile(MatrixTile(
            model.plan.port("probs"),
            title = "next-token probabilities",
            signedNorm = false,
        ).apply {
            width = s(SPINE_WIDTH)
            height = s(HEAD_HEIGHT)
            tracksLiveRow = true
        })

        scene.connectFromGraph()
        CompositorLayout(
            scale,
            verticalFlow = VerticalFlow.BOTTOM_TO_TOP,
            density = LayoutDensity.COMPACT,
        ).apply(scene)

        for (tile in scene.tiles) {
            (tile as? MatrixTile)?.gradientSource = model.grads.of(tile.tensor)
        }

        val checkpoints = listOf(model.plan.port("resid0")) + (0 until config.numLayers).flatMap { l ->
            listOf(model.plan.port("layers.$l.attn_resid"), model.plan.port("layers.$l.resid"))
        }
        scene.lens = LogitLens(
            embedWeight = model.params.getValue("unembed.weight").tensor,
            normWeight = model.params.getValue("final_norm.gamma").tensor,
            eps = config.normEps,
            sources = checkpoints,
            normBias = model.params.getValue("final_norm.beta").tensor,
            meanCenter = true,
        )
        return scene
    }

    private const val SPINE_WIDTH = 170.0
    private const val SPINE_HEIGHT = 120.0
    private const val WEIGHT_SIZE = 70.0
    private const val ACTIVATION_WIDTH = 95.0
    private const val ACTIVATION_HEIGHT = 70.0
    private const val DECK_SIZE = 130.0
    private const val BIAS_WIDTH = 95.0
    private const val BIAS_HEIGHT = 8.0
    private const val HEAD_HEIGHT = 80.0
}
