package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.events.TrainerEvents
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
            trainer.events.beginTraining.on(Dispatchers.Swing) {
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
            trainer.events.errorUpdated.fireAsync(TrainingStats(trainer.lastTrainingError, null, trainer.lastTrainingAccuracy, trainer.lastTestingAccuracy))
            supervisedNetwork.onTrainerConfigChanged()
            onTrainerConfigEdited?.invoke()
        }.display()
    }

    /**
     * Run after the properties dialog commits, for parts of the training dialog that are drawn from the
     * config but are not owned here, such as the data tables' window banding. A plain callback rather than
     * an event, since this class owns the action and the dialog owns everything that has to react to it.
     */
    var onTrainerConfigEdited: (() -> Unit)? = null

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

            val buttonPanel = JPanel(MigLayout("ins 0, gap 8px"))
            buttonPanel.add(JButton(TimeSeriesPlotActions.getClearGraphAction(errorPlot.graphPanel)))
            buttonPanel.add(JButton(TimeSeriesPlotActions.getPropertiesDialogAction(errorPlot.graphPanel)))
            add(buttonPanel, "wrap, align center, gapbottom 20px")
        }

        val runTools = JPanel().apply { layout = MigLayout("nogrid, ins 4, gap 8px") }
        stepButton = JButton(stepAction)
        runTools.add(stepButton)
        runStopToggleButton = ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on(Dispatchers.Swing) {
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

        // Effective Step Size is always shown, so keep it grouped with the other always-visible
        // rows (Iterations, Training Error) instead of after the conditional testing/accuracy rows,
        // which would leave it detached from the text above when those are hidden.
        fun formatStepSize(value: Double?) = value?.let { String.format("%.3g", it) } ?: "N/A"
        val stepSizeValue = JLabel(formatStepSize(trainer.lastEffectiveStepSize)).apply {
            toolTipText = "RMS of the optimizer's per-parameter update last iteration. " +
                "For Adam in steady state ≈ learning rate; for SGD ≈ lr·||g||. " +
                "Near 0 ⇒ optimizer is flat-lining; very large ⇒ updates may be diverging."
        }
        labelPanel.addItem("Effective Step Size:", stepSizeValue)

        val testingErrorValue = JLabel("N/A")
        val testingErrorLabel = labelPanel.addItem("Testing ${errorDescriptionString()}", testingErrorValue)

        val trainingAccuracyValue = JLabel(trainer.lastTrainingAccuracy?.let { "${(it * 100).format(1)}%" } ?: "N/A")
        val trainingAccuracyLabel = labelPanel.addItem("Training Accuracy:", trainingAccuracyValue)

        val testingAccuracyValue = JLabel(trainer.lastTestingAccuracy?.let { "${(it * 100).format(1)}%" } ?: "N/A")
        val testingAccuracyLabel = labelPanel.addItem("Testing Accuracy:", testingAccuracyValue)

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

        layout = MigLayout("ins 0, gap 12px 0px")
        add(runTools)
        add(errorPlotPanel, "grow, gapbottom 0px")
    }

}


class ErrorTimeSeries(events: TrainerEvents, iterationSupplier: () -> Int) : JPanel() {

    constructor(trainer: SupervisedTrainer) : this(trainer.events, { trainer.iteration })

    val graphPanel: TimeSeriesPlotPanel

    private val errorRemover: () -> Unit

    private val resetJob: Job

    init {
        layout = MigLayout("ins 0, gap 0px 0px")

        // TODO: Consider passing some of these values in
        val model = TimeSeriesModel()
        model.timeSupplier = iterationSupplier
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
        graphPanel.seriesRemovalEnabled = false

        add(graphPanel, "growx, growy, push") // Make graph fill the panel

        model.addTimeSeries("Training Error")

        errorRemover = events.errorUpdated.on(Dispatchers.Swing) { trainingStats ->
            model.addData(0, iterationSupplier().toDouble(), trainingStats.trainingError)
            trainingStats.testingError?.let {
                if (model.timeSeriesList.size == 1) {
                    model.addTimeSeries("Testing Error")
                }
                model.addData(1, iterationSupplier().toDouble(), it)
            }
        }

        resetJob = events.iterationReset.on(Dispatchers.Swing) {
            model.clearData()
        }
    }

    /** Detaches the plot from the trainer's events; for dialogs whose trainer outlives them. */
    fun dispose() {
        errorRemover()
        resetJob.cancel()
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
class AddRemoveRows(
    val tables: List<SimbrainJTable>,
    private val rowsPerOperation: () -> Int = { 1 },
    private val unitName: String = "row"
) : JPanel() {

    init {
        layout = MigLayout("ins 0, gap 8px")
        add(JButton().apply {
            icon = ResourceManager.getSmallIcon("menu_icons/AddTableRow.png")
            toolTipText = "Add ${if (unitName == "row") "a" else "one"} $unitName to input and target tables"
            addActionListener {
                repeat(rowsPerOperation().coerceAtLeast(1)) { tables.forEach { it.model.insertRowAtBottom() } }
            }
        })
        add(JButton().apply {
            icon = ResourceManager.getSmallIcon("menu_icons/DeleteTableRow.png")
            toolTipText = "Remove the last $unitName from input and target tables"
            addActionListener {
                val count = rowsPerOperation().coerceAtLeast(1)
                repeat(count) { tables.forEach { table -> if (table.model.rowCount > 0) table.model.deleteLastRow() } }
            }
        })
        add(JButton().apply {
            icon = ResourceManager.getSmallIcon("menu_icons/PenToSquare.png")
            toolTipText = "Set number of $unitName in input and target tables"
            addActionListener {
                val currentRows = if (tables.isNotEmpty()) tables[0].model.rowCount else 0
                val rowsPerUnit = rowsPerOperation().coerceAtLeast(1)
                val currentUnits = currentRows / rowsPerUnit
                val input = showInputDialog("Enter number of $unitName:", currentUnits.toString())
                
                input?.let { inputStr ->
                    try {
                        val numUnits = inputStr.toInt()
                        if (numUnits >= 0) {
                            tables.forEach { it.model.setNumRows(numUnits * rowsPerUnit) }
                        } else {
                            showErrorDialog("Number of $unitName must be non-negative", "Invalid Input")
                        }
                    } catch (e: NumberFormatException) {
                        showErrorDialog("Please enter a valid integer", "Invalid Input")
                    }
                }
            }
        })
    }
}
