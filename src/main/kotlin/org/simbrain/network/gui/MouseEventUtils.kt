package org.simbrain.network.gui

import org.piccolo2d.event.PInputEvent
import org.simbrain.util.piccolo.applyMouseButtonFix
import javax.swing.JPopupMenu

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
     * Applies both the drag reset and mouse button fix to a popup menu.
     * This is a convenience function that combines both fixes.
     */
    fun applyContextMenuFixes(networkPanel: NetworkPanel, event: PInputEvent, menu: JPopupMenu) {
        resetDragOperations(networkPanel, event)
        menu.applyMouseButtonFix(networkPanel.canvas)
    }
}
