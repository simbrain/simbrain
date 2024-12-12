package org.simbrain.util

import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JSplitPane

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
        setDividerSize(8)
        dividerLocation = width ?: defaultSize
    }

    fun hideDock() {
        dockComponent.isVisible = false
        setDividerSize(0)
        dividerLocation = 0
    }

    fun toggleDock(width: Int? = null) {
        if (dockComponent.isVisible) {
            hideDock()
        } else {
            showDock(width)
        }
    }
}
