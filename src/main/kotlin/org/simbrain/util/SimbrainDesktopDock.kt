package org.simbrain.util

import org.intellij.lang.annotations.MagicConstant
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JSplitPane
import javax.swing.border.EmptyBorder

class SimbrainDesktopDock(
    val mainComponent: JComponent,
    val dockComponent: JComponent,
    orientation: Int,
    val defaultSize: Int,
): JSplitPane(
    orientation,
    if (orientation == VERTICAL_SPLIT) mainComponent else dockComponent,
    if (orientation == HORIZONTAL_SPLIT) mainComponent else dockComponent) {
    init {
        dividerLocation = defaultSize
        border = BorderFactory.createEmptyBorder()
        hideDock() // Default behavior for now is closed docks
    }

    fun showDock(width: Int? = null) {
        dockComponent.isVisible = true
        isVisible = true
        dividerSize = 8
        dividerLocation = width ?: defaultSize
    }

    fun hideDock() {
        dockComponent.isVisible = false
        dividerSize = 0
        dividerLocation = 0
    }

    fun toggleDock() {
        if (dockComponent.isVisible) {
            hideDock()
        } else {
            showDock()
        }
    }
}
