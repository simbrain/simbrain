package org.simbrain.network.gui.actions


import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.NeuronGroupNode
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
     * - ALLITEMS: if at least one synapse or neuron is selected
     * - SOURCE_NEURONS: if at least one neuron is designated as source neuron
     * - SOURCE_AND_TARGET_NEURONS: if at least one neuron is designated as
     * source and one neuron is designated as target.
     * - SOURCE_AND_TARGET_NEURON_GROUPS: if at least one neuron group is
     * designated as source and one neuron group is designated as target.
     */
    enum class EnablingCondition {
        NEURONS, SYNAPSES, ALLITEMS, SOURCE_NEURONS, SOURCE_AND_TARGET_NEURONS, SOURCE_AND_TARGET_NEURON_GROUPS
    }

    init {
        updateAction()
        networkPanel.selectionManager.events.onSelection { _, _ -> updateAction() }
    }

    private fun updateAction() {
        isEnabled = with(networkPanel.selectionManager) {
            print(selection)
            when (updateType) {
                EnablingCondition.NEURONS -> selection.any { it is NeuronNode }
                EnablingCondition.SYNAPSES -> selection.any { it is SynapseNode }
                EnablingCondition.ALLITEMS -> selection.isNotEmpty()
                EnablingCondition.SOURCE_NEURONS -> sourceSelection.any { it is NeuronNode }
                EnablingCondition.SOURCE_AND_TARGET_NEURONS ->
                    sourceSelection.any { it is NeuronNode } && selection.any { it is NeuronNode }
                EnablingCondition.SOURCE_AND_TARGET_NEURON_GROUPS ->
                    sourceSelection.any { it is NeuronGroupNode } && selection.any { it is NeuronGroupNode }
            }
        }
    }

}