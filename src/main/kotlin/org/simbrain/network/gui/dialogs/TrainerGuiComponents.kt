package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.events.TrainingStats
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.trainers.SupervisedNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotActions
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.*
import org.simbrain.util.table.*
import org.simbrain.util.widgets.ToggleButton
import smile.math.matrix.Matrix
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Controls used by Supervised learning dialogs.
 */
class TrainerControls(private val trainer: SupervisedTrainer, supervisedNetwork: SupervisedNetwork, networkPanel: NetworkPanel): JPanel(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + job

    val iterationsLabel = JLabel(trainer.iteration.toString())

    // Validation state for row count matching
    private var isValidationEnabled = true
    
    internal val runAction = createAction(
        name = "Run",
        iconPath ="menu_icons/Play.png",
        description = "Iterate training until stop button is pressed"
    ) {
        trainer.startTraining()
    }

    private val stopAction = createAction(
        name = "Stop",
        iconPath = "menu_icons/Stop.png",
        description = "Stop training",
    ) {
        trainer.stopTraining()
    }

    internal val stepAction = createAction(
        description = "Iterate training once",
        iconPath =  "menu_icons/Step.png",
        initBlock = {
            trainer.events.beginTraining.on {
                isEnabled = false
            }
            trainer.events.endTraining.on {
                isEnabled = isValidationEnabled
            }
        }
    ) {
       trainer.trainOnce()
    }

    private val initializeParameters = createAction(
        name = "Init parameters",
        description = "Initialize weights using selected strategy and biases using randomizer from network preferences",
        iconPath = "menu_icons/Rand.png",
    ) {
        trainer.randomize()
    }

    private val trainerPropsAction = createAction(
        name = "Trainer properties",
        description = "Edit trainer properties",
        iconPath = "menu_icons/Tools.png",
    ) {
        supervisedNetwork.trainerConfig.createEditorDialog {
            (it.updateType as? SupervisedTrainer.UpdateMethod.Batch)?.let { batchUpdate ->
                if (batchUpdate.batchSize !in 1..supervisedNetwork.trainingSet.size) {
                    batchUpdate.batchSize = batchUpdate.batchSize.coerceIn(1, supervisedNetwork.trainingSet.size)
                    showWarningDialog("Batch size exceeds training set size; setting to ${batchUpdate.batchSize}")
                }
            }
            trainer.events.errorUpdated.fire(TrainingStats(trainer.lastTrainingError, null, trainer.lastTrainingAccuracy, trainer.lastTestingAccuracy))
        }.display()
    }

    // Store references to buttons for validation updates
    private lateinit var stepButton: JButton
    private lateinit var runStopToggleButton: ToggleButton
    
    /**
     * Updates the validation state based on input and target table row counts for both training and testing data.
     * Disables training buttons if row counts don't match.
     */
    fun updateValidationState(
        trainingInputRows: Int, 
        trainingTargetRows: Int,
        testingInputRows: Int,
        testingTargetRows: Int,
        trainingValid: Boolean,
        testingValid: Boolean
    ) {
        val isValid = trainingValid && testingValid
        isValidationEnabled = isValid
        
        // Update button states
        stepAction.isEnabled = isValid && !trainer.isRunning
        runAction.isEnabled = isValid
        
        // Update tooltips to show validation message
        val validationMessage = when {
            isValid -> "Iterate training once"
            !trainingValid && !testingValid -> 
                "Cannot train: Training data (${trainingInputRows} inputs, ${trainingTargetRows} targets) and testing data (${testingInputRows} inputs, ${testingTargetRows} targets) have mismatched row counts. All tables must have matching row counts."
            !trainingValid -> 
                "Cannot train: Training data has ${trainingInputRows} input rows and ${trainingTargetRows} target rows. Row counts must match."
            !testingValid -> 
                "Cannot train: Testing data has ${testingInputRows} input rows and ${testingTargetRows} target rows. Row counts must match."
            else -> "Cannot train: Row count validation failed"
        }
        stepAction.putValue(Action.SHORT_DESCRIPTION, validationMessage)
        
        val runValidationMessage = when {
            isValid -> "Iterate training until stop button is pressed"
            !trainingValid && !testingValid -> 
                "Cannot train: Both training and testing data have mismatched input/target row counts"
            !trainingValid -> 
                "Cannot train: Training data input and target tables must have the same number of rows"
            !testingValid -> 
                "Cannot train: Testing data input and target tables must have the same number of rows"
            else -> "Cannot train: Row count validation failed"
        }
        runAction.putValue(Action.SHORT_DESCRIPTION, runValidationMessage)
    }

    init {
        
        // Cancel the trainer's coroutine scope when this component is disposed
        onWindowClose {
            trainer.job.cancel()
            job.cancel()
        }

        val errorPlotPanel = JPanel().apply {
            layout = MigLayout("ins 0, gap 0px 0px, fillx, wrap")
            val errorPlot = ErrorTimeSeries(trainer)
            add(errorPlot, "growx, wrap")

            val buttonPanel = JPanel(MigLayout("ins 0, gap 0px 0px"))
            buttonPanel.add(JButton(TimeSeriesPlotActions.getClearGraphAction(errorPlot.graphPanel)))
            buttonPanel.add(JButton(TimeSeriesPlotActions.getPropertiesDialogAction(errorPlot.graphPanel)))
            add(buttonPanel, "wrap, align center, gapbottom 20px")
        }

        val runTools = JPanel().apply { layout = MigLayout("nogrid ") }
        stepButton = JButton(stepAction)
        runTools.add(stepButton)
        runStopToggleButton = ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on {
                this@TrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                setAction("Stop")
            }
            trainer.events.endTraining.on {
                this@TrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                setAction("Run")
            }
        }
        runTools.add(runStopToggleButton)
        val initParamsButton = JButton(initializeParameters)
        initParamsButton.hideActionText = true
        runTools.add(initParamsButton)
        runTools.add(JButton(trainerPropsAction), "wrap")
        val labelPanel = LabelledItemPanel()
        labelPanel.addItem("Iterations:", iterationsLabel)
        labelPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    trainer.iteration = 0
                    trainer.events.iterationReset.fire()
                    iterationsLabel.text = trainer.iteration.toString()
                }
            }
        })
        val trainingErrorValue = JLabel(trainer.lastTrainingError.roundToString(4))
        fun errorDescriptionString() = "Mean Error (${supervisedNetwork.trainerConfig.updateType}; ${supervisedNetwork.trainerConfig.lossFunction.shortName})"
        val trainingErrorLabel = labelPanel.addItem("Training ${errorDescriptionString()}", trainingErrorValue)
        
        val testingErrorValue = JLabel("N/A")
        val testingErrorLabel = labelPanel.addItem("Testing ${errorDescriptionString()}", testingErrorValue)
        
        val trainingAccuracyValue = JLabel(trainer.lastTrainingAccuracy?.let { "${(it * 100).format(1)}%" } ?: "N/A")
        val trainingAccuracyLabel = labelPanel.addItem("Training Accuracy:", trainingAccuracyValue)
        
        val testingAccuracyValue = JLabel(trainer.lastTestingAccuracy?.let { "${(it * 100).format(1)}%" } ?: "N/A")
        val testingAccuracyLabel = labelPanel.addItem("Testing Accuracy:", testingAccuracyValue)

        fun formatStepSize(value: Double?) = value?.let { String.format("%.3g", it) } ?: "N/A"
        val stepSizeValue = JLabel(formatStepSize(trainer.lastEffectiveStepSize)).apply {
            toolTipText = "RMS of the optimizer's per-parameter update last iteration. " +
                "For Adam in steady state ≈ learning rate; for SGD ≈ lr·||g||. " +
                "Near 0 ⇒ optimizer is flat-lining; very large ⇒ updates may be diverging."
        }
        labelPanel.addItem("Effective Step Size:", stepSizeValue)

        fun updateLabelVisibility() {
            val showTestingLoss = supervisedNetwork.trainerConfig.testConfiguration.enabled
            val showTrainingAccuracy = supervisedNetwork.trainerConfig.computeAccuracy
            val showTestingAccuracy = supervisedNetwork.trainerConfig.computeAccuracy && 
                                           supervisedNetwork.trainerConfig.testConfiguration.enabled
            
            testingErrorLabel.isVisible = showTestingLoss
            testingErrorValue.isVisible = showTestingLoss
            trainingAccuracyLabel.isVisible = showTrainingAccuracy
            trainingAccuracyValue.isVisible = showTrainingAccuracy
            testingAccuracyLabel.isVisible = showTestingAccuracy
            testingAccuracyValue.isVisible = showTestingAccuracy
        }
        
        updateLabelVisibility()
        
        runTools.add(labelPanel)

        trainer.events.errorUpdated.on(Dispatchers.Swing) { trainingStats ->
            iterationsLabel.text = "" + trainer.iteration
            trainingErrorValue.text = "" + trainingStats.trainingError.format(4)
            trainingErrorLabel.text = "Training ${errorDescriptionString()}"
            
            trainingStats.testingError?.let { testingError ->
                testingErrorValue.text = "" + testingError.format(4)
                testingErrorLabel.text = "Testing ${errorDescriptionString()}"
            }
            
            updateLabelVisibility()
            
            trainingStats.trainingAccuracy?.let { accuracy ->
                trainingAccuracyValue.text = "${(accuracy * 100).format(1)}%"
            }
            
            trainingStats.testingAccuracy?.let { accuracy ->
                testingAccuracyValue.text = "${(accuracy * 100).format(1)}%"
            }

            stepSizeValue.text = formatStepSize(trainingStats.effectiveStepSize)
        }

        layout = MigLayout("ins 0, gap 0px 0px")
        add(runTools)
        add(errorPlotPanel, "grow, gapbottom 0px")
    }

}


class ErrorTimeSeries(trainer: SupervisedTrainer) : JPanel() {

    val graphPanel: TimeSeriesPlotPanel

    init {
        layout = MigLayout("ins 0, gap 0px 0px")

        // TODO: Consider passing some of these values in
        val model = TimeSeriesModel()
        model.timeSupplier = { trainer.iteration }
        model.rangeLowerBound = 0.0
        model.rangeUpperBound = 5.0
        model.fixedWidth = true
        model.windowSize = 1000
        model.isAutoRange = true
        model.useAutoRangeMinimumUpperBound = true
        model.autoRangeMinimumUpperBound = 1.0
        graphPanel = TimeSeriesPlotPanel(model)
        graphPanel.chartPanel.chart.setTitle("")
        graphPanel.chartPanel.chart.xyPlot.domainAxis.label = "Iterations"
        graphPanel.chartPanel.chart.xyPlot.rangeAxis.label = "Error"
        graphPanel.preferredSize = Dimension(graphPanel.preferredSize.width, 200)

        graphPanel.removeAllButtonsFromToolBar()

        add(graphPanel, "growx, growy, push") // Make graph fill the panel

        model.addTimeSeries("Training Error")

        trainer.events.errorUpdated.on(Dispatchers.Swing) { trainingStats ->
            model.addData(0, trainer.iteration.toDouble(), trainingStats.trainingError)
            trainingStats.testingError?.let {
                if (model.timeSeriesList.size == 1) {
                    model.addTimeSeries("Testing Error")
                }
                model.addData(1, trainer.iteration.toDouble(), it)
            }
        }

        trainer.events.iterationReset.on(Dispatchers.Swing, wait = true) {
            model.clearData()
        }
    }
}



/**
 * Default config for a matrix editor.
 */
class MatrixEditor(matrix: Matrix, rowNames: List<String>? = null, columnNames: List<String>? = null) : SimbrainTablePanel(
    MatrixDataFrame(matrix), false
) {
    init {
        addAction(table.importCsv)
        addAction(table.exportCsv())
        addAction(table.randomizeAction)
        addAction(table.showBoxPlotAction)
        preferredSize = Dimension(400, 250)
        if (columnNames != null) {
            model.columnNames = columnNames
        }
        if (rowNames != null) {
            model.rowNames = rowNames
        }
    }
}

/**
 * Panel with buttons to add or removes rows from the end of the provided tables
 */
class AddRemoveRows(val tables: List<SimbrainJTable>) : JPanel() {

    init {
        layout = MigLayout("ins 0, gap 2px")
        // Add row
        add(JButton().apply {
            icon = ResourceManager.getSmallIcon("menu_icons/AddTableRow.png")
            toolTipText = "Insert row at bottom of input and target tables"
            addActionListener {
                tables.forEach { it.model.insertRowAtBottom() }
            }
        })
        add(JButton().apply {
            icon = ResourceManager.getSmallIcon("menu_icons/DeleteTableRow.png")
            toolTipText = "Delete last row of input and target tables"
            addActionListener {
                tables.forEach { it.model.deleteLastRow() }
            }
        })
        // Set number of rows
        add(JButton().apply {
            icon = ResourceManager.getSmallIcon("menu_icons/PenToSquare.png")
            toolTipText = "Set number of rows in input and target tables"
            addActionListener {
                val currentRows = if (tables.isNotEmpty()) tables[0].model.rowCount else 0
                val input = javax.swing.JOptionPane.showInputDialog(
                    this@AddRemoveRows,
                    "Enter number of rows:",
                    "Set Number of Rows",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    currentRows.toString()
                ) as String?
                
                input?.let { inputStr ->
                    try {
                        val numRows = inputStr.toInt()
                        if (numRows >= 0) {
                            tables.forEach { it.model.setNumRows(numRows) }
                        } else {
                            javax.swing.JOptionPane.showMessageDialog(
                                this@AddRemoveRows,
                                "Number of rows must be non-negative",
                                "Invalid Input",
                                javax.swing.JOptionPane.ERROR_MESSAGE
                            )
                        }
                    } catch (e: NumberFormatException) {
                        javax.swing.JOptionPane.showMessageDialog(
                            this@AddRemoveRows,
                            "Please enter a valid integer",
                            "Invalid Input",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        })
    }
}