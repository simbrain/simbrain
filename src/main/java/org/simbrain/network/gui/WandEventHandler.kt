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
import org.simbrain.network.gui.MouseEventHandler.MouseCursor
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandPalette
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandRadius
import org.simbrain.network.gui.nodes.NeuronNode
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
        if (node is NeuronNode) {
            modifyNode(node)
        }
    }

    override fun mouseReleased(event: PInputEvent?) {
        super.mouseReleased(event)
        val action = wandPalette.selectedAction

        // Let the action handle its own undo if needed (e.g., ConnectFromSourceAction)
        action?.endAction(networkPanel)

        // TODO: Move this logic to the action itself. Right now only activation actions are tracked.
        if (undoState.isNotEmpty() && action !is ConnectFromSourceAction) {
            @Suppress("UNCHECKED_CAST")
            val activationDiffs = undoState.filterKeys { it is Neuron }
                as Map<Neuron, Double>

            if (activationDiffs.isNotEmpty()) {
                val redos = activationDiffs.keys.associateWith { it.activation }
                networkPanel.undoManager.addUndoableAction(
                    description = action?.undoDescription(activationDiffs.size) ?: "Wand Action",
                    undo = {
                        activationDiffs.forEach { (neuron, previousActivation) ->
                            neuron.activation = previousActivation
                        }
                    },
                    redo = {
                        redos.forEach { (neuron, redoActivation) ->
                            neuron.activation = redoActivation
                        }
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

        // Apply action to all neurons in bounds
        for (node in highlightedNodes) {
            if (node is NeuronNode) {
                modifyNode(node)
            }
        }
    }

    override fun endDrag(event: PInputEvent?) {
        super.endDrag(event)
    }

    /**
     * Apply the selected wand action to the model.
     * Skips models that have already been operated on in this drag session.
     *
     * @param node node to act on
     */
    private fun modifyNode(node: NeuronNode) {
        val model = node.neuron
        // Skip if already touched in this session
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
