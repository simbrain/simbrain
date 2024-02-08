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
package org.simbrain.network.gui.nodes

import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.display
import java.awt.event.InputEvent
import javax.swing.JDialog
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/**
 * **ScreenElement** extends a Piccolo node with property change, tool tip,
 * and property dialog, and support. Screen elements are automatically support the primary user interactions in the
 * network panel.
 */
abstract class ScreenElement protected constructor(val networkPanel: NetworkPanel) : PPath.Float() {
    /**
     * Create a new abstract screen element with the specified network panel.
     */
    init {
        addInputEventListener(ContextMenuEventHandler())
        addInputEventListener(PropertyDialogEventHandler())
    }

    /**
     * Returns a reference to the model object this node represents.
     */
    abstract val model: NetworkModel

    /**
     * Return true if this screen element accepts a source [NodeHandle].
     */
    open fun acceptsSourceHandle(): Boolean {
        return false
    }

    /**
     * Return true if this screen element is draggable.
     */
    abstract val isDraggable: Boolean

    /**
     * Return a String to use as tool tip text for this screen element. Return null if this
     * screen element does not have tool tip text.
     */
    open val toolTipText: String?
        get() = null

    /**
     * Return a context menu specific to this screen element or null if none.
     */
    open val contextMenu: JPopupMenu?
        get() = null

    /**
     * Return a property dialog for this screen element, or null if it does not have one.
     */
    open val propertyDialog: JDialog?
        get() = null

    /**
     * Screen element-specific context menu event handler.
     */
    private inner class ContextMenuEventHandler : PBasicInputEventHandler() {
        /**
         * Show the context menu.
         */
        private fun showContextMenu(event: PInputEvent) {
            event.isHandled = true
            if (contextMenu != null) {
                val canvasPosition = event.canvasPosition
                // networkPanel.getPlacementManager().setLastClickedPosition(canvasPosition);
                contextMenu?.show(networkPanel.canvas, canvasPosition.x.toInt(), canvasPosition.y.toInt())
            }
        }

        override fun mousePressed(event: PInputEvent) {
            if (event.isPopupTrigger) {
                showContextMenu(event)
            }
        }

        override fun mouseReleased(event: PInputEvent) {
            if (event.isPopupTrigger) {
                showContextMenu(event)
            }
        }
    }

    /**
     * Property dialog event handler.
     */
    private inner class PropertyDialogEventHandler : PBasicInputEventHandler() {
        init {
            eventFilter = PInputEventFilter(InputEvent.BUTTON1_MASK)
        }

        override fun mouseClicked(event: PInputEvent) {
            if (event.clickCount == 2) {
                event.isHandled = true
                SwingUtilities.invokeLater {
                    propertyDialog?.display()
                }
            }
        }
    }

    /**
     * Returns true if the provided bounds intersect this screen element
     */
    open fun isIntersecting(bound: PBounds?): Boolean {
        return globalBounds.intersects(bound)
    }

    /**
     * Select this element.
     */
    fun select() {
        networkPanel.selectionManager.add(this)
    }
}
