package org.simbrain.network.gui

import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import javax.swing.AbstractAction

abstract class ConditionallyEnabledAction(
        val networkPanel: NetworkPanel,
        title: String,
        private val updateType: EnablingCondition
) : AbstractAction(title) {

    /**
     * Possible conditions under which to enable the action:
     * - NEURONS: if at least one neuron is selected
     * - SYNAPSES: if at least one synapse is selected
     * - ALLITEMS: if at least one network model is selected
     * - SOURCE_NEURONS: if at least one neuron is designated as source neuron
     * - SOURCE_AND_TARGET_NEURONS: if at least one neuron is designated as
     * source and one neuron is designated as target.
     * - SOURCE_AND_TARGET_NEURON_GROUPS: if at least one neuron group is
     * designated as source and one neuron group is designated as target.
     */
    enum class EnablingCondition {
        NEURONS, SYNAPSES, ALLITEMS, SOURCE_NEURONS, SOURCE_AND_TARGET_NEURONS
    }

    init {
        updateAction()
        networkPanel.selectionManager.events.selection.on { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on { _, _ -> updateAction() }
    }

    private fun updateAction() {
        isEnabled = networkPanel.selectionManager.checkEnablingFunction(updateType)
    }
}

fun NetworkSelectionManager.checkEnablingFunction(condition: ConditionallyEnabledAction.EnablingCondition): Boolean {
    return when (condition) {
        ConditionallyEnabledAction.EnablingCondition.NEURONS -> selection.any { it is NeuronNode }
        ConditionallyEnabledAction.EnablingCondition.SYNAPSES -> selection.any { it is SynapseNode }
        ConditionallyEnabledAction.EnablingCondition.ALLITEMS -> selection.isNotEmpty()
        ConditionallyEnabledAction.EnablingCondition.SOURCE_NEURONS -> sourceSelection.any { it is NeuronNode }
        ConditionallyEnabledAction.EnablingCondition.SOURCE_AND_TARGET_NEURONS ->
            sourceSelection.any { it is NeuronNode } && selection.any { it is NeuronNode }
    }
}