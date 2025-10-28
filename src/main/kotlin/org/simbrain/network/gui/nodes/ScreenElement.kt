package org.simbrain.network.gui.nodes

import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventFilter
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createTooltipTextWithLocation
import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.util.int
import org.simbrain.util.piccolo.firstScreenElement
import java.awt.event.InputEvent
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
        addInputEventListener(object : ToolTipTextUpdater(networkPanel) {
            override fun getToolTipText(): String? {
                return this@ScreenElement.toolTipText
            }
        })
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

    open fun createEditDialog(): StandardDialog? {
        return null
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
        get() = (model as? LocatableModel)?.let { createTooltipTextWithLocation(it) }

    /**
     * Return a context menu specific to this screen element or null if none.
     */
    open val contextMenu: JPopupMenu?
        get() = null

    /**
     * Return a property dialog for this screen element, or null if it does not have one.
     */
    open val propertyDialog: StandardDialog?
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
            val (x, y) = event.canvasPosition.int
            
            contextMenu?.let { menu ->
                // Apply both drag reset and mouse button fixes using the utility
                org.simbrain.network.gui.MouseEventUtils.applyContextMenuFixes(networkPanel, event, menu)
                menu.show(networkPanel.canvas, x, y)
            }
            
            event.pickedNode.firstScreenElement?.let {
                networkPanel.selectionManager.add(it)
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
        return globalFullBounds.intersects(bound)
    }

    /**
     * Select this element.
     */
    fun select() {
        networkPanel.selectionManager.add(this)
    }
}
