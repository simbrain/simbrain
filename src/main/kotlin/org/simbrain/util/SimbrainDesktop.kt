package org.simbrain.util

import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Point
import java.awt.Rectangle

data class Placement(var location: Point? = null, var size: Point? = null)

fun SimbrainDesktop.place(workspaceComponent: WorkspaceComponent, placement: Placement.() -> Unit) {
    val (location, size) = Placement().apply(placement)
    val desktopComponent = getDesktopComponent(workspaceComponent)
    val bounds = desktopComponent.parentFrame.bounds
    desktopComponent.parentFrame.bounds = Rectangle(
        location?.x ?: bounds.x,
        location?.y ?: bounds.y,
        size?.x ?: bounds.width,
        size?.y ?: bounds.height
    )
}