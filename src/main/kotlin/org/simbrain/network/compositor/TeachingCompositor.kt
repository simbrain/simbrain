package org.simbrain.network.compositor

import org.simbrain.network.llm.TeachingTransformerModel

/**
 * Assembles the compositor scene for a [TeachingTransformerModel]: the residual stream as a
 * vertical spine of real seq x dim checkpoint tiles (the trunk between checkpoints IS the skip
 * connection), with the attention and MLP limbs branching right and rejoining the spine at
 * add-glyph junctions, weight tiles feeding each projection, the multi-head attention deck, and
 * a logit lens docked beside every checkpoint. Edges and their op glyphs are derived from the
 * plan graph, never hand-wired.
 *
 * Data flows top-down: embeddings at the top, logits and the prediction at the bottom.
 */
object TeachingCompositor {

    fun buildScene(model: TeachingTransformerModel, weightsTransposed: Boolean = false): CompositorScene {
        val config = model.config
        val scene = CompositorScene(PlanGraph(model.plan))

        fun weightTile(name: String, title: String, x: Double, y: Double, size: Double = WEIGHT_SIZE) {
            scene.addTile(MatrixTile(
                model.params.getValue(name),
                kind = TileKind.WEIGHT,
                title = title,
                displayTransposed = weightsTransposed,
            ).apply {
                this.x = x; this.y = y
                width = size; height = size
            })
        }

        fun activationTile(portName: String, title: String, x: Double, y: Double, w: Double, h: Double) {
            scene.addTile(MatrixTile(model.plan.port(portName), title = title).apply {
                this.x = x; this.y = y
                width = w; height = h
            })
        }

        fun spineTile(portName: String, title: String, y: Double) {
            scene.addTile(MatrixTile(
                model.plan.port(portName),
                kind = TileKind.RESIDUAL,
                title = title,
                quantileNorm = true,
            ).apply {
                x = 0.0; this.y = y
                width = SPINE_WIDTH; height = SPINE_HEIGHT
            })
        }

        weightTile("embed.table", "embedding", -230.0, 0.0)
        weightTile("embed.pos", "positions", -130.0, 0.0)
        spineTile("resid0", "residual in", 0.0)

        for (l in 0 until config.numLayers) {
            val prefix = "layers.$l"
            val top = LAYER_TOP + l * LAYER_PITCH

            weightTile("$prefix.attn.wq", "Wq", WEIGHT_X, top)
            weightTile("$prefix.attn.wk", "Wk", WEIGHT_X, top + 95.0)
            weightTile("$prefix.attn.wv", "Wv", WEIGHT_X, top + 190.0)
            activationTile("$prefix.attn.q", "q", QKV_X, top, QKV_WIDTH, QKV_HEIGHT)
            activationTile("$prefix.attn.k", "k", QKV_X, top + 95.0, QKV_WIDTH, QKV_HEIGHT)
            activationTile("$prefix.attn.v", "v", QKV_X, top + 190.0, QKV_WIDTH, QKV_HEIGHT)
            scene.addTile(DeckTile(
                model.plan.port("$prefix.attn.weights"),
                slices = config.numHeads,
                title = "attention",
            ).apply {
                x = DECK_X; y = top + 25.0
                width = DECK_SIZE; height = DECK_SIZE
            })
            weightTile("$prefix.attn.wo", "Wo", ATTN_OUT_X, top - 40.0, 60.0)
            activationTile("$prefix.attn.out", "attn out", ATTN_OUT_X, top + 60.0, QKV_WIDTH, QKV_HEIGHT)
            spineTile("$prefix.attn_resid", "residual + attn", top + 210.0)

            val mlpTop = top + 420.0
            weightTile("$prefix.mlp.w1", "W1", WEIGHT_X, mlpTop)
            activationTile("$prefix.mlp.act", "hidden (ReLU)", QKV_X, mlpTop, QKV_WIDTH, QKV_HEIGHT)
            weightTile("$prefix.mlp.w2", "W2", DECK_X, mlpTop)
            activationTile("$prefix.mlp.out", "mlp out", ATTN_OUT_X, mlpTop, QKV_WIDTH, QKV_HEIGHT)
            spineTile("$prefix.resid", "residual + mlp", mlpTop + 130.0)
        }

        val finalY = LAYER_TOP + config.numLayers * LAYER_PITCH + 90.0
        weightTile("unembed.weight", "unembedding", WEIGHT_X, finalY)
        activationTile("logits", "logits", 0.0, finalY, SPINE_WIDTH, 80.0)
        scene.addTile(MatrixTile(
            model.plan.port("probs"),
            title = "next-token probabilities",
            signedNorm = false,
        ).apply {
            x = 0.0; y = finalY + 130.0
            width = SPINE_WIDTH; height = 80.0
        })

        scene.connectFromGraph()

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
    private const val LAYER_TOP = 190.0
    private const val LAYER_PITCH = 680.0
    private const val WEIGHT_X = 260.0
    private const val WEIGHT_SIZE = 70.0
    private const val QKV_X = 380.0
    private const val QKV_WIDTH = 95.0
    private const val QKV_HEIGHT = 70.0
    private const val DECK_X = 540.0
    private const val DECK_SIZE = 130.0
    private const val ATTN_OUT_X = 730.0
}
