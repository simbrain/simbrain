package org.simbrain.network.gui

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.util.sizeIncluding
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
            add(undoAction())
            add(redoAction())
            add(undoHistoryAction())
            addSeparator()
            add(cutAction)
            add(copyAction)
            add(pasteAction)
            add(duplicateAction)
            addSeparator()
            add(deleteAction)
            addSeparator()
            add(clearSourceNeurons)
            add(setSourceNeurons)
            addSeparator()
            add(connectionMenu)
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
            add(createSupervisedModelAction)
            addSeparator()
            add(neuronCollectionAction)
            addSeparator()
            add(alignMenu)
            add(spaceMenu)
            addSeparator()
            add(editSelectedModelsAction)
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
            add(addGroupAction)
            add(addNeuronArrayAction)
            add(addClassifierAction)
            //add(addActivationSequenceAction)
            //add(addTransformerBlockAction)
            // add(addDeepNetAction)
            add(newNetworkMenu)
            addSeparator()
            add(addTextAction)
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
            add(createLayeredFreeNeurons())
            add(fastGridAction)
            add(fastSparseAction)
            add(decayWeightsAction)
            add(pruneWeightsAction)
            add(randomizePolarityAction)
            add(exportSimbrainWebFormatAction)
        }
    }

val NetworkPanel.newNetworkMenu
    get() = JMenu("Add subnetwork").apply {
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
        add(ShowHelpAction("https://docs.simbrain.net/docs/network/"))
    }

fun NetworkPanel.creatContextMenu() = JPopupMenu().apply {
    with(networkActions) {

        // Insert actions
        add(newNeuronAction)
        add(addNeuronsAction)
        addSeparator()
        add(addGroupAction)
        add(addNeuronArrayAction)
        add(addClassifierAction)
        //add(addActivationSequenceAction)
        //add(addTransformerBlockAction)
        // add(addDeepNetAction)
        add(newNetworkMenu)
        addSeparator()
        add(addTextAction)
        addSeparator()

        // Clipboard actions
        clipboardActions.forEach { add(it) }

        // Connection actions
        addSeparator()
        add(clearSourceNeurons)
        add(setSourceNeurons)
        addSeparator()

        // Preferences
        add(showNetworkPropertiesAction)
    }
}

val NetworkPanel.viewMenu
    get() = JMenu("View").apply {
        with(networkActions) {
            add(JMenu("Zoom").apply {
                add(zoomInAction())
                add(zoomOutAction())
                add(resetZoomAction())
                addSeparator()
                add(JCheckBoxMenuItem(toggleAutoZoom).apply {
                    this.state = networkPanel.autoZoom
                    networkPanel.network.events.zoomModeChanged.on {
                        this.state = it
                    }
                })
            })
            addSeparator()
            add(JMenu("Toolbars").apply {
                add(showMainToolBarAction.toMenuItem().apply { isSelected = mainToolBar.isVisible })
                add(showEditToolBarAction.toMenuItem().apply { isSelected = editToolBar.isVisible })
            })
            addSeparator()
            add(JCheckBoxMenuItem(toggleFreeWeightVisibility).apply {
                this.state = networkPanel.freeWeightsVisible
                networkPanel.network.events.freeWeightVisibilityChanged.on {
                    this.state = it
                }
            })
            add(JCheckBoxMenuItem(toggleSynapseSpikingOnlyVisibility).apply {
                this.state = networkPanel.synapseSpikingOnlyVisible
                networkPanel.network.events.synapseSpikingOnlyVisibilityChanged.on {
                    this.state = it
                }
            })
            addSeparator()
            add(showWeightMatrixAction)
            addSeparator()
            add(showPriorityTableAction)
        }
    }

val NetworkPanel.connectionMenu
    get() = JMenu("Connect neurons").apply {
        with(networkActions) {
            connectionActions.forEach { add(it.toMenuItem()) }
        }
    }

fun NetworkPanel.createNeuronContextMenu(currentNeuron: Neuron? = null): JPopupMenu {
    val panel = this
    return with(networkActions) {
        val selectedNeuronList = selectionManager.filterSelectedModels<Neuron>()
        val count = selectedNeuronList.sizeIncluding(currentNeuron)
        JPopupMenu().apply {
            add(cutAction)
            add(copyAction)
            add(pasteAction)
            add(duplicateAction)
            add(deleteAction)
            addSeparator()
            add(panel.createAction(
                name = "Edit $count ${if (count == 1) "neuron" else "neurons"}...",
                description = "Set the properties of selected neurons (Cmd/Ctrl-E)",
                keyboardShortcut = CmdOrCtrl + 'E',
            ) {
                panel.filterSelectedNodeByClass<NeuronNode>().firstOrNull()?.createEditDialog()?.display()
            })
            addSeparator()
        add(clearSourceNeurons)
        add(setSourceNeurons)
        add(connectionMenu)
        addSeparator()
        add(showLayoutDialogAction)
        addSeparator()
        add(neuronCollectionAction)
        addSeparator()
        add(showNetworkPropertiesAction)
        addSeparator()
        if (selectedNeuronList.size > 1) {
            add(alignMenu)
            add(spaceMenu)
            addSeparator()
        }
        add(JMenu("Select").apply {
            add(selectIncomingWeightsAction)
            add(selectOutgoingWeightsAction)
        })
        if (selectedNeuronList.isNotEmpty()) {
            addSeparator()
            add(selectedNeuronList.createCoupleActivationToTimeSeriesAction())
        }
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
}

fun NetworkPanel.createSynapseContextMenu(currentSynapse: Synapse? = null): JPopupMenu {
    val panel = this
    return with(networkActions) {
        val selectedSynapses = selectionManager.filterSelectedModels<Synapse>()
        val count = selectedSynapses.sizeIncluding(currentSynapse)
        JPopupMenu().apply {
            add(cutAction)
            add(copyAction)
            add(pasteAction)
            add(duplicateAction)
            add(deleteAction)
            addSeparator()
            add(panel.createAction(
                name = "Edit $count ${if (count == 1) "synapse" else "synapses"}...",
                description = "Set the properties of selected synapses (Cmd/Ctrl-E)",
                keyboardShortcut = CmdOrCtrl + 'E',
            ) {
                panel.filterSelectedNodeByClass<SynapseNode>().firstOrNull()?.createEditDialog()?.display()
            })
            if (selectedSynapses.isNotEmpty()) {
                addSeparator()
                add(selectedSynapses.createCoupleWeightToTimeSeriesAction())
            }
            if (selectionManager.filterSelectedNodes<SynapseNode>().size == 1) {
                val node = selectionManager.filterSelectedNodes<SynapseNode>()[0]
                addSeparator()
                add(CouplingMenu(networkComponent, node.synapse))
            }
        }
    }
}

fun NetworkComponent.createCouplingMenu(container: AttributeContainer) = CouplingMenu(this, container)

fun AbstractAction.toMenuItem() = JCheckBoxMenuItem(this)

/**
 * Helper function for Java classes to create an edit action for a neuron group.
 * Uses the displayName of the group in the action name and handles dialog display.
 */
fun NetworkPanel.createEditNeuronGroupAction(neuronGroup: AbstractNeuronCollection, getDialog: () -> org.simbrain.util.StandardDialog?): AbstractAction {
    return createAction(name = "Edit ${neuronGroup.displayName}...") {
        getDialog()?.apply {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}

/**
 * Helper function for Java classes to create an edit action for a neuron collection.
 * Uses the displayName of the collection in the action name and handles dialog display.
 */
fun NetworkPanel.createEditNeuronCollectionAction(neuronCollection: NeuronCollection, getDialog: () -> org.simbrain.util.StandardDialog?): AbstractAction {
    return createAction(name = "Edit ${neuronCollection.displayName}...") {
        getDialog()?.apply {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}
