package org.simbrain.util.uisnapshot

import org.piccolo2d.PCanvas
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.Lfm2StackCompositor
import org.simbrain.network.compositor.TokenProbabilitySnapshot
import org.simbrain.network.llm.*
import org.simbrain.network.tensor.Blas
import java.awt.Dimension

/**
 * Paper figure: the LFM2 structure view stopped right after the prompt ("The capital of France
 * is"), so the depth strip's logit lens shows the next-token prediction developing layer by layer
 * and the probability card shows the final distribution. Crop to the depth strip and card for the
 * paper. The context window is kept short so the prompt's rows fill visibly. Needs the LFM2.5-230M weights in the Simbrain or HF cache.
 */
class PaperLfm2LogitLensSnapshot : UiSnapshotDef {
    override val name = "paper-lfm2-logit-lens"

    override fun build(): PCanvas {
        val weightsDir = Lfm2Weights.findWeightsDirectory()
            ?: error("LFM2.5-230M weights not found in the Simbrain or HF cache")

        Blas.numThreads = 4
        val model = Lfm2Model(Lfm2Config(maxSeqLen = 8), Safetensors.load(weightsDir.resolve("model.safetensors")))
        val tokenizer = LlmTokenizer(weightsDir.resolve("tokenizer.json"))
        val scene = Lfm2StackCompositor.buildScene(model)
        // Select (outline) the layer where the lens prediction becomes " Paris"; must happen
        // before the run since history is recorded for the watched layer.
        scene.layerSelector?.invoke(System.getenv("PAPER_LAYER")?.toInt() ?: 11)

        val prompt = System.getenv("PAPER_PROMPT") ?: "The capital of France is"
        var probabilities: TokenProbabilitySnapshot? = null
        for (id in tokenizer.encode(prompt)) {
            val position = model.position
            val logits = model.forwardToken(id)
            scene.publish(position)
            var best = 0
            for (j in 1 until logits.size) if (logits.data.get(j) > logits.data.get(best)) best = j
            probabilities = TokenProbabilitySnapshot.topK(logits, 6, best)
        }

        val node = CompositorNode(
            scene,
            tokenLabel = { id -> tokenizer.decode(intArrayOf(id)) },
            probabilitySnapshot = { probabilities },
        )
        val bounds = node.fullBoundsReference
        node.setOffset(-bounds.x, -bounds.y)

        return PCanvas().apply {
            layer.addChild(node)
            preferredSize = Dimension(bounds.width.toInt(), bounds.height.toInt())
        }
    }
}
