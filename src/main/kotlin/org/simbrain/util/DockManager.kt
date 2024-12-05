package org.simbrain.util

import javax.swing.JComponent
import javax.swing.JSplitPane

class DockManager(
    val dock: JComponent,
    val splitter: JSplitPane,
    val defaultSize: Int,
) {
    init {
        splitter.dividerLocation = defaultSize
    }

    fun toggleDock() {
        if (dock.isVisible) {
            dock.isVisible = false
            splitter.dividerLocation = 0
        } else {
            dock.isVisible = true
            splitter.dividerLocation = defaultSize
        }
    }
}
