package org.simbrain.network.gui

import org.piccolo2d.event.PInputEvent
import javax.swing.JPopupMenu
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * Utility functions for handling mouse event issues in Piccolo2D, particularly
 * around context menus and drag operations.
 */
object MouseEventUtils {
    
    /**
     * Resets any ongoing drag operations when showing a context menu.
     * This is a critical fix to prevent drag state issues in Piccolo2D.
     */
    fun resetDragOperations(networkPanel: NetworkPanel, event: PInputEvent) {
        networkPanel.canvas.inputEventListeners.filterIsInstance<MouseEventHandler>()
            .forEach { handler ->
                try {
                    // Force end any ongoing drag by dispatching a synthetic mouse release event
                    val syntheticMouseEvent = java.awt.event.MouseEvent(
                        networkPanel.canvas,
                        java.awt.event.MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(),
                        0, // no modifiers
                        event.canvasPosition.x.toInt(),
                        event.canvasPosition.y.toInt(),
                        1, // click count
                        false, // not popup trigger
                        event.button
                    )
                    
                    // Dispatch the event through the canvas to properly trigger endDrag
                    if (handler.isDragging) {
                        networkPanel.canvas.dispatchEvent(syntheticMouseEvent)
                    }
                } catch (ex: Exception) {
                    // Silently ignore errors in drag state reset
                }
            }
    }
    
    /**
     * Creates a PopupMenuListener that fixes the Piccolo2D mouse button counting bug
     * when JPopupMenu consumes mouse release events.
     */
    fun createMouseButtonFixListener(networkPanel: NetworkPanel): PopupMenuListener {
        return object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {}
            
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
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
                            false, // popup trigger = false to prevent feedback loop
                            java.awt.event.MouseEvent.BUTTON3
                        )
                        
                        // Dispatch the synthetic event through the canvas
                        networkPanel.canvas.dispatchEvent(mouseEvent)
                    }
                } catch (ex: Exception) {
                    // Silently ignore reflection errors
                }
            }
            
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        }
    }
    
    /**
     * Applies both the drag reset and mouse button fix to a popup menu.
     * This is a convenience function that combines both fixes.
     */
    fun applyContextMenuFixes(networkPanel: NetworkPanel, event: PInputEvent, menu: JPopupMenu) {
        resetDragOperations(networkPanel, event)
        menu.addPopupMenuListener(createMouseButtonFixListener(networkPanel))
    }
}
