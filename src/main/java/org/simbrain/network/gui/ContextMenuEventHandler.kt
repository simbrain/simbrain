package org.simbrain.network.gui

import kotlinx.coroutines.launch
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.simbrain.util.Utils

class ContextMenuEventHandler(private val networkPanel: NetworkPanel) : PBasicInputEventHandler() {

    private fun showContextMenu(event: PInputEvent) {
        networkPanel.launch {
            val contextMenu = networkPanel.creatContextMenu()
            val canvasPosition = event.canvasPosition
            
            // Apply both drag reset and mouse button fixes using the utility
            MouseEventUtils.applyContextMenuFixes(networkPanel, event, contextMenu)
            
            contextMenu.show(networkPanel.canvas, canvasPosition.x.toInt(), canvasPosition.y.toInt())
            networkPanel.canvas.camera.localToView(canvasPosition)
            // Set this position so that new objects are added here
            networkPanel.network.placementManager.lastClickedLocation = canvasPosition
        }
    }

    override fun mousePressed(event: PInputEvent) {
        super.mousePressed(event)
        if (event.isPopupTrigger) {
            showContextMenu(event)
        }
    }

    override fun mouseReleased(event: PInputEvent) {
        super.mouseReleased(event)
        if (!Utils.isMacOSX()) {
            if (event.isPopupTrigger) {
                showContextMenu(event)
            }
        }
    }

}