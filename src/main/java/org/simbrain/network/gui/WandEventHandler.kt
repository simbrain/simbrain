/**
 * Wand strokes on the network canvas. A stroke runs from mouse press to release and applies one action from the
 * wand palette to everything the brush passes over: the selected action, or the secondary action if Shift was held
 * when the stroke began and the palette has one.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.MouseEventHandler.MouseCursor
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandPalette
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
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

    /**
     * Tracks pixels (neuron-array / weight-matrix cells) already operated on this drag session,
     * so a single drag doesn't randomize / increment the same pixel repeatedly.
     */
    private val touchedPixels = mutableSetOf<PixelTarget>()

    /**
     * Tracks pending apply jobs so we can await them before finalizing the action.
     */
    private val pendingJobs = mutableListOf<Job>()

    /**
     * The action the stroke in progress applies. Chosen when the stroke begins and kept until it ends, so releasing
     * Shift or changing the palette part way through does not hand one action's undo state to another.
     */
    private var strokeAction: WandAction? = null

    private var strokeInProgress = false

    /**
     * Whether the cursor shows the secondary action rather than the selected one.
     */
    var cursorShowsSecondary = false
        private set

    /**
     * Tell the wand whether Shift is held, so the cursor shows the action the next stroke would apply. Ignored
     * during a stroke, whose cursor stays that of the action it began with. While another tool is current this is
     * only noted, for the cursor drawn when the wand is next switched to.
     */
    fun shiftChanged(shiftDown: Boolean) {
        if (strokeInProgress || shiftDown == cursorShowsSecondary) return
        cursorShowsSecondary = shiftDown
        if (networkPanel.mouseCursor === MouseCursor.Wand) refreshCursor()
    }

    /**
     * Redraw the wand cursor for the action a stroke would now apply, and show it if the wand is the current tool.
     */
    fun refreshCursor() {
        val action = (if (cursorShowsSecondary) wandPalette.secondaryAction else null)
            ?: wandPalette.selectedAction
            ?: return
        MouseCursor.Wand.update(action.color, action.radius)
        if (networkPanel.mouseCursor === MouseCursor.Wand) networkPanel.cursor = MouseCursor.Wand.cursor
    }

    override fun mouseMoved(event: PInputEvent) {
        super.mouseMoved(event)
        shiftChanged(event.isShiftDown)
    }

    override fun mousePressed(event: PInputEvent) {
        super.mousePressed(event)
        undoState = mutableMapOf()
        touchedModels.clear()
        touchedPixels.clear()
        pendingJobs.clear()

        // A stroke whose release never arrived is over all the same
        strokeInProgress = false
        shiftChanged(event.isShiftDown)
        strokeInProgress = true
        // Without a secondary action Shift changes nothing, as the cursor shows
        strokeAction = (if (event.isShiftDown) wandPalette.secondaryAction else null) ?: wandPalette.selectedAction

        // Notify action that we're starting
        strokeAction?.beginAction(networkPanel)

        applyUnderBrush(event)
    }

    override fun mouseReleased(event: PInputEvent) {
        super.mouseReleased(event)
        val action = strokeAction
        val strokeUndoState = undoState
        val jobs = pendingJobs.toList()
        strokeAction = null
        strokeInProgress = false
        shiftChanged(event.isShiftDown)

        // Launch a coroutine to await all pending apply jobs, then finalize
        networkPanel.launch(Dispatchers.Swing) {
            jobs.joinAll()
            if (strokeUndoState.isNotEmpty()) networkPanel.networkComponent.markChanged()
            action?.endAction(networkPanel, strokeUndoState)
        }
    }

    override fun startDrag(event: PInputEvent?) {
        super.startDrag(event)
    }

    override fun drag(event: PInputEvent) {
        super.drag(event)
        applyUnderBrush(event)
    }

    /**
     * Apply the stroke's action to everything under the brush at the position of [event].
     */
    private fun applyUnderBrush(event: PInputEvent) {
        val action = strokeAction ?: return
        val baseRadius = action.radius
        // Scale radius by inverse of zoom so cursor size matches effect area
        val viewScale = networkPanel.canvas.camera.viewScale
        val radius = baseRadius / viewScale

        // Create elliptical bounds
        val position = event.position
        val ellipse = Ellipse2D.Double(
            position.x - radius / 2,
            position.y - radius / 2,
            radius,
            radius
        )
        boundsFilter.setEllipse(ellipse)

        val highlightedNodes = networkPanel.canvas.layer.root.getAllNodes(boundsFilter, null)

        // Apply action to all neurons and synapses in bounds
        for (node in highlightedNodes) {
            when (node) {
                is NeuronNode -> modifyModel(node.neuron, action)
                is SynapseNode -> modifyModel(node.synapse, action)
            }
        }

        // Apply action to neuron-array / weight-matrix pixels under the cursor
        networkPanel.pixelsInGlobalEllipse(ellipse).forEach { pixel -> modifyPixel(pixel, action) }
    }

    override fun endDrag(event: PInputEvent?) {
        super.endDrag(event)
    }

    /**
     * Apply the stroke's wand action to a model.
     * Skips models that have already been operated on in this drag session.
     * The action and the undo state are those of the stroke as it is now, not as it is when the launched work
     * runs, which can be after the stroke has ended and another has begun.
     */
    private fun modifyModel(model: NetworkModel, action: WandAction) {
        if (model in touchedModels) return
        touchedModels.add(model)
        val strokeUndoState = undoState
        val job = networkPanel.launch(Dispatchers.Swing) {
            action.apply(model, networkPanel, strokeUndoState)
        }
        pendingJobs.add(job)
    }

    /**
     * Apply the stroke's wand action to a single array / matrix pixel.
     * Skips pixels already operated on in this drag session.
     */
    private fun modifyPixel(pixel: PixelTarget, action: WandAction) {
        if (pixel in touchedPixels) return
        touchedPixels.add(pixel)
        val strokeUndoState = undoState
        val job = networkPanel.launch(Dispatchers.Swing) {
            action.applyToPixel(pixel, networkPanel, strokeUndoState)
        }
        pendingJobs.add(job)
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
     * network panel's cursor is `MouseCursor.Wand`. Movement with no button down is accepted too, for the cursor
     * to follow Shift, and so is the release that ends a stroke after another tool was switched to part way
     * through it, for the stroke to be finished off.
     */
    private inner class WandEventFilter : PInputEventFilter(InputEvent.BUTTON1_MASK) {
        override fun acceptsEvent(event: PInputEvent?, type: Int): Boolean {
            val mouseCursor = networkPanel.mouseCursor

            if (strokeInProgress && type == MouseEvent.MOUSE_RELEASED) return super.acceptsEvent(event, type)
            return mouseCursor === MouseCursor.Wand
                    && (type == MouseEvent.MOUSE_MOVED || super.acceptsEvent(event, type))
        }
    }
}
