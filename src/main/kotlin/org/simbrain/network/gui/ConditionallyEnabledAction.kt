package org.simbrain.network.gui

import org.simbrain.network.core.LocatableModel
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseNode
import javax.swing.AbstractAction

abstract class ConditionallyEnabledAction(
        val networkPanel: NetworkPanel,
        title: String,
        private val enablingCondition: NetworkPanel.() -> Boolean
) : AbstractAction(title) {

    init {
        updateAction()
        networkPanel.selectionManager.events.selection.on { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on { _, _ -> updateAction() }
        // Also listen to clipboard changes for conditions that depend on clipboard state
        Clipboard.addClipboardListener { updateAction() }
    }

    private fun updateAction() {
        isEnabled = enablingCondition(networkPanel)
    }
}

/**
 * Predefined enabling conditions for common use cases.
 * 
 * You can also create ad-hoc conditions inline, for example:
 * ```
 * val myAction = networkPanel.createConditionallyEnabledAction(
 *     name = "My Action",
 *     enablingCondition = {
 *         selectionManager.selectedModels.size > 5 && !Clipboard.isEmpty
 *     }
 * ) { ... }
 * ```
 */
object EnablingConditions {
    
    /** Enable if at least one neuron is selected */
    val NEURONS: NetworkPanel.() -> Boolean = {
        selectionManager.selection.any { it is NeuronNode }
    }
    
    /** Enable if at least one synapse is selected */
    val SYNAPSES: NetworkPanel.() -> Boolean = {
        selectionManager.selection.any { it is SynapseNode }
    }
    
    /** Enable if at least one network model is selected */
    val ALLITEMS: NetworkPanel.() -> Boolean = {
        selectionManager.selection.isNotEmpty()
    }
    
    /** Enable if at least one locatable model is selected */
    val LOCATABLE_MODELS: NetworkPanel.() -> Boolean = {
        selectionManager.selectedModels.any { it is LocatableModel }
    }
    
    /** Enable if at least one neuron is designated as source neuron */
    val SOURCE_NEURONS: NetworkPanel.() -> Boolean = {
        selectionManager.sourceSelection.any { it is NeuronNode }
    }
    
    /** Enable if at least one neuron is designated as source and one neuron is designated as target */
    val SOURCE_AND_TARGET_NEURONS: NetworkPanel.() -> Boolean = {
        selectionManager.sourceSelection.any { it is NeuronNode } &&
        selectionManager.selection.any { it is NeuronNode }
    }
    
    /** Enable if clipboard is not empty */
    val CLIPBOARD_NOT_EMPTY: NetworkPanel.() -> Boolean = {
        !Clipboard.isEmpty
    }
}