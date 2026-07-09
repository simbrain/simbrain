package org.simbrain.util.uisnapshot

import org.piccolo2d.PCanvas
import org.simbrain.network.compositor.AttentionTile
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.CompositorScene
import org.simbrain.network.compositor.Lfm2StackCompositor
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
 * Renders the LFM2 structure view after a real 48-token greedy decode ("The capital of France
 * is"): one stacked layer-block anatomy flipped to a conv layer — conv limb live, attention limb
 * dimmed — beside the depth strip with the logit lens. Needs the LFM2.5-230M weights in the HF
 * cache (run `uv run src/test/python/lfm2_export_reference.py` once to fetch them).
 */
class Lfm2StackConvSnapshot : UiSnapshotDef {
    override val name = "lfm2-stack-conv"
    override fun build() = buildLfm2StackCanvas { scene ->
        scene.layerSelector?.invoke(3)
    }
}

/** The block flipped to an attention layer, attention deck on head 5 with the KV decks coupled. */
class Lfm2StackAttnSnapshot : UiSnapshotDef {
    override val name = "lfm2-stack-attn"
    override fun build() = buildLfm2StackCanvas { scene ->
        scene.layerSelector?.invoke(8)
        val attention = scene.tile("block.attn.weights") as AttentionTile
        attention.selectedHead = 5
        scene.onHeadSelected?.invoke(attention, 5)
    }
}

private fun buildLfm2StackCanvas(decorate: (CompositorScene) -> Unit): PCanvas {
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
    val scene = Lfm2StackCompositor.buildScene(model, displaySeq)

    // Decorate (layer/head flips) BEFORE the run: history is recorded for the watched layer,
    // so the showcased layer should be watched while the tokens stream through.
    decorate(scene)

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

    val node = CompositorNode(scene, tokenLabel = { id -> "\"${tokenizer.decode(intArrayOf(id))}\"" })
    val bounds = node.fullBoundsReference
    node.setOffset(-bounds.x, -bounds.y)

    return PCanvas().apply {
        layer.addChild(node)
        preferredSize = Dimension(bounds.width.toInt(), bounds.height.toInt())
    }
}
