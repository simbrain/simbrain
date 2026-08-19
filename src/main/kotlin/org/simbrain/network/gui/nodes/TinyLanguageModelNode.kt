package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.piccolo2d.util.PBounds
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.TokenProbabilityCardStyle
import org.simbrain.network.compositor.TokenProbabilitySnapshot
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.MouseEventHandler
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.gui.dialogs.ErrorTimeSeries
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.util.*
import java.awt.Dialog
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Point2D
import javax.swing.*

/**
 * Canvas node for a [TinyLanguageModel]: an interaction box and the compositor spine interior.
 * The flagship interaction is op-level micro-stepping — step a forward pass or a whole training
 * step one op at a time, with the active op's glyph glowing, un-computed tiles dimmed, and the
 * backward half filling gradient views.
 */
class TinyLanguageModelNode(networkPanel: NetworkPanel, val tinyLanguageModel: TinyLanguageModel) :
    ScreenElement(networkPanel) {

    private val interactionBox = TinyLanguageModelInteractionBox(networkPanel)

    private var compositorNode: CompositorNode? = null

    init {
        addChild(interactionBox)
        interactionBox.setText(tinyLanguageModel.displayName)

        val events = tinyLanguageModel.events
        val subscriptions = listOf(
            events.labelChanged.on(swingDispatcher) { _, _ -> interactionBox.setText(tinyLanguageModel.displayName) },
            events.locationChanged.on(swingDispatcher) { pullLocationFromModel() },
            events.modelRebuilt.on(swingDispatcher) { rebuildInterior() },
            events.updated.on(Dispatchers.Default) { events.updateGraphics.fire() },
            events.updateGraphics.on(swingDispatcher) { refreshView() },
            events.stepRefused.on(swingDispatcher) { reason -> flashStepNotice(reason) },
            // A successful step outdates any refusal notice; drop it rather than letting the timer run out.
            events.updated.on(swingDispatcher) {
                if (stepNotice != null) {
                    stepNotice = null
                    stepNoticeTimer.stop()
                    refreshView()
                }
            },
        )
        val errorRemover = tinyLanguageModel.trainer.events.errorUpdated.on(swingDispatcher) { refreshView() }
        // Undo builds a fresh node, so a deleted node's subscriptions can go for good.
        events.deleted.on(Dispatchers.Default) {
            subscriptions.forEach(Job::cancel)
            errorRemover()
        }

        rebuildInterior()
        pullLocationFromModel()
    }

    override val model: NetworkModel get() = tinyLanguageModel

    override val isDraggable = true

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        tinyLanguageModel.location =
            Point2D.Double(tinyLanguageModel.location.x + dx, tinyLanguageModel.location.y + dy)
    }

    private fun pullLocationFromModel() {
        setOffset(tinyLanguageModel.location.x, tinyLanguageModel.location.y)
    }

    override fun isIntersecting(bound: PBounds?): Boolean = interactionBox.isIntersecting(bound)

    private fun rebuildInterior() {
        compositorNode?.removeFromParent()
        compositorNode = CompositorNode(
            tinyLanguageModel.scene,
            networkPanel.canvas,
            tokenLabel = { id -> tinyLanguageModel.tokenLabels?.getOrNull(id)?.let { "“$it”" } ?: "#$id" },
            probabilitySnapshot = {
                tinyLanguageModel.tokenProbabilitySnapshot
                    ?: tinyLanguageModel.tokenLabels?.let { TokenProbabilitySnapshot.full(DoubleArray(it.size), -1) }
            },
            probabilityCardStyle = TokenProbabilityCardStyle(
                title = "current-position token probabilities",
                width = 1020.0,
                height = 315.0,
                columns = 18,
                visibleRows = 5,
            ),
            probabilityCardPosition = { scene, bounds, card ->
                tinyLanguageModel.probabilityCardLayout?.let { Point2D.Double(it[0], it[1]) } ?: run {
                    // Top of the diagram, centered in the open space right of the spine.
                    val probs = scene.tile("probs")
                    val left = probs.x + probs.width + 40.0
                    val x = left + ((bounds.maxX - left - card.cardWidth) / 2).coerceAtLeast(0.0)
                    Point2D.Double(x, probs.y)
                }
            },
            onProbabilityCardMoved = { x, y -> tinyLanguageModel.probabilityCardLayout = doubleArrayOf(x, y) },
            lensSpace = 120.0,
            isPanMode = { networkPanel.mouseCursor == MouseEventHandler.MouseCursor.Pan },
        ).also {
            it.onLayoutChanged = {
                tinyLanguageModel.captureViewState()
                positionInteractionBox()
            }
            addChild(it)
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

    /** A transient explanation of why a step was refused, shown in place of the walk status. */
    private var stepNotice: String? = null

    private val stepNoticeTimer = Timer(3500) {
        stepNotice = null
        refreshView()
    }.apply { isRepeats = false }

    private fun flashStepNotice(refusal: TinyLanguageModel.StepRefusal) {
        stepNotice = when (refusal) {
            TinyLanguageModel.StepRefusal.TRAINING_WALK_IN_PROGRESS ->
                "a training walk is under way — step it (b) or finish it (shift-b) first"
            TinyLanguageModel.StepRefusal.FORWARD_WALK_IN_PROGRESS ->
                "a forward walk is under way — step it (f) or finish it (shift-b) first"
            TinyLanguageModel.StepRefusal.EMPTY_CONTEXT ->
                "the context window is empty — nothing to run forward"
            TinyLanguageModel.StepRefusal.NO_TRAINING_WINDOWS ->
                "no training corpus — nothing to train on"
            TinyLanguageModel.StepRefusal.NO_WALK_IN_PROGRESS ->
                "no walk under way — start one with f (forward pass) or b (training)"
            TinyLanguageModel.StepRefusal.TRAINER_RUNNING ->
                "training is running — stop it before stepping ops"
        }
        stepNoticeTimer.restart()
        refreshView()
    }

    private fun refreshView() {
        val model = tinyLanguageModel.model
        val notice = stepNotice
        compositorNode?.syncStepState(
            tinyLanguageModel.pendingOp(),
            tinyLanguageModel.scene.staleTiles(model.plan.cursor),
            notice ?: tinyLanguageModel.stepStatusText(),
            notice != null,
        )
        compositorNode?.refreshDirtyTiles()
    }

    override val propertyDialog: StandardDialog
        get() = tinyLanguageModel.createEditorDialog("Edit ${tinyLanguageModel.displayName}")

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
                showInputDialog("Name:", tinyLanguageModel.label)?.let { tinyLanguageModel.label = it }
            })
            add(createAction(
                name = "Edit ${tinyLanguageModel.displayName}...",
                description = "Set the learning rate, sampling, and diagram scale",
            ) { propertyDialog.display() })
            add(createAction(
                name = "Train...",
                description = "Open the trainer: run, stop, or step training and watch the loss curve",
            ) { trainingDialog().display() })
            addSeparator()
            add(networkPanel.networkActions.stepTransformerForwardOpAction)
            add(networkPanel.networkActions.stepTransformerTrainingOpAction)
            add(JMenuItem(networkPanel.networkActions.finishTransformerStepWalkAction).apply {
                isEnabled = tinyLanguageModel.stepWalkInProgress
            })
            addSeparator()
            add(createAction(
                name = "Clear context window",
                description = "Empty the window; a coupled non-empty document restores " +
                    "itself on the next play, so clear the document too for a full reset",
            ) { tinyLanguageModel.clearWindow() })
            addSeparator()
            add(JCheckBoxMenuItem("Show last training gradients", tinyLanguageModel.gradientView).apply {
                isEnabled = tinyLanguageModel.hasGradients
                toolTipText = "Swap each tile to the gradients its last backward pass wrote; tiles " +
                    "without gradients keep their forward values. Switches itself on for the backward " +
                    "half of a training walk and back off when the context returns. Available once a " +
                    "training step has run"
                addActionListener {
                    tinyLanguageModel.gradientView = isSelected
                    refreshView()
                }
            })
            addSeparator()
            add(networkPanel.networkComponent.createCouplingMenu(tinyLanguageModel))
        }

    private fun trainingDialog(): StandardDialog {
        val trainer = tinyLanguageModel.trainer
        val iterationsLabel = JLabel("Iterations: ${trainer.iteration}")
        val lossLabel = JLabel("Loss: ${trainer.lastTrainingError.roundToString(4)}")
        val accuracyLabel = JLabel("Accuracy: ${trainer.lastTrainingAccuracy?.let { "${(it * 100).roundToString(1)}%" } ?: "N/A"}")
        val windowsLabel = JLabel("Training windows: ${trainer.trainingWindows.size}")

        val trainButton = JButton("Train").apply { addActionListener { trainer.launch { trainer.startTraining() } } }
        val stopButton = JButton("Stop").apply { addActionListener { trainer.launch { trainer.stopTraining() } } }
        val stepButton = JButton("Step").apply { addActionListener { trainer.launch { trainer.trainOnce() } } }
        fun syncButtons(running: Boolean) {
            trainButton.isEnabled = !running
            stepButton.isEnabled = !running
            stopButton.isEnabled = running
        }
        syncButtons(trainer.isRunning)

        val errorTimeSeries = ErrorTimeSeries(trainer.events) { trainer.iteration }
        val panel = JPanel(MigLayout("ins 10, wrap 4", "[][][][grow]"))
        panel.add(trainButton)
        panel.add(stopButton)
        panel.add(stepButton)
        panel.add(iterationsLabel)
        panel.add(lossLabel)
        panel.add(accuracyLabel)
        panel.add(windowsLabel, "span 2")
        panel.add(errorTimeSeries, "span 4, grow, push")

        val beginRemover = trainer.events.beginTraining.on(swingDispatcher) { syncButtons(true) }
        val endJob = trainer.events.endTraining.on(swingDispatcher) { syncButtons(false) }
        val statsRemover = trainer.events.errorUpdated.on(swingDispatcher) { stats ->
            iterationsLabel.text = "Iterations: ${trainer.iteration}"
            lossLabel.text = "Loss: ${stats.trainingError.roundToString(4)}"
            accuracyLabel.text = "Accuracy: ${stats.trainingAccuracy?.let { "${(it * 100).roundToString(1)}%" } ?: "N/A"}"
        }
        // The trainer outlives this dialog, so detach everything it registered; closing also
        // stops a running training.
        panel.onWindowClose {
            beginRemover()
            endJob.cancel()
            statsRemover()
            errorTimeSeries.dispose()
            trainer.launch { trainer.stopTraining() }
        }

        val parentWindow = SwingUtilities.getWindowAncestor(networkPanel)
        return StandardDialog(parentWindow as? JFrame, "Train ${tinyLanguageModel.displayName}").apply {
            contentPane = panel
            isModal = true
            isAlwaysOnTop = false
            modalityType = Dialog.ModalityType.APPLICATION_MODAL
            addWindowFocusListener(object : WindowAdapter() {
                override fun windowGainedFocus(e: WindowEvent?) {
                    toFront()
                }
            })
        }
    }

    override fun refreshTheme() {
        interactionBox.refreshTheme()
        compositorNode?.refreshTheme()
    }

    private inner class TinyLanguageModelInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu get() = this@TinyLanguageModelNode.contextMenu

        override val propertyDialog: StandardDialog get() = trainingDialog()

        override val model: NetworkModel get() = this@TinyLanguageModelNode.tinyLanguageModel
    }
}
