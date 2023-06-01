package org.simbrain.network.gui

import kotlinx.coroutines.launch
import org.simbrain.network.connections.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.core.decayStrengthBasedOnLength
import org.simbrain.network.gui.actions.ConditionallyEnabledAction.EnablingCondition
import org.simbrain.network.gui.actions.ShowLayoutDialogAction
import org.simbrain.network.gui.actions.TestInputAction
import org.simbrain.network.gui.actions.connection.ClearSourceNeurons
import org.simbrain.network.gui.actions.connection.SetSourceNeurons
import org.simbrain.network.gui.actions.dl4j.AddNeuronArrayAction
import org.simbrain.network.gui.actions.edit.*
import org.simbrain.network.gui.actions.modelgroups.AddGroupAction
import org.simbrain.network.gui.actions.modelgroups.NeuronCollectionAction
import org.simbrain.network.gui.actions.neuron.AddNeuronsAction
import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction
import org.simbrain.network.gui.actions.neuron.ShowPrioritiesAction
import org.simbrain.network.gui.actions.selection.*
import org.simbrain.network.gui.actions.synapse.SetSynapsePropertiesAction
import org.simbrain.network.gui.actions.synapse.ShowWeightMatrixAction
import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction
import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction
import org.simbrain.network.gui.dialogs.*
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog
import org.simbrain.network.gui.dialogs.network.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.stats.ProbabilityDistribution
import java.awt.event.KeyEvent
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
    val deleteAction = networkPanel.createConditionallyEnabledAction(
        name = "Delete",
        description = """Delete selected node(s) ("Backspace" or "Delete")""",
        enablingCondition = EnablingCondition.ALLITEMS,
        iconPath = "menu_icons/DeleteNeuron.png",
        keyboardShortcuts = listOf(KeyCombination(KeyEvent.VK_DELETE), KeyCombination(KeyEvent.VK_BACK_SPACE))
    ) {
        launch { deleteSelectedObjects() }
    }
    val neuronCollectionAction = NeuronCollectionAction(networkPanel)
    val newNeuronAction = networkPanel.createAction(
        name = "Add Neuron",
        description = """Add or "put" new node (p)""",
        iconPath = "menu_icons/AddNeuron.png",
        keyboardShorcut = KeyCombination('P')
    ) {
        val neuron = Neuron(network)
        network.addNetworkModel(neuron)
        network.selectModels(listOf(neuron))
    }
    val pasteAction = PasteAction(networkPanel)
    val randomizeObjectsAction = RandomizeObjectsAction(networkPanel)
    val selectAllAction = SelectAllAction(networkPanel)
    val selectAllNeuronsAction = SelectAllNeuronsAction(networkPanel)
    val selectAllWeightsAction = SelectAllWeightsAction(networkPanel)
    val selectIncomingWeightsAction = SelectIncomingWeightsAction(networkPanel)
    val selectOutgoingWeightsAction = SelectOutgoingWeightsAction(networkPanel)
    val selectionEditModeAction = SelectionEditModeAction(networkPanel)
    val setNeuronPropertiesAction = SetNeuronPropertiesAction(networkPanel)
    val setSourceNeurons = SetSourceNeurons(networkPanel)
    val setSynapsePropertiesAction = SetSynapsePropertiesAction(networkPanel)

    val showEditToolBarAction = ShowEditToolBarAction(networkPanel)
    val showLayoutDialogAction = ShowLayoutDialogAction(networkPanel)
    val showPrioritiesAction = ShowPrioritiesAction(networkPanel)
    val showWeightMatrixAction = ShowWeightMatrixAction(networkPanel)
    val spaceHorizontalAction = SpaceHorizontalAction(networkPanel)
    val spaceVerticalAction = SpaceVerticalAction(networkPanel)
    val testInputAction = TestInputAction(networkPanel)
    val textEditModeAction = TextEditModeAction(networkPanel)
    val wandEditModeAction = WandEditModeAction(networkPanel)
    val zoomToFitPageAction = ZoomToFitPageAction(networkPanel)

    val showMainToolBarAction = ShowMainToolBarAction(networkPanel)

    val showNetworkUpdaterDialog = networkPanel.createAction(
        name = "Edit Update Sequence...",
        description = "Edit the update sequence for this network",
        iconPath = "menu_icons/Sequence.png"
    ) {
        NetworkUpdateManagerPanel(networkPanel.network).displayInDialog()
    }

    val showNetworkPreferencesAction = networkPanel.createAction(
        name = "Network Preferences...",
        description = "Show the network preference dialog",
        iconPath = "menu_icons/Prefs.png",
        keyboardShorcut = CmdOrCtrl + ','
    ) {
        getPreferenceDialog(NetworkPreferences).apply {
            addClosingTask {
                // TODO: Temp
                // networkPanel.canvas.background = NetworkPreferences.networkBackgroundColor
            }
        }.display()
    }

    val iterateNetworkAction = networkPanel.createAction(
        name = "Iterate network",
        description = "Step network update algorithm (\"spacebar\")",
        iconPath = "menu_icons/Step.png",
        keyboardShorcut = KeyCombination(KeyEvent.VK_SPACE)
    ) {
        networkPanel.network.update()
    }

    val addDeepNetAction = if (Utils.isM1Mac()) {
        networkPanel.createAction(
            name = "Add Deep Network...",
            description = "Deep Network is not currently supported on M1 Macs",
            keyboardShorcut = CmdOrCtrl + Shift + 'D'
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
            keyboardShorcut = CmdOrCtrl + Shift + 'D'
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
        keyboardShorcut = KeyCombination('5')
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
        keyboardShorcut = CmdOrCtrl + Shift + 'S'
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
        keyboardShortcuts = CmdOrCtrl + 'R',
        enablingCondition = EnablingCondition.SYNAPSES
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
            addGroupAction("LMS (Least Mean Squares)") { networkPanel.showLMSCreationDialog() },
            addGroupAction("SOM Network") { SOMCreationDialog(networkPanel) },
            addGroupAction("SRN (Simple Recurrent Network)") { networkPanel.showSRNCreationDialog() }
        )

    fun applyConnectionAction(strategy: ConnectionStrategy): AbstractAction {
        return networkPanel.createConditionallyEnabledAction(
            name = "Connect ${strategy.name}...",
            enablingCondition = EnablingCondition.SOURCE_AND_TARGET_NEURONS
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

    fun createSynapseGroupVisibilityAction() = networkPanel.createAction(
        name = "Toggle visibility of selected synapse groups",
        keyboardShorcut = CmdOrCtrl + 'T',
        initBlock = {
            isEnabled = networkPanel.selectionManager.filterSelectedModels<SynapseGroup2>().isNotEmpty() &&
                    networkPanel.selectionManager.filterSelectedModels<SynapseGroup2>().none {
                        it.synapses.size > networkPanel.network.synapseGroupExpendedVisibilityThreshold
                    }
        }
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
        enablingCondition = EnablingCondition.SYNAPSES
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
        enablingCondition = EnablingCondition.SYNAPSES
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
     * Prune selected weights. Weights whose absolute value is less then the threshold are removed.
     */
    val fastSparseAction  = networkPanel.createConditionallyEnabledAction(
        name = "Create sparse connection",
        enablingCondition = EnablingCondition.SOURCE_AND_TARGET_NEURONS
    ) {
        val sparsity: String = JOptionPane.showInputDialog(
            null,
            "Sparsity (0 to 1):",
            ".1"
        )
        val sparse = Sparse().apply {
            connectionDensity = sparsity.toDouble()
        }
        sparse.connectNeurons(network,
            selectionManager.filterSelectedSourceModels<Neuron>(),
            selectionManager.filterSelectedModels<Neuron>())
    }

    /**
     * Randomize the polarity of selected nodes. Note that this will change the polarity of outgoing synapses.
     */
    val randomizePolarityAction = networkPanel.createConditionallyEnabledAction(
        name = "Randomize polarity of selected neurons",
        enablingCondition = EnablingCondition.NEURONS
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
        keyboardShortcuts = CmdOrCtrl + 'L',
        enablingCondition = EnablingCondition.NEURONS
    ) {
        GridLayout().layoutNeurons(selectionManager.filterSelectedModels<Neuron>())
        network.events.zoomToFitPage.fireAndForget()
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
        }.also { network.selectModels(it) }
        network.events.zoomToFitPage.fireAndSuspend()
    }
}
