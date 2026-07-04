package org.simbrain.network.compositor

import org.simbrain.network.llm.Lfm2Config
import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.llm.LlmTokenizer
import org.simbrain.network.llm.Safetensors
import org.simbrain.network.tensor.Blas
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * Headless visual check for the M3 compositor: runs a real greedy decode, publishes every token
 * into the scene, and renders the [CompositorNode] offscreen to PNGs — one plain frame and one
 * with trace focus and a selection active. No window, no screen capture.
 *
 * Usage: run this main with the test runtime classpath; pass an output directory as the first
 * argument (defaults to the working directory).
 */
fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "." }).apply { mkdirs() }
    val hub = Path.of(System.getProperty("user.home"), ".cache", "huggingface", "hub",
        "models--LiquidAI--LFM2.5-230M", "snapshots")
    val snapshot = (if (hub.exists()) hub.listDirectoryEntries() else emptyList())
        .firstOrNull { it.resolve("model.safetensors").exists() }
        ?: error("LFM2.5-230M weights not found in the HF cache")

    Blas.numThreads = 4
    val config = Lfm2Config(maxSeqLen = 256)
    val model = Lfm2Model(config, Safetensors.load(snapshot.resolve("model.safetensors")))
    val tokenizer = LlmTokenizer(snapshot.resolve("tokenizer.json"))

    val displaySeq = 48
    val attentionLayer = 8
    val scene = Lfm2Compositor.buildScene(model, displaySeq, attentionLayer)

    val promptIds = tokenizer.encode("The capital of France is")
    var next = -1
    val start = System.nanoTime()
    for (i in 0 until displaySeq) {
        val id = if (i < promptIds.size) promptIds[i] else next
        val position = model.position
        val logits = model.forwardToken(id)
        scene.publish(position)
        var best = 0
        for (j in 1 until logits.size) if (logits.data.get(j) > logits.data.get(best)) best = j
        next = best
        if (i >= promptIds.size - 1) print(tokenizer.decode(intArrayOf(next)))
    }
    println()
    println("decoded $displaySeq tokens at %.1f tok/s (lens on)".format(displaySeq / ((System.nanoTime() - start) / 1e9)))

    val node = CompositorNode(scene, tokenLabel = { id -> "\"${tokenizer.decode(intArrayOf(id))}\"" })

    fun snap(name: String) {
        val bounds = node.fullBoundsReference
        val image = node.toImage(bounds.width.toInt(), bounds.height.toInt(), null) as BufferedImage
        val file = File(outDir, name)
        ImageIO.write(image, "png", file)
        println("wrote ${file.absolutePath} (${image.width}x${image.height})")
    }

    snap("compositor_plain.png")

    scene.setTrace(scene.tile("layers.$attentionLayer.attn.weights"))
    scene.selection.set(listOf(scene.tile("layers.3.resid")))
    node.refreshTheme()
    snap("compositor_trace.png")

    (scene.tile("layers.$attentionLayer.attn.weights") as AttentionTile).selectedHead = 5
    node.refreshDirtyTiles()
    snap("compositor_head5.png")
}
