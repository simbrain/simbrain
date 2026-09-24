/** Builds network menus; type-specific context editors leave the shared selection-editing shortcut intact. */
package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.util.sizeIncluding
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.*
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener

/** Lists the captured targets per model type while keeping Cmd-E on the shared all-types action. */
fun NetworkPanel.createSelectionEditMenu(): JMenu {
    val panel = this
    val menu = JMenu("Edit selected models")
    fun rebuild() {
        val plan = SelectionEditPlan(selectionManager.selectedModels)
        val (label, tooltip) = plan.description
        menu.removeAll()
        menu.text = label?.removeSuffix("...") ?: "Edit selected models"
        menu.toolTipText = tooltip
        menu.isEnabled = plan.groups.isNotEmpty()
        if (!menu.isEnabled) return
        menu.add(panel.createAction(name = "All...", description = tooltip) { event ->
            networkActions.editSelectedModelsAction.actionPerformed(event)
        }.apply {
            putValue(Action.ACCELERATOR_KEY, networkActions.editSelectedModelsAction.getValue(Action.ACCELERATOR_KEY))
        })
        menu.addSeparator()
        plan.groups.forEach { models ->
            val groupPlan = SelectionEditPlan(models)
            menu.add(panel.createAction(
                name = "${models.size} ${modelTypeNoun(models.first(), models.size)}...",
                description = groupPlan.description.second
            ) {
                groupPlan.createDialogs(panel).forEach { it.display() }
            })
        }
    }
    rebuild()
    selectionManager.events.selection.on(Dispatchers.Swing) { _, _ -> rebuild() }
    menu.addMenuListener(object : MenuListener {
        override fun menuSelected(e: MenuEvent) = rebuild()
        override fun menuDeselected(e: MenuEvent) {}
        override fun menuCanceled(e: MenuEvent) {}
    })
    return menu
}

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
            add(createSelectionEditMenu())
            addSeparator()
            add(selectionMenu)
        }
    }

/**
 * Create and return a new Insert menu for this Network panel. The last section holds models built from the
 * current selection, which stay disabled until the needed source and target are selected.
 */
val NetworkPanel.insertMenu
    get() = JMenu("Insert").apply {
        with(networkActions) {
            add(newNeuronAction)
            add(addNeuronsAction)
            addSeparator()
            add(addGroupAction)
            add(addNeuronArrayAction)
            add(addTensorAction)
            add(addClassifierAction)
            add(addActivationSequenceAction)
            add(addLanguageModelAction)
            add(addTinyLanguageModelAction)
            // add(addDeepNetAction)
            add(newNetworkMenu)
            addSeparator()
            add(addTextAction)
            addSeparator()
            add(neuronCollectionAction)
            add(createSupervisedModelAction)
            add(createConvolutionalNeuralNetworkAction)
        }
    }

/**
 * Source-then-target connection workflow: mark source neurons, select targets, then pick how to connect them.
 */
val NetworkPanel.connectMenu
    get() = JMenu("Connect").apply {
        with(networkActions) {
            add(setSourceNeurons)
            add(clearSourceNeurons)
            addSeparator()
            add(connectionMenu)
            addSeparator()
            // TODO: Sync this with "2" and "3" ways of connecting both neuron groups and free neurons
            add(connectWithWeightMatrix)
            add(connectWithSynapseGroup)
            add(connectWithGapJunction)
        }
    }

val NetworkPanel.arrangeMenu
    get() = JMenu("Arrange").apply {
        with(networkActions) {
            add(showLayoutDialogAction)
            addSeparator()
            add(alignMenu)
            add(spaceMenu)
        }
    }

/**
 * Special one-off actions.
 */
val NetworkPanel.actionMenu
    get() = JMenu("Actions").apply {
        with(networkActions) {
            add(randomizeObjectsAction)
            add(showSynapseAdjustmentPanel)
            addSeparator()
            // Alphabetical by action name
            add(fast100)
            add(createLayeredFreeNeurons())
            add(fastGridAction)
            add(fastSparseAction)
            add(decayWeightsAction)
            add(pruneWeightsAction)
            add(randomizePolarityAction)
            add(exportSimbrainWebFormatAction)
            addSeparator()
            add(showNetworkDebugInfoAction)
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
            add(selectAllExcitatorySynapsesAction)
            add(selectAllInhibitorySynapsesAction)
            add(selectAllNeuronsAction)
            add(selectIncomingWeightsAction)
            add(selectOutgoingWeightsAction)
        }
    }

val NetworkPanel.helpMenu
    get() = JMenu("Help").apply {
        add(ShowHelpAction("https://docs.simbrain.net/docs/network/"))
    }

/** Canvas popup; its insert section is taken from [insertMenu] so the two stay in sync. */
fun NetworkPanel.creatContextMenu() = JPopupMenu().apply {
    with(networkActions) {
        insertMenu.menuComponents.forEach { add(it) }
        addSeparator()
        clipboardActions.forEach { add(it) }
        addSeparator()
        add(connectMenu)
        add(arrangeMenu)
        addSeparator()
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
                    networkPanel.network.events.zoomModeChanged.on(Dispatchers.Swing) {
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
                networkPanel.network.events.freeWeightVisibilityChanged.on(Dispatchers.Swing) {
                    this.state = it
                }
            })
            add(JCheckBoxMenuItem(toggleSynapseSpikingOnlyVisibility).apply {
                this.state = networkPanel.synapseSpikingOnlyVisible
                networkPanel.network.events.synapseSpikingOnlyVisibilityChanged.on(Dispatchers.Swing) {
                    this.state = it
                }
            })
            val shapeCaptionsItem = JCheckBoxMenuItem(toggleShapeCaptions)
            add(shapeCaptionsItem)
            // The preference can also change from the preferences dialog, so sync when the menu opens
            addMenuListener(object : MenuListener {
                override fun menuSelected(e: MenuEvent?) {
                    shapeCaptionsItem.state = NetworkPreferences.showShapeCaptions
                }
                override fun menuDeselected(e: MenuEvent?) {}
                override fun menuCanceled(e: MenuEvent?) {}
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
                description = "Set the properties of selected neurons",
            ) {
                panel.filterSelectedNodeByClass<NeuronNode>().firstOrNull()?.createEditDialog()?.display()
            })
            addSeparator()
            add(connectMenu)
            add(arrangeMenu)
            add(neuronCollectionAction)
            addSeparator()
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
            addSeparator()
            add(showNetworkPropertiesAction)
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
                description = "Set the properties of selected synapses",
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
fun NetworkPanel.createEditNeuronGroupAction(neuronGroup: NeuronCollection, getDialog: () -> org.simbrain.util.StandardDialog?): AbstractAction {
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
