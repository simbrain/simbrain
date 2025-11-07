package org.simbrain.network.gui.nodes

import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.*
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.displayInDialog
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu


class SynapseGroupInteractionBox(
    networkPanel: NetworkPanel,
    val synapseGroup: SynapseGroup,
    val synapseGroupNode: SynapseGroupNode
) : InteractionBox(networkPanel) {

    override val propertyDialog: StandardDialog
        get() = synapseGroupNode.getDialog()

    override val model: SynapseGroup
        get() = synapseGroup

    override val isDraggable: Boolean = false

    override val contextMenu: JPopupMenu
        get() {

            val menu = JPopupMenu()

            menu.add(networkPanel.createAction(
                name = "Rename synapse group..."
            ) {
                val newName = JOptionPane.showInputDialog("Name:", synapseGroup.label)
                if (newName != null) {
                    synapseGroup.label = newName
                }
            })
            menu.add(networkPanel.createAction(
                iconPath = "menu_icons/minus.png",
                name = "Delete synapse group"
            ) {
                synapseGroup.delete()
            })
            
            menu.addSeparator()
            
            // Edit
            menu.add(networkPanel.createAction(name = "Edit ${synapseGroup.displayName}...") {
                synapseGroupNode.getDialog().makeVisible()
            })

            // Selection stuff
            menu.addSeparator()
            menu.add(networkPanel.createAction(name = "Select synapses") {
                synapseGroup.synapses.forEach { it.select() }
            })
            menu.add(networkPanel.createAction(name = "Select incoming neurons") {
                synapseGroup.source.neuronList.forEach { it.select() }
            })
            menu.add(networkPanel.createAction(name = "Select outgoing neurons") {
                synapseGroup.target.neuronList.forEach { it.select() }
            })

            // Weight adjustment
            menu.addSeparator()
            menu.add(
                JMenuItem(networkPanel.createAction(
                    iconPath = "menu_icons/grid.png",
                    name = "Show weight matrix...",
                ) {
                    WeightMatrixViewer(
                        synapseGroup.source.neuronList,
                        synapseGroup.target.neuronList
                    ).displayInDialog {
                        commitChanges()
                    }
                })
            )

            // Freezing actions
            menu.addSeparator()
            menu.add(networkPanel.createAction(
                name = "Clamp synapses",
                description = "Clamp all synapses in this group (prevent learning)"
            )
            {
                synapseGroup.synapses.forEach { it.clamped = true }
            })
            menu.add(networkPanel.createAction(
                name = "Unclamp synapses",
                description = "Unclamp all synapses in this group (allow learning)"
            )
            {
                synapseGroup.synapses.forEach { it.clamped = false }
            })

            // Synapse Enabling actions
            menu.addSeparator()
            menu.add(networkPanel.createAction(
                name = "Disable synapses",
                description = "Enable all synapses in this group (allow activation to pass through synapses)"
            )
            {
                synapseGroup.synapses.forEach { it.isEnabled = false }
            })
            menu.add(networkPanel.createAction(
                name = "Enable synapses",
                description = "Enable all synapses in this group (allow activation to pass through synapses)"
            )
            {
                synapseGroup.synapses.forEach { it.isEnabled = true }
            })

            // Synapse Visibility
            menu.addSeparator()
            menu.add(networkPanel.networkActions.createSynapseGroupVisibilityAction())

            // Coupling menu
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(synapseGroup)
            menu.addSeparator()
            menu.add(couplingMenu)
            return menu
        }

    override val toolTipText get() = createTooltipText(synapseGroup) { synapseGroup.toString() }

}