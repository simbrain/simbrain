package org.simbrain.util.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import smile.io.Read
import smile.plot.swing.BoxPlot
import smile.plot.swing.Histogram
import smile.plot.swing.PlotGrid
import java.io.File
import javax.swing.JOptionPane

/**
 * Default directory where tables are stored.
 */
private val CSV_DIRECTORY = "." + Utils.FS + "simulations" + Utils.FS + "tables"

fun SimbrainDataViewer.addSimpleDefaults()  {
    addAction(table.zeroFillAction)
    addAction(table.randomizeAction)
}

val DataViewerTable.randomizeAction
    get() = createAction(
        "menu_icons/Rand.png",
        "Randomize",
        "Randomize selected cells",
        CmdOrCtrl + 'R'
    ) {
        randomizeSelectedCells()
    }

val DataViewerTable.randomizeColumnAction
    get() = createAction(
        "menu_icons/Rand.png",
        "Randomize column",
        "Randomize cells in selected column",
    ) {
        model.randomizeColumn(selectedColumn)
    }

val DataViewerTable.zeroFillAction
    get() = createAction(
        "menu_icons/Fill.png",
        "Zero Fill",
        "Zero Fill selected cells",
        'Z'
    ) {
        zeroFillSelectedCells()
    }

val DataViewerTable.fillAction
    get() = createAction(
        "menu_icons/Fill.png",
        "Fill...",
        "Fill selected cells"
    ) {
        val fillVal = JOptionPane.showInputDialog(this, "Value:", "0").toDouble()
        fillSelectedCells(fillVal)
    }


val DataViewerTable.editRadomizerAction
    get() = createAction(
        "menu_icons/Prefs.png",
        "Edit randomizer...",
        "Edit table wide randomizer"
    ) {
        val editor = AnnotatedPropertyEditor(model.cellRandomizer)
        val dialog: StandardDialog = editor.dialog
        // dialog.addClosingTask { updateChartSettings() }
        dialog.isModal = true
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

val DataViewerTable.showHistogramAction
    get() = createAction(
        "menu_icons/histogram.png",
        "Histogram",
        "Create histograms for data in selected column"
    ) {
        GlobalScope.launch(context = Dispatchers.Default) {
            val canvas = Histogram.of(model.getDoubleColumn(selectedColumn)).canvas();
            canvas.window()
        }
    }

val DataViewerTable.showBoxPlotAction
    get() = createAction(
        "menu_icons/BarChart.png", // TODO Better Icon
        "Boxplot column",
        "Create boxplot for data in selected column"
    ) {
        GlobalScope.launch(context = Dispatchers.Default) {
            val canvas = BoxPlot.of(*model.getColumnMajorArray()).canvas();
            canvas.window()
        }
    }


// TODO: Make this usable outside of DataFrameWrapper
// Maybe be possible to adapt that code to a more generic context
val DataViewerTable.showScatterPlotAction
    get() = createAction(
        "menu_icons/ScatterIcon.png",
        "Scatter Plots",
        "Show all pairwise scatter plots across columns"
    ) {
        GlobalScope.launch(context = Dispatchers.Default) {
            // TODO: User should be able to set which column is class
            // TODO: Set mark
            if (model is DataFrameWrapper) {
                val canvas = PlotGrid.splom(model.df, '.', "V1")
                canvas.window()
            }
        }
    }

val DataViewerTable.importArff
    get() = createAction(
        "menu_icons/Import.png",
        "Import arff file...",
        "Import WEKA arff file"
    ) {
        val chooser = SFileChooser(CSV_DIRECTORY, "", "arff")
        val arffFile: File = chooser.showOpenDialog()
        if (model is DataFrameWrapper) {
            model.df = Read.arff(arffFile.absolutePath)
            model.fireTableStructureChanged()
        }
    }



