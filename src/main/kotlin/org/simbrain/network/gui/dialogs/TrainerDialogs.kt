package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.addSubnetworkAction
import org.simbrain.network.gui.nodes.subnetworkNodes.BackpropNetworkNode
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.SupervisedNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.util.*
import org.simbrain.util.table.*
import org.simbrain.util.widgets.ToggleButton
import java.awt.Cursor
import java.awt.Dialog
import java.awt.Dimension
import javax.swing.*

fun TrainingDataset.createDataSetPanel(parentDialog: StandardDialog? = null, applyAction: suspend DataSetPanel.(selectedRow: Int) -> Unit): DataSetPanel {
    
    fun createDataFrame(
        data: MutableList<MutableList<Double>>,
        rowNames: List<String>?,
        columnNames: List<String>?,
        expectedColumnCount: Int
    ): BasicDataFrame {
        // Always create explicit columns to simplify logic
        val columns = (0 until expectedColumnCount).map { i ->
            val columnName = columnNames?.getOrNull(i) ?: "Column ${i + 1}"
            Column(columnName, Column.DataType.DoubleType)
        }.toMutableList()
        
        return BasicDataFrame(
            data.map { it.map { value -> value as Any? }.toMutableList() }.toMutableList(), 
            columns
        ).also { dataFrame ->
            rowNames?.let { names -> dataFrame.rowNames = names }
            // No need to set columnNames again since we already set them in the Column objects
        }
    }
    
    val inputDataFrame = createDataFrame(inputs, inputRowNames, inputColumnNames, inputSize)
    val targetDataFrame = createDataFrame(targets, targetRowNames, targetColumnNames, targetSize)

    return DataSetPanel(inputDataFrame, targetDataFrame, applyAction = applyAction, parentDialog = parentDialog)
}

fun SimbrainTablePanel.applyCommonTrainerAttributes(parentDialog: StandardDialog? = null) {
    addAction(table.importCSVAction(parentComponent = parentDialog))
    addAction(table.exportCsv(parentComponent = parentDialog))
    addAction(table.randomizeAction)
    addAction(table.showBoxPlotAction)
    preferredSize = Dimension(400, 250)
}

class DataSetPanel(
    val inputDataFrame: BasicDataFrame,
    val targetDataFrame: BasicDataFrame,
    applyAction: suspend DataSetPanel.(selectedRow: Int) -> Unit,
    parentDialog: StandardDialog? = null
): JPanel() {

    val rowErrorJLabel = JLabel("")
    
    // Callback for when row counts change
    var onRowCountChanged: ((inputRowCount: Int, targetRowCount: Int) -> Unit)? = null

    val inputs = SimbrainTablePanel(inputDataFrame, false).apply {
        applyCommonTrainerAttributes(parentDialog)
        toolbar.addSeparator()
        val advanceRowCheckbox = JCheckBox("Auto advance").apply { isSelected = true }
        toolbar.add(
            table.createApplyAction("Apply inputs") {
                applyAction(it)
                if (advanceRowCheckbox.isSelected) {
                    incrementSelectedRow()
                }
            }
        )
        toolbar.add(advanceRowCheckbox)
        toolbar.addSeparator()
        toolbar.add(rowErrorJLabel)
    }

    val targets = SimbrainTablePanel(targetDataFrame, false).apply {
        applyCommonTrainerAttributes(parentDialog)
    }

    val addRemoveRows = AddRemoveRows(listOf(inputs.table, targets.table))

    init {
        layout = MigLayout("gap 0px 0px, ins 0")
        add(JLabel("Inputs"))
        add(JLabel("Targets"), "wrap")
        add(inputs)
        add(targets, "wrap")
        add(JLabel("Edit rows:"), "split 2")
        add(addRemoveRows)
        
        // Listen for table model changes to update validation
        val tableModelListener = javax.swing.event.TableModelListener {
            onRowCountChanged?.invoke(inputDataFrame.rowCount, targetDataFrame.rowCount)
        }
        
        inputDataFrame.addTableModelListener(tableModelListener)
        targetDataFrame.addTableModelListener(tableModelListener)
    }

}


/**
 * Generic training dialog for supervised learning.
 */
context(NetworkPanel)
fun SupervisedNetwork.getSupervisedTrainingDialog(): StandardDialog {
    val supervisedNetwork = this
    val parentWindow = SwingUtilities.getWindowAncestor(this@NetworkPanel)
    return StandardDialog(parentWindow as? JFrame, "Train Network").apply {

        isModal = true
        isAlwaysOnTop = false  // Ensure this is not always on top
        modalityType = Dialog.ModalityType.APPLICATION_MODAL
        
        // Ensure proper focus management for child dialogs
        addWindowFocusListener(object : java.awt.event.WindowAdapter() {
            override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                // When this dialog gains focus, ensure it stays in front
                toFront()
            }
        })

        // Run training algorithm
        val runControls = JPanel()
        runControls.layout = MigLayout("gap 0px 0px, ins 0")
        val trainer = SupervisedTrainer(network, supervisedNetwork)
        val trainerControls = TrainerControls(trainer, supervisedNetwork, this@NetworkPanel)

        suspend fun DataSetPanel.commonApplyAction(selectedRow: Int) {
            with(network) {
                inputLayer.setActivations(inputDataFrame.getRow<Double>(selectedRow).toDoubleArray())
                this@SupervisedNetwork.forwardPass()
                trainerConfig.lossFunction.scalarLoss(
                    outputLayer.activations,
                    targetDataFrame.getRow<Double>(selectedRow).toDoubleArray().toColumnVector()
                ).also { rowErrorJLabel.text = "${trainerConfig.lossFunction.shortName}: ${it.format(4)}" }
            }
        }

        fun createDataSetPanel(dataSet: TrainingDataset) = dataSet.createDataSetPanel(this@apply) { selectedRow ->
            commonApplyAction(
                selectedRow
            )
        }

        val trainingDataSetPanel = createDataSetPanel(trainingSet)
        val testingDataSetPanel = createDataSetPanel(testingSet)

        // Set up validation callbacks
        fun updateOverallValidation() {
            val trainingValid = trainingDataSetPanel.inputDataFrame.rowCount == trainingDataSetPanel.targetDataFrame.rowCount
            val testingValid = testingDataSetPanel.inputDataFrame.rowCount == testingDataSetPanel.targetDataFrame.rowCount
            
            trainerControls.updateValidationState(
                trainingInputRows = trainingDataSetPanel.inputDataFrame.rowCount,
                trainingTargetRows = trainingDataSetPanel.targetDataFrame.rowCount,
                testingInputRows = testingDataSetPanel.inputDataFrame.rowCount,
                testingTargetRows = testingDataSetPanel.targetDataFrame.rowCount,
                trainingValid = trainingValid,
                testingValid = testingValid
            )
        }
        
        trainingDataSetPanel.onRowCountChanged = { _, _ ->
            updateOverallValidation()
        }
        
        testingDataSetPanel.onRowCountChanged = { _, _ ->
            updateOverallValidation()
        }
        
        // Initial validation check
        updateOverallValidation()

        fun DataSetPanel.exportMatrixDataSet() = TrainingDataset(
            inputs.table.model.get2DDoubleList().toMutableListOfLists(),
            targets.table.model.get2DDoubleList().toMutableListOfLists(),
            inputSize = inputDataFrame.columnNames.size,
            targetSize = targetDataFrame.columnNames.size,
            inputRowNames = inputDataFrame.rowNames.map { it.toString() } as List<String>?,
            targetRowNames = targetDataFrame.rowNames.map { it.toString() } as List<String>?,
            inputColumnNames = inputDataFrame.columnNames,
            targetColumnNames = targetDataFrame.columnNames
        )

        fun syncDataSet() {
            trainingSet = trainingDataSetPanel.exportMatrixDataSet()
            testingSet = testingDataSetPanel.exportMatrixDataSet()
        }

        val dataSetTabPane = JTabbedPane().apply {
            addTab("Training data", trainingDataSetPanel)
            addTab("Testing data", testingDataSetPanel)
        }

        trainer.events.beginTraining.on(Dispatchers.Default) { syncDataSet() }
        runControls.add(trainerControls, "span, growx, wrap")
        runControls.add(dataSetTabPane, "wrap")

        addCommitTask { syncDataSet() }

        contentPane = runControls

        setAsDoneDialog()
    }
}

context(NetworkPanel)
fun getUnsupervisedTrainingPanel(unsupervisedNetwork: UnsupervisedNetwork, trainAction: context(Network)() -> Unit = {}): StandardDialog {
    val parentWindow = SwingUtilities.getWindowAncestor(this@NetworkPanel)
    return StandardDialog(parentWindow as? JFrame, "Train Network").apply dialog@ {

        isModal = true

        val mainPanel = JPanel().apply {
            layout = MigLayout("gap 0px 0px, ins 15")
        }

        val trainer = unsupervisedNetwork.trainer
        
        // Track the training job so we can cancel it when dialog closes
        var trainingJob: Job? = null

        val runControls = JPanel().apply { layout = MigLayout("nogrid") }

        val stepAction = createAction(
            description = "Iterate training once",
            iconPath = "menu_icons/Step.png",
        ) {
            with(network) {
                launch {
                    trainer.events.beginTraining.fire().await()
                    trainer.trainOnce(unsupervisedNetwork)
                    trainer.events.endTraining.fire()
                }
            }
        }

        runControls.add(JButton(stepAction))

        val runAction = createAction(
            name = "Run",
            description = "Run training algorithm",
            iconPath = "menu_icons/Play.png",
        ) {
            with(network) {
                trainingJob = launch {
                    trainer.startTraining(unsupervisedNetwork)
                }
            }
        }
        val stopAction = createAction(
            name = "Stop",
            description = "Stop training algorithm",
            iconPath = "menu_icons/Stop.png",
        ) {
            launch {
                trainer.stopTraining()
            }
            trainingJob?.cancel()
        }
        runControls.add(ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on {
                this@dialog.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                setAction("Stop")
            }
            trainer.events.endTraining.on {
                this@dialog.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                setAction("Run")
            }
        })

        val resetAction = createAction(
            name = "Randomize",
            description = "Randomize network and reset iterations",
            iconPath = "menu_icons/Rand.png",
        ) {
            unsupervisedNetwork.randomize(null)
            trainer.iteration = 0
            trainer.events.progressUpdated.fire("Iteration" to trainer.iteration)
        }
        val resetButton = JButton(resetAction)
        resetButton.hideActionText = true
        runControls.add(resetButton)

        val labelPanel = LabelledItemPanel()
        val iterationsLabel = JLabel(trainer.iteration.toString())
        labelPanel.addItem("Iterations:", iterationsLabel)
        runControls.add(labelPanel, "wrap")

        trainer.events.progressUpdated.on(Dispatchers.Swing, wait = true) {
            iterationsLabel.text = "" + trainer.iteration
        }

        runControls.layout = MigLayout("gap 0px 0px, ins 0")

        val preferencesButton = JButton(createAction(
            name = "Trainer properties",
            description = "Edit trainer preferences",
            iconPath = "menu_icons/Tools.png",
        ) {
            trainer.createEditorDialog().display()
        })
        runControls.add(preferencesButton)

        // Create data frame for unsupervised data with explicit columns like DataSetPanel
        fun createUnsupervisedDataFrame(data: MutableList<MutableList<Double>>): BasicDataFrame {
            val inputSize = unsupervisedNetwork.inputLayer.size
            val columns = (0 until inputSize).map { i ->
                Column("Input ${i + 1}", Column.DataType.DoubleType)
            }.toMutableList()
            
            return BasicDataFrame(
                data.copy().map { it.map { value -> value as Any? }.toMutableList() }.toMutableList(),
                columns
            )
        }

        // Create data panels for training and testing data
        fun createUnsupervisedDataPanel(data: MutableList<MutableList<Double>>) = object : JPanel() {
            val dataFrame = createUnsupervisedDataFrame(data)
            val tablePanel = SimbrainTablePanel(dataFrame, false).apply {
                applyCommonTrainerAttributes(this@dialog)
                toolbar.addSeparator()
                val advanceRowCheckbox = JCheckBox("Auto advance").apply { isSelected = true }
                toolbar.add(
                    table.createApplyAction("Apply inputs") {
                        unsupervisedNetwork.inputLayer.setActivations(dataFrame.getRow<Double>(it).toDoubleArray())
                        trainAction(network)
                        if (advanceRowCheckbox.isSelected) {
                            incrementSelectedRow()
                        }
                    }
                )
                toolbar.add(advanceRowCheckbox)
            }
            val addRemoveRows = AddRemoveRows(listOf(tablePanel.table))
            init {
                layout = MigLayout("gap 0px 0px, ins 0")
                add(tablePanel, "wrap")
                add(addRemoveRows)
            }
        }

        val trainingDataPanel = createUnsupervisedDataPanel(unsupervisedNetwork.trainingData)
        val testingDataPanel = createUnsupervisedDataPanel(unsupervisedNetwork.testingData)

        fun syncDataSet() {
            // Extract data from the table panels and update the network's data
            unsupervisedNetwork.trainingData = trainingDataPanel.tablePanel.table.model.get2DDoubleList().toMutableListOfLists()
            unsupervisedNetwork.testingData = testingDataPanel.tablePanel.table.model.get2DDoubleList().toMutableListOfLists()
        }

        val dataSetTabPane = JTabbedPane().apply {
            addTab("Training data", trainingDataPanel)
            addTab("Testing data", testingDataPanel)
        }

        trainer.events.beginTraining.on(Dispatchers.Default) { syncDataSet() }

        mainPanel.add(runControls, "wrap, gapbottom 10px")
        mainPanel.add(dataSetTabPane, "span, grow")

        addCommitTask { syncDataSet() }
        addCloseTask {
            trainingJob?.cancel()
        }

        contentPane = mainPanel
        setAsDoneDialog()
    }
}

context(NetworkPanel)
fun UnsupervisedNetwork.makeTrainerPanel(): StandardDialog = getUnsupervisedTrainingPanel(this) {
    this.trainOnCurrentPattern()
}

fun NetworkPanel.showSRNCreationDialog(): StandardDialog {

    val creator = SRNNetwork.SRNCreator(
        network.placementManager.lastClickedLocation
    )
    return creator.createEditorDialog {
        addSubnetworkAction(this@NetworkPanel) { creator.create() }
    }

}

fun main() {
    val networkComponent = NetworkComponent("")
    val np = NetworkPanel(networkComponent)
    val result = with(networkComponent.network) {
        val backpropNetwork = BackpropNetwork(intArrayOf(50, 20, 50))
        addNetworkModelAsync(backpropNetwork)
        backpropNetwork
    }
    BackpropNetworkNode(np, result).propertyDialog.display()
}

context(Network)
fun UnsupervisedNetwork.createTrainOnPatternAction() = createAction(
    name = "Train on current pattern...",
    description = "Train network on current pattern for specified number of iterations.",
) {
    val iterations: Int? = showNumericInputDialog("Iterations: ", NetworkPreferences.numberOfIterations)?.toInt()
    if (iterations != null) {
        NetworkPreferences.numberOfIterations = iterations
        runWithProgressWindow(iterations, batchSize = 10) {
            trainOnCurrentPattern()
        }
    }
}

context(NetworkPanel)
fun UnsupervisedNetwork.createTrainOnceOnPatternAction() = createAction(
    name = "Train once on current pattern",
    description = "Train network for one iteration on current pattern.",
    keyboardShortcut = KeyCombination('T')
) {
    with(network) {trainOnCurrentPattern()}
}