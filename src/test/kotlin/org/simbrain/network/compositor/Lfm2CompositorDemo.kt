package org.simbrain.network.compositor

import org.piccolo2d.PCanvas
import org.simbrain.network.llm.Lfm2Config
import org.simbrain.network.llm.Lfm2Model
import org.simbrain.network.llm.Lfm2Weights
import org.simbrain.network.llm.LlmTokenizer
import org.simbrain.network.llm.Safetensors
import org.simbrain.network.tensor.Blas
import org.simbrain.network.tensor.FloatTensor
import java.awt.BorderLayout
import java.awt.geom.Point2D
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.JToolBar
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Standalone LFM2 structure-view demo: greedy decode streaming into one stacked layer-block
 * anatomy with the depth strip and logit lens. Interactions: click or wheel a strip row to flip
 * the whole block to that layer, wheel the attention/cache decks to flip heads, click/marquee
 * select, drag to move tiles, double-click a tile to trace its data-flow paths, hover for cell
 * values, mouse wheel to zoom, drag empty canvas to pan.
 *
 * Needs the LFM2.5-230M weights (download them once in Simbrain, or run
 * `uv run src/test/python/lfm2_export_reference.py`). Run with:
 * `./gradlew -PmainClass=org.simbrain.network.compositor.Lfm2CompositorDemoKt run` or from the IDE.
 */
fun main() {
    val snapshot = Lfm2Weights.findWeightsDirectory()
        ?: error("LFM2.5-230M weights not found; download them once in Simbrain or run lfm2_export_reference.py")

    Blas.numThreads = 4
    val config = Lfm2Config(maxSeqLen = 512)
    val model = Lfm2Model(config, Safetensors.load(snapshot.resolve("model.safetensors")))
    val tokenizer = LlmTokenizer(snapshot.resolve("tokenizer.json"))

    val scene = Lfm2StackCompositor.buildScene(model)
    val attentionTile = scene.tile("block.attn.weights") as AttentionTile

    SwingUtilities.invokeLater {
        val canvas = PCanvas()
        val node = CompositorNode(scene, canvas, tokenLabel = { id -> "“${tokenizer.decode(intArrayOf(id))}”" })
        canvas.layer.addChild(node)
        canvas.addMouseWheelListener { e ->
            val factor = if (e.preciseWheelRotation < 0) 1.1 else 1 / 1.1
            val viewPos = canvas.camera.localToView(Point2D.Double(e.x.toDouble(), e.y.toDouble()))
            canvas.camera.scaleViewAboutPoint(factor, viewPos.x, viewPos.y)
        }

        val promptField = JTextField("The capital of France is", 24)
        val stepsSpinner = JSpinner(SpinnerNumberModel(30, 1, config.maxSeqLen, 1))
        val layerCombo = JComboBox((0 until config.numLayers).map {
            "layer $it (${if (it in config.attentionLayers) "attn" else "conv"})"
        }.toTypedArray())
        val headCombo = JComboBox((0 until config.numHeads).map { "head $it" }.toTypedArray())
        val lensCheck = JCheckBox("Logit lens", true)
        val generateButton = JButton("Generate")
        val status = JLabel("Ready — weights loaded from ${snapshot.fileName}")

        layerCombo.addActionListener {
            scene.layerSelector?.invoke(layerCombo.selectedIndex)
            node.refreshStackState()
        }
        headCombo.addActionListener {
            attentionTile.selectedHead = headCombo.selectedIndex
            scene.onHeadSelected?.invoke(attentionTile, headCombo.selectedIndex)
            node.refreshStackState()
        }
        lensCheck.addActionListener { scene.lens?.enabled = lensCheck.isSelected }

        var worker: Thread? = null
        var running = false

        fun argmax(logits: FloatTensor): Int {
            var best = 0
            for (i in 1 until logits.size) {
                if (logits.data.get(i) > logits.data.get(best)) best = i
            }
            return best
        }

        generateButton.addActionListener {
            if (running) {
                running = false
                return@addActionListener
            }
            running = true
            generateButton.text = "Stop"
            val prompt = promptField.text
            val steps = stepsSpinner.value as Int
            worker = thread(name = "lfm2-compositor-decode") {
                model.reset()
                scene.reset()
                SwingUtilities.invokeAndWait { node.refreshDirtyTiles() }
                val promptIds = tokenizer.encode(prompt)
                val generated = StringBuilder()
                val total = minOf(promptIds.size + steps, config.maxSeqLen)
                var nextToken = -1
                val start = System.nanoTime()
                for (i in 0 until total) {
                    if (!running) break
                    val id = if (i < promptIds.size) promptIds[i] else nextToken
                    val position = model.position
                    val logits = model.forwardToken(id)
                    scene.publish(position)
                    nextToken = argmax(logits)
                    if (i >= promptIds.size - 1) generated.append(tokenizer.decode(intArrayOf(nextToken)))
                    val tokensPerSec = (i + 1) / ((System.nanoTime() - start) / 1e9)
                    SwingUtilities.invokeAndWait {
                        node.refreshDirtyTiles()
                        status.text = "token ${i + 1}/$total — %.1f tok/s — %s".format(tokensPerSec, generated)
                    }
                }
                SwingUtilities.invokeLater {
                    running = false
                    generateButton.text = "Generate"
                }
            }
        }

        val toolbar = JToolBar().apply {
            isFloatable = false
            add(JLabel(" Prompt: "))
            add(promptField)
            add(JLabel(" Tokens: "))
            add(stepsSpinner)
            add(generateButton)
            addSeparator()
            add(JLabel(" Block: "))
            add(layerCombo)
            add(JLabel(" Attention: "))
            add(headCombo)
            add(lensCheck)
        }

        JFrame("LFM2.5 Compositor — structure view").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            layout = BorderLayout()
            add(toolbar, BorderLayout.NORTH)
            add(canvas, BorderLayout.CENTER)
            add(status, BorderLayout.SOUTH)
            setSize(1400, 950)
            setLocationRelativeTo(null)
            isVisible = true
        }
        canvas.camera.animateViewToCenterBounds(node.fullBoundsReference, true, 0)
    }
}
