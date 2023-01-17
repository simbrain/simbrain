package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.trainers.LMSTrainer
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotActions
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.ResourceManager
import org.simbrain.util.Utils.round
import org.simbrain.util.createAction
import org.simbrain.util.createSuspendAction
import org.simbrain.util.table.*
import org.simbrain.util.widgets.ToggleButton
import smile.math.matrix.Matrix
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

class TrainerControls(lmsTrainer: LMSTrainer, errorText: String = "Error") : JPanel(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + job

    val iterationsLabel = JLabel("--- ")

    var errorBar = JProgressBar()

    var numTicks = 1000

    private val runAction = createSuspendAction(
        name = "Run",
        iconPath ="menu_icons/Play.png",
        description = "Iterate training until stop button is pressed."
    ) {
        lmsTrainer.startTraining()
    }

    private val stopAction = createSuspendAction(
        name = "Stop",
        iconPath = "menu_icons/Stop.png",
        description = "Stop training."
    ) {
        lmsTrainer.stopTraining()
    }

    private val stepAction = createSuspendAction(
        "menu_icons/Step.png", description = "Iterate training once."
    ) {
        lmsTrainer.iterate()
    }

    private val randomizeAction = createAction(
        "menu_icons/Rand.png", description = "Randomize network.",
    ) {
        lmsTrainer.lmsNet.randomize()
    }

    init {

        val errorPlot = ErrorTimeSeries(lmsTrainer, errorText)

        val runTools = JPanel().apply { layout = MigLayout("nogrid ") }
        runTools.add(ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            lmsTrainer.events.beginTraining.on {
                setAction("Stop")
            }
            lmsTrainer.events.endTraining.on {
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

        lmsTrainer.events.errorUpdated.on {
            iterationsLabel.text = "" + lmsTrainer.iteration
            errorBar.value = (numTicks * lmsTrainer.error).toInt()
            errorBar.string = "" + round(lmsTrainer.error, 4)
        }

        layout = MigLayout("ins 0, gap 0px 0px")
        add(runTools)
        add(errorPlot, "grow")
    }

}

// TODO: Generalize to trainer
class ErrorTimeSeries(lmsTrainer: LMSTrainer, errorText: String = "Error") : JPanel() {

    val graphPanel: TimeSeriesPlotPanel

    init {
        val mainPanel = JPanel()

        // TODO: Consider passing some of these values in
        val model = TimeSeriesModel { lmsTrainer.iteration }
        model.rangeLowerBound = 0.0
        model.rangeUpperBound = 5.0
        model.isFixedWidth = true
        model.isAutoRange = false
        graphPanel = TimeSeriesPlotPanel(model)
        graphPanel.updateChartSettings() // TODO
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
        lmsTrainer.events.errorUpdated.on {
            model.addData(0, lmsTrainer.iteration.toDouble(), lmsTrainer.error)
        }
    }
}


/**
 * Default config for a matrix editor.
 */
class MatrixEditor(matrix: Matrix) : SimbrainDataViewer(
    MatrixDataWrapper(matrix), false
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
class AddRemoveRows(val table1: DataViewerTable, val table2: DataViewerTable) : JPanel() {

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