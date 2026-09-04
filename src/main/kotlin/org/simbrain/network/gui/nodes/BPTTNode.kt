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

import kotlinx.coroutines.launch
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.*
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
            // A different truncation depth is a different number of columns, so the network is now a
            // different width.
            if (unrolledViewNode != null) requestZoomToFit()
        }
        events.locationChanged.on(swingDispatcher) { positionUnrolledView() }
        events.displayDataUpdated.on(swingDispatcher) {
            refreshUnrolledActivations()
            // Training moves the weights, and the drawn copies of the matrices have to follow.
            unrolledViewNode?.refreshMatrices()
        }
        events.deleted.on(swingDispatcher) { detachUnrolledView() }
        // A layer changing how it draws itself changes what the columns should look like and how big they
        // are. Only nudges the layout, since the new size is not known until that layer's node lays out.
        listOf(bptt.inputLayer, bptt.hiddenLayer, bptt.outputLayer).forEach { layer ->
            layer.events.visualPropertiesChanged.on(swingDispatcher) { invalidateLayout() }
        }
        syncUnrolledView()
        if (bptt.unrolledView) {
            // Reached when a network is restored with the view already showing, where no toggle event
            // will arrive, and where the weight matrix nodes do not exist yet to be looked up.
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

    /**
     * The columns are placed against the rolled network's drawn extent rather than against its layers,
     * because it also draws its weight matrix nodes and a recurrent arrow that bulges out to the left,
     * which is the side the columns are on.
     *
     * Measured from the subnetwork's contents rather than from the outline, because the outline now
     * encloses the unrolled view as well, and positioning the view against bounds it is itself part of
     * would feed back on itself.
     *
     * Null before the contents have been laid out, which happens when a saved network is restored with the
     * view already showing. [layoutChildren] retries once the measurement becomes available.
     */
    private fun measureRolledLeftEdge(): kotlin.Double? =
        outlinedObjectBounds.takeUnless { it.isEmpty }?.x

    /**
     * Rebuilds the drawing once the rolled network becomes measurable, and again whenever a layer changes
     * how it draws itself. Both are checked here rather than driven by an event because a layer's size is
     * pushed back from its node only after that node lays out, so a mode change is not fully known at the
     * moment it is announced.
     */
    override fun layoutChildren() {
        super.layoutChildren()
        val view = unrolledViewNode ?: return
        if ((!view.laidOut && measureRolledLeftEdge() != null) || view.stale()) {
            view.rebuild()
            positionUnrolledView()
            refreshUnrolledActivations()
            // Reached when the columns are first drawn, and whenever a layer changes how wide it draws
            // itself, both of which change the extent the canvas should be fitted to.
            requestZoomToFit()
        }
    }

    /**
     * Hide the recurrent matrix's loop while the network is unrolled. The chain of arrows across the
     * columns is that same connection drawn a step at a time, so showing both says it twice. Only the loop
     * goes: the matrix itself stays visible and editable, and its image box already sits along the arrow
     * carrying the previous step into the live one, which is where that application happens.
     */
    private fun syncRecurrentArrow() {
        // Launched because the matrix's node may not have been built yet, which is the case when a saved
        // network is restored with the view already showing.
        networkPanel.network.launch(swingDispatcher) {
            networkPanel.modelNodeMap.getImmediately<WeightMatrixNode>(bptt.hiddenToHidden)
                ?.arrowVisible = !bptt.unrolledView
        }
    }

    private fun syncUnrolledView() {
        syncRecurrentArrow()
        if (bptt.unrolledView) {
            if (unrolledViewNode == null) {
                unrolledViewNode = BPTTUnrolledView(bptt, networkPanel, ::measureRolledLeftEdge).also {
                    addChild(it)
                    // Enclosed by the outline so the columns read as belonging to the subnetwork rather
                    // than as loose drawing beside it.
                    setDecoration(it)
                }
            }
            positionUnrolledView()
            refreshUnrolledActivations()
        } else {
            detachUnrolledView()
        }
        // Both directions change the extent: showing the columns widens the network by several times,
        // and hiding them narrows it back again.
        requestZoomToFit()
    }

    /**
     * Push the recorded timesteps into the drawn columns.
     *
     * The history's last entry is the step the layers currently hold, and the rolled network draws that
     * one itself, so the list is lined up against the columns from the right. A history shorter than the
     * window leaves the oldest columns empty, which is the honest picture just after a reset: those steps
     * have not happened yet. A history longer than the drawing can show, which happens once the
     * truncation depth passes the column cap, drops off its oldest end.
     */
    private fun refreshUnrolledActivations() {
        val view = unrolledViewNode ?: return
        view.clearColumns()
        val trace = bptt.unrolledActivations
        val offset = view.stepCount - trace.size
        trace.forEachIndexed { index, byLayer ->
            val step = offset + index
            val input = byLayer[bptt.inputLayer] ?: return@forEachIndexed
            val hidden = byLayer[bptt.hiddenLayer] ?: return@forEachIndexed
            val output = byLayer[bptt.outputLayer] ?: return@forEachIndexed
            view.showActivations(step, input.toDoubleArray(), hidden.toDoubleArray(), output.toDoubleArray())
        }
    }

    private fun detachUnrolledView() {
        unrolledViewNode?.let { removeChild(it) }
        unrolledViewNode = null
        setDecoration(null)
    }

    private fun positionUnrolledView() {
        unrolledViewNode?.syncPosition()
    }

    /**
     * Ask the canvas to fit itself around the network again. The drawn columns are part of what the
     * subnetwork covers, so showing them, hiding them, or changing how many there are moves where the
     * network ends, and a view fitted to the previous extent leaves them off the edge of the screen.
     *
     * Only called where that extent actually changes, and not from [positionUnrolledView], which also runs
     * while the user is dragging the network about; re-fitting on every drag would fight them for control
     * of the camera.
     *
     * A request rather than a zoom, so that it stays the panel's decision: it is ignored when the user has
     * turned auto zoom off, and repeated requests collapse into one.
     */
    private fun requestZoomToFit() {
        networkPanel.network.events.zoomToFitPage.fire()
    }
}
