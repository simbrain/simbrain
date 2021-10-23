package org.simbrain.util.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import smile.plot.swing.BoxPlot
import smile.plot.swing.Histogram
import smile.plot.swing.PlotGrid
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JOptionPane
import javax.swing.KeyStroke

/**
 * Default directory where tables are stored.
 */
private val CSV_DIRECTORY = "." + Utils.FS + "simulations" + Utils.FS + "tables"

fun DataViewerTable.getRandomizeAction() = object : AbstractAction() {

    init {
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"))
        putValue(NAME, "Randomize")
        putValue(SHORT_DESCRIPTION, "Randomize selected cells")
    }

    override fun actionPerformed(e: ActionEvent) {
        randomizeSelectedCells()
    }
}

fun DataViewerTable.getZeroFillAction() = object : AbstractAction() {

    init {
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Fill.png"))
        putValue(NAME, "Zero Fill")
        putValue(SHORT_DESCRIPTION, "Zero Fill selected cells")
    }

    override fun actionPerformed(e: ActionEvent) {
        // val `val` = JOptionPane.showInputDialog(this, "Value:", "0")
        // fill(`val`.toDouble())

        zeroFillSelectedCells()
    }
}

fun DataViewerTable.getFillAction() = object : AbstractAction() {

    init {
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Fill.png"))
        putValue(NAME, "Fill...")
        putValue(SHORT_DESCRIPTION, "Fill selected cells")
    }

    override fun actionPerformed(e: ActionEvent) {
        val fillVal = JOptionPane.showInputDialog(this@getFillAction, "Value:", "0").toDouble()
        fillSelectedCells(fillVal)
    }
}

fun DataViewerTable.getRandomizeColumnAction() = object : AbstractAction() {

    init {
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Rand.png"))
        putValue(NAME, "Randomize column")
        putValue(SHORT_DESCRIPTION, "Randomize cells in selected column")
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('R', java.awt.event.InputEvent.META_DOWN_MASK))
    }

    override fun actionPerformed(e: ActionEvent) {
        model.randomizeColumn(selectedColumn)
    }
}

fun DataViewerTable.getEditRandomizerAction() = object : AbstractAction() {

    init {
        putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Prefs.png"))
        putValue(NAME, "Edit randomizer")
        putValue(SHORT_DESCRIPTION, "Edit table wide randoomizer")
    }

    override fun actionPerformed(e: ActionEvent) {
        // TODO: Get object type thing to work
        val editor = AnnotatedPropertyEditor(model.cellRandomizer)
        val dialog: StandardDialog = editor.dialog
        // dialog.addClosingTask { updateChartSettings() }
        dialog.isModal = true
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }
}

// TODO: Make this usable outside of DataFrameWrapper
// Maybe be possible to adapt that code to a more generic context
fun DataFrameWrapper.getShowScatterPlotAction(): Action {
    return object : AbstractAction() {

        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/ScatterIcon.png"))
            putValue(NAME, "Scatter plots")
            putValue(SHORT_DESCRIPTION, "Show all pairwise scatter plots across columns")
        }

        override fun actionPerformed(arg0: ActionEvent) {

            GlobalScope.launch(context = Dispatchers.Default) {
                // TODO: Set column to use
                // TODO: Set mark
                val canvas = PlotGrid.splom(df, '.', "class")
                canvas.window()
            }
        }
    }
}


fun DataViewerTable.getShowHistogramAction(): Action {
    return object : AbstractAction() {

        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/histogram.png"))
            putValue(NAME, "Histogram")
            putValue(SHORT_DESCRIPTION, "Create histograms of selected column")
        }

        override fun actionPerformed(arg0: ActionEvent) {

            GlobalScope.launch(context = Dispatchers.Default) {
                val canvas = Histogram.of(model.getDoubleArray(selectedColumn)).canvas();
                canvas.window()
            }
        }
    }
}

fun DataViewerTable.getShowPlotAction(): Action {
    return object : AbstractAction() {

        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/PieChart.png"))
            putValue(NAME, "Show plots")
            putValue(SHORT_DESCRIPTION, "Create histograms and other plots...")
        }

        override fun actionPerformed(arg0: ActionEvent) {

            GlobalScope.launch(context = Dispatchers.Default) {
                val canvas = BoxPlot.of(*model.getColumnMajorArray()).canvas();
                canvas.window()
            }
        }
    }
}


