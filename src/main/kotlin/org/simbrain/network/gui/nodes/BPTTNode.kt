/**
 * Canvas node for [BPTTNetwork].
 *
 * Draws the network in rolled-up form, where the hidden layer's self-connection is rendered by the
 * standard [WeightMatrixNode] machinery as a recurrent arrow, and optionally shows a
 * [BPTTUnrolledView] beside it.
 *
 * The unrolled picture is a child of this node but deliberately not part of the subnetwork outline: it
 * illustrates a derived structure rather than any model object, so the outline stays wrapped around
 * the real layers. Being a child is what gets it counted by zoom-to-fit, which unions the bounds of
 * screen elements only and would otherwise leave it off the edge of the view.
 */
package org.simbrain.network.gui.nodes

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.toDoubleArray
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.util.point
import kotlinx.coroutines.launch
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.simbrain.util.swingDispatcher
import javax.swing.JPopupMenu

class BPTTNode(networkPanel: NetworkPanel, private val bptt: BPTTNetwork) :
    SubnetworkNode(networkPanel, bptt) {

    private var unrolledViewNode: BPTTUnrolledView? = null

    private var highlightingAttached = false

    init {
        val events = bptt.events
        events.displayModeChanged.on(swingDispatcher) {
            syncUnrolledView()
            if (bptt.unrolledView) attachSharedWeightHighlighting()
        }
        // Truncation depth changes the number of columns, and it is what the info text reports.
        events.customInfoUpdated.on(swingDispatcher) {
            unrolledViewNode?.rebuild()
            positionUnrolledView()
            // A rebuild starts the columns empty, so the values on show have to be put back.
            refreshUnrolledActivations()
        }
        events.locationChanged.on(swingDispatcher) { positionUnrolledView() }
        events.displayDataUpdated.on(swingDispatcher) { refreshUnrolledActivations() }
        events.deleted.on(swingDispatcher) { detachUnrolledView() }
        syncUnrolledView()
        if (bptt.unrolledView) {
            // Reached when a network is restored with the view already showing, where no toggle event
            // will arrive to attach the hover handlers.
            networkPanel.network.launch(swingDispatcher) { attachSharedWeightHighlighting() }
        }
    }

    override val model: NetworkModel
        get() = bptt

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            applyBasicActions()

            add(networkPanel.createAction(
                name = if (bptt.unrolledView) "Hide unrolled view" else "Show unrolled view",
                description = "Show the network unrolled over time next to its rolled-up form"
            ) {
                bptt.unrolledView = !bptt.unrolledView
            })
            add(networkPanel.createAction(
                name = "Unroll one more step",
                description = "Increase the truncation depth, letting the gradient reach one step further back"
            ) {
                setTruncationDepth(bptt.trainerConfig.truncationDepth + 1)
            })
            add(networkPanel.createAction(
                name = "Unroll one fewer step",
                description = "Decrease the truncation depth, cutting the gradient off one step sooner"
            ) {
                setTruncationDepth(bptt.trainerConfig.truncationDepth - 1)
            })
            addSeparator()

            add(networkPanel.createAction(name = "Train...") {
                bptt.getSupervisedTrainingDialog().display()
            })
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { bptt.getSupervisedTrainingDialog() }

    override fun refreshTheme() {
        super.refreshTheme()
        unrolledViewNode?.rebuild()
        positionUnrolledView()
        refreshUnrolledActivations()
    }

    /**
     * Truncation depth doubles as a display setting here, since it is the number of unrolled columns.
     * Updating the info text is what makes the view rebuild at the new depth.
     */
    private fun setTruncationDepth(depth: Int) {
        bptt.trainerConfig.truncationDepth = depth.coerceAtLeast(1)
        bptt.updateStateInfoText()
    }

    /**
     * Hovering one of the real weight matrices lights every drawn instance of it in the unrolled view.
     * This is the one thing the unrolled picture cannot say on its own: the columns look like separate
     * matrices, and they are not.
     */
    private suspend fun attachSharedWeightHighlighting() {
        if (highlightingAttached) return
        val roles = listOf(
            bptt.wmList[0] to BPTTUnrolledView.SharedWeights.INPUT_TO_HIDDEN,
            bptt.wmList[1] to BPTTUnrolledView.SharedWeights.HIDDEN_TO_OUTPUT,
            bptt.hiddenToHidden to BPTTUnrolledView.SharedWeights.RECURRENT
        )
        roles.forEach { (matrix, role) ->
            // getImmediately rather than getNode: the latter blocks for ten seconds and then throws if a
            // node is missing, which is not worth risking on the event thread for a decoration.
            val node = networkPanel.modelNodeMap.getImmediately<WeightMatrixNode>(matrix) ?: return@forEach
            node.imageBox.addInputEventListener(object : PBasicInputEventHandler() {
                override fun mouseEntered(event: PInputEvent) {
                    unrolledViewNode?.highlight(role)
                }

                override fun mouseExited(event: PInputEvent) {
                    unrolledViewNode?.highlight(null)
                }
            })
        }
        highlightingAttached = true
    }

    private fun syncUnrolledView() {
        if (bptt.unrolledView) {
            if (unrolledViewNode == null) {
                unrolledViewNode = BPTTUnrolledView(bptt).also { addChild(it) }
            }
            positionUnrolledView()
            refreshUnrolledActivations()
        } else {
            detachUnrolledView()
        }
    }

    /**
     * Push the most recent window's per-timestep activations into the drawn columns. Step zero is the
     * rolled network itself, which draws its own values, so the columns take the steps after it.
     */
    private fun refreshUnrolledActivations() {
        val view = unrolledViewNode ?: return
        bptt.unrolledActivations.forEachIndexed { step, byLayer ->
            if (step == 0) return@forEachIndexed
            val input = byLayer[bptt.inputLayer] ?: return@forEachIndexed
            val hidden = byLayer[bptt.hiddenLayer] ?: return@forEachIndexed
            val output = byLayer[bptt.outputLayer] ?: return@forEachIndexed
            view.showActivations(step, input.toDoubleArray(), hidden.toDoubleArray(), output.toDoubleArray())
        }
    }

    private fun detachUnrolledView() {
        unrolledViewNode?.let { removeChild(it) }
        unrolledViewNode = null
    }

    private fun positionUnrolledView() {
        unrolledViewNode?.syncPosition()
    }

    companion object {
        private const val UNROLLED_VIEW_GAP = 60.0
    }
}
