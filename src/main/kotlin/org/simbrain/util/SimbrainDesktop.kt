package org.simbrain.util

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
    val (location, width, height) = Placement().apply(placement)
    val desktopComponent = getDesktopComponent(workspaceComponent)
    val bounds = desktopComponent.parentFrame.bounds
    desktopComponent.parentFrame.bounds = Rectangle(
        location?.x ?: bounds.x,
        location?.y ?: bounds.y,
        width ?: bounds.width,
        height ?: bounds.height
    )
}