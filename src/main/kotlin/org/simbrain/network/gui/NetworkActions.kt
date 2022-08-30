package org.simbrain.network.gui

import org.simbrain.network.connections.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.core.decayStrengthBasedOnLength
import org.simbrain.network.gui.actions.ConditionallyEnabledAction
import org.simbrain.network.gui.actions.ShowDebugAction
import org.simbrain.network.gui.actions.ShowLayoutDialogAction
import org.simbrain.network.gui.actions.TestInputAction
import org.simbrain.network.gui.actions.connection.ClearSourceNeurons
import org.simbrain.network.gui.actions.connection.SetSourceNeurons
import org.simbrain.network.gui.actions.dl4j.AddNeuronArrayAction
import org.simbrain.network.gui.actions.edit.*
import org.simbrain.network.gui.actions.modelgroups.AddGroupAction
import org.simbrain.network.gui.actions.modelgroups.NeuronCollectionAction
import org.simbrain.network.gui.actions.network.*
import org.simbrain.network.gui.actions.neuron.AddNeuronsAction
import org.simbrain.network.gui.actions.neuron.NewNeuronAction
import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction
import org.simbrain.network.gui.actions.neuron.ShowPrioritiesAction
import org.simbrain.network.gui.actions.selection.*
import org.simbrain.network.gui.actions.synapse.SetSynapsePropertiesAction
import org.simbrain.network.gui.actions.synapse.ShowWeightMatrixAction
import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction
import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction
import org.simbrain.network.gui.actions.toolbar.ShowRunToolBarAction
import org.simbrain.network.gui.dialogs.createSynapseAdjustmentPanel
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog
import org.simbrain.network.gui.dialogs.network.*
import org.simbrain.network.gui.dialogs.showDeepNetCreationDialog
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.stats.ProbabilityDistribution
import javax.swing.AbstractAction
import javax.swing.JCheckBoxMenuItem
import javax.swing.JOptionPane

class NetworkActions(val networkPanel: NetworkPanel) {

    // TODO: Convert these to inline actions as below.
    val addNeuronArrayAction = AddNeuronArrayAction(networkPanel)
    val addNeuronsAction = AddNeuronsAction(networkPanel)
    val alignHorizontalAction = AlignHorizontalAction(networkPanel)
    val alignVerticalAction = AlignVerticalAction(networkPanel)
    val clearNodeActivationsAction = ClearSelectedObjects(networkPanel)
    val clearSourceNeurons = ClearSourceNeurons(networkPanel)
    val copyAction = CopyAction(networkPanel)
    val cutAction = CutAction(networkPanel)
    val deleteAction = DeleteAction(networkPanel)
    val iterateNetworkAction = IterateNetworkAction(networkPanel)
    val neuronCollectionAction = NeuronCollectionAction(networkPanel)
    val newNeuronAction = NewNeuronAction(networkPanel)
    val pasteAction = PasteAction(networkPanel)
    val randomizeObjectsAction = RandomizeObjectsAction(networkPanel)
    val runNetworkAction = RunNetworkAction(networkPanel)
    val selectAllAction = SelectAllAction(networkPanel)
    val selectAllNeuronsAction = SelectAllNeuronsAction(networkPanel)
    val selectAllWeightsAction = SelectAllWeightsAction(networkPanel)
    val selectIncomingWeightsAction = SelectIncomingWeightsAction(networkPanel)
    val selectOutgoingWeightsAction = SelectOutgoingWeightsAction(networkPanel)
    val selectionEditModeAction = SelectionEditModeAction(networkPanel)
    val setNeuronPropertiesAction = SetNeuronPropertiesAction(networkPanel)
    val setSourceNeurons = SetSourceNeurons(networkPanel)
    val setSynapsePropertiesAction = SetSynapsePropertiesAction(networkPanel)

    val showDebugAction = ShowDebugAction(networkPanel)
    val showEditToolBarAction = ShowEditToolBarAction(networkPanel)
    val showLayoutDialogAction = ShowLayoutDialogAction(networkPanel)
    val showMainToolBarAction = ShowMainToolBarAction(networkPanel)
    val showNetworkPreferencesAction = ShowNetworkPreferencesAction(networkPanel)
    val showNetworkUpdaterDialog = ShowNetworkUpdaterDialog(networkPanel)
    val showPrioritiesAction = ShowPrioritiesAction(networkPanel)
    val showRunToolBarAction = ShowRunToolBarAction(networkPanel)
    val showWeightMatrixAction = ShowWeightMatrixAction(networkPanel)
    val spaceHorizontalAction = SpaceHorizontalAction(networkPanel)
    val spaceVerticalAction = SpaceVerticalAction(networkPanel)
    val stopNetworkAction = StopNetworkAction(networkPanel)
    val testInputAction = TestInputAction(networkPanel)
    val textEditModeAction = TextEditModeAction(networkPanel)
    val wandEditModeAction = WandEditModeAction(networkPanel)
    val zoomToFitPageAction = ZoomToFitPageAction(networkPanel)

    val addDeepNetAction = if (Utils.isM1Mac()) {
        networkPanel.createAction(
            name = "Add Deep Network...",
            description = "Deep Network is not currently supported on M1 Macs",
            keyCombo = CmdOrCtrl + Shift + 'D'
        ) {
            JOptionPane.showConfirmDialog(
                null,
                "Deep Network / TensorFlow for Java is not currently supported on M1 Macs."
            )
        }.also { it.isEnabled = false }
    } else {
        networkPanel.createAction(
            name = "Add Deep Network...",
            description = "Create a new deep network",
            keyCombo = CmdOrCtrl + Shift + 'D'
        ) {
            showDeepNetCreationDialog()
        }
    }

    /**
     * Should be called from a combo box menu item
     */
    val toggleFreeWeightVisibility = networkPanel.createAction(
        name = "Toggle Weight Visibility",
        description = "Toggle visibilty of free weights",
        keyCombo = KeyCombination('5')
    ) { event ->
        event.source.let {
            if (it is JCheckBoxMenuItem) {
                freeWeightsVisible = it.state
            } else {
                freeWeightsVisible = !freeWeightsVisible
            }
        }
    }

    val addSmileClassifier = networkPanel.createAction(
        name = "Add Smile Classifier...",
        description = "Create a new Smile classifier",
        keyCombo = CmdOrCtrl + Shift + 'S'
    ) {
        showClassifierCreationDialog()
    }

    val connectSelectedModels = networkPanel.createAction(
        name = "Connect selected objects...",
        description = "Creates synapse, weight matrix, etc. between selected source and target entities",
    ) {
        connectSelectedModelsDefault()
    }

    val connectWithWeightMatrix = networkPanel.createAction(
        name = "Connect selected objects with weight matrix",
    ) {
        // This will automatically connect arrays (which is all this action should be called for) with weight matrices
        connectSelectedModelsDefault()
    }

    val connectWithSynapseGroup = networkPanel.createAction(
        name = "Connect selected neuron groups with synapse group",
    ) {
        selectionManager.connectNeuronGroups()
    }

    val neuronGroupAction = addGroupAction("Add Neuron Group...") {
        NeuronGroupDialog(it)
    }

    val clipboardActions
        get() = listOf(copyAction, cutAction, pasteAction)

    val networkControlActions
        get() = listOf(runNetworkAction, stopNetworkAction)

    val networkEditingActions
        get() = listOf(newNeuronAction, deleteAction)

    val networkModeActions
        get() = listOf<AbstractAction>(
            selectionEditModeAction,
            textEditModeAction,
            wandEditModeAction
        )

    val showSynapseAdjustmentPanel = networkPanel.createConditionallyEnabledAction(
        iconPath = "menu_icons/Rand.png",
        name = "Weight randomization dialog...",
        keyCombo = CmdOrCtrl + 'R',
        enablingCondition = ConditionallyEnabledAction.EnablingCondition.SYNAPSES
    ) {
        createSynapseAdjustmentPanel(
            network.getModels<Synapse>().toList(),
            network.weightRandomizer,
            network.excitatoryRandomizer,
            network.inhibitoryRandomizer
        )?.displayInDialog()
    }

    // TODO: Note: the lambda parameter `NetworkPanel` is not used
    private fun addGroupAction(name: String, createDialog: AddGroupAction.(NetworkPanel) -> StandardDialog) =
        AddGroupAction(networkPanel, name, createDialog)

    val newNetworkActions
        get() = listOf(
            addGroupAction("Backprop") { BackpropCreationDialog(networkPanel) },
            addGroupAction("Competitive Network") { CompetitiveCreationDialog(networkPanel) },
            addGroupAction("Feed Forward Network") { FeedForwardCreationDialog(networkPanel) },
            addGroupAction("Hopfield") { HopfieldCreationDialog(networkPanel) },
            // addGroupAction("LMS (Least Mean Squares)") { LMSCreationDialog(networkPanel) },
            addGroupAction("SOM Network") { SOMCreationDialog(networkPanel) },
            // addGroupAction("SRN (Simple Recurrent Network)") { SRNCreationDialog(networkPanel) }
        )

    fun applyConnectionAction(strategy: ConnectionStrategy): AbstractAction {
        return networkPanel.createConditionallyEnabledAction(
            name = "Connect ${strategy.name}...",
            enablingCondition = ConditionallyEnabledAction.EnablingCondition.SOURCE_AND_TARGET_NEURONS
        ) {
            val connectionSelector = ConnectionSelector(strategy)
            ConnectionStrategyPanel(connectionSelector).displayInDialog {
                commitChanges()
                connectionSelector.cs.connectNeurons(
                    network,
                    selectionManager.filterSelectedSourceModels<Neuron>(),
                    selectionManager.filterSelectedModels<Neuron>()
                )
            }
        }
    }

    val connectionActions
        get() = listOf(
            applyConnectionAction(AllToAll()),
            applyConnectionAction(DistanceBased()),
            applyConnectionAction(OneToOne()),
            applyConnectionAction(FixedDegree()),
            applyConnectionAction(RadialGaussian()),
            applyConnectionAction(RadialProbabilistic()),
            applyConnectionAction(Sparse())
        )

    val editConnectionStrategy = networkPanel.createAction(
        name = "Edit connection strategy...",
    ) {
        ConnectionStrategyPanel(network.neuronConnector).displayInDialog {
            commitChanges()
        }
    }

    val synapseGroupVisibilityAction = networkPanel.createAction(
        name = "Toggle visibility of selected synapse groups",
        keyCombo = CmdOrCtrl + 'T'
    ) {
        val sgs = selectionManager.filterSelectedModels<SynapseGroup2>()
        sgs.forEach {
            it.displaySynapses = !it.displaySynapses
        }
    }

    /**
     * Decay selected weights using a [DecayFunction] selected by the user.
     */
    val decayWeightsAction = networkPanel.createConditionallyEnabledAction(
        name = "Decay selected weights based on axon length",
        enablingCondition = ConditionallyEnabledAction.EnablingCondition.SYNAPSES
    ) {
        DecayFunction.DecayFunctionSelector().createDialog {
            selectionManager.filterSelectedModels<Synapse>()
                .decayStrengthBasedOnLength(it.decayFunction)
        }.display()
    }

    /**
     * Prune selected weights. Weights whose absolute value is less then the threshold are removed.
     */
    val pruneWeightsAction = networkPanel.createConditionallyEnabledAction(
        name = "Prune selected weights",
        enablingCondition = ConditionallyEnabledAction.EnablingCondition.SYNAPSES
    ) {
        val threshold: String = JOptionPane.showInputDialog(
            null,
            "Pruning threshold:",
            ".5"
        )
        selectionManager.filterSelectedModels<Synapse>()
            .filter { Math.abs(it.strength) < threshold.toDouble() }
            .forEach { it.delete() }
    }

    /**
     * Randomize the polarity of selected nodes. Note that this will change the polarity of outgoing synapses.
     */
    val randomizePolarityAction = networkPanel.createConditionallyEnabledAction(
        name = "Randomize polarity of selected neurons",
        enablingCondition = ConditionallyEnabledAction.EnablingCondition.NEURONS
    ) {
        // TODO: Indicate the threshold somehow in a prompt
        ProbabilityDistribution.Randomizer().createDialog { dist ->
            selectionManager.filterSelectedModels<Neuron>().forEach { n ->
                if (dist.sampleDouble() > .5) n.polarity = SimbrainConstants.Polarity.EXCITATORY
                else n.polarity = SimbrainConstants.Polarity.INHIBITORY
            }
        }.display()
    }

    /**
     * Quick action to apply a grid layout to selected nodes
     */
    val fastGridAction = networkPanel.createConditionallyEnabledAction(
        name = "Apply grid layout to selected nodes",
        keyCombo = CmdOrCtrl + 'L',
        enablingCondition = ConditionallyEnabledAction.EnablingCondition.NEURONS
    ) {
        GridLayout().layoutNeurons(selectionManager.filterSelectedModels<Neuron>())
        zoomToFitPage()
    }

    /**
     * Quick create 100 nodes
     */
    val fast100 = networkPanel.createAction(
        name = "Add 100 nodes",
    ) {
        List(100) { Neuron(network) }.apply {
            network.addNetworkModels(this)
            GridLayout().layoutNeurons(this)
        }.onEach {
            it.events.fireSelected()
        }
        zoomToFitPage()
    }
}
