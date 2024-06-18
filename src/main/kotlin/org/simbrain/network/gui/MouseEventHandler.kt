/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.topLeftLocation
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.Utils
import org.simbrain.util.minus
import org.simbrain.util.piccolo.SelectionMarquee
import org.simbrain.util.piccolo.firstScreenElement
import org.simbrain.util.piccolo.screenElements
import org.simbrain.util.rectangle
import java.awt.Color
import java.awt.event.InputEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

class MouseEventHandler(val networkPanel: NetworkPanel) : PDragSequenceEventHandler() {

    private enum class Mode { SELECTION, PAN, DRAG }

    private var mode = Mode.DRAG

    private var priorSelection = setOf<ScreenElement>()

    private lateinit var marqueeStartPosition: Point2D

    private lateinit var marqueeEndPosition: Point2D

    /**
     * Red line that shows what the delta for the [PlacementManager] will be.
     */
    private var placementManagerDelta: PPath? = null

    private val PInputEvent.isPanKeyDown get() = if (Utils.isMacOSX()) isMetaDown else isControlDown

    private val selectionMarquee by lazy {
        with(marqueeStartPosition) { SelectionMarquee(x.toFloat(), y.toFloat()) }.also {
            networkPanel.canvas.layer.addChild(it)
            it.visible = false
        }
    }

    override fun mouseClicked(event: PInputEvent?) {
        super.mouseClicked(event)
        event?.position?.let {
            networkPanel.network.placementManager.lastClickedLocation = it
        }
    }

    init {
        // Only handle events in selection mode
        eventFilter = object : PInputEventFilter(InputEvent.BUTTON1_MASK) {
            override fun acceptsEvent(event: PInputEvent, type: Int): Boolean {
                val editMode = networkPanel.editMode
                return if (editMode.isSelection && super.acceptsEvent(event, type)) {
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * Handles beginnings of drag as well as single-click events.
     */
    override fun startDrag(event: PInputEvent) {

        super.startDrag(event)

        val pickedNode: PNode? = event.pickedNode
        pickedNode?.firstScreenElement?.let { pickedScreenElement ->
            mode = Mode.DRAG
            // Toggle selection
            if (event.isShiftDown) {
                networkPanel.selectionManager.toggle(pickedScreenElement)
            }
            // Required so that clicking to drag does not de-select all other nodes
            if (pickedScreenElement !in networkPanel.selectionManager.selection) {
                if (!event.isShiftDown) {
                    networkPanel.selectionManager.set(pickedScreenElement)
                }
            }
        }

        priorSelection = networkPanel.selectionManager.selection.toMutableSet()
        marqueeStartPosition = event.position
        marqueeEndPosition = event.position
        selectionMarquee.reset()

        when {
            event.isPanKeyDown -> mode = Mode.PAN
            pickedNode is PCamera -> {
                if (!event.isShiftDown) networkPanel.selectionManager.clear()
                mode = Mode.SELECTION
            }
        }
    }

    override fun drag(event: PInputEvent) {
        super.drag(event)
        when (mode) {
            Mode.PAN -> pan(event)
            Mode.SELECTION -> select(event)
            Mode.DRAG -> dragItems(event)
        }
        marqueeEndPosition = event.position
    }


    override fun endDrag(event: PInputEvent) {
        super.endDrag(event)
        if (mode == Mode.SELECTION) {
            selectionMarquee.visible = false
        } else {
            dragItems(event)
            priorSelection = setOf()

            // Reset the anchor point in the placement manager
            val topLeft = networkPanel.selectionManager.filterSelectedModels<LocatableModel>().topLeftLocation
            val pm = networkPanel.network.placementManager
            pm.anchorPoint = topLeft

            // Only reset the delta if alt/option key is down
            if (event.pickedNode != null && event.isAltDown) {
                event.pickedNode.firstScreenElement?.model.let {
                    if (it is LocatableModel) {
                        pm.deltaDragMap[it.javaClass.kotlin] = topLeft - pm.previousAnchorPoint
                    }
                }
            }
        }
        networkPanel.canvas.layer.removeChild(placementManagerDelta)
        networkPanel.network.events.zoomToFitPage.fire()
    }

    /**
     * Pans the camera in response to the pan event provided. (From the source code for PanEventHandler. Note that
     * "autopan"--from that class--is not being used. Not sure what is being lost by not using it.)
     *
     * @param event contains details about the drag used to translate the view
     * @author Jesse Grosjean
     */
    private fun pan(event: PInputEvent) {
        val camera = event.camera!!
        if (camera.viewBounds.contains(event.position)) {
            with(event.delta) { camera.translateView(width, height) }
        }
    }

    private fun select(event: PInputEvent) {
        val bound = PBounds(rectangle(marqueeStartPosition, event.position)).apply {
            selectionMarquee.globalToLocal(this)
            selectionMarquee.reset() // todo: better way?
            selectionMarquee.append(Rectangle2D.Double(x, y, width, height), false)
            selectionMarquee.visible = true
        }

        val selectedNodes = networkPanel.canvas.layer.root.getAllNodes(
            BoundsFilter(bound), null
        ).filterIsInstance<ScreenElement>()

        val finalSelection = if (event.isShiftDown) {
            (priorSelection + selectedNodes) - (priorSelection intersect selectedNodes)
        } else {
            selectedNodes
        }
        networkPanel.selectionManager.set(finalSelection)
    }

    /**
     * Search through what's clicked on, upwards through parents, to find the first draggable item, and then
     * drag that. See [screenElements].
     */
    private fun dragItems(event: PInputEvent) {
        val delta = event.position - marqueeEndPosition
        networkPanel.selectionManager.selection.map { it.screenElements.firstOrNull(ScreenElement::isDraggable) }
            .forEach { it?.offset(delta.x, delta.y) }

        // Show placementManagerDelta for placement manager
        if (event.isAltDown) {
            val topLeft = networkPanel.selectionManager.filterSelectedModels<LocatableModel>().topLeftLocation
            val pm = networkPanel.network.placementManager
            networkPanel.canvas.layer.removeChild(placementManagerDelta)
            placementManagerDelta = PPath.createLine(
                topLeft.x, topLeft.y,
                pm.previousAnchorPoint.x,
                pm.previousAnchorPoint.y
            ).apply {
                this.stroke = PPath.DEFAULT_STROKE
                this.strokePaint = Color.red
            }
            networkPanel.canvas.layer.addChild(placementManagerDelta)
        }
    }

    /**
     * A filter that determines whether a given pnode is selectable or not. Bounds are updated as the lasso tool is
     * dragged.
     */
    private class BoundsFilter(val bound: PBounds) : PNodeFilter {

        override fun accept(node: PNode): Boolean {
            val boundsIntersects = when (node) {
                is ScreenElement -> node.isIntersecting(bound)
                else -> node.globalBounds.intersects(bound)
            }
            return node.pickable && boundsIntersects && node !is PLayer && node !is PCamera && node !is SelectionMarquee
        }

        override fun acceptChildrenOf(node: PNode) =
            (node.childrenPickable || node is PCamera || node is PLayer) && node !is SelectionMarquee
    }
}