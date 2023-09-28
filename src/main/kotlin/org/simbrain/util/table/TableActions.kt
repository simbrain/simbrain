package org.simbrain.util.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.plot.projection.ProjectionComponent2
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.workspace.gui.SimbrainDesktop
import smile.io.Read
import smile.plot.swing.BoxPlot
import smile.plot.swing.Histogram
import smile.plot.swing.PlotGrid
import javax.swing.JOptionPane

/**
 * Default directory where tables are stored.
 */
private val TABLE_DIRECTORY = "." + Utils.FS + "simulations" + Utils.FS + "tables"

fun SimbrainDataViewer.addSimpleDefaults()  {
    addAction(table.zeroFillAction)
    addAction(table.randomizeAction)
}

val DataViewerTable.randomizeAction
    get() = createAction(
        name = "Randomize",
        description = "Randomize selected cells",
        iconPath = "menu_icons/Rand.png",
        keyboardShortcut = CmdOrCtrl + 'R'
    ) {
        randomizeSelectedCells()
    }

val DataViewerTable.randomizeColumnAction
    get() = createAction(
        name = "Randomize column",
        description = "Randomize cells in selected column",
        iconPath = "menu_icons/Rand_C.png"
    ) {
        model.randomizeColumn(selectedColumn)
    }

val DataViewerTable.zeroFillAction
    get() = createAction(
        name = "Zero Fill",
        description = "Zero Fill selected cells",
        iconPath = "menu_icons/Fill_0.png",
        keyboardShortcut = 'Z'
    ) {
        zeroFillSelectedCells()
    }

val DataViewerTable.fillAction
    get() = createAction(
        name = "Fill...",
        description = "Fill selected cells",
        iconPath = "menu_icons/Fill.png"
    ) {
        val fillVal = JOptionPane.showInputDialog(this, "Value:", "0").toDouble()
        fillSelectedCells(fillVal)
    }

val DataViewerTable.editRandomizerAction
    get() = createAction(
        name = "Edit randomizer...",
        description = "Edit table wide randomizer",
        iconPath = "menu_icons/Prefs.png"
    ) {
        AnnotatedPropertyEditor(objectWrapper("Table Randomizer", model.cellRandomizer)).displayInDialog()
    }

val DataViewerTable.insertColumnAction
    get() = createAction(
        name = "Insert column",
        description = "Insert column to the right of selected column, or as the left-most column if none is selected.",
        iconPath = "menu_icons/AddTableColumn.png"
    ) {
        insertColumn()
    }

val DataViewerTable.deleteColumnAction
    get() = createAction(
        name = "Delete columns",
        description = "Delete selected columns",
        iconPath = "menu_icons/DeleteColumnTable.png"
    ) {
        deleteSelectedColumns()
    }

val DataViewerTable.insertRowAction
    get() = createAction(
        name = "Insert row",
        description = "Insert row to above the selected row, or as the bottom row if none is selected.",
        iconPath = "menu_icons/AddTableRow.png"
    ) {
        insertRow()
    }

val DataViewerTable.deleteRowAction
    get() = createAction(
        name = "Delete rows",
        description = "Delete selected rows",
        iconPath = "menu_icons/DeleteRowTable.png"
    ) {
        deleteSelectedRows()
    }


val DataViewerTable.showHistogramAction
    get() = createAction(
        iconPath = "menu_icons/histogram.png",
        name = "Histogram",
        description = "Create histograms for data in selected column"
    ) {
        launch(Dispatchers.Swing) {
            val canvas = Histogram.of(model.getDoubleColumn(selectedColumn)).canvas();
            canvas.window()
        }
    }

val DataViewerTable.showBoxPlotAction
    get() = createAction(
        name = "Boxplot column",
        description = "Create boxplot for data all numeric columns",
        iconPath = "menu_icons/BarChart.png" // TODO Better Icon
    ) {
        launch(context = Dispatchers.Default) {
            val canvas = BoxPlot.of(*model.getColumnMajorArray()).canvas();
            canvas.window()
        }
    }


// TODO: Make this usable outside of DataFrameWrapper
// Maybe be possible to adapt that code to a more generic context
val DataViewerTable.showScatterPlotAction
    get() = createAction(
        name = "Scatter Plots",
        description = "Show all pairwise scatter plots across columns",
        iconPath = "menu_icons/ScatterIcon.png"
    ) {
        launch(context = Dispatchers.Default) {
            // TODO: User should be able to set which column is class
            // TODO: Set mark
            if (model is DataFrameWrapper) {
                val canvas = PlotGrid.splom(model.df, '.', "V1")
                canvas.window()
            }
        }
    }

val DataViewerTable.openProjectionAction get() = createAction(
    iconPath = "menu_icons/ProjectionIcon.png",
    description = "Open Projection"
) {
    withContext(Dispatchers.Default) {
        val projectionComponent = ProjectionComponent2("$name Projection")
        projectionComponent.projector.useHotColor = false
        SimbrainDesktop.workspace.addWorkspaceComponent(projectionComponent)
        val points = model.get2DDoubleArray()
        points.forEach { projectionComponent.addPoint(it) }
    }
}

val DataViewerTable.importArff
    get() = createAction(
        name = "Import arff file...",
        description = "Import WEKA arff file",
        iconPath = "menu_icons/Import.png"
    ) {
        val chooser = SFileChooser(TABLE_DIRECTORY, "", "arff")
        val arffFile = chooser.showOpenDialog()
        if (arffFile != null) {
            model.let {
                if (it is DataFrameWrapper) {
                    it.df = Read.arff(arffFile.absolutePath)
                    it.fireTableStructureChanged()
                } else if (it is BasicDataWrapper) {
                    val df = Read.arff(arffFile.absolutePath)
                    val columns = df.names().zip(df.types())
                        .map { (name, type) -> Column(name, type.getColumnDataType()) }.toMutableList()
                    val dfData = (0 until df.nrow()).map { i ->
                        (0 until df.ncol()).map { j ->
                            df[i][j]
                        }.toMutableList()
                    }.toMutableList()
                    it.data = dfData
                    it.columns = columns
                    it.fireTableStructureChanged()
                }
            }
        }
    }

val DataViewerTable.importCsv
    get() = importCSVAction()

fun DataViewerTable.importCSVAction(fixedColumns: Boolean = false) = createAction(
    name ="Import csv...",
    description = "Import comma separated values file",
    iconPath= "menu_icons/Import.png"
) {
    val chooser = SFileChooser(TABLE_DIRECTORY, "", "csv")
    val csvFile = chooser.showOpenDialog()
    fun checkColumns(numColumns: Int): Boolean {
        if (numColumns != model.columnCount) {
            JOptionPane.showOptionDialog(
                null,
                "Trying to import a table with the wrong number of columns ",
                "Warning",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, null, null)
            return false
        }
        return true
    }
    if (csvFile != null) {
        model.let {
            if (it is BasicDataWrapper) {
                val importedData = createFrom2DArray(Utils.getStringMatrix(csvFile))
                if (checkColumns(importedData.columnCount)) {
                    it.data = importedData.data
                    it.fireTableStructureChanged()
                }
            } else if (it is DataFrameWrapper) {
                val data = Read.csv(csvFile.absolutePath)
                if (checkColumns(data.ncol())) {
                    it.df = data
                    it.fireTableStructureChanged()
                }
            }
        }
    }
}

val DataViewerTable.editColumnAction
    get() = createAction(
        name = "Edit column...",
        description =  "Edit column properties",
        iconPath = "menu_icons/Prefs.png"
    ) {
        if (model is BasicDataWrapper) {
            if (selectedColumn >= 0) {
                // TODO: Add access to histogram etc. from here?
                AnnotatedPropertyEditor(model.columns[selectedColumn]).displayInDialog()
            }
        }
    }
