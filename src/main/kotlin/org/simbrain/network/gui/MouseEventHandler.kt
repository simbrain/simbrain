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

import org.apache.commons.lang3.SystemUtils
import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.extras.nodes.PStyledText
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.minus
import org.simbrain.util.piccolo.SelectionMarquee
import org.simbrain.util.piccolo.firstScreenElement
import org.simbrain.util.piccolo.isDoubleClick
import org.simbrain.util.rectangle
import java.awt.event.InputEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

class MouseEventHandler(val networkPanel: NetworkPanel) : PDragSequenceEventHandler() {

    private enum class Mode { SELECTION, PAN, DRAG }

    private var mode = Mode.DRAG

    private var priorSelection = setOf<ScreenElement>()

    private lateinit var marqueeStartPosition: Point2D

    private lateinit var marqueeEndPosition: Point2D

    private val selectionMarquee by lazy {
        with(marqueeStartPosition) { SelectionMarquee(x.toFloat(), y.toFloat()) }.also {
            networkPanel.canvas.layer.addChild(it)
            it.visible = false
        }
    }

    override fun startDrag(event: PInputEvent) {

        super.startDrag(event)

        val pickedNode: PNode? = event.pickedNode

        pickedNode?.firstScreenElement?.let { pickedScreenElement ->
            mode = Mode.DRAG
            if (pickedScreenElement !in networkPanel.selectedNodes) {
                if (event.isShiftDown) {
                    networkPanel.toggleSelection(pickedScreenElement)
                } else {
                    networkPanel.setSelection(listOf(pickedScreenElement))
                }
            }
        }

        priorSelection = networkPanel.selectedNodes.toMutableSet()
        marqueeStartPosition = event.position
        marqueeEndPosition = event.position
        selectionMarquee.reset()

        when {
            event.isPanKeyDown -> mode = Mode.PAN
            pickedNode is PCamera -> {
                networkPanel.placementManager.lastClickedLocation = event.position
                if (!event.isShiftDown) networkPanel.clearSelection()
                mode = Mode.SELECTION
            }
            pickedNode is PStyledText && event.isDoubleClick -> {
                networkPanel.clearSelection()
                networkPanel.textHandle.startEditing(event, pickedNode)
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
            priorSelection = setOf()
        }
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

        networkPanel.setSelection(finalSelection)

    }

    private fun dragItems(event: PInputEvent) {
        val delta = event.position - marqueeEndPosition
        networkPanel.selectedNodes.filter { it.isDraggable }
                .forEach { it.offset(delta.x, delta.y) }
    }

    private val PInputEvent.isPanKeyDown get() = if (SystemUtils.IS_OS_MAC) isMetaDown else isControlDown

    init {
        eventFilter = object : PInputEventFilter(InputEvent.BUTTON1_MASK) {
            override fun acceptsEvent(event: PInputEvent, type: Int): Boolean {
                val editMode = networkPanel.editMode
                return if (editMode.isSelection && super.acceptsEvent(event, type)) {
                    networkPanel.textHandle.stopEditing()
                    true
                } else {
                    false
                }
            }
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