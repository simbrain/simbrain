package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.connections.RandomWeightInitializer
import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.PercentExcitatoryPanel
import org.simbrain.network.gui.dialogs.SynapseAdjustmentPanel
import org.simbrain.network.gui.dialogs.createTestInputPanel
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.util.*
import org.simbrain.util.piccolo.SceneGraphBrowser
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.propertyeditor.wrapperWidget
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel

fun NetworkPanel.showTextPropertyDialog(textNodes: Collection<TextNode>) {
    TextDialog(textNodes).apply {
        setLocationRelativeTo(this@showTextPropertyDialog)
        isVisible = true
    }
}

fun NetworkPanel.showEditDialogsForSelectedModels() {
    selectionManager.selection.groupBy { it::class }.forEach { (_, nodes) ->
        nodes.firstOrNull()?.createEditDialog()?.display()
    }
}

fun NetworkPanel.showNeuronArrayCreationDialog() {
    NeuronArray.CreationTemplate().createEditorDialog {
        val neuronArray = it.create()
        network.addNetworkModelAsync(neuronArray)
        undoManager.addUndoableAction(
            description = "Create neuron array ${neuronArray.id}",
            undo = { neuronArray.delete() },
            redo = { network.addNetworkModel(neuronArray, usePlacementManager = false, useAutoAssignedId = false) }
        )
    }.also {
        it.title = "Create Neuron Array"
    }.display()
}

fun NetworkPanel.showActivationSequenceCreationDialog() {
    ActivationSequence.CreationTemplate().createEditorDialog {
        val activationSequence = it.create()
        network.addNetworkModelAsync(activationSequence)
        undoManager.addUndoableAction(
            description = "Create activation sequence ${activationSequence.id}",
            undo = { activationSequence.delete() },
            redo = { network.addNetworkModel(activationSequence, usePlacementManager = false, useAutoAssignedId = false) }
        )
    }.also {
        it.title = "Create Activation Sequence"
    }.display()
}

fun NetworkPanel.showTransformerBlockCreationDialog() {
    TransformerBlock.CreationTemplate().createEditorDialog {
        val transformerBlock = it.create()
        network.addNetworkModelAsync(transformerBlock)
        undoManager.addUndoableAction(
            description = "Add transformer block ${transformerBlock.id}",
            undo = { transformerBlock.delete() },
            redo = { network.addNetworkModel(transformerBlock, usePlacementManager = false, useAutoAssignedId = false) }
        )
    }.also {
        it.title = "Create Transformer Block"
    }.display()
}

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

    val weightInitializer = synapseGroup.connectionStrategy.weightInitializer
    val (exRandomizer, inRandomizer) = if (weightInitializer is RandomWeightInitializer) {
        weightInitializer.exRandomizer to weightInitializer.inRandomizer
    } else {
        synapseGroup.weightRandomizer to synapseGroup.weightRandomizer
    }
    val synapseAdjustmentPanel = SynapseAdjustmentPanel(
        synapseGroup.synapses,
        synapseGroup.weightRandomizer,
        exRandomizer,
        inRandomizer
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
    val creator = ClassifierNetwork.ClassifierCreator()
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

fun NetworkPanel.showPriorityTableDialog() {
    val dialog = StandardDialog()
    dialog.title = "Network Model Priorities"

    val allModels = network.allModels.sortedBy { it.priority }

    val columnNames = arrayOf("Display Name", "Priority")
    val data = allModels.map { model ->
        arrayOf(model.displayName, model.priority)
    }.toTypedArray()

    val tableModel = object : DefaultTableModel(data, columnNames) {
        override fun getColumnClass(column: Int): Class<*> {
            return when (column) {
                0 -> String::class.java
                1 -> Integer::class.java
                else -> Any::class.java
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column == 1 // Only priority column is editable
        }

        override fun setValueAt(aValue: Any?, row: Int, column: Int) {
            if (column == 1 && aValue != null) {
                try {
                    val priority = when (aValue) {
                        is Int -> aValue
                        is String -> aValue.toInt()
                        else -> aValue.toString().toInt()
                    }
                    super.setValueAt(priority, row, column)
                } catch (e: NumberFormatException) {
                    // Invalid number, don't update the table
                    super.setValueAt(getValueAt(row, column), row, column) // Keep original value
                }
            }
        }
    }

    val table = JTable(tableModel).apply {
        setRowSelectionAllowed(true)
        setColumnSelectionAllowed(false)
        setCellSelectionEnabled(true)
        gridColor = Color.LIGHT_GRAY
        autoCreateRowSorter = true

        // Custom cell editor for priority column that selects all text on focus
        val priorityEditor = object : DefaultCellEditor(JTextField()) {
            override fun getTableCellEditorComponent(
                table: JTable?,
                value: Any?,
                isSelected: Boolean,
                row: Int,
                column: Int
            ): java.awt.Component {
                val textField = super.getTableCellEditorComponent(table, value, isSelected, row, column) as JTextField
                textField.selectAll()
                return textField
            }
        }

        // Set the custom editor for the priority column (column 1)
        columnModel.getColumn(1).cellEditor = priorityEditor
    }

    val scrollPane = JScrollPane(table)
    scrollPane.preferredSize = Dimension(400, 300)

    dialog.contentPane = scrollPane

    // Add commit task to save priority changes when dialog is closed with OK
    dialog.addCommitTask {
        for (row in 0 until tableModel.rowCount) {
            val newPriority = tableModel.getValueAt(row, 1) as Int
            allModels[row].priority = newPriority
        }
    }

    dialog.display()
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
