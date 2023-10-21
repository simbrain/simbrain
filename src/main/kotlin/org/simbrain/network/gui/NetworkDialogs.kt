package org.simbrain.network.gui

import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.gui.dialogs.*
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.util.StandardDialog
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.displayInDialog
import org.simbrain.util.piccolo.SceneGraphBrowser
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.propertyeditor.wrapperWidget
import org.simbrain.util.table.*
import org.simbrain.util.widgets.ApplyPanel.createApplyPanel
import org.simbrain.util.widgets.EditablePanel
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
        val neuronArray = it.create(network)
        network.addNetworkModelAsync(neuronArray)
    }.also {
        it.title = "Create Neuron Array"
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

fun NetworkPanel.createNeuronGroupDialog(neuronGroup: NeuronGroup) =
    NeuronGroupDialog(this, neuronGroup).apply {
        title = "Neuron Group Dialog"
        setAsDoneDialog()
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
fun NetworkPanel.showInputPanel(neurons: List<Neuron>) {
    createTestInputPanel(neurons).displayInDialog()
}

/**
 * Show weight matrix panel for weights connecting current source (red) and target (green) nodes.
 */
fun NetworkPanel.showWeightMatrix() {
    createWeightMatrixViewerOnSelectedNeurons().displayInDialog {
        commitChanges()
    }.apply {
        title = "Weight Matrix Viewer"
    }
}

fun SynapseGroupNode.getDialog(): StandardDialog {

    val dialog = StandardDialog()
    val tabbedPane = JTabbedPane()
    var matrixViewerPanel = weightMatrixViewer()

    val sap = SynapseAdjustmentPanel(
        synapseGroup.synapses,
        synapseGroup.weightRandomizer,
        synapseGroup.connectionStrategy.exRandomizer,
        synapseGroup.connectionStrategy.inRandomizer
    )

    val connPanel = ConnectionStrategyPanel(synapseGroup.connectionStrategy)
    val connectionStrategyPanel = createApplyPanel(connPanel).apply {
        addActionListener {
            connPanel.commitChanges()
            synapseGroup.connectionStrategy = connPanel.connectionStrategy
            synapseGroup.applyConnectionStrategy()
        }
    }

    synapseGroup.events.synapseListChanged.on {
        sap.fullUpdate()
    }

    dialog.contentPane = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        add(tabbedPane)
        tabbedPane.addTab("Weights", sap)
        tabbedPane.addTab("Connection Strategy", connectionStrategyPanel)
        tabbedPane.add("Weight Matrix", matrixViewerPanel)
    }

    dialog.addClosingTask {
        matrixViewerPanel.commitChanges()
    }

    return dialog
}

/**
 * Show dialog for Smile classifier creation
 */
fun NetworkPanel.showClassifierCreationDialog() {
    val creator = SmileClassifier.ClassifierCreator(network.idManager.getProposedId(SmileClassifier::class.java))
    AnnotatedPropertyEditor(creator).displayInDialog {
        commitChanges()
        network.addNetworkModelAsync(creator.create(network))
    }.also {
        it.title = "Create Smile Classifier"
    }
}

class ConnectionStrategyPanel(connectionStrategy: ConnectionStrategy) : EditablePanel() {

    val strategySelector = objectWrapper("Connection Strategy", connectionStrategy)
    val connectionStrategy get() = strategySelector.editingObject
    val editor = AnnotatedPropertyEditor(strategySelector)
    val percentExcitatoryPanel = PercentExcitatoryPanel(connectionStrategy.percentExcitatory)
    var sparsePanel: SparsePanel? = null

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
                // Custom Sparse Panel
                if (value is Sparse) {
                    sparsePanel?.let { itemPanel.remove(it) }
                    sparsePanel = SparsePanel(value)
                    itemPanel.addItem(sparsePanel)
                    // TODO: Put it in the panel itself?
                    // comp.editorPanel.addItem(sparsePanel)
                } else {
                    sparsePanel?.let { itemPanel.remove(it) }
                    // comp.editorPanel.removeItem(sparsePanel)
                }
            }
        }

        widget.events.valueChanged.on {
            updatePanel(widget.value)
        }

        updatePanel(widget.value)
    }

    override fun fillFieldValues() {
    }

    override fun commitChanges(): Boolean {
        editor.commitChanges()
        connectionStrategy.percentExcitatory = percentExcitatoryPanel.getPercentAsProbability() * 100
        connectionStrategy.let {
            if (it is Sparse) {
                sparsePanel?.applyChanges(it)
            }
        }
        return true
    }

}