package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.core.NetworkModel
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
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Controls used by Supervised learning dialogs.
 */
class TrainerControls<SN>(trainer: SupervisedTrainer<SN>, supervisedNetwork: SN, networkPanel: NetworkPanel): JPanel(), CoroutineScope where SN: SupervisedNetwork, SN: NetworkModel {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + job

    val iterationsLabel = JLabel(trainer.iteration.toString())

    private val runAction = createAction(
        name = "Run",
        iconPath ="menu_icons/Play.png",
        description = "Iterate training until stop button is pressed"
    ) {
        with(networkPanel.network) { trainer.run { supervisedNetwork.startTraining() } }
    }

    private val stopAction = createAction(
        name = "Stop",
        iconPath = "menu_icons/Stop.png",
        description = "Stop training.",
    ) {
        trainer.stopTraining()
    }

    private val stepAction = createAction(
        name = "Step",
        description = "Iterate training once",
        iconPath =  "menu_icons/Step.png",
    ) {
        trainer.events.beginTraining.fire().await()
        with(networkPanel.network) { supervisedNetwork.run { trainer.trainOnce() } }
        trainer.events.endTraining.fire()
    }

    private val randomizeAction = createAction(
        name = "Randomize",
        description = "Randomize network",
        iconPath = "menu_icons/Rand.png",
    ) {
        supervisedNetwork.randomize()
    }

    private val trainerPropsAction = createAction(
        name = "Trainer properties",
        description = "Edit trainer properties",
        iconPath = "menu_icons/Prefs.png",
    ) {
        trainer.createEditorDialog {
            (it.updateType as? SupervisedTrainer.UpdateMethod.Batch)?.let { batchUpdate ->
                if (batchUpdate.batchSize !in 1..supervisedNetwork.trainingSet.size) {
                    batchUpdate.batchSize = batchUpdate.batchSize.coerceIn(1, supervisedNetwork.trainingSet.size)
                    showWarningDialog("Batch size exceeds training set size; setting to ${batchUpdate.batchSize}")
                }
            }
            trainer.events.errorUpdated.fire(trainer.lastError)
        }.display()
    }

    init {

        val errorPlotPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            val errorPlot = ErrorTimeSeries(trainer)
            add(errorPlot)
            JPanel().apply {
                add(JButton(TimeSeriesPlotActions.getClearGraphAction(errorPlot.graphPanel)))
                add(JButton(TimeSeriesPlotActions.getPropertiesDialogAction(errorPlot.graphPanel)))
            }.also { add(it) }
        }

        val runTools = JPanel().apply { layout = MigLayout("nogrid ") }
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
        runTools.add(JButton(stepAction))
        val randomizeButton = JButton(randomizeAction)
        randomizeButton.hideActionText = true
        runTools.add(randomizeButton)
        runTools.add(JButton(trainerPropsAction), "wrap")
        val labelPanel = LabelledItemPanel()
        labelPanel.addItem("Iterations:", iterationsLabel)
        val errorValue = JLabel(trainer.lastError.roundToString(4))
        fun errorDescriptionString() = "Mean Error (${trainer.updateType}; ${trainer.lossFunction.shortName})"
        val errorLabel = labelPanel.addItem(errorDescriptionString(), errorValue)
        runTools.add(labelPanel)

        trainer.events.errorUpdated.on(Dispatchers.Swing) { error ->
            iterationsLabel.text = "" + trainer.iteration
            errorValue.text = "" + error.format(4)
            errorLabel.text = errorDescriptionString()
        }

        layout = MigLayout("ins 0, gap 0px 0px")
        add(runTools)
        add(errorPlotPanel, "grow")
    }

}

class ErrorTimeSeries(trainer: SupervisedTrainer<*>) : JPanel() {

    val graphPanel: TimeSeriesPlotPanel

    init {
        val mainPanel = JPanel()

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
        graphPanel.chartPanel.chart.removeLegend()
        graphPanel.preferredSize = Dimension(graphPanel.preferredSize.width, 200)

        graphPanel.removeAllButtonsFromToolBar()
        // TODO: The buttons below are useful but take up space; find a better place for them.
        // graphPanel.addClearGraphDataButton()
        // graphPanel.addPreferencesButton()
        mainPanel.add(graphPanel)
        add(mainPanel)

        model.addTimeSeries("Error")
        trainer.events.errorUpdated.on(Dispatchers.Swing) {
            model.addData(0, trainer.iteration.toDouble(), it)
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
        addAction(table.showHistogramAction)
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
class AddRemoveRows(val table1: SimbrainJTable, val table2: SimbrainJTable) : JPanel() {

    init {
        // Add row
        add(JButton().apply {
            icon = ResourceManager.getImageIcon("menu_icons/AddTableRow.png")
            toolTipText = "Insert row at bottom of input and target tables"
            addActionListener {
                table1.model.insertRowAtBottom()
                table2.model.insertRowAtBottom()
            }
        })
        add(JButton().apply {
            icon = ResourceManager.getImageIcon("menu_icons/DeleteRowTable.png")
            toolTipText = "Delete last row of input and target tables"
            addActionListener {
                table1.model.deleteLastRow()
                table2.model.deleteLastRow()
            }
        })
    }
}