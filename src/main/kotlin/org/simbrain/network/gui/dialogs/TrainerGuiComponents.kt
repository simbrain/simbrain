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
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Controls used by Supervised learning dialogs.
 */
class TrainerControls(trainer: SupervisedTrainer, supervisedNetwork: SupervisedNetwork, networkPanel: NetworkPanel): JPanel(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + job

    val iterationsLabel = JLabel(trainer.iteration.toString())

    private val runAction = createAction(
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

    private val stepAction = createAction(
        description = "Iterate training once",
        iconPath =  "menu_icons/Step.png",
        initBlock = {
            trainer.events.beginTraining.on {
                isEnabled = false
            }
            trainer.events.endTraining.on {
                isEnabled = true
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
        runTools.add(JButton(stepAction))
        runTools.add(ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on {
                this@TrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                setAction("Stop")
            }
            trainer.events.endTraining.on {
                this@TrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                setAction("Run")
            }
        })
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
        val errorValue = JLabel(trainer.lastTrainingError.roundToString(4))
        fun errorDescriptionString() = "Mean Error (${supervisedNetwork.trainerConfig.updateType}; ${supervisedNetwork.trainerConfig.lossFunction.shortName})"
        val errorLabel = labelPanel.addItem(errorDescriptionString(), errorValue)
        
        // Add accuracy labels for softmax networks (always create, but conditionally show)
        val trainingAccuracyValue = JLabel(trainer.lastTrainingAccuracy?.format(3) ?: "N/A")
        val trainingAccuracyLabel = labelPanel.addItem("Training Accuracy:", trainingAccuracyValue)
        
        val testingAccuracyValue = JLabel(trainer.lastTestingAccuracy?.format(3) ?: "N/A")
        val testingAccuracyLabel = labelPanel.addItem("Testing Accuracy:", testingAccuracyValue)
        
        // Function to update accuracy label visibility
        fun updateAccuracyVisibility() {
            val shouldShowTrainingAccuracy = supervisedNetwork.trainerConfig.computeAccuracy
            val shouldShowTestingAccuracy = supervisedNetwork.trainerConfig.computeAccuracy && 
                                           supervisedNetwork.trainerConfig.testConfiguration.enabled
            
            trainingAccuracyLabel.isVisible = shouldShowTrainingAccuracy
            trainingAccuracyValue.isVisible = shouldShowTrainingAccuracy
            testingAccuracyLabel.isVisible = shouldShowTestingAccuracy
            testingAccuracyValue.isVisible = shouldShowTestingAccuracy
        }
        
        // Set initial visibility
        updateAccuracyVisibility()
        
        runTools.add(labelPanel)

        trainer.events.errorUpdated.on(Dispatchers.Swing) { trainingStats ->
            iterationsLabel.text = "" + trainer.iteration
            errorValue.text = "" + trainingStats.trainingError.format(4)
            errorLabel.text = errorDescriptionString()
            
            // Update accuracy visibility (in case configuration changed)
            updateAccuracyVisibility()
            
            // Update training accuracy value (only when available)
            trainingStats.trainingAccuracy?.let { accuracy ->
                trainingAccuracyValue.text = accuracy.format(3)
            }
            
            // Update testing accuracy value (only when available, keep previous value otherwise)
            trainingStats.testingAccuracy?.let { accuracy ->
                testingAccuracyValue.text = accuracy.format(3)
            }
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