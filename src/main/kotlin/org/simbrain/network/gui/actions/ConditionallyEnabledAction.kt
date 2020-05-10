package org.simbrain.network.gui.actions


import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.NeuronGroupNode
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import javax.swing.AbstractAction

abstract class ConditionallyEnabledAction(
        val np: NetworkPanel,
        title: String,
        private val updateType: EnablingCondition
) : AbstractAction(title) {

    // TODO: Yulin I'm sure this is wrong... check?
    // I got rid of the protected on networkpanel. The networkPanel name was also a conflict.
    // Also the enabling stuff was failing

    fun getNetworkPanel() = np
    enum class EnablingCondition {
        NEURONS, SYNAPSES, ALLITEMS, SOURCE_NEURONS, SOURCE_AND_TARGET_NEURONS, SOURCE_AND_TARGET_NEURON_GROUPS
    }

    init {
        isEnabled = true
//        //TODO
//        isEnabled = with(np.selectionManager) {
//            print(selection)
//            when (updateType) {
//                EnablingCondition.NEURONS -> selection.any { it is NeuronNode }
//                EnablingCondition.SYNAPSES -> selection.any { it is SynapseNode }
//                EnablingCondition.ALLITEMS -> selection.isNotEmpty()
//                EnablingCondition.SOURCE_NEURONS -> sourceSelection.any { it is NeuronNode }
//                EnablingCondition.SOURCE_AND_TARGET_NEURONS ->
//                    sourceSelection.any { it is NeuronNode } && selection.any { it is NeuronNode }
//                EnablingCondition.SOURCE_AND_TARGET_NEURON_GROUPS ->
//                    sourceSelection.any { it is NeuronGroupNode } && selection.any { it is NeuronGroupNode }
//            }
//        }
    }

}