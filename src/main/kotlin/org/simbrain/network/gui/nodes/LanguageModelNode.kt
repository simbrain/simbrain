package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.piccolo2d.PCamera
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.Lfm2Weights
import org.simbrain.network.llm.LlmPreferences
import org.simbrain.util.NetworkTheme
import org.simbrain.util.RateLimitedEdtAction
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
import org.simbrain.workspace.WorkspacePreferences
import java.awt.geom.Point2D
import java.beans.PropertyChangeListener
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
 * status line with the generated text. Generation is paced by the workspace: a loaded model is
 * armed automatically, so playing or stepping the network generates one token per iteration;
 * the context menu resets the context window for a fresh run.
 */
class LanguageModelNode(networkPanel: NetworkPanel, val languageModel: LanguageModel) : ScreenElement(networkPanel) {

    private val interactionBox = LanguageModelInteractionBox(networkPanel)

    private val statusText = PText().apply {
        font = Theme.body
        textPaint = NetworkTheme.current.valueText
    }

    private var compositorNode: CompositorNode? = null

    private var loadError: String? = null

    /**
     * The status line rides the interaction box's zoom counter-scale: both stay readable at
     * overview zoom, and the status keeps clear of the box's enlarged footprint.
     */
    private val statusZoomListener = PropertyChangeListener { placeStatusText() }

    init {
        addChild(interactionBox)
        addChild(statusText)
        interactionBox.setText(languageModel.displayName)
        networkPanel.canvas.camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, statusZoomListener)

        val events = languageModel.events
        events.labelChanged.on(swingDispatcher) { _, _ -> interactionBox.setText(languageModel.displayName) }
        events.locationChanged.on(swingDispatcher) { pullLocationFromModel() }
        events.weightsLoaded.on(swingDispatcher) { rebuildInterior() }
        events.updated.on(Dispatchers.Default) { events.updateGraphics.fire() }
        events.updateGraphics.on(swingDispatcher) { refreshViewThrottled() }

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
        languageModel.loaded?.let { state ->
            compositorNode = CompositorNode(
                state.scene,
                networkPanel.canvas,
                tokenLabel = { id -> "“${state.tokenizer.decode(intArrayOf(id))}”" },
            ).also {
                it.onLayoutChanged = { languageModel.captureViewState() }
                addChild(it)
            }
        }
        // The interior's background can grow over the header as tiles move; keep the
        // interaction box and status line painting (and picking) above it.
        statusText.raiseToTop()
        interactionBox.raiseToTop()
        placeChildren()
        refreshView()
    }

    /**
     * The header anchors at the node origin, not under the interior: the interior's bounds
     * change as tiles move, and a bottom anchor would drift away from the box. The interior
     * starts below the header line at its largest zoom counter-scale, so it never slides
     * under the box or status as the camera zooms out.
     */
    private fun placeChildren() {
        interactionBox.setOffset(0.0, 0.0)
        placeStatusText()
        val reserve = interactionBox.height * InteractionBox.zoomRescale(0.0) + 12.0
        compositorNode?.let { interior ->
            val bounds = interior.fullBoundsReference
            interior.offset(-bounds.x, reserve - bounds.y)
        }
    }

    /** One header line: the status rides beside the box at the box's zoom counter-scale. */
    private fun placeStatusText() {
        val rescale = InteractionBox.zoomRescale(networkPanel.canvas.camera.viewScale)
        statusText.scale = rescale
        val box = interactionBox.fullBoundsReference
        statusText.setOffset(box.width + 8.0 * rescale, (box.height - statusText.height * rescale) / 2)
    }

    /**
     * Generation-driven refreshes coalesce to the workspace repaint rate limit: syncing dirty
     * tiles invalidates most of the interior, so at full decode speed a refresh per token would
     * queue a full repaint per token. User-initiated paths (flips, menu actions) call
     * [refreshView] directly.
     */
    private val refreshViewThrottled = RateLimitedEdtAction({ WorkspacePreferences.repaintIntervalMs }) { refreshView() }

    private fun refreshView() {
        compositorNode?.refreshDirtyTiles()
        val line = statusLine()
        if (statusText.text != line) statusText.text = line
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
            state.model.position >= state.model.config.maxSeqLen ->
                "Context window full — right-click to reset it — …$tail"
            languageModel.text.isEmpty() -> "Ready — play or step the network"
            else -> "Stopped — …$tail"
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
                } else if (state.model.position < state.model.config.maxSeqLen) {
                    add(createAction("Resume generation") {
                        languageModel.resumeGeneration()
                        refreshView()
                    })
                }
                add(createAction("Reset context window") {
                    languageModel.startGeneration()
                    refreshView()
                })
                addSeparator()
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

    private inner class LanguageModelInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu get() = this@LanguageModelNode.contextMenu

        override val propertyDialog: StandardDialog get() = this@LanguageModelNode.propertyDialog

        override val model: NetworkModel get() = this@LanguageModelNode.languageModel
    }
}
