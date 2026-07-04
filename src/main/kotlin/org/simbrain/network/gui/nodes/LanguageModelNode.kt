package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.piccolo2d.PCanvas
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.TensorTile
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.Lfm2Weights
import org.simbrain.network.llm.LlmPreferences
import org.simbrain.util.NetworkTheme
import org.simbrain.util.StandardDialog
import org.simbrain.util.Theme
import org.simbrain.util.createAction
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.showDirectorySelectionDialog
import org.simbrain.util.showInputDialog
import org.simbrain.util.showWarningConfirmDialog
import org.simbrain.util.showWarningDialog
import org.simbrain.util.swingDispatcher
import java.awt.Dimension
import java.awt.Image
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Point2D
import java.nio.file.Path
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JRadioButtonMenuItem
import javax.swing.SwingUtilities

/**
 * Canvas node for a [LanguageModel]: an interaction box for whole-node selection and dragging,
 * the compositor interior (which owns its own tile selection, moves, trace, and tooltips), and a
 * status line with the generated text. Generation is paced by the workspace: arm it from the
 * context menu, then play or step the network — one iteration is one token.
 */
class LanguageModelNode(networkPanel: NetworkPanel, val languageModel: LanguageModel) : ScreenElement(networkPanel) {

    private val interactionBox = LanguageModelInteractionBox(networkPanel)

    private val statusText = PText().apply {
        font = Theme.body
        textPaint = NetworkTheme.current.valueText
    }

    private var compositorNode: CompositorNode? = null

    private var loadError: String? = null

    private var previewImage: PImage? = null
    private var previewLayer = -1
    private var previewPosition = -1
    private val previewCache = HashMap<Int, Pair<Int, Image>>()

    init {
        addChild(interactionBox)
        addChild(statusText)
        interactionBox.setText(languageModel.displayName)

        val events = languageModel.events
        events.labelChanged.on(swingDispatcher) { _, _ -> interactionBox.setText(languageModel.displayName) }
        events.locationChanged.on(swingDispatcher) { pullLocationFromModel() }
        events.weightsLoaded.on(swingDispatcher) { rebuildInterior() }
        events.updated.on(Dispatchers.Default) { events.updateGraphics.fire() }
        events.updateGraphics.on(swingDispatcher) { refreshView() }

        rebuildInterior()
        pullLocationFromModel()

        if (!languageModel.isLoaded && languageModel.weightsDirectory.isNotEmpty()) {
            loadInBackground()
        }
    }

    override val model: NetworkModel get() = languageModel

    override val isDraggable = true

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        languageModel.location = Point2D.Double(languageModel.location.x + dx, languageModel.location.y + dy)
    }

    private fun pullLocationFromModel() {
        setOffset(languageModel.location.x, languageModel.location.y)
    }

    override fun isIntersecting(bound: PBounds?): Boolean = interactionBox.isIntersecting(bound)

    private fun rebuildInterior() {
        compositorNode?.removeFromParent()
        compositorNode = null
        previewImage?.removeFromParent()
        previewImage = null
        previewLayer = -1
        previewCache.clear()
        languageModel.loaded?.let { state ->
            compositorNode = CompositorNode(
                state.scene,
                networkPanel.canvas,
                tokenLabel = { id -> "“${state.tokenizer.decode(intArrayOf(id))}”" },
            ).also {
                it.onLayoutChanged = { languageModel.captureViewState() }
                it.onTileHover = { tile -> updateLayerPreview(tile) }
                addChild(it)
            }
        }
        placeChildren()
        refreshView()
    }

    private fun layerOf(tileId: String): Int? =
        Regex("""layers\.(\d+)\.resid""").matchEntire(tileId)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Hovering a layer's residual tile pops a cached offscreen render of that layer's full
     * anatomy beside the spine; the raster refreshes when generation has advanced since it was
     * taken. The anatomy dialog in the context menu is the live, explorable version.
     */
    private fun updateLayerPreview(tile: TensorTile?) {
        val layer = tile?.id?.let(::layerOf)
        val position = languageModel.loaded?.model?.position ?: -1
        if (layer == previewLayer && position == previewPosition) return
        previewImage?.removeFromParent()
        previewImage = null
        previewLayer = layer ?: -1
        previewPosition = position
        if (layer == null || tile == null) return
        val interior = compositorNode ?: return
        val image = previewCache[layer]?.takeIf { it.first == position }?.second ?: run {
            val scene = languageModel.layerScene(layer) ?: return
            val anatomy = CompositorNode(scene)
            val bounds = anatomy.fullBoundsReference
            val scale = minOf(PREVIEW_MAX / bounds.width, PREVIEW_MAX / bounds.height, 1.0)
            anatomy.toImage(
                (bounds.width * scale).toInt().coerceAtLeast(1),
                (bounds.height * scale).toInt().coerceAtLeast(1),
                null
            ).also { previewCache[layer] = position to it }
        }
        val interiorBounds = interior.fullBoundsReference
        previewImage = PImage(image).apply {
            pickable = false
            val y = (tile.y + interior.yOffset)
                .coerceAtMost(interiorBounds.maxY - image.getHeight(null))
                .coerceAtLeast(interiorBounds.y)
            setOffset(interiorBounds.maxX + 24.0, y)
        }.also { addChild(it) }
    }

    private fun showLayerAnatomyDialog(layer: Int) {
        val scene = languageModel.layerScene(layer) ?: return
        val state = languageModel.loaded ?: return
        val kind = if (layer in state.model.config.attentionLayers) "attention" else "conv"
        val canvas = PCanvas()
        val node = CompositorNode(scene, canvas)
        val bounds = node.fullBoundsReference
        node.setOffset(-bounds.x, -bounds.y)
        canvas.layer.addChild(node)
        canvas.preferredSize = Dimension(760, 860)
        val job = languageModel.events.updateGraphics.on(swingDispatcher) { node.refreshDirtyTiles() }
        StandardDialog().apply {
            title = "${languageModel.displayName} — layer $layer ($kind) anatomy"
            isModal = false
            contentPane = canvas
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    job.cancel()
                }
            })
            pack()
            SwingUtilities.invokeLater {
                canvas.camera.animateViewToCenterBounds(node.fullBoundsReference, true, 0)
            }
        }.display()
    }

    private fun placeChildren() {
        interactionBox.setOffset(0.0, 0.0)
        val top = interactionBox.fullBoundsReference.height + 6.0
        val interior = compositorNode
        if (interior != null) {
            val bounds = interior.fullBoundsReference
            interior.offset(-bounds.x, top - bounds.y)
            statusText.setOffset(0.0, top + interior.fullBoundsReference.height + 8.0)
        } else {
            statusText.setOffset(0.0, top)
        }
    }

    private fun refreshView() {
        compositorNode?.refreshDirtyTiles()
        statusText.text = statusLine()
    }

    private fun statusLine(): String {
        loadError?.let { return "Weights failed to load: $it" }
        val state = languageModel.loaded
            ?: return if (languageModel.weightsDirectory.isEmpty()) {
                "No weights — right-click to locate or download them"
            } else {
                "Loading weights…"
            }
        val tail = languageModel.text.takeLast(90).replace('\n', ' ')
        return when {
            languageModel.isGenerating ->
                "Generating, token ${state.model.position}/${state.model.config.maxSeqLen} " +
                        "(play or step the network) — …$tail"
            languageModel.text.isEmpty() -> "Ready — right-click to start generation"
            else -> "Done — …$tail"
        }
    }

    override val propertyDialog: StandardDialog
        get() = languageModel.createEditorDialog("Language Model Settings")

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(createAction("Rename...") {
                showInputDialog("Name:", languageModel.label)?.let { languageModel.label = it }
            })
            add(createAction(name = "Remove language model", coroutineScope = networkPanel.network) {
                languageModel.deleteBlocking()
            })
            addSeparator()
            add(createAction("Generation settings...") { propertyDialog.display() })
            val state = languageModel.loaded
            if (state != null) {
                if (languageModel.isGenerating) {
                    add(createAction("Stop generation") {
                        languageModel.stopGeneration()
                        refreshView()
                    })
                } else {
                    add(createAction("Start generation") { languageModel.startGeneration() })
                }
                addSeparator()
                add(JMenu("Layer anatomy").apply {
                    (0 until state.model.config.numLayers).forEach { layer ->
                        val kind = if (layer in state.model.config.attentionLayers) "attention" else "conv"
                        add(createAction("Layer $layer ($kind)...") { showLayerAnatomyDialog(layer) })
                    }
                })
                add(JMenu("Attention layer").apply {
                    state.model.config.attentionLayers.sorted().forEach { layer ->
                        add(JRadioButtonMenuItem("Layer $layer", layer == languageModel.attentionLayer).apply {
                            addActionListener { languageModel.attentionLayer = layer }
                        })
                    }
                })
                add(JMenu("Attention head").apply {
                    (0 until state.model.config.numHeads).forEach { head ->
                        add(JRadioButtonMenuItem("Head $head", head == languageModel.selectedHead).apply {
                            addActionListener {
                                languageModel.selectedHead = head
                                compositorNode?.refreshDirtyTiles()
                            }
                        })
                    }
                })
                add(JCheckBoxMenuItem("Logit lens", languageModel.lensEnabled).apply {
                    addActionListener { languageModel.lensEnabled = isSelected }
                })
            } else {
                addSeparator()
                add(createAction("Locate weights...") { locateWeights() })
                add(createAction("Download weights (${Lfm2Weights.MODEL_NAME})...") { downloadWeights() })
            }
        }

    private fun locateWeights() {
        val dir = showDirectorySelectionDialog() ?: return
        if (!Lfm2Weights.isValidWeightsDirectory(Path.of(dir))) {
            showWarningDialog("No model.safetensors and tokenizer.json in $dir")
            return
        }
        LlmPreferences.weightsDirectory = dir
        languageModel.weightsDirectory = dir
        loadInBackground()
    }

    private fun downloadWeights() {
        if (showWarningConfirmDialog(Lfm2Weights.downloadNotice) != JOptionPane.OK_OPTION) return
        networkPanel.network.launch(Dispatchers.Default) {
            val dir = Lfm2Weights.download() ?: return@launch
            languageModel.weightsDirectory = dir.toString()
            loadInBackground()
        }
    }

    private fun loadInBackground() {
        loadError = null
        SwingUtilities.invokeLater { refreshView() }
        networkPanel.network.launch(Dispatchers.Default) {
            runCatching { languageModel.loadWeights() }.onFailure {
                loadError = it.message ?: it.javaClass.simpleName
                SwingUtilities.invokeLater { refreshView() }
            }
        }
    }

    override fun refreshTheme() {
        interactionBox.refreshTheme()
        statusText.textPaint = NetworkTheme.current.valueText
        compositorNode?.refreshTheme()
    }

    companion object {
        private const val PREVIEW_MAX = 700.0
    }

    private inner class LanguageModelInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu get() = this@LanguageModelNode.contextMenu

        override val propertyDialog: StandardDialog get() = this@LanguageModelNode.propertyDialog

        override val model: NetworkModel get() = this@LanguageModelNode.languageModel
    }
}
