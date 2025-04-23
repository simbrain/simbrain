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
import org.piccolo2d.util.PNodeFilter
import org.simbrain.network.core.Neuron
import org.simbrain.network.gui.MouseEventHandler.MouseCursor
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandRadius
import org.simbrain.network.gui.nodes.NeuronNode
import java.awt.event.InputEvent
import java.awt.geom.Ellipse2D

/**
 * Wand event handler. Change activation when dragging over neurons.
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

    private var diffsForUndo = mutableMapOf<Neuron, Double>()

    override fun mousePressed(event: PInputEvent) {
        super.mousePressed(event)
        diffsForUndo = mutableMapOf()

        val node = event.path.pickedNode
        if (node is NeuronNode) {
            modifyNode(node)
        }

        // networkPanel.setLastClickedPosition(event.getPosition());
        // if (event.getPath().getPickedNode() instanceof PCamera) {
        //    networkPanel.setBeginPosition(event.getPosition());
        //}
    }

    override fun mouseReleased(event: PInputEvent?) {
        super.mouseReleased(event)
        if (diffsForUndo.isNotEmpty()) {
            val diffs = diffsForUndo
            val redos = diffs.map { it.key }.associateWith { it.activation }
            networkPanel.undoManager.addUndoableAction(
                description = "Wand Actions on ${diffs.count()} Neurons",
                undo = {
                    diffs.forEach { (neuron, previousActivation) ->
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

    override fun startDrag(event: PInputEvent?) {
        super.startDrag(event)
    }

    override fun drag(event: PInputEvent) {
        super.drag(event)

        val radius = wandRadius

        // Create elliptical bounds
        val position = event.getPosition()
        val ellipse = Ellipse2D.Double(
            position.x - radius / 2,
            position.y - radius / 2,
            radius.toDouble(),
            radius.toDouble()
        )
        boundsFilter.setEllipse(ellipse)

        val highlightedNodes = networkPanel.canvas.layer.getRoot().getAllNodes(boundsFilter, null)

        // Auto-highlighter mode
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
     * The wand "action" goes here.
     *
     * @param node node to act on
     */
    private fun modifyNode(node: NeuronNode) {
        val neuron = node.neuron
        diffsForUndo.putIfAbsent(neuron, neuron.activation)
        neuron.activation = neuron.upperBound
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

            if (mouseCursor === MouseCursor.Wand && super.acceptsEvent(event, type)) {
                return true
            } else {
                return false
            }
        }
    }
}
