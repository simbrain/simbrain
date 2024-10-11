package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.*
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.util.*
import org.simbrain.util.piccolo.SceneGraphBrowser
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.propertyeditor.wrapperWidget
import org.simbrain.util.table.*
import java.awt.Dialog
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.*

fun NetworkPanel.showTextPropertyDialog(textNodes: Collection<TextNode>) {
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
    NeuronArray.CreationTemplate().createEditorDialog {
        val neuronArray = it.create()
        network.addNetworkModel(neuronArray)
    }.also {
        it.title = "Create Neuron Array"
    }.display()
}

fun NetworkPanel.showActivationStackCreationDialog() {
    ActivationActivationSequence.CreationTemplate().createEditorDialog {
        val activationStack = it.create()
        network.addNetworkModel(activationStack)
    }.also {
        it.title = "Create Activation Stack"
    }.display()
}

fun NetworkPanel.showTransformerBlockCreationDialog() {
    TransformerBlock.CreationTemplate().createEditorDialog {
        val transformerBlock = it.create()
        network.addNetworkModel(transformerBlock)
    }.also {
        it.title = "Create Transformer Block"
    }.display()
}

val NetworkPanel.neuronDialog
    get() = selectionManager.filterSelectedModels<Neuron>().let { neurons ->
        if (neurons.isEmpty()) {
            null
        } else {
            NeuronDialog(neurons).apply { modalityType = Dialog.ModalityType.MODELESS }
        }
    }

val NetworkPanel.synapseDialog
    get() =
        SynapseDialog.createSynapseDialog(selectionManager.filterSelectedModels<Synapse>())

fun NetworkPanel.createNeuronGroupDialog(neuronGroup: AbstractNeuronCollection) = neuronGroup.createEditorDialog()

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
    // np.initScreenElements()
    frame.contentPane = np
    frame.preferredSize = Dimension(500, 500)
    frame.pack()
    frame.isVisible = true
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(we: WindowEvent) {
            System.exit(0)
        }
    })
    // System.out.println(np.debugString());
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

/**
 * Shows a dialog that allows the user to send inputs from a [SimbrainDataTable] to the provided neurons.
 */
fun showInputPanel(neurons: List<Neuron>) {
    createTestInputPanel(neurons).displayInDialog()
}

fun SynapseGroupNode.getDialog(): StandardDialog {

    val dialog = StandardDialog().also { it.okButton.isVisible = false; it.cancelButton.isVisible = false }
    val tabbedPane = JTabbedPane()


    val synapsesEditor = AnnotatedPropertyEditor(synapseGroup.synapses)
    val connectionStrategyPanel = ConnectionStrategyPanel(synapseGroup.connectionStrategy)
    val matrixViewer = WeightMatrixViewer(synapseGroup.source.neuronList, synapseGroup.target.neuronList)

    val synapseAdjustmentPanel = SynapseAdjustmentPanel(
        synapseGroup.synapses,
        synapseGroup.weightRandomizer,
        synapseGroup.connectionStrategy.exRandomizer,
        synapseGroup.connectionStrategy.inRandomizer
    ) {
        synapsesEditor.refreshValues()
        matrixViewer.refreshValues()
        connectionStrategyPanel.percentExcitatoryPanel.setPercentExcitatory(synapseGroup.synapses.percentExcitatory())
    }

    val unregister = synapseGroup.events.updated.on(dispatcher = Dispatchers.Swing) {
        synapseAdjustmentPanel.fullUpdate()
    }

    dialog.addCloseTask {
        unregister()
    }

    val synapsesEditorApplyPanel = synapsesEditor.createApplyPanel {
        commitChanges()
        synapseAdjustmentPanel.fullUpdate()
        matrixViewer.refreshValues()
        connectionStrategyPanel.percentExcitatoryPanel.setPercentExcitatory(synapseGroup.synapses.percentExcitatory())
    }

    val connectionStrategyApplyPanel = connectionStrategyPanel.createApplyPanel {
        commitChanges()
        synapseGroup.connectionStrategy = connectionStrategy
        synapseGroup.applyConnectionStrategy()
        synapseAdjustmentPanel.fullUpdate()
        synapsesEditor.refreshValues()
        matrixViewer.refreshValues()
    }

    val matrixViewerApplyPanel = matrixViewer.createApplyPanel {
        commitChanges()
        synapseAdjustmentPanel.fullUpdate()
        synapsesEditor.refreshValues()
        connectionStrategyPanel.percentExcitatoryPanel.setPercentExcitatory(synapseGroup.synapses.percentExcitatory())
    }

    dialog.contentPane = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        add(tabbedPane)
        tabbedPane.addTab("Weights", synapseAdjustmentPanel)
        tabbedPane.addTab("Update Rule", synapsesEditorApplyPanel)
        tabbedPane.addTab("Connection Strategy", connectionStrategyApplyPanel)
        tabbedPane.add("Weight Matrix", matrixViewerApplyPanel)
    }

    return dialog
}

/**
 * Show dialog for Smile classifier creation
 */
fun NetworkPanel.showClassifierCreationDialog() {
    val creator = SmileClassifier.ClassifierCreator()
    AnnotatedPropertyEditor(creator).displayInDialog {
        commitChanges()
        network.addNetworkModel(creator.create(network))
    }.also {
        it.title = "Create Classifier"
    }
}

class ConnectionStrategyPanel(connectionStrategy: ConnectionStrategy) : JPanel() {

    val strategySelector = objectWrapper("Connection Strategy", connectionStrategy)
    val connectionStrategy get() = strategySelector.editingObject
    val editor = AnnotatedPropertyEditor(strategySelector)
    val percentExcitatoryPanel = PercentExcitatoryPanel(connectionStrategy.percentExcitatory)

    init {
        add(editor)
        val widget = editor.wrapperWidget

        fun updatePanel(value: Any?) {
            if (value is ConnectionStrategy) {
                val itemPanel = editor.defaultLabelledItemPanel
                if (value.usesPolarity) {
                    itemPanel.addItem(percentExcitatoryPanel)
                } else {
                    itemPanel.remove(percentExcitatoryPanel)
                }
            }
        }

        widget.events.valueChanged.on {
            updatePanel(widget.value)
        }

        updatePanel(widget.value)
    }


    fun commitChanges(): Boolean {
        editor.commitChanges()
        connectionStrategy.percentExcitatory = percentExcitatoryPanel.getPercentAsProbability() * 100
        return true
    }

}