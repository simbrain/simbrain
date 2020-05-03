package org.simbrain.network.gui

import org.simbrain.network.gui.actions.SetTextPropertiesAction
import org.simbrain.network.gui.actions.ShowDebugAction
import org.simbrain.network.gui.actions.ShowLayoutDialogAction
import org.simbrain.network.gui.actions.TestInputAction
import org.simbrain.network.gui.actions.connection.ClearSourceNeurons
import org.simbrain.network.gui.actions.connection.SetSourceNeurons
import org.simbrain.network.gui.actions.edit.*
import org.simbrain.network.gui.actions.modelgroups.AddGroupAction
import org.simbrain.network.gui.actions.network.*
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
import javax.swing.AbstractAction
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu


class NetworkActionManager(val networkPanel: NetworkPanel) {

    val actions = mapOf(
            ZoomToFitPageAction::class.java to ZoomToFitPageAction(networkPanel),
            NewNeuronAction::class.java to NewNeuronAction(networkPanel),
            ClearNodeActivationsAction::class.java to ClearNodeActivationsAction(networkPanel),
            RandomizeObjectsAction::class.java to RandomizeObjectsAction(networkPanel),
            SelectAllAction::class.java to SelectAllAction(networkPanel),
            DeleteAction::class.java to DeleteAction(networkPanel),
            CopyAction::class.java to CopyAction(networkPanel),
            CutAction::class.java to CutAction(networkPanel),
            PasteAction::class.java to PasteAction(networkPanel),
            IterateNetworkAction::class.java to IterateNetworkAction(networkPanel),
            RunNetworkAction::class.java to RunNetworkAction(networkPanel),
            StopNetworkAction::class.java to StopNetworkAction(networkPanel),
            ShowDebugAction::class.java to ShowDebugAction(networkPanel),
            ShowNetworkPreferencesAction::class.java to ShowNetworkPreferencesAction(networkPanel),
            AlignHorizontalAction::class.java to AlignHorizontalAction(networkPanel),
            AlignVerticalAction::class.java to AlignVerticalAction(networkPanel),
            SpaceVerticalAction::class.java to SpaceVerticalAction(networkPanel),
            SpaceHorizontalAction::class.java to SpaceHorizontalAction(networkPanel),
            SetNeuronPropertiesAction::class.java to SetNeuronPropertiesAction(networkPanel),
            SetSynapsePropertiesAction::class.java to SetSynapsePropertiesAction(networkPanel),
            SelectAllWeightsAction::class.java to SelectAllWeightsAction(networkPanel),
            SelectAllNeuronsAction::class.java to SelectAllNeuronsAction(networkPanel),
            ShowMainToolBarAction::class.java to ShowMainToolBarAction(networkPanel),
            ShowEditToolBarAction::class.java to ShowEditToolBarAction(networkPanel),
            ShowRunToolBarAction::class.java to ShowRunToolBarAction(networkPanel),
            SelectIncomingWeightsAction::class.java to SelectIncomingWeightsAction(networkPanel),
            SelectOutgoingWeightsAction::class.java to SelectOutgoingWeightsAction(networkPanel),
            SetTextPropertiesAction::class.java to SetTextPropertiesAction(networkPanel),
            ShowWeightMatrixAction::class.java to ShowWeightMatrixAction(networkPanel),
            ShowAdjustSynapsesDialog::class.java to ShowAdjustSynapsesDialog(networkPanel),
            ShowAdjustConnectivityDialog::class.java to ShowAdjustConnectivityDialog(networkPanel),
            ShowNetworkUpdaterDialog::class.java to ShowNetworkUpdaterDialog(networkPanel),
            TestInputAction::class.java to TestInputAction(networkPanel),
            ShowLayoutDialogAction::class.java to ShowLayoutDialogAction(networkPanel),
            SetSourceNeurons::class.java to SetSourceNeurons(networkPanel),
            ClearSourceNeurons::class.java to ClearSourceNeurons(networkPanel),
            ShowPrioritiesAction::class.java to ShowPrioritiesAction(networkPanel),
            ShowWeightsAction::class.java to ShowWeightsAction(networkPanel),
            SelectionEditModeAction::class.java to SelectionEditModeAction(networkPanel),
            TextEditModeAction::class.java to TextEditModeAction(networkPanel),
            WandEditModeAction::class.java to WandEditModeAction(networkPanel)
    )

    inline fun <reified T : AbstractAction> getAction() = actions[T::class.java]

    //TODO: Yulin please check these.  Could use reified below?
    /**
     * Gets an action within a [JCheckBoxMenuItem].  Can just be
     * used as a menu item without using the checkbox.
     */
     inline fun <reified T : AbstractAction> getMenuItem(): JCheckBoxMenuItem {
        return JCheckBoxMenuItem(this.getAction<T>());
    }
    inline fun <reified T : AbstractAction> getMenuItem(checked:Boolean): JCheckBoxMenuItem {
        val menuItem = getMenuItem<T>()
        menuItem.setSelected(checked);
        return menuItem;
    }
    val clipboardActions
        get() = listOf(getAction<CopyAction>(),getAction<CutAction>(),getAction<PasteAction>())

    val networkControlActions
        get() = listOf(getAction<RunNetworkAction>(),getAction<StopNetworkAction>())

    val networkEditingActions
        get() = listOf(getAction<NewNeuronAction>(),getAction<DeleteAction>())

    // TODO: Yulin, isn't the below more verbose than the above?
    val networkModeActions
        get() = listOf<Class<out AbstractAction>>(
                SelectionEditModeAction::class.java,
                TextEditModeAction::class.java,
                WandEditModeAction::class.java
        ).map { actions[it]!! }

    val neuronGroupAction = AddGroupAction(networkPanel, NeuronGroupDialog::class.java, "Add Neuron Group")

    private fun AbstractAction.toMenuItem() = JCheckBoxMenuItem(this)

    val newNetworkActions get() = listOf(
            AddGroupAction(networkPanel, BackpropCreationDialog::class.java, "Backprop"),
            AddGroupAction(networkPanel, CompetitiveCreationDialog::class.java, "Competitive Network"),
            AddGroupAction(networkPanel, FeedForwardCreationDialog::class.java, "Feed Forward Network"),
            AddGroupAction(networkPanel, HopfieldCreationDialog::class.java, "Hopfield"),
            AddGroupAction(networkPanel, LMSCreationDialog::class.java, "LMS (Least Mean Squares)"),
            AddGroupAction(networkPanel, SOMCreationDialog::class.java, "SOM Network"),
            AddGroupAction(networkPanel, SRNCreationDialog::class.java, "SRN (Simple Recurrent Network)")
    )

    val newNetworkMenu get() = JMenu("Insert Network").apply {
        newNetworkActions.forEach { add(it) }
    }

}