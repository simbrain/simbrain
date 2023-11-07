package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.trainers.IterableTrainer
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotActions
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.ResourceManager
import org.simbrain.util.Utils.round
import org.simbrain.util.createAction
import org.simbrain.util.table.*
import org.simbrain.util.widgets.ToggleButton
import smile.math.matrix.Matrix
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

/**
 * Controls used by Supervised learning dialogs.
 */
class TrainerControls(trainer: IterableTrainer, errorText: String = "Error") : JPanel(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + job

    val iterationsLabel = JLabel("--- ")

    var errorBar = JProgressBar()

    var numTicks = 1000

    private val runAction = createAction(
        name = "Run",
        iconPath ="menu_icons/Play.png",
        description = "Iterate training until stop button is pressed",
    ) {
        trainer.startTraining()
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
        trainer.iterate()
    }

    private val randomizeAction = createAction(
        name = "Randomize",
        description = "Randomize network",
        iconPath = "menu_icons/Rand.png",
    ) {
        trainer.randomize()
    }

    init {

        val errorPlot = ErrorTimeSeries(trainer, errorText)

        val runTools = JPanel().apply { layout = MigLayout("nogrid ") }
        runTools.add(ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on {
                setAction("Stop")
            }
            trainer.events.endTraining.on {
                setAction("Run")
            }
        })
        runTools.add(JButton(stepAction))
        val randomizeButton = JButton(randomizeAction)
        randomizeButton.hideActionText = true
        runTools.add(randomizeButton)
        runTools.add(JButton(TimeSeriesPlotActions.getClearGraphAction(errorPlot.graphPanel)))
        runTools.add(JButton(TimeSeriesPlotActions.getPropertiesDialogAction(errorPlot.graphPanel)), "wrap")
        val labelPanel = LabelledItemPanel()
        labelPanel.addItem("Iterations:", iterationsLabel)
        numTicks = 10
        errorBar = JProgressBar(0, numTicks)
        errorBar.isStringPainted = true
        labelPanel.addItem(errorText, errorBar)
        runTools.add(labelPanel)

        trainer.events.errorUpdated.on {
            iterationsLabel.text = "" + trainer.iteration
            errorBar.value = (numTicks * trainer.error).toInt()
            errorBar.string = "" + round(trainer.error, 4)
        }

        layout = MigLayout("ins 0, gap 0px 0px")
        add(runTools)
        add(errorPlot, "grow")
    }

}

class ErrorTimeSeries(trainer: IterableTrainer, errorText: String = "Error") : JPanel() {

    val graphPanel: TimeSeriesPlotPanel

    init {
        val mainPanel = JPanel()

        // TODO: Consider passing some of these values in
        val model = TimeSeriesModel { trainer.iteration }
        model.rangeLowerBound = 0.0
        model.rangeUpperBound = 5.0
        model.fixedWidth = true
        model.isAutoRange = true
        model.fixedRangeThreshold = 5.0
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

        model.addScalarTimeSeries(errorText)
        trainer.events.errorUpdated.on(Dispatchers.Swing, wait = true) {
            model.addData(0, trainer.iteration.toDouble(), trainer.error)
        }
    }
}


/**
 * Default config for a matrix editor.
 */
class MatrixEditor(matrix: Matrix) : SimbrainTablePanel(
    MatrixDataFrame(matrix), false
) {
    init {
        addAction(table.importCsv)
        addAction(table.randomizeAction)
        addAction(table.showBoxPlotAction)
        addAction(table.showHistogramAction)
        preferredSize = Dimension(400, 250)
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