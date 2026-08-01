package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.piccolo2d.util.PBounds
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.HistoryView
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.Lfm2Weights
import org.simbrain.network.llm.LlmPreferences
import org.simbrain.util.*
import org.simbrain.workspace.WorkspacePreferences
import java.awt.geom.Point2D
import java.nio.file.Path
import javax.swing.*

/**
 * Canvas node for a [LanguageModel]: an interaction box for whole-node selection and dragging,
 * the compositor interior (which owns its own tile selection, moves, trace, and tooltips).
 * Generation is paced by the workspace: playing or
 * stepping the network generates one token per iteration whenever the model can advance;
 * the context menu reseeds the context window from the prompt.
 */
class LanguageModelNode(networkPanel: NetworkPanel, val languageModel: LanguageModel) : ScreenElement(networkPanel) {

    private val interactionBox = LanguageModelInteractionBox(networkPanel)

    private var compositorNode: CompositorNode? = null

    private var loadError: String? = null

    init {
        addChild(interactionBox)
        interactionBox.setText(languageModel.displayName)

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
                probabilitySnapshot = { languageModel.tokenProbabilitySnapshot },
                probabilityCardPosition = { scene, bounds, _ ->
                    languageModel.probabilityCardLayout?.let { Point2D.Double(it[0], it[1]) }
                        ?: Point2D.Double(bounds.x - 24.0, scene.tile("block.resid").y)
                },
                onProbabilityCardMoved = { x, y -> languageModel.probabilityCardLayout = doubleArrayOf(x, y) },
            ).also {
                it.onLayoutChanged = {
                    languageModel.captureViewState()
                    positionInteractionBox()
                }
                addChild(it)
            }
        }
        // The interior's background can grow over the interaction box; keep the box painting
        // (and picking) above it.
        interactionBox.raiseToTop()
        placeChildren()
        refreshView()
    }

    /** The interaction box's bottom stays attached to the compositor's top border. */
    private fun placeChildren() {
        compositorNode?.let { interior ->
            val fullBounds = interior.fullBoundsReference
            val outlineBounds = interior.outlineBoundsInParentCoordinates()
            val outlineTop = interactionBox.fullBoundsReference.height + 6.0
            interior.offset(-fullBounds.x, outlineTop - outlineBounds.y)
            positionInteractionBox()
        }
    }

    override fun layoutChildren() {
        if (compositorNode != null) positionInteractionBox()
    }

    private fun positionInteractionBox() {
        val outlineBounds = compositorNode?.outlineBoundsInParentCoordinates() ?: return
        interactionBox.centerFullBoundsOnPoint(
            outlineBounds.centerX,
            outlineBounds.y - interactionBox.fullBounds.height / 2 + 0.5,
        )
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
    }

    override val propertyDialog: StandardDialog
        get() = languageModel.createEditorDialog("Edit ${languageModel.displayName}")

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(networkPanel.networkActions.cutAction)
            add(networkPanel.networkActions.copyAction)
            add(networkPanel.networkActions.pasteAction)
            add(networkPanel.networkActions.duplicateAction)
            add(networkPanel.networkActions.deleteAction)
            addSeparator()
            add(createAction(
                name = "Rename...",
                description = "Set the label shown on the model's header box",
            ) {
                showInputDialog("Name:", languageModel.label)?.let { languageModel.label = it }
            })
            add(createAction(
                name = "Edit ${languageModel.displayName}...",
                description = "Set the generation properties: prompt mode, sampling, budget",
            ) { propertyDialog.display() })
            addSeparator()
            val state = languageModel.loaded
            if (state != null) {
                add(createAction(
                    name = "Clear context window",
                    description = "Empty the window; a coupled non-empty document restores " +
                        "itself on the next play, so clear the document too for a full reset",
                ) {
                    languageModel.clearWindow()
                    refreshView()
                })
                addSeparator()
                add(JMenu("Attention head").apply {
                    val group = ButtonGroup()
                    (0 until state.model.config.numHeads).forEach { head ->
                        add(JRadioButtonMenuItem("Head $head", head == languageModel.selectedHead).apply {
                            group.add(this)
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
                add(JMenu("Token history").apply {
                    val group = ButtonGroup()
                    fun item(label: String, mode: HistoryView, tip: String) =
                        JRadioButtonMenuItem(label, languageModel.historyView == mode).apply {
                            group.add(this)
                            toolTipText = tip
                            addActionListener {
                                languageModel.historyView = mode
                                compositorNode?.refreshDirtyTiles()
                            }
                        }
                    add(item("Show", HistoryView.FULL,
                        "Record every token's activations and show the full history"))
                    add(item("Ghost", HistoryView.GHOSTED,
                        "Past tokens ghost out: only the current token's activations and the " +
                            "caches are actually in the model"))
                    add(item("Off", HistoryView.OFF,
                        "Keep no token history: tiles show just the current token and layer " +
                            "flips are instant; switching back re-derives the history"))
                })
                add(JMenu("Inactive limb").apply {
                    val group = ButtonGroup()
                    fun item(label: String, hide: Boolean, tip: String) =
                        JRadioButtonMenuItem(label, languageModel.hideInactiveLimb == hide).apply {
                            group.add(this)
                            toolTipText = tip
                            addActionListener {
                                languageModel.hideInactiveLimb = hide
                                compositorNode?.refreshStackState()
                            }
                        }
                    add(item("Ghost", false,
                        "Keep the limb the selected layer doesn't use faintly visible"))
                    add(item("Hide", true,
                        "Show only the selected layer's anatomy; the unused limb disappears"))
                })
                add(JMenu("Layer depth").apply {
                    val group = ButtonGroup()
                    fun item(label: String, showCards: Boolean, tip: String) =
                        JRadioButtonMenuItem(label, languageModel.showLayerCards == showCards).apply {
                            group.add(this)
                            toolTipText = tip
                            addActionListener {
                                languageModel.showLayerCards = showCards
                                compositorNode?.refreshStackState()
                            }
                        }
                    add(item("Show cards", true,
                        "Show the layered-card depth cue behind each stacked tensor"))
                    add(item("Current layer only", false,
                        "Hide depth cards so each stacked tensor shows only its selected layer"))
                })
            } else {
                add(createAction(
                    name = "Locate weights...",
                    description = "Point Simbrain at a folder holding model.safetensors and tokenizer.json",
                ) { locateWeights() })
                add(createAction(
                    name = "Download weights (${Lfm2Weights.MODEL_NAME})...",
                    description = "Download the weights (~460 MB) from Hugging Face and cache them locally",
                ) { downloadWeights() })
            }
            addSeparator()
            add(networkPanel.networkComponent.createCouplingMenu(languageModel))
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
            LlmPreferences.weightsDirectory = dir.toString()
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
        compositorNode?.refreshTheme()
    }

    private inner class LanguageModelInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu get() = this@LanguageModelNode.contextMenu

        override val propertyDialog: StandardDialog get() = this@LanguageModelNode.propertyDialog

        override val model: NetworkModel get() = this@LanguageModelNode.languageModel
    }
}
