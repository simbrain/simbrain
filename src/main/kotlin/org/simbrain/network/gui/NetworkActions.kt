package org.simbrain.network.gui

import org.simbrain.network.connections.*
import org.simbrain.network.gui.actions.ShowDebugAction
import org.simbrain.network.gui.actions.ShowLayoutDialogAction
import org.simbrain.network.gui.actions.TestInputAction
import org.simbrain.network.gui.actions.connection.ApplyConnectionAction
import org.simbrain.network.gui.actions.connection.ClearSourceNeurons
import org.simbrain.network.gui.actions.connection.SetSourceNeurons
import org.simbrain.network.gui.actions.dl4j.AddMultiLayerNet
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
import org.simbrain.network.gui.actions.synapse.*
import org.simbrain.network.gui.actions.toolbar.ShowEditToolBarAction
import org.simbrain.network.gui.actions.toolbar.ShowMainToolBarAction
import org.simbrain.network.gui.actions.toolbar.ShowRunToolBarAction
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog
import org.simbrain.network.gui.dialogs.network.*
import org.simbrain.util.StandardDialog
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke


class NetworkActions(val networkPanel: NetworkPanel) {

    val addMultiLayerNet = AddMultiLayerNet(networkPanel)
    val addNeuronArrayAction = AddNeuronArrayAction(networkPanel)
    val addNeuronsAction = AddNeuronsAction(networkPanel)
    val addSynapseGroupAction = AddSynapseGroupAction(networkPanel)
    val alignHorizontalAction = AlignHorizontalAction(networkPanel)
    val alignVerticalAction = AlignVerticalAction(networkPanel)
    val clearNodeActivationsAction = ClearNodeActivationsAction(networkPanel)
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
//    val setTextPropertiesAction = SetTextPropertiesAction(networkPanel) TODO
    val showAdjustConnectivityDialog = ShowAdjustConnectivityDialog(networkPanel)
    val showAdjustSynapsesDialog = ShowAdjustSynapsesDialog(networkPanel)
    val showDebugAction = ShowDebugAction(networkPanel)
    val showEditToolBarAction = ShowEditToolBarAction(networkPanel)
    val showLayoutDialogAction = ShowLayoutDialogAction(networkPanel)
    val showMainToolBarAction = ShowMainToolBarAction(networkPanel)
    val showNetworkPreferencesAction = ShowNetworkPreferencesAction(networkPanel)
    val showNetworkUpdaterDialog = ShowNetworkUpdaterDialog(networkPanel)
    val showPrioritiesAction = ShowPrioritiesAction(networkPanel)
    val showRunToolBarAction = ShowRunToolBarAction(networkPanel)
    val showWeightMatrixAction = ShowWeightMatrixAction(networkPanel)
    val showWeightsAction = ShowWeightsAction(networkPanel)
    val spaceHorizontalAction = SpaceHorizontalAction(networkPanel)
    val spaceVerticalAction = SpaceVerticalAction(networkPanel)
    val stopNetworkAction = StopNetworkAction(networkPanel)
    val testInputAction = TestInputAction(networkPanel)
    val textEditModeAction = TextEditModeAction(networkPanel)
    val wandEditModeAction = WandEditModeAction(networkPanel)
    val zoomToFitPageAction = ZoomToFitPageAction(networkPanel)

    val neuronGroupAction = addGroupAction("Add Neuron Group") {
        NeuronGroupDialog(it).also {
            networkPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('g'), this)
            networkPanel.actionMap.put(this, this)
        }
    }

    val clipboardActions
        get() = listOf(copyAction, cutAction, pasteAction)

    val networkControlActions
        get() = listOf(runNetworkAction, stopNetworkAction)

    val networkEditingActions
        get() = listOf(newNeuronAction, deleteAction)

    val networkModeActions get() = listOf<AbstractAction>(
            selectionEditModeAction,
            textEditModeAction,
            wandEditModeAction
    )

    val newNetworkActions get() = listOf(
            addGroupAction("Backprop") { BackpropCreationDialog(networkPanel) },
            addGroupAction("Competitive Network") { CompetitiveCreationDialog(networkPanel) },
            addGroupAction("Feed Forward Network") { FeedForwardCreationDialog(networkPanel) },
            addGroupAction("Hopfield") { HopfieldCreationDialog(networkPanel) },
            addGroupAction("LMS (Least Mean Squares)") { LMSCreationDialog(networkPanel) },
            addGroupAction("SOM Network") { SOMCreationDialog(networkPanel) },
            addGroupAction("SRN (Simple Recurrent Network)") { SRNCreationDialog(networkPanel) }
    )

    val connectionActions get() = listOf(
            applyConnectionAction("All to all", AllToAll()),
            applyConnectionAction("One-to-one", OneToOne()),
            applyConnectionAction("Radial (Gaussian)", RadialGaussian()),
            applyConnectionAction("Radial (Simple)", RadialSimple()),
            applyConnectionAction("Sparse", Sparse())
    )

    // TODO: Note: the lambda parameter `NetworkPanel` is not used
    private fun addGroupAction(name: String, createDialog: AddGroupAction.(NetworkPanel) -> StandardDialog)
            = AddGroupAction(networkPanel, name, createDialog)

    private fun applyConnectionAction(name: String, connectionStrategy: ConnectionStrategy) =
            ApplyConnectionAction(networkPanel, connectionStrategy, name)

}