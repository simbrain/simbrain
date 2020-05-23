package org.simbrain.network.gui

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
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
import org.simbrain.util.StandardDialog
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.piccolo.SceneGraphBrowser
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.Dialog
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel

fun NetworkPanel.showTextPropertyDialog(textNodes: List<TextNode>) {
    TextDialog(textNodes).apply {
        setLocationRelativeTo(this@showTextPropertyDialog)
        isVisible = true
    }
}

fun NetworkPanel.showSelectedNeuronProperties() {
    NeuronDialog(selectionManager.filterSelectedModels<Neuron>()).apply {
        modalityType = Dialog.ModalityType.MODELESS
        pack()
        setLocationRelativeTo(this@showSelectedNeuronProperties)
        isVisible = true
    }
}

fun NetworkPanel.showSelectedSynapseProperties() {
    SynapseDialog.createSynapseDialog(selectionManager.filterSelectedModels<Synapse>()).apply {
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

val NetworkPanel.neuronDialog get() = selectionManager.filterSelectedModels<Neuron>().let { neurons ->
    if (neurons.isEmpty()) {
        null
    } else {
        NeuronDialog(neurons).apply { modalityType = Dialog.ModalityType.MODELESS }
    }
}

val NetworkPanel.synapseDialog get() =
    SynapseDialog.createSynapseDialog(selectionManager.filterSelectedModels<Synapse>())

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

/**
 * Display the provided network in a dialog
 *
 * @param network the model network to show
 */
fun showNetwork(networkComponent: NetworkComponent) {
    // TODO: Creation outside of desktop lacks menus
    val frame = JFrame()
    val np = NetworkPanel(networkComponent)
    // component?.getDesktop()?.addInternalFrame(frame)
    //np.initScreenElements()
    frame.contentPane = np
    frame.preferredSize = Dimension(500, 500)
    frame.pack()
    frame.isVisible = true
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(we: WindowEvent) {
            System.exit(0)
        }
    })
    //System.out.println(np.debugString());
}

fun NetworkPanel.displayPanel(panel: JPanel?, title: String?): GenericFrame? {
    val frame = GenericJInternalFrame()
    frame.contentPane = panel
    frame.pack()
    frame.isResizable = true
    frame.isMaximizable = true
    frame.isIconifiable = true
    frame.isClosable = true
    frame.title = title
    frame.isVisible = true
    return frame
}


fun NetworkPanel.showPiccoloDebugger() {
    StandardDialog().apply {
        contentPane = SceneGraphBrowser(canvas.root)
        title = "Piccolo Scenegraph Browser"
        isModal = false
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}