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

    private fun currentStats() = TrainingStats(
        trainingError = trainer.lastTrainingError,
        trainingAccuracy = trainer.lastTrainingAccuracy,
        testingAccuracy = trainer.lastTestingAccuracy,
        effectiveStepSize = trainer.lastEffectiveStepSize
    )

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
                    showMessage("Batch size exceeds training set size; setting to ${batchUpdate.batchSize}", MessageKind.WARNING)
                }
            }
            trainer.events.errorUpdated.fireAsync(currentStats())
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
        val statsPanel = TrainingStatsPanel(
            events = trainer.events,
            iteration = { trainer.iteration },
            resetIteration = {
                trainer.iteration = 0
                trainer.events.iterationReset.fire()
            },
            initialStats = currentStats()
        ) {
            with(supervisedNetwork.trainerConfig) {
                TrainingStatsSettings(
                    lossName = lossFunction.shortName,
                    updateMethod = updateType.toString(),
                    testingEnabled = testConfiguration.enabled,
                    computeAccuracy = computeAccuracy,
                    showStepSize = showEffectiveStepSize,
                    stoppingCondition = stoppingCondition
                )
            }
        }
        runTools.add(statsPanel)

        layout = MigLayout("ins 0, gap 12px 0px")
        add(runTools)
        add(errorPlotPanel, "grow, gapbottom 0px")
    }

}

/**
 * What [TrainingStatsPanel] needs from a trainer's configuration, read again on every refresh so edits to the
 * trainer properties show up without rebuilding the panel.
 */
data class TrainingStatsSettings(
    val lossName: String,
    val updateMethod: String? = null,
    val testingEnabled: Boolean,
    val computeAccuracy: Boolean,
    val showStepSize: Boolean,
    val stoppingCondition: SupervisedTrainer.StoppingCondition
)

/**
 * The iteration count and training statistics shown beside the run controls of the supervised and CNN trainers.
 * Rows tied to a trainer setting are left out while that setting is off. Double-clicking resets the iteration count.
 */
class TrainingStatsPanel(
    events: TrainerEvents,
    private val iteration: () -> Int,
    resetIteration: () -> Unit,
    initialStats: TrainingStats,
    private val settings: () -> TrainingStatsSettings
) : LabelledItemPanel() {

    private data class Row(val label: String, val value: JLabel, val tooltip: String? = null)

    private val iterationsValue = JLabel(iteration().toString())
    private val trainingErrorValue = JLabel(initialStats.trainingError.format(4))
    private val testingErrorValue = JLabel(initialStats.testingError?.format(4) ?: "N/A")
    private val trainingAccuracyValue = JLabel(formatAccuracy(initialStats.trainingAccuracy))
    private val testingAccuracyValue = JLabel(formatAccuracy(initialStats.testingAccuracy))
    private val stepSizeValue = JLabel(formatStepSize(initialStats.effectiveStepSize)).apply {
        toolTipText = "RMS of the optimizer's per-parameter update last iteration. " +
            "For Adam in steady state ≈ learning rate; for SGD ≈ lr·||g||. " +
            "Near 0 ⇒ optimizer is flat-lining; very large ⇒ updates may be diverging."
    }
    private val earlyStoppingValue = JLabel()

    private var shownRows = emptyList<Row>()

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    resetIteration()
                    iterationsValue.text = iteration().toString()
                }
            }
        })

        events.errorUpdated.on(Dispatchers.Swing) { stats ->
            trainingErrorValue.text = stats.trainingError.format(4)
            stats.testingError?.let { testingErrorValue.text = it.format(4) }
            stats.trainingAccuracy?.let { trainingAccuracyValue.text = formatAccuracy(it) }
            stats.testingAccuracy?.let { testingAccuracyValue.text = formatAccuracy(it) }
            stepSizeValue.text = formatStepSize(stats.effectiveStepSize)
            refresh()
        }
        // The stopping condition is checked after the stats for an iteration go out, so the early stopping row
        // is refreshed again once a run ends to show the state that stopped it.
        events.endTraining.on(Dispatchers.Swing) { refresh() }

        refresh()
    }

    /**
     * Update the iteration count and early stopping status, and rebuild the rows if the trainer's settings now call
     * for different ones.
     */
    fun refresh() {
        val current = settings()
        iterationsValue.text = iteration().toString()
        earlyStoppingValue.text = current.stoppingCondition.getEarlyStoppingStatus() ?: ""

        // Rows are rebuilt rather than hidden because the layout keeps the gap of a hidden row, which leaves a
        // visible hole when several hidden rows fall between shown ones.
        val rows = buildList {
            add(Row("Iterations:", iterationsValue))
            add(Row(
                "Training Error (${current.lossName}):",
                trainingErrorValue,
                "Mean ${current.lossName} over the examples in the last ${current.updateMethod?.let { "$it " } ?: ""}update"
            ))
            if (current.testingEnabled) {
                add(Row("Testing Error (${current.lossName}):", testingErrorValue, "Mean ${current.lossName} over the testing data"))
            }
            if (current.computeAccuracy) add(Row("Training Accuracy:", trainingAccuracyValue))
            if (current.computeAccuracy && current.testingEnabled) add(Row("Testing Accuracy:", testingAccuracyValue))
            if (current.showStepSize) add(Row("Effective Step Size:", stepSizeValue))
            if (current.stoppingCondition.useEarlyStopping && current.testingEnabled) {
                add(Row("Early Stopping:", earlyStoppingValue))
            }
        }
        if (rows != shownRows) {
            clearItems()
            rows.forEach { row -> addItem(row.label, row.value).toolTipText = row.tooltip }
            shownRows = rows
            revalidate()
            repaint()
        }
    }

    private fun formatAccuracy(value: Double?) = value?.let { "${(it * 100).format(1)}%" } ?: "N/A"

    private fun formatStepSize(value: Double?) = value?.let { String.format("%.3g", it) } ?: "N/A"
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
                            showMessage("Number of $unitName must be non-negative", MessageKind.ERROR, title = "Invalid Input")
                        }
                    } catch (e: NumberFormatException) {
                        showMessage("Please enter a valid integer", MessageKind.ERROR, title = "Invalid Input")
                    }
                }
            }
        })
    }
}
