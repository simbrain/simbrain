package org.simbrain.network.compositor

import org.simbrain.network.llm.Lfm2Model

/**
 * Assembles the M3 compositor scene for an [Lfm2Model]: the residual stream as one history tile
 * per layer (stacked bottom-up in data-flow order), a causal attention map for one attention
 * layer with retained per-head history, and a logit-lens strip beside the stack. Tile edges are
 * derived from the op graph, so the residual chain and the hop through the attention tile come
 * from the plan itself rather than hand-wiring.
 *
 * [displaySeq] caps how many token rows the history tiles retain and display; generation beyond
 * it still runs, the views just stop appending.
 */
object Lfm2Compositor {

    fun buildScene(model: Lfm2Model, displaySeq: Int, attentionLayer: Int): CompositorScene {
        val config = model.config
        require(attentionLayer in config.attentionLayers) {
            "Layer $attentionLayer is not an attention layer (${config.attentionLayers})"
        }
        val scene = CompositorScene(PlanGraph(model.plan))

        val residPorts = listOf(model.plan.port("embed")) +
                (0 until config.numLayers).map { model.plan.port("layers.$it.resid") }
        val residTitles = listOf("embed") + (0 until config.numLayers).map { layer ->
            "layer $layer (${if (layer in config.attentionLayers) "attn" else "conv"})"
        }
        for ((i, port) in residPorts.withIndex()) {
            scene.addTile(VectorHistoryTile(port, displaySeq, residTitles[i]).apply {
                x = 0.0
                y = (residPorts.size - 1 - i) * RESID_PITCH
                width = RESID_WIDTH
                height = RESID_HEIGHT
            })
        }

        scene.addTile(
            AttentionTile(
                model.plan.port("layers.$attentionLayer.attn.weights"),
                config.numHeads, displaySeq,
                "layer $attentionLayer attention"
            ).apply {
                x = ATTENTION_X
                y = (config.numLayers - attentionLayer - 0.5) * RESID_PITCH + RESID_HEIGHT / 2 - ATTENTION_SIZE / 2
                width = ATTENTION_SIZE
                height = ATTENTION_SIZE
            }
        )

        scene.connectFromGraph()
        scene.lens = LogitLens(
            embedWeight = model.plan.port("model.embed_tokens.weight").tensor,
            normWeight = model.plan.port("model.embedding_norm.weight").tensor,
            eps = config.normEps,
            sources = residPorts,
        )
        return scene
    }

    private const val RESID_WIDTH = 440.0
    private const val RESID_HEIGHT = 96.0
    private const val RESID_PITCH = 150.0
    private const val ATTENTION_X = 720.0
    private const val ATTENTION_SIZE = 220.0
}
