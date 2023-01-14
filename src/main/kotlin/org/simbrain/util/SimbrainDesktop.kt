package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Point
import java.awt.Rectangle

/**
 * Utility to make it easy to set location of a [SimbrainDesktop] component.
 */
data class Placement(
    var location: Point? = null,
    var width: Int? = null,
    var height: Int? = null
)

fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, placement: Placement.() -> Unit) {
    workspace.launch {
        val (location, width, height) = Placement().apply(placement)
        val desktopComponent = withContext(Dispatchers.Main) {
            getDesktopComponent(workspaceComponent)
        }
        val bounds = desktopComponent.parentFrame.bounds
        desktopComponent.parentFrame.bounds = Rectangle(
            location?.x ?: bounds.x,
            location?.y ?: bounds.y,
            width ?: bounds.width,
            height ?: bounds.height
        )
    }
}

fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, x: Int, y: Int, width: Int, height: Int) {
    val desktopComponent = getDesktopComponent(workspaceComponent)
    desktopComponent.parentFrame.bounds = Rectangle(x, y, width, height)
}