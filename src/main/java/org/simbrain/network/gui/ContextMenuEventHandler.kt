package org.simbrain.network.gui

import kotlinx.coroutines.launch
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent

class ContextMenuEventHandler(private val networkPanel: NetworkPanel) : PBasicInputEventHandler() {

    private fun showContextMenu(event: PInputEvent) {
        networkPanel.launch {
            val contextMenu = networkPanel.creatContextMenu()
            val canvasPosition = event.canvasPosition
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

}