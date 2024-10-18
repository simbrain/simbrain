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
package org.simbrain.world.odorworld.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PLayer
import org.piccolo2d.PNode
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PNodeFilter
import org.simbrain.util.*
import org.simbrain.util.piccolo.SelectionMarquee
import org.simbrain.util.piccolo.parents
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.showTilePicker
import java.awt.event.InputEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/**
 * Handle simbrain drag events, which pan the canvas, create lassos for
 * selection. handles selection an, toggle selection, drags objects as
 * appropriate, updates relevant graphics parameters like "last clicked
 * position".
 *
 *
 * Coding this properly requires tracking picked nodes and their parents fairly
 * closely. To see the scene graph hierarchy for debugging this use ctrl-c while
 * a network panel is open.
 *
 * @author Michael Heuer
 * @author Jeff Yoshimi
 */
class WorldMouseHandler(
    private val odorWorldPanel: OdorWorldPanel,
    private val world: OdorWorld
) : PDragSequenceEventHandler() {
    // TODO: Factor out common features and move to piccolo utility
    /**
     * Selection marquee.
     */
    private var marquee: SelectionMarquee? = null

    /**
     * Picked node, if any, at the beginning of this drag sequence.
     */
    private var pickedNode: PNode? = null

    /**
     * Marquee selection start position.
     */
    private var marqueeStartPosition: Point2D? = null

    /**
     * Bounds filter.
     */
    private val boundsFilter: BoundsFilter

    /**
     * Prior selection, if any. Required for shift-lasso selection.
     */
    private val priorSelection = LinkedHashSet<PNode>()

    private val selectedEntityLocations = HashMap<OdorWorldEntity, Point2D>()


    /**
     * Create a new selection event handler.
     *
     * @param odorWorldPanel parent panel
     */
    init {
        boundsFilter = BoundsFilter()
        eventFilter = SelectionEventFilter()
    }

    override fun mousePressed(mouseEvent: PInputEvent) {
        super.mousePressed(mouseEvent)

        if (world == null) {
            return
        }

        // Set last clicked position, used in many areas for "placement" of
        // objects in the last clicked position on screen.
        world.lastClickedPosition = mouseEvent.position
    }

    override fun mouseClicked(event: PInputEvent) {
        // System.out.println("In drag event handler mouse clicked:" + event);

        super.mouseClicked(event)

        // Set picked node
        val node = event.path.pickedNode

        // System.out.println("Mouse clicked / Picked node: " + node);

        // Double click on entities to edit them
        if (event.clickCount != 1) {
            (node.parent as? EntityNode).let {
                if (it != null) {
                    odorWorldPanel.editSelectedEntities()
                } else {
                    // TODO: On right click, show sub-menu with layers, then show dialog below
                    val tileMap = odorWorldPanel.world.tileMap
                    showTilePicker(tileMap.tileSets, event.getCurrentTileId()) { tileId: Int? ->
                        val p = tileMap.pixelToGridCoordinate(world.lastClickedPosition)
                        odorWorldPanel.world.tileMap.setTile(p.x, p.y, tileId!!)
                    }
                }
            }
            return
        }

        // Clicking in empty parts of the canvas removes green selections.
        if (node is PCamera) {
            if (!event.isShiftDown) {
                odorWorldPanel.clearSelection()
            }
        }
    }

    override fun startDrag(event: PInputEvent) {
        super.startDrag(event)

        marqueeStartPosition = event.position
        val (x0, y0) = event.position

        // Set the initially picked node
        val pickedNode = event.path.pickedNode
        this.pickedNode = pickedNode

        // Cases where nothing was clicked on
        if (noObjectWasClickedOn()) {
            if (event.isShiftDown) {
                priorSelection.clear()
                priorSelection.addAll(odorWorldPanel.selection)
            } else {
                // Don't clear selection when panning screen
                if (!event.isMetaDown) {
                    odorWorldPanel.clearSelection()
                }
            }

            // Create a new selection marquee at the mouse position
            marquee = SelectionMarquee(x0.toFloat(), y0.toFloat())

            // Add marquee as child of the network panel's layer
            odorWorldPanel.canvas.layer.addChild(marquee)
            return
        }

        // Either start dragging selected node(s) or toggle selection (if shift
        // is pressed).
        if (odorWorldPanel.isSelected(pickedNode)) {
            if (event.isShiftDown) {
                odorWorldPanel.toggleSelection(pickedNode)
            }
        } else {
            if (event.isShiftDown) {
                odorWorldPanel.toggleSelection(pickedNode)
            } else {
                odorWorldPanel.selection = mutableSetOf(pickedNode)
            }
        }

        odorWorldPanel.selectedEntityNodes.forEach {
            selectedEntityLocations[it.entity] = it.entity.location
        }
    }

    override fun drag(event: PInputEvent) {
        super.drag(event)

        // If the command/control button is down,
        // pan the canvas.
        if (event.isMetaDown) {
            pan(event)
            return
        }

        // The case where nothing was clicked on initially. So draw the lasso
        // and select things.
        if (noObjectWasClickedOn()) {
            // Select lassoed nodes
            val position = event.position
            val rect = PBounds()
            rect.add(marqueeStartPosition)
            rect.add(position)
            marquee!!.globalToLocal(rect)
            marquee!!.reset() // todo: better way?
            marquee!!.append(
                Rectangle2D.Float(
                    rect.getX().toFloat(),
                    rect.getY().toFloat(),
                    rect.getWidth().toFloat(),
                    rect.getHeight().toFloat()
                ), false
            )
            boundsFilter.setBounds(rect)
            val highlightedNodes = odorWorldPanel.canvas.layer.root
                .getAllNodes(boundsFilter, null)
                .filterIsInstance<PNode>()
                .toMutableSet()
            // Toggle things if shift is being pressed
            if (event.isShiftDown) {
                val selection = (priorSelection + highlightedNodes).toMutableSet()
                selection.removeAll(priorSelection intersect  highlightedNodes)
                odorWorldPanel.selection = selection
            } else {
                odorWorldPanel.selection = highlightedNodes
            }
            return
        }

        // Where is the drag in relation to the initially clicked on object
        val pickedNodeLocation = (pickedNode?.parents?.firstOrNull { it is EntityNode } as? EntityNode)?.entity?.location ?: return

        val delta = event.position - pickedNodeLocation

        val newLocations = selectedEntityLocations.map { (entity, location) ->
            entity to location + delta
        }

        val canMoveInX = newLocations.all { (entity, location) -> world.isInXBounds(location) }
        val canMoveInY = newLocations.all { (entity, location) -> world.isInYBounds(location) }

        newLocations.forEach { (entity, location) ->
            if (canMoveInX) {
                selectedEntityLocations[entity]?.let { it.setLocation(location.x, it.y) }
            }
            if (canMoveInY) {
                selectedEntityLocations[entity]?.let { it.setLocation(it.x, location.y) }
            }
        }


        // Continue to drag nodes that have already been selected
        for (node in odorWorldPanel.selectedEntityNodes) {
            node.entity.location = selectedEntityLocations[node.entity]!!
        }
    }

    override fun endDrag(event: PInputEvent) {
        super.endDrag(event)

        // Nothing was being dragged
        if (noObjectWasClickedOn()) {
            // End lasso selection
            marquee!!.removeFromParent()
            marquee = null
            marqueeStartPosition = null
            return
        }

        // End drag selected node(s)
        pickedNode = null

        selectedEntityLocations.clear()
        priorSelection.clear()
        odorWorldPanel.repaint()
    }

    /**
     * Encapsulate logic for determining the case where no object (neuron node,
     * synpase node, etc) was clicked on at the beginning of this drag
     * sequence.
     *
     * @return true if no object was clicked on, false otherwise.
     */
    private fun noObjectWasClickedOn(): Boolean {
        val pickedNodeNull = (pickedNode == null)
        val cameraPicked = (pickedNode is PCamera)
        return (pickedNodeNull || cameraPicked)
    }

    fun PInputEvent.getCurrentTileId(): Int? {
        if (isMouseEvent) {
            val layer = odorWorldPanel.world.selectedLayer
            val tileMap = odorWorldPanel.world.tileMap
            val (x, y) = tileMap.pixelToGridCoordinate(position)
            return layer[x, y]
        }
        return null
    }

    /**
     * A filter that determines whether a given pnode is selectable or not.
     * Bounds are updated as the lasso tool is dragged.
     */
    internal inner class BoundsFilter : PNodeFilter {
        /**
         * Bounds.
         */
        private var bounds: PBounds? = null

        /**
         * Set the bounds for this bounds filter to `bounds`.
         *
         * @param bounds bounds for this bounds filter
         */
        fun setBounds(bounds: PBounds?) {
            this.bounds = bounds
        }

        /**
         * @param node
         * @return
         * @see PNodeFilter
         */
        override fun accept(node: PNode): Boolean {
            val isPickable = node.pickable
            val boundsIntersects = node.globalBounds.intersects(bounds)
            val isLayer = (node is PLayer)
            val isCamera = (node is PCamera)
            val isMarquee = (marquee == node)

            return (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee)
        }

        /**
         * @param node
         * @return
         * @see PNodeFilter
         */
        override fun acceptChildrenOf(node: PNode): Boolean {
            val areChildrenPickable = node.childrenPickable
            val isCamera = (node is PCamera)
            val isLayer = (node is PLayer)
            val isMarquee = (marquee == node)

            return ((areChildrenPickable || isCamera || isLayer) && !isMarquee)
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when the
     * network panel's edit mode is `EditMode.SELECTION`.
     */
    private inner class SelectionEventFilter: PInputEventFilter(InputEvent.BUTTON1_MASK)

    /**
     * Pans the camera in response to the pan event provided. (From the source
     * code for PanEventHandler. Note that "autopan"--from that class--is not
     * being used. Not sure what is being lost by not using it.)
     *
     * @param event contains details about the drag used to translate the view
     * @author Jesse Grosjean
     */
    private fun pan(event: PInputEvent) {
        val (x, y, w, h) = event.camera.viewBounds
        val dx = event.delta.width
        val dy = event.delta.height
        odorWorldPanel.canvas.setViewBounds(Rectangle2D.Double(x - dx, y - dy, w, h))
    }
}
