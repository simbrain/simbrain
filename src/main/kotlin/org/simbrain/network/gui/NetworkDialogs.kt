package org.simbrain.network.gui

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.dl4j.NeuronArray
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.gui.dialogs.dl4j.MultiLayerNetCreationDialog
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Dialog

fun NetworkPanel.showTextPropertyDialog(textNodes: List<TextNode>) {
    TextDialog(textNodes).apply {
        setLocationRelativeTo(this@showTextPropertyDialog)
        isVisible = true
    }
}

fun NetworkPanel.showSelectedNeuronProperties() {
    NeuronDialog(selectionManager.selectedModelsOf<Neuron>()).apply {
        modalityType = Dialog.ModalityType.MODELESS
        pack()
        setLocationRelativeTo(this@showSelectedNeuronProperties)
        isVisible = true
    }
}


fun NetworkPanel.showSelectedSynapseProperties() {
    SynapseDialog.createSynapseDialog(selectionManager.selectedModelsOf<Synapse>()).apply {
        modalityType = Dialog.ModalityType.MODELESS
        pack()
        setLocationRelativeTo(this@showSelectedSynapseProperties)
        isVisible = true
    }
}


fun NetworkPanel.showNeuronArrayCreationDialog() {
    val template = NeuronArray.CreationTemplate(network.idManager.getProposedId(NeuronArray::class.java))
    AnnotatedPropertyEditor(template).dialog.apply {
        addClosingTask {
            val neuronArray = template.create(network)
            placementManager.addNewModelObject(neuronArray)
            network.addNeuronArray(neuronArray)
        }
        pack()
        setLocationRelativeTo(this@showNeuronArrayCreationDialog)
        isVisible = true
    }
}

fun NetworkPanel.showMultiLayerNetworkCreationDialog() {
    MultiLayerNetCreationDialog(this).apply {
        pack()
        setLocationRelativeTo(this)
        isVisible = true
    }
}

val NetworkPanel.neuronDialog get() = selectionManager.selectedModelsOf<Neuron>().let { neurons ->
    if (neurons.isEmpty()) {
        null
    } else {
        NeuronDialog(neurons).apply { modalityType = Dialog.ModalityType.MODELESS }
    }
}

val NetworkPanel.synapseDialog get() =
    SynapseDialog.createSynapseDialog(selectionManager.selectedModelsOf<Synapse>())

fun NetworkPanel.createNeuronGroupDialog(neuronGroup: NeuronGroup) =
        NeuronGroupDialog(this, neuronGroup).apply {
            title = "Neuron Group Dialog"
            setAsDoneDialog()
            modalityType = Dialog.ModalityType.MODELESS
        }

fun NetworkPanel.createSynapseGroupDialog(synapseGroup: SynapseGroup) =
        SynapseGroupDialog.createSynapseGroupDialog(this, synapseGroup).apply {
            modalityType = Dialog.ModalityType.MODELESS
        }

