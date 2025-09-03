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
                // Add a popup menu listener to fix mouse button state when menu closes
                menu.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
                    override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) {}
                    
                    override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) {
                        // Fix for Piccolo2D mouse button counting bug when JPopupMenu consumes mouse release events
                        try {
                            val inputManager = networkPanel.canvas.root.defaultInputManager
                            val buttonsField = inputManager.javaClass.getDeclaredField("buttonsPressed")
                            buttonsField.isAccessible = true
                            val currentCount = buttonsField.getInt(inputManager)
                            
                            // Only fix if we have exactly 1 unmatched button (the right mouse button)
                            if (currentCount == 1) {
                                // Create a synthetic mouse release event for the right button
                                val mouseEvent = java.awt.event.MouseEvent(
                                    networkPanel.canvas,
                                    java.awt.event.MouseEvent.MOUSE_RELEASED,
                                    System.currentTimeMillis(),
                                    java.awt.event.InputEvent.BUTTON3_DOWN_MASK,
                                    0, 0, // x, y coordinates (not important for this fix)
                                    1, // click count
                                    true, // popup trigger
                                    java.awt.event.MouseEvent.BUTTON3
                                )
                                
                                // Dispatch the synthetic event through the canvas
                                networkPanel.canvas.dispatchEvent(mouseEvent)
                            }
                        } catch (ex: Exception) {
                            // Silently ignore reflection errors
                        }
                    }
                    
                    override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) {}
                })
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
