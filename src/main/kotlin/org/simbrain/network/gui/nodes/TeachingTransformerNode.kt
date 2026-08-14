package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.piccolo2d.util.PBounds
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.compositor.TokenProbabilityCardStyle
import org.simbrain.network.compositor.TokenProbabilitySnapshot
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.network.gui.dialogs.ErrorTimeSeries
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.util.*
import java.awt.geom.Point2D
import javax.swing.*

/**
 * Canvas node for a [TeachingTransformer]: an interaction box and the compositor spine interior.
 * The flagship interaction is op-level micro-stepping — step a forward pass or a whole training
 * step one op at a time, with the active op's glyph glowing, un-computed tiles dimmed, and the
 * backward half filling gradient views.
 */
class TeachingTransformerNode(networkPanel: NetworkPanel, val teachingTransformer: TeachingTransformer) :
    ScreenElement(networkPanel) {

    private val interactionBox = TeachingTransformerInteractionBox(networkPanel)

    private var compositorNode: CompositorNode? = null

    init {
        addChild(interactionBox)
        interactionBox.setText(teachingTransformer.displayName)

        val events = teachingTransformer.events
        events.labelChanged.on(swingDispatcher) { _, _ -> interactionBox.setText(teachingTransformer.displayName) }
        events.locationChanged.on(swingDispatcher) { pullLocationFromModel() }
        events.modelRebuilt.on(swingDispatcher) { rebuildInterior() }
        events.updated.on(Dispatchers.Default) { events.updateGraphics.fire() }
        events.updateGraphics.on(swingDispatcher) { refreshView() }
        events.stepRefused.on(swingDispatcher) { reason -> flashStepNotice(reason) }
        // A successful step outdates any refusal notice; drop it rather than letting the timer run out.
        events.updated.on(swingDispatcher) {
            if (stepNotice != null) {
                stepNotice = null
                stepNoticeTimer.stop()
                refreshView()
            }
        }
        teachingTransformer.trainer.events.errorUpdated.on(swingDispatcher) { refreshView() }

        rebuildInterior()
        pullLocationFromModel()
    }

    override val model: NetworkModel get() = teachingTransformer

    override val isDraggable = true

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        teachingTransformer.location =
            Point2D.Double(teachingTransformer.location.x + dx, teachingTransformer.location.y + dy)
    }

    private fun pullLocationFromModel() {
        setOffset(teachingTransformer.location.x, teachingTransformer.location.y)
    }

    override fun isIntersecting(bound: PBounds?): Boolean = interactionBox.isIntersecting(bound)

    private fun rebuildInterior() {
        compositorNode?.removeFromParent()
        compositorNode = CompositorNode(
            teachingTransformer.scene,
            networkPanel.canvas,
            tokenLabel = { id -> teachingTransformer.tokenLabels?.getOrNull(id)?.let { "“$it”" } ?: "#$id" },
            probabilitySnapshot = {
                teachingTransformer.tokenProbabilitySnapshot
                    ?: teachingTransformer.tokenLabels?.let { TokenProbabilitySnapshot.full(DoubleArray(it.size), -1) }
            },
            probabilityCardStyle = TokenProbabilityCardStyle(
                title = "current-position token probabilities",
                width = 1020.0,
                height = 315.0,
                columns = 18,
                visibleRows = 5,
            ),
            probabilityCardPosition = { scene, _, card ->
                teachingTransformer.probabilityCardLayout?.let { Point2D.Double(it[0], it[1]) } ?: run {
                    val logits = scene.tile("logits")
                    val unembedding = scene.tile("unembed.weight")
                    Point2D.Double(logits.x + logits.width + 85.0, unembedding.y + unembedding.height / 2)
                }
            },
            onProbabilityCardMoved = { x, y -> teachingTransformer.probabilityCardLayout = doubleArrayOf(x, y) },
        ).also {
            it.onLayoutChanged = {
                teachingTransformer.captureViewState()
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

    private fun flashStepNotice(refusal: TeachingTransformer.StepRefusal) {
        stepNotice = when (refusal) {
            TeachingTransformer.StepRefusal.TRAINING_WALK_IN_PROGRESS ->
                "a training walk is under way — step it (b) or finish it (shift-b) first"
            TeachingTransformer.StepRefusal.FORWARD_WALK_IN_PROGRESS ->
                "a forward walk is under way — step it (f) or finish it (shift-b) first"
            TeachingTransformer.StepRefusal.EMPTY_CONTEXT ->
                "the context window is empty — nothing to run forward"
            TeachingTransformer.StepRefusal.NO_TRAINING_WINDOWS ->
                "no training corpus — nothing to train on"
            TeachingTransformer.StepRefusal.NO_WALK_IN_PROGRESS ->
                "no walk under way — start one with f (forward pass) or b (training)"
        }
        stepNoticeTimer.restart()
        refreshView()
    }

    private fun refreshView() {
        val model = teachingTransformer.model
        val notice = stepNotice
        compositorNode?.syncStepState(
            teachingTransformer.pendingOp(),
            teachingTransformer.scene.staleTiles(model.plan.cursor),
            notice ?: teachingTransformer.stepStatusText(),
            notice != null,
        )
        compositorNode?.refreshDirtyTiles()
    }

    override val propertyDialog: StandardDialog
        get() = teachingTransformer.createEditorDialog("Edit ${teachingTransformer.displayName}")

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
                showInputDialog("Name:", teachingTransformer.label)?.let { teachingTransformer.label = it }
            })
            add(createAction(
                name = "Edit ${teachingTransformer.displayName}...",
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
                isEnabled = teachingTransformer.stepWalkInProgress
            })
            addSeparator()
            add(createAction(
                name = "Clear context window",
                description = "Empty the window; a coupled non-empty document restores " +
                    "itself on the next play, so clear the document too for a full reset",
            ) { teachingTransformer.clearWindow() })
            addSeparator()
            add(JCheckBoxMenuItem("Show last training gradients", teachingTransformer.gradientView).apply {
                isEnabled = teachingTransformer.hasGradients
                toolTipText = "Swap each tile to the gradients its last backward pass wrote; tiles " +
                    "without gradients keep their forward values. Switches itself on for the backward " +
                    "half of a training walk and back off when the context returns. Available once a " +
                    "training step has run"
                addActionListener {
                    teachingTransformer.gradientView = isSelected
                    refreshView()
                }
            })
            addSeparator()
            add(networkPanel.networkComponent.createCouplingMenu(teachingTransformer))
        }

    private fun trainingDialog(): StandardDialog {
        val trainer = teachingTransformer.trainer
        val iterationsLabel = JLabel("Iterations: ${trainer.iteration}")
        val lossLabel = JLabel("Loss: ${trainer.lastTrainingError.roundToString(4)}")
        val accuracyLabel = JLabel("Accuracy: ${trainer.lastTrainingAccuracy?.let { "${(it * 100).roundToString(1)}%" } ?: "N/A"}")
        val windowsLabel = JLabel("Training windows: ${trainer.trainingWindows.size}")

        val panel = JPanel(MigLayout("ins 10, wrap 4", "[][][][grow]"))
        panel.add(JButton("Train").apply { addActionListener { trainer.launch { trainer.startTraining() } } })
        panel.add(JButton("Stop").apply { addActionListener { trainer.launch { trainer.stopTraining() } } })
        panel.add(JButton("Step").apply { addActionListener { trainer.launch { trainer.trainOnce() } } })
        panel.add(iterationsLabel)
        panel.add(lossLabel)
        panel.add(accuracyLabel)
        panel.add(windowsLabel, "span 2")
        panel.add(ErrorTimeSeries(trainer.events) { trainer.iteration }, "span 4, grow, push")

        trainer.events.errorUpdated.on(swingDispatcher) { stats ->
            iterationsLabel.text = "Iterations: ${trainer.iteration}"
            lossLabel.text = "Loss: ${stats.trainingError.roundToString(4)}"
            accuracyLabel.text = "Accuracy: ${stats.trainingAccuracy?.let { "${(it * 100).roundToString(1)}%" } ?: "N/A"}"
        }

        return StandardDialog(panel).apply {
            title = "Train ${teachingTransformer.displayName}"
            isModal = false
        }
    }

    override fun refreshTheme() {
        interactionBox.refreshTheme()
        compositorNode?.refreshTheme()
    }

    private inner class TeachingTransformerInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu get() = this@TeachingTransformerNode.contextMenu

        override val propertyDialog: StandardDialog get() = trainingDialog()

        override val model: NetworkModel get() = this@TeachingTransformerNode.teachingTransformer
    }
}
