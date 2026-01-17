package org.simbrain.network.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.MouseEventHandler.MouseCursor
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandPalette
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandRadius
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import java.awt.event.InputEvent
import java.awt.geom.Ellipse2D

/**
 * Wand event handler. Applies the selected wand action when dragging over neurons.
 */
class WandEventHandler(val networkPanel: NetworkPanel) : PDragSequenceEventHandler() {
    /**
     * Bounds filter.
     */
    private val boundsFilter: BoundsFilter = BoundsFilter()

    /**
     * Create a new selection event handler.
     *
     * @param networkPanel
     */
    init {
        eventFilter = WandEventFilter()
    }

    /**
     * Tracks state for undo. Different actions may store different types.
     */
    private var undoState = mutableMapOf<Any, Any?>()

    /**
     * Tracks models that have already been operated on in this drag session.
     * This prevents repeat actions (e.g., randomizing the same neuron multiple times).
     */
    private val touchedModels = mutableSetOf<NetworkModel>()

    override fun mousePressed(event: PInputEvent) {
        super.mousePressed(event)
        undoState = mutableMapOf()
        touchedModels.clear()

        // Notify action that we're starting
        wandPalette.selectedAction?.beginAction(networkPanel)

        val node = event.path.pickedNode
        when (node) {
            is NeuronNode -> modifyModel(node.neuron)
            is SynapseNode -> modifyModel(node.synapse)
        }
    }

    override fun mouseReleased(event: PInputEvent?) {
        super.mouseReleased(event)
        val action = wandPalette.selectedAction

        // Let the action handle its own undo if needed (e.g., ConnectFromSourceAction)
        action?.endAction(networkPanel)

        // TODO: Move this logic to the action itself.
        if (undoState.isNotEmpty() && action !is ConnectFromSourceAction) {
            @Suppress("UNCHECKED_CAST")
            val neuronDiffs = undoState.filterKeys { it is Neuron } as Map<Neuron, Double>
            @Suppress("UNCHECKED_CAST")
            val synapseDiffs = undoState.filterKeys { it is Synapse } as Map<Synapse, Double>

            val totalCount = neuronDiffs.size + synapseDiffs.size
            if (totalCount > 0) {
                val neuronRedos = neuronDiffs.keys.associateWith { it.activation }
                val synapseRedos = synapseDiffs.keys.associateWith { it.strength }

                networkPanel.undoManager.addUndoableAction(
                    description = action?.undoDescription(totalCount) ?: "Wand Action",
                    undo = {
                        neuronDiffs.forEach { (neuron, prev) -> neuron.activation = prev }
                        synapseDiffs.forEach { (synapse, prev) -> synapse.strength = prev }
                    },
                    redo = {
                        neuronRedos.forEach { (neuron, redo) -> neuron.activation = redo }
                        synapseRedos.forEach { (synapse, redo) -> synapse.strength = redo }
                    }
                )
            }
        }
    }

    override fun startDrag(event: PInputEvent?) {
        super.startDrag(event)
    }

    override fun drag(event: PInputEvent) {
        super.drag(event)

        val radius = wandRadius

        // Create elliptical bounds
        val position = event.position
        val ellipse = Ellipse2D.Double(
            position.x - radius / 2,
            position.y - radius / 2,
            radius.toDouble(),
            radius.toDouble()
        )
        boundsFilter.setEllipse(ellipse)

        val highlightedNodes = networkPanel.canvas.layer.root.getAllNodes(boundsFilter, null)

        // Apply action to all neurons and synapses in bounds
        for (node in highlightedNodes) {
            when (node) {
                is NeuronNode -> modifyModel(node.neuron)
                is SynapseNode -> modifyModel(node.synapse)
            }
        }
    }

    override fun endDrag(event: PInputEvent?) {
        super.endDrag(event)
    }

    /**
     * Apply the selected wand action to a model.
     * Skips models that have already been operated on in this drag session.
     */
    private fun modifyModel(model: NetworkModel) {
        if (model in touchedModels) return
        touchedModels.add(model)
        wandPalette.selectedAction?.apply(model, networkPanel, undoState)
    }

    /**
     * Bounds filter.
     */
    private inner class BoundsFilter : PNodeFilter {
        /**
         * Bounds.
         */
        private var ellipse: Ellipse2D.Double? = null

        /**
         * Set the bounds for this bounds filter to `bounds`.
         *
         * @param ellipse bounds for this bounds filter
         */
        fun setEllipse(ellipse: Ellipse2D.Double) {
            this.ellipse = ellipse
        }

        /**
         * @param node
         * @return
         * @see PNodeFilter
         */
        override fun accept(node: PNode): Boolean {
            val isPickable = node.pickable
            val boundsIntersects = ellipse!!.intersects(node.globalBounds)
            val isLayer = (node is PLayer)
            val isCamera = (node is PCamera)

            return (isPickable && boundsIntersects && !isLayer && !isCamera)
        }

        override fun acceptChildrenOf(node: PNode): Boolean {
            val areChildrenPickable = node.childrenPickable
            val isCamera = (node is PCamera)
            val isLayer = (node is PLayer)
            return (areChildrenPickable || isCamera || isLayer)
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when the
     * network panel's cursor is `MouseCursor.Wand`.
     */
    private inner class WandEventFilter : PInputEventFilter(InputEvent.BUTTON1_MASK) {
        override fun acceptsEvent(event: PInputEvent?, type: Int): Boolean {
            val mouseCursor = networkPanel.mouseCursor

            return mouseCursor === MouseCursor.Wand && super.acceptsEvent(event, type)
        }
    }
}
