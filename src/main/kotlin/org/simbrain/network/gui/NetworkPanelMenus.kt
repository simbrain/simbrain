package org.simbrain.network.gui

import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.AbstractAction
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JPopupMenu

val NetworkPanel.editMenu
    get() = JMenu("Edit").apply {
        with(networkActions) {
            add(cutAction)
            add(copyAction)
            add(pasteAction)
            add(deleteAction)
            addSeparator()
            add(clearSourceNeurons)
            add(setSourceNeurons)
            addSeparator()
            add(connectionMenu)
            add(editConnectionStrategy)
            addSeparator()
            add(randomizeObjectsAction)
            add(showSynapseAdjustmentPanel)
            addSeparator()
            // TODO: Sync this with "2" and "3" ways of connecting both neuron groups and free neurons
            add(connectWithWeightMatrix)
            add(connectWithSynapseGroup)
            addSeparator()
            add(showLayoutDialogAction)
            addSeparator()
            add(neuronCollectionAction)
            addSeparator()
            add(alignMenu)
            add(spaceMenu)
            addSeparator()
            add(setNeuronPropertiesAction)
            add(setSynapsePropertiesAction)
            addSeparator()
            add(selectionMenu)
        }
    }

/**
 * Create and return a new Insert menu for this Network panel.
 *
 * @return a new Insert menu for this Network panel
 */
val NetworkPanel.insertMenu
    get() = JMenu("Insert").apply {
        with(networkActions) {
            add(newNeuronAction)
            add(addNeuronsAction)
            addSeparator()
            add(neuronGroupAction)
            add(addNeuronArrayAction)
            add(addDeepNetAction)
            add(addSmileClassifier)
            addSeparator()
            add(newNetworkMenu)
            add(testInputAction)
            add(showWeightMatrixAction)
        }
    }

/**
 * Special one-off actions.
 */
val NetworkPanel.actionMenu
    get() = JMenu("Actions").apply {
        with(networkActions) {
            // Alphabetical by action name
            add(fast100)
            add(fastGridAction)
            add(fastSparseAction)
            add(decayWeightsAction)
            add(pruneWeightsAction)
            add(randomizePolarityAction)
        }
    }

val NetworkPanel.newNetworkMenu
    get() = JMenu("Insert Network").apply {
        networkActions.newNetworkActions.forEach { add(it) }
    }

val NetworkPanel.alignMenu
    get() = JMenu("Align").apply {
        with(networkActions) {
            add(alignHorizontalAction)
            add(alignVerticalAction)
        }
    }

val NetworkPanel.spaceMenu
    get() = JMenu("Space").apply {
        with(networkActions) {
            add(spaceHorizontalAction)
            add(spaceVerticalAction)
        }
    }

val NetworkPanel.selectionMenu
    get() = JMenu("Select").apply {
        with(networkActions) {
            add(selectAllAction)
            add(selectAllWeightsAction)
            add(selectAllNeuronsAction)
            add(selectIncomingWeightsAction)
            add(selectOutgoingWeightsAction)
        }
    }

val NetworkPanel.helpMenu
    get() = JMenu("Help").apply {
        add(ShowHelpAction("Pages/Network.html"))
    }

fun NetworkPanel.creatContextMenu() = JPopupMenu().apply {
    with(networkActions) {

        // Insert actions
        add(newNeuronAction)
        add(addNeuronsAction)
        add(addNeuronArrayAction)
        add(addDeepNetAction)
        add(addSmileClassifier)
        add(newNetworkMenu)
        addSeparator()

        // Clipboard actions
        clipboardActions.forEach { add(it) }
        addSeparator()

        // Connection actions
        add(clearSourceNeurons)
        add(setSourceNeurons)
        addSeparator()

        // Preferences
        add(showNetworkPreferencesAction)
    }
}

val NetworkPanel.viewMenu
    get() = JMenu("View").apply {
        with(networkActions) {
            add(JMenu("Toolbars").apply {
                add(showMainToolBarAction.toMenuItem().apply { isSelected = mainToolBar.isVisible })
                add(showEditToolBarAction.toMenuItem().apply { isSelected = editToolBar.isVisible })
            })
            addSeparator()
            add(JCheckBoxMenuItem(showPrioritiesAction).apply { this.state = networkPanel.prioritiesVisible })
            add(JCheckBoxMenuItem(toggleFreeWeightVisibility).apply {
                this.state = networkPanel.freeWeightsVisible
                networkPanel.network.events.freeWeightVisibilityChanged.on {
                    this.state = it
                }
            })
        }
    }

val NetworkPanel.connectionMenu
    get() = JMenu("Connect Neurons").apply {
        with(networkActions) {
            connectionActions.forEach { add(it.toMenuItem()) }
        }
    }

val NetworkPanel.neuronContextMenu
    get() = with(networkActions) {
        JPopupMenu().apply {
            add(cutAction)
            add(copyAction)
            add(pasteAction)
            add(deleteAction)
            addSeparator()
            add(clearSourceNeurons)
            add(setSourceNeurons)
            add(connectionMenu)
            addSeparator()
            add(showLayoutDialogAction)
            addSeparator()
            add(showNetworkPreferencesAction)
            addSeparator()
            if (selectionManager.filterSelectedNodes<NeuronNode>().size > 1) {
                add(alignMenu)
                add(spaceMenu)
                addSeparator()
            }
            add(setNeuronPropertiesAction)
            addSeparator()
            add(JMenu("Select").apply {
                add(selectIncomingWeightsAction)
                add(selectOutgoingWeightsAction)
            })
            addSeparator()
            add(testInputAction)
            add(showWeightMatrixAction)
            if (selectionManager.filterSelectedNodes<NeuronNode>().size == 1) {
                val node = selectionManager.filterSelectedNodes<NeuronNode>()[0]
                addSeparator()
                add(CouplingMenu(node.networkPanel.networkComponent, node.neuron))
            }
        }
    }

val NetworkPanel.synapseContextMenu
    get() = with(networkActions) {
        JPopupMenu().apply {
            add(cutAction)
            add(copyAction)
            add(pasteAction)
            addSeparator()
            add(deleteAction)
            addSeparator()
            add(setSynapsePropertiesAction)
        }
    }

fun NetworkComponent.createCouplingMenu(container: AttributeContainer) = CouplingMenu(this, container)

fun AbstractAction.toMenuItem() = JCheckBoxMenuItem(this)