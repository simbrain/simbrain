package org.simbrain.workspace.gui

import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.Workspace
import javax.swing.JMenuItem

/**
 * A menu item corresponding to a potential coupling. When the menuitem is
 * invoked, a coupling is created (See ActionPerformed in CouplingMenuItem.java).
 */
data class CouplingMenuItem(
        val workspace: Workspace,
        val description: String,
        val producer: Producer,
        val consumer: Consumer
) {
    fun create() = CouplingJMenuItem(this)
}

class CouplingJMenuItem(private val menuItem: CouplingMenuItem) : JMenuItem(menuItem.description) {
    init {
        icon = null
        addActionListener {
            menuItem.workspace.couplingManager.createCoupling(menuItem.producer, menuItem.consumer)
            isSelected = true
        }
    }
}