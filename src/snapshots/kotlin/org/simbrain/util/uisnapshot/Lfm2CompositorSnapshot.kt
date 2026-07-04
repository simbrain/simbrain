package org.simbrain.util.uisnapshot

import org.piccolo2d.PCanvas
import org.simbrain.network.compositor.AttentionTile
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.Lfm2Compositor
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
 * Renders the LFM2 compositor after a real 48-token greedy decode ("The capital of France is"):
 * residual-stream heatmaps per layer, the layer-8 attention map, and the logit-lens strip.
 * Needs the LFM2.5-230M weights in the HF cache (run `uv run src/test/python/lfm2_export_reference.py`
 * once to fetch them).
 */
class Lfm2CompositorSnapshot : UiSnapshotDef {
    override val name = "lfm2-compositor"
    override fun build() = buildLfm2CompositorCanvas { }
}

/** The compositor with interior state active: trace on the attention tile, a selected residual tile, head 5. */
class Lfm2CompositorTraceSnapshot : UiSnapshotDef {
    override val name = "lfm2-compositor-trace"
    override fun build() = buildLfm2CompositorCanvas { scene ->
        val attention = scene.tile("layers.8.attn.weights") as AttentionTile
        attention.selectedHead = 5
        scene.setTrace(attention)
        scene.selection.set(listOf(scene.tile("layers.3.resid")))
    }
}

private fun buildLfm2CompositorCanvas(decorate: (CompositorScene) -> Unit): PCanvas {
    val hub = Path.of(System.getProperty("user.home"), ".cache", "huggingface", "hub",
        "models--LiquidAI--LFM2.5-230M", "snapshots")
    val weightsDir = (if (hub.exists()) hub.listDirectoryEntries() else emptyList())
        .firstOrNull { it.resolve("model.safetensors").exists() }
        ?: error("LFM2.5-230M weights not found in the HF cache")

    Blas.numThreads = 4
    val config = Lfm2Config(maxSeqLen = 256)
    val model = Lfm2Model(config, Safetensors.load(weightsDir.resolve("model.safetensors")))
    val tokenizer = LlmTokenizer(weightsDir.resolve("tokenizer.json"))

    val displaySeq = 48
    val scene = Lfm2Compositor.buildScene(model, displaySeq, attentionLayer = 8)

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

    decorate(scene)
    val node = CompositorNode(scene, tokenLabel = { id -> "\"${tokenizer.decode(intArrayOf(id))}\"" })
    val bounds = node.fullBoundsReference
    node.setOffset(-bounds.x, -bounds.y)

    return PCanvas().apply {
        layer.addChild(node)
        preferredSize = Dimension(bounds.width.toInt(), bounds.height.toInt())
    }
}
