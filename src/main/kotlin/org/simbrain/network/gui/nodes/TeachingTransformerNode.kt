package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.simbrain.network.compositor.CompositorNode
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.ErrorTimeSeries
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.network.llm.TeachingTransformerModel
import org.simbrain.util.NetworkTheme
import org.simbrain.util.StandardDialog
import org.simbrain.util.Theme
import org.simbrain.util.createAction
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.roundToString
import org.simbrain.util.showInputDialog
import org.simbrain.util.swingDispatcher
import java.awt.geom.Point2D
import javax.swing.JButton
import javax.swing.JCheckBoxMenuItem
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPopupMenu

/**
 * Canvas node for a [TeachingTransformer]: an interaction box, the compositor spine interior,
 * and a status line with training and stepping state. The flagship interaction is op-level
 * micro-stepping — step a forward pass or a whole training step one op at a time, with the
 * active op's glyph glowing, un-computed tiles dimmed, and the backward half filling gradient
 * views.
 */
class TeachingTransformerNode(networkPanel: NetworkPanel, val teachingTransformer: TeachingTransformer) :
    ScreenElement(networkPanel) {

    private val interactionBox = TeachingTransformerInteractionBox(networkPanel)

    private val statusText = PText().apply {
        font = Theme.body
        textPaint = NetworkTheme.current.valueText
    }

    private var compositorNode: CompositorNode? = null

    private var gradientView = false

    init {
        addChild(interactionBox)
        addChild(statusText)
        interactionBox.setText(teachingTransformer.displayName)

        val events = teachingTransformer.events
        events.labelChanged.on(swingDispatcher) { _, _ -> interactionBox.setText(teachingTransformer.displayName) }
        events.locationChanged.on(swingDispatcher) { pullLocationFromModel() }
        events.modelRebuilt.on(swingDispatcher) { rebuildInterior() }
        events.updated.on(Dispatchers.Default) { events.updateGraphics.fire() }
        events.updateGraphics.on(swingDispatcher) { refreshView() }
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
        ).also {
            it.onLayoutChanged = { teachingTransformer.captureViewState() }
            addChild(it)
        }
        placeChildren()
        refreshView()
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
        val model = teachingTransformer.model
        compositorNode?.syncStepState(
            teachingTransformer.pendingOp(),
            teachingTransformer.scene.staleTiles(model.plan.cursor),
        )
        compositorNode?.refreshDirtyTiles()
        statusText.text = statusLine()
    }

    private fun statusLine(): String {
        val trainer = teachingTransformer.trainer
        val model = teachingTransformer.model
        val generation = when {
            teachingTransformer.waitingForInput -> "waiting for input"
            else -> "writing (play or step the network)"
        }
        val phase = when (model.stepPhase) {
            TeachingTransformerModel.StepPhase.IDLE ->
                if (model.plan.cursor != 0) "stepping forward: ${teachingTransformer.pendingOp()?.name}" else null
            TeachingTransformerModel.StepPhase.FORWARD -> "train step, forward: ${model.nextOp()?.name}"
            TeachingTransformerModel.StepPhase.BACKWARD -> "train step, backward: ${model.nextOp()?.name}"
        }
        val training = when {
            trainer.isRunning -> "training, iteration ${trainer.iteration}, loss ${trainer.lastTrainingError.roundToString(4)}"
            trainer.iteration > 0 -> "trained ${trainer.iteration} iterations, loss ${trainer.lastTrainingError.roundToString(4)}"
            else -> "untrained"
        }
        val context = teachingTransformer.contextTokens
            .takeLast(8)
            .joinToString(" ") { teachingTransformer.tokenLabels?.getOrNull(it) ?: "#$it" }
        return listOfNotNull(
            generation,
            training,
            phase,
            if (context.isNotEmpty()) "context: …$context" else null,
        ).joinToString("  ·  ")
    }

    override val propertyDialog: StandardDialog
        get() = teachingTransformer.createEditorDialog("Teaching Transformer Settings")

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(createAction("Rename...") {
                showInputDialog("Name:", teachingTransformer.label)?.let { teachingTransformer.label = it }
            })
            add(createAction(name = "Remove teaching transformer", coroutineScope = networkPanel.network) {
                teachingTransformer.deleteBlocking()
            })
            addSeparator()
            add(createAction("Train...") { trainingDialog().display() })
            add(createAction("Settings...") { propertyDialog.display() })
            addSeparator()
            add(createAction("Step forward pass one op") {
                teachingTransformer.stepInferenceOp()
            })
            add(createAction("Step training one op") {
                teachingTransformer.stepTrainingOp()
            })
            add(createAction("Finish current step walk") {
                val model = teachingTransformer.model
                while (model.stepPhase != TeachingTransformerModel.StepPhase.IDLE) teachingTransformer.stepTrainingOp()
                while (model.plan.cursor != 0) teachingTransformer.stepInferenceOp()
            })
            addSeparator()
            add(createAction("Clear context") { teachingTransformer.clearWindow() })
            addSeparator()
            add(JCheckBoxMenuItem("Gradient view", gradientView).apply {
                addActionListener {
                    gradientView = isSelected
                    teachingTransformer.scene.setGradientView(isSelected)
                    teachingTransformer.scene.publish()
                    refreshView()
                }
            })
            add(JCheckBoxMenuItem("Logit lens", teachingTransformer.lensEnabled).apply {
                addActionListener {
                    teachingTransformer.lensEnabled = isSelected
                    refreshView()
                }
            })
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
        statusText.textPaint = NetworkTheme.current.valueText
        compositorNode?.refreshTheme()
    }

    private inner class TeachingTransformerInteractionBox(net: NetworkPanel) : InteractionBox(net) {

        override val contextMenu: JPopupMenu get() = this@TeachingTransformerNode.contextMenu

        override val propertyDialog: StandardDialog get() = this@TeachingTransformerNode.propertyDialog

        override val model: NetworkModel get() = this@TeachingTransformerNode.teachingTransformer
    }
}
