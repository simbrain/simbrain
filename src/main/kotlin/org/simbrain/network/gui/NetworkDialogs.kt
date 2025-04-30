package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.PercentExcitatoryPanel
import org.simbrain.network.gui.dialogs.SynapseAdjustmentPanel
import org.simbrain.network.gui.dialogs.createTestInputPanel
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
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ListSelectionListener

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
        undoManager.addUndoableAction(
            description = "Create neuron array ${neuronArray.id}",
            undo = { neuronArray.delete() },
            redo = { network.addNetworkModel(neuronArray, usePlacementManager = false, useAutoAssignedId = false)?.await() }
        )
    }.also {
        it.title = "Create Neuron Array"
    }.display()
}

fun NetworkPanel.showActivationSequenceCreationDialog() {
    ActivationSequence.CreationTemplate().createEditorDialog {
        val activationSequence = it.create()
        network.addNetworkModel(activationSequence)
        undoManager.addUndoableAction(
            description = "Create activation sequence ${activationSequence.id}",
            undo = { activationSequence.delete() },
            redo = { network.addNetworkModel(activationSequence, usePlacementManager = false, useAutoAssignedId = false)?.await() }
        )
    }.also {
        it.title = "Create Activation Sequence"
    }.display()
}

fun NetworkPanel.showTransformerBlockCreationDialog() {
    TransformerBlock.CreationTemplate().createEditorDialog {
        val transformerBlock = it.create()
        network.addNetworkModel(transformerBlock)
        undoManager.addUndoableAction(
            description = "Add transformer block ${transformerBlock.id}",
            undo = { transformerBlock.delete() },
            redo = { network.addNetworkModel(transformerBlock, usePlacementManager = false, useAutoAssignedId = false)?.await() }
        )
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
        addSubnetworkAction(this@NetworkPanel) { creator.create(network) }
    }.also {
        it.title = "Create Classifier"
    }
}

fun NetworkPanel.showUndoHistoryDialog() {
    val dialog = JDialog(JFrame.getFrames().firstOrNull(), "Undo / Redo History", true).apply {
        defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        setSize(600, 400)
        setLocationRelativeTo(this@showUndoHistoryDialog)
    }

    // Create list models for undo and redo stacks
    val undoListModel = DefaultListModel<String>().apply {
        addAll(undoManager.undoStack.reversed().mapIndexed { index, action ->
            "${index + 1}. ${action.description}"
        })
    }

    val redoListModel = DefaultListModel<String>().apply {
        addAll(undoManager.redoStack.reversed().mapIndexed { index, action ->
            "${index + 1}. ${action.description}"
        })
    }

    // Create lists for undo and redo stacks
    val undoJList = JList(undoListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        if (model.size > 0) {
            selectedIndex = 0
        }
        border = EmptyBorder(5, 5, 5, 5)
    }

    val redoJList = JList(redoListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        // If nothing is selected in undo list and there is something in redo list, select first entry
        if(undoJList.selectedIndex == -1) {
            if (model.size > 0) {
                selectedIndex = 0
            }
        }
        border = EmptyBorder(5, 5, 5, 5)
    }

    // Create buttons
    val closeButton = JButton("Close").apply {
        addActionListener {
            dialog.dispose()
        }
    }

    val goToButton = JButton("Go To Selected Point").apply {
        addActionListener {
            val undoIndex = undoJList.selectedIndex
            val redoIndex = redoJList.selectedIndex

            if (undoIndex != -1) {
                // Go to a point in the undo stack
                // We need to undo (undoIndex + 1) operations
                val operationsToUndo = undoIndex + 1
                this@showUndoHistoryDialog.launch {
                    repeat(operationsToUndo) {
                        undoManager.undo()
                    }
                    dialog.dispose()
                }
            } else if (redoIndex != -1) {
                // Go to a point in the redo stack
                // We need to redo (redoIndex + 1) operations
                val operationsToRedo = redoIndex + 1
                this@showUndoHistoryDialog.launch {
                    repeat(operationsToRedo) {
                        undoManager.redo()
                    }
                    dialog.dispose()
                }
            }
        }
    }

    // Enable the Go To button when an item is selected in either list
    val listSelectionListener = ListSelectionListener { e ->
        // If a selection is made in one list, clear the selection in the other list
        if (e.source === undoJList && !e.valueIsAdjusting && undoJList.selectedIndex != -1) {
            redoJList.clearSelection()
        } else if (e.source === redoJList && !e.valueIsAdjusting && redoJList.selectedIndex != -1) {
            undoJList.clearSelection()
        }

        // Enable the Go To button if an item is selected in either list
        goToButton.isEnabled = undoJList.selectedIndex != -1 || redoJList.selectedIndex != -1
    }

    undoJList.addListSelectionListener(listSelectionListener)
    redoJList.addListSelectionListener(listSelectionListener)

    // Create panels for undo and redo lists with descriptive headers
    val undoPanel = JPanel(BorderLayout()).apply {
        add(JLabel("Undo Stack (${undoListModel.size()} items)"), BorderLayout.NORTH)
        add(JScrollPane(undoJList), BorderLayout.CENTER)
        border = EmptyBorder(5, 5, 5, 5)
    }

    val redoPanel = JPanel(BorderLayout()).apply {
        add(JLabel("Redo Stack (${redoListModel.size()} items)"), BorderLayout.NORTH)
        add(JScrollPane(redoJList), BorderLayout.CENTER)
        border = EmptyBorder(5, 5, 5, 5)
    }

    // Create split pane to hold both panels
    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, undoPanel, redoPanel).apply {
        dividerLocation = 300
        resizeWeight = 0.5
    }

    // Create button panel
    val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
        add(goToButton)
        add(closeButton)
    }

    // Add components to dialog
    dialog.contentPane.layout = BorderLayout()
    dialog.contentPane.add(splitPane, BorderLayout.CENTER)
    dialog.contentPane.add(buttonPanel, BorderLayout.SOUTH)

    dialog.isVisible = true
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
