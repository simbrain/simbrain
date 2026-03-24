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
import org.simbrain.util.propertyeditor.EditableObject
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

fun NetworkPanel.showAddNeuronArrayDialog(sourceNeuronArray: NeuronArray) {
    NeuronArray.CreationTemplate().apply {
        numNodes = (sourceNeuronArray.size / 2).coerceAtLeast(1)
    }.createEditorDialog {
        addNeuronArray(sourceNeuronArray, it)
    }.also {
        it.title = "Add Neuron Array from ${sourceNeuronArray.displayName}"
    }.display()
}

internal fun NetworkPanel.addNeuronArray(
    sourceNeuronArray: NeuronArray,
    template: NeuronArray.CreationTemplate
): Pair<NeuronArray, WeightMatrix> {
    val targetNeuronArray = template.create()
    targetNeuronArray.shouldBePlaced = false
    val connector = WeightMatrix(sourceNeuronArray, targetNeuronArray)
    network.addNetworkModelAsync(targetNeuronArray, usePlacementManager = false)
    targetNeuronArray.setLocation(sourceNeuronArray.locationX, sourceNeuronArray.locationY - 400)
    network.addNetworkModelAsync(connector, usePlacementManager = false)
    undoManager.addUndoableAction(
        description = "Add neuron array from ${sourceNeuronArray.id}",
        undo = { network.deleteModels(listOf(connector, targetNeuronArray)) },
        redo = {
            network.addNetworkModel(targetNeuronArray, usePlacementManager = false, useAutoAssignedId = false)
            targetNeuronArray.setLocation(sourceNeuronArray.locationX, sourceNeuronArray.locationY - 400)
            network.addNetworkModel(connector, usePlacementManager = false, useAutoAssignedId = false)
        }
    )
    return targetNeuronArray to connector
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

fun NetworkPanel.showTensorCreationDialog() {
    TensorLayer.CreationTemplate().createEditorDialog {
        val tensor = it.create()
        network.addNetworkModelAsync(tensor)
        undoManager.addUndoableAction(
            description = "Create tensor ${tensor.id}",
            undo = { tensor.delete() },
            redo = { network.addNetworkModel(tensor, usePlacementManager = false, useAutoAssignedId = false) }
        )
    }.also {
        it.title = "Create Tensor"
    }.display()
}

/**
 * Dialog for adding a convolution layer (target Tensor + ConvolutionConnector) from a source Tensor.
 */
fun NetworkPanel.showAddConvLayerDialog(sourceTensorLayer: TensorLayer) {
    val template = ConvLayerTemplate()
    template.createEditorDialog {
        addConvLayer(sourceTensorLayer, it)
    }.also {
        it.title = "Add Conv Layer from ${sourceTensorLayer.displayName}"
    }.display()
}

/**
 * Dialog for adding a pooling layer (target Tensor + PoolingConnector) from a source Tensor.
 */
fun NetworkPanel.showAddPoolLayerDialog(sourceTensorLayer: TensorLayer) {
    val template = PoolLayerTemplate()
    template.createEditorDialog {
        addPoolLayer(sourceTensorLayer, it)
    }.also {
        it.title = "Add Pool Layer from ${sourceTensorLayer.displayName}"
    }.display()
}

internal fun NetworkPanel.addConvLayer(sourceTensorLayer: TensorLayer, template: ConvLayerTemplate): Pair<TensorLayer, ConvolutionConnector> {
    val outputShape = sourceTensorLayer.shape.convOutputShape(
        template.kernelSize, template.stride, template.padding, template.numFilters
    )
    val targetTensorLayer = TensorLayer(outputShape)
    targetTensorLayer.activationFunction = template.activation
    targetTensorLayer.shouldBePlaced = false
    val connector = ConvolutionConnector(
        sourceTensorLayer, targetTensorLayer,
        template.kernelSize, template.numFilters, template.stride, template.padding
    )
    network.addNetworkModelAsync(targetTensorLayer, usePlacementManager = false)
    targetTensorLayer.setLocation(sourceTensorLayer.locationX, sourceTensorLayer.locationY - 400)
    network.addNetworkModelAsync(connector, usePlacementManager = false)
    undoManager.addUndoableAction(
        description = "Add conv layer from ${sourceTensorLayer.id}",
        undo = { network.deleteModels(listOf(targetTensorLayer)) },
        redo = {
            network.addNetworkModel(targetTensorLayer, usePlacementManager = false, useAutoAssignedId = false)
            targetTensorLayer.setLocation(sourceTensorLayer.locationX, sourceTensorLayer.locationY - 400)
            network.addNetworkModel(connector, usePlacementManager = false, useAutoAssignedId = false)
        }
    )
    return targetTensorLayer to connector
}

internal fun NetworkPanel.addPoolLayer(sourceTensorLayer: TensorLayer, template: PoolLayerTemplate): Pair<TensorLayer, PoolingConnector> {
    val outputShape = sourceTensorLayer.shape.poolOutputShape(template.poolSize, template.stride)
    val targetTensorLayer = TensorLayer(outputShape)
    targetTensorLayer.shouldBePlaced = false
    val connector = PoolingConnector(sourceTensorLayer, targetTensorLayer, template.poolSize, template.stride, template.poolingType)
    network.addNetworkModelAsync(targetTensorLayer, usePlacementManager = false)
    targetTensorLayer.setLocation(sourceTensorLayer.locationX, sourceTensorLayer.locationY - 400)
    network.addNetworkModelAsync(connector, usePlacementManager = false)
    undoManager.addUndoableAction(
        description = "Add pool layer from ${sourceTensorLayer.id}",
        undo = { network.deleteModels(listOf(targetTensorLayer)) },
        redo = {
            network.addNetworkModel(targetTensorLayer, usePlacementManager = false, useAutoAssignedId = false)
            targetTensorLayer.setLocation(sourceTensorLayer.locationX, sourceTensorLayer.locationY - 400)
            network.addNetworkModel(connector, usePlacementManager = false, useAutoAssignedId = false)
        }
    )
    return targetTensorLayer to connector
}

/**
 * Add a flatten layer: creates a NeuronArray sized to the source Tensor's total element count
 * and a FlattenConnector linking them. No dialog needed since the size is fully determined.
 */
fun NetworkPanel.addFlattenLayer(sourceTensorLayer: TensorLayer) {
    val flatSize = sourceTensorLayer.shape.size
    val targetArray = NeuronArray(flatSize)
    targetArray.shouldBePlaced = false
    val connector = FlattenConnector(sourceTensorLayer, targetArray)
    network.addNetworkModelAsync(targetArray, usePlacementManager = false)
    targetArray.setLocation(sourceTensorLayer.locationX, sourceTensorLayer.locationY - 400)
    network.addNetworkModelAsync(connector, usePlacementManager = false)
    undoManager.addUndoableAction(
        description = "Add flatten layer from ${sourceTensorLayer.id}",
        undo = { network.deleteModels(listOf(connector, targetArray)) },
        redo = {
            network.addNetworkModel(targetArray, usePlacementManager = false, useAutoAssignedId = false)
            targetArray.setLocation(sourceTensorLayer.locationX, sourceTensorLayer.locationY - 400)
            network.addNetworkModel(connector, usePlacementManager = false, useAutoAssignedId = false)
        }
    )
}

/**
 * Template for convolution layer creation dialog.
 */
class ConvLayerTemplate : EditableObject {
    @UserParameter(label = "Kernel Size", description = "Spatial size of kernel", minimumValue = 1.0, order = 1)
    var kernelSize = 3

    @UserParameter(label = "Num Filters", description = "Number of output filters", minimumValue = 1.0, order = 2)
    var numFilters = 8

    @UserParameter(label = "Stride", description = "Convolution stride", minimumValue = 1.0, order = 3)
    var stride = 1

    @UserParameter(
        label = "Padding",
        description = "Valid keeps the kernel inside the input with no padding, while Same pads the input so the kernel can extend beyond the original edges.",
        order = 4
    )
    var padding = Padding.SAME

    @UserParameter(label = "Activation", description = "Activation function for output tensor", order = 5)
    var activation = TensorActivation.RELU

    override val name = "Convolution Layer"
}

/**
 * Template for pooling layer creation dialog.
 */
class PoolLayerTemplate : EditableObject {
    @UserParameter(label = "Pool Size", description = "Spatial size of pooling window", minimumValue = 1.0, order = 1)
    var poolSize = 2

    @UserParameter(label = "Stride", description = "Pooling stride", minimumValue = 1.0, order = 2)
    var stride = 2

    @UserParameter(label = "Pooling Type", description = "MAX or AVERAGE pooling", order = 3)
    var poolingType = PoolingType.MAX

    override val name = "Pooling Layer"
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

fun NetworkPanel.createNeuronCollectionDialog(neuronGroup: NeuronCollection) = neuronGroup.createEditorDialog()

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

fun NetworkPanel.showNetworkDebugInfoDialog() {
    val sb = StringBuilder()

    fun loc(model: LocatableModel) = "(${model.location.x.toInt()}, ${model.location.y.toInt()})"

    fun appendModel(model: NetworkModel, indent: String = "") {
        val type = model::class.simpleName ?: "Unknown"
        val name = model.displayName
        val line = buildString {
            append("$indent[$type] $name")
            when (model) {
                is NeuronArray -> {
                    append("  shape: ${model.shapeString}")
                    append("  loc: ${loc(model)}")
                }
                is TensorLayer -> {
                    append("  shape: ${model.shape}")
                    append("  loc: ${loc(model)}")
                }
                is NeuronCollection -> {
                    append("  neurons: ${model.neuronList.size}")
                    append("  loc: ${loc(model)}")
                }
                is WeightMatrix -> {
                    append("  shape: ${model.weights.nrow()} x ${model.weights.ncol()}")
                }
                is SynapseGroup -> {
                    append("  synapses: ${model.size()}")
                }
                is TensorConnector -> {
                    append("  ${model::class.simpleName}")
                }
                is NetworkTextObject -> {
                    val preview = model.text.replace('\n', ' ').take(40)
                    append("  \"$preview\"")
                    append("  loc: ${loc(model)}")
                }
                is Connector -> {
                    // generic fallback for other Connector subclasses
                }
                is Neuron -> {
                    append("  rule: ${model.type}")
                    append("  loc: ${loc(model)}")
                }
            }
        }
        sb.appendLine(line)
    }

    sb.appendLine("=== Network Debug Info ===")
    sb.appendLine("Time: ${network.timeLabel}  |  Time step: ${network.timeStep}")
    sb.appendLine()

    val freeNeurons = network.freeNeurons.toList()
    val freeSynapses = network.freeSynapses.toList()
    val otherModels = network.allModels.filter { it !is Neuron && it !is Synapse }

    sb.appendLine("--- Models (${network.allModels.size}) ---")
    otherModels.forEach { model ->
        appendModel(model)
        if (model is org.simbrain.network.subnetworks.Subnetwork) {
            model.modelList.all.forEach { child -> appendModel(child, "  ") }
        }
    }

    if (freeNeurons.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("--- Free neurons (${freeNeurons.size}) ---")
        freeNeurons.forEach { appendModel(it) }
    }

    if (freeSynapses.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("--- Free synapses (${freeSynapses.size}) ---")
        freeSynapses.forEach { appendModel(it) }
    }

    showMessageDialog(sb.toString(), "Network Debug Info", rows = 30, columns = 60)
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
