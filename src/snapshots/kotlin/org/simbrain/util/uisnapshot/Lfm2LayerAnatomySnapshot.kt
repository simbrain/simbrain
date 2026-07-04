package org.simbrain.util.uisnapshot

import org.piccolo2d.PCanvas
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.Lfm2LayerCompositor
import org.simbrain.network.llm.Lfm2Config
import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.llm.LlmTokenizer
import org.simbrain.network.llm.Safetensors
import org.simbrain.network.tensor.Blas
import java.awt.Dimension
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * Renders one LFM2 layer's full anatomy after a real 24-token greedy decode: the gated
 * short-conv layer with its live sliding window (layer 0), or the GQA attention layer with the
 * per-Q-head attention deck and column-sliced KV-cache decks (layer 2). Needs the LFM2.5-230M
 * weights in the HF cache.
 */
class Lfm2ConvLayerSnapshot : UiSnapshotDef {
    override val name = "lfm2-conv-layer"
    override fun build() = buildLayerAnatomyCanvas(layer = 0)
}

class Lfm2AttentionLayerSnapshot : UiSnapshotDef {
    override val name = "lfm2-attention-layer"
    override fun build() = buildLayerAnatomyCanvas(layer = 2)
}

private fun buildLayerAnatomyCanvas(layer: Int): PCanvas {
    val hub = Path.of(System.getProperty("user.home"), ".cache", "huggingface", "hub",
        "models--LiquidAI--LFM2.5-230M", "snapshots")
    val weightsDir = (if (hub.exists()) hub.listDirectoryEntries() else emptyList())
        .firstOrNull { it.resolve("model.safetensors").exists() }
        ?: error("LFM2.5-230M weights not found in the HF cache")

    Blas.numThreads = 4
    val model = Lfm2Model(Lfm2Config(maxSeqLen = 64), Safetensors.load(weightsDir.resolve("model.safetensors")))
    val tokenizer = LlmTokenizer(weightsDir.resolve("tokenizer.json"))

    val displaySeq = 24
    val scene = Lfm2LayerCompositor.buildScene(model, layer, displaySeq)

    val promptIds = tokenizer.encode("The capital of France is")
    var next = -1
    for (i in 0 until displaySeq) {
        val id = if (i < promptIds.size) promptIds[i] else next
        val position = model.position
        val logits = model.forwardToken(id)
        scene.publish(position)
        var best = 0
        for (j in 1 until logits.size) if (logits.data.get(j) > logits.data.get(best)) best = j
        next = best
    }

    val node = CompositorNode(scene)
    val bounds = node.fullBoundsReference
    node.setOffset(-bounds.x, -bounds.y)

    return PCanvas().apply {
        this.layer.addChild(node)
        preferredSize = Dimension(bounds.width.toInt(), bounds.height.toInt())
    }
}
