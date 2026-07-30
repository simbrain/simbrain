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
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.util.point
import org.simbrain.util.swingDispatcher
import javax.swing.JPopupMenu

class BPTTNode(networkPanel: NetworkPanel, private val bptt: BPTTNetwork) :
    SubnetworkNode(networkPanel, bptt) {

    private var unrolledViewNode: BPTTUnrolledView? = null

    init {
        val events = bptt.events
        events.displayModeChanged.on(swingDispatcher) { syncUnrolledView() }
        // Truncation depth changes the number of columns, and it is what the info text reports.
        events.customInfoUpdated.on(swingDispatcher) {
            unrolledViewNode?.rebuild()
            positionUnrolledView()
        }
        events.locationChanged.on(swingDispatcher) { positionUnrolledView() }
        events.deleted.on(swingDispatcher) { detachUnrolledView() }
        syncUnrolledView()
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
    }

    private fun syncUnrolledView() {
        if (bptt.unrolledView) {
            if (unrolledViewNode == null) {
                unrolledViewNode = BPTTUnrolledView(bptt).also { addChild(it) }
            }
            positionUnrolledView()
        } else {
            detachUnrolledView()
        }
    }

    private fun detachUnrolledView() {
        unrolledViewNode?.let { removeChild(it) }
        unrolledViewNode = null
    }

    private fun positionUnrolledView() {
        val view = unrolledViewNode ?: return
        val bounds = outline.globalFullBounds
        view.globalTranslation = point(
            bounds.maxX + UNROLLED_VIEW_GAP,
            bounds.y + (bounds.height - view.fullBounds.height) / 2
        )
    }

    companion object {
        private const val UNROLLED_VIEW_GAP = 60.0
    }
}
