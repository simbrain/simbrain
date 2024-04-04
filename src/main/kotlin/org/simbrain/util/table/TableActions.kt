package org.simbrain.util.table

import com.Ostermiller.util.CSVPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.*
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.widgets.MatrixPlot
import org.simbrain.workspace.gui.SimbrainDesktop
import smile.io.Read
import smile.plot.swing.BoxPlot
import smile.plot.swing.Histogram
import smile.plot.swing.PlotGrid
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.reflect.KClass

/**
 * Default directory where tables are stored.
 */
private val TABLE_DIRECTORY = "." + Utils.FS + "simulations" + Utils.FS + "tables"

fun SimbrainTablePanel.addSimpleDefaults() {
    addAction(table.zeroFillAction)
    addAction(table.randomizeAction)
}

val SimbrainJTable.randomizeAction
    get() = createAction(
        name = "Randomize",
        description = "Randomize selected cells",
        iconPath = "menu_icons/Rand.png",
        keyboardShortcut = CmdOrCtrl + 'R'
    ) {
        randomizeSelectedCells()
    }

val SimbrainJTable.randomizeColumnAction
    get() = createAction(
        name = "Randomize column",
        description = "Randomize cells in selected column",
        iconPath = "menu_icons/Rand_C.png"
    ) {
        model.randomizeColumn(selectedColumn)
    }

val SimbrainJTable.zeroFillAction
    get() = createAction(
        name = "Zero Fill",
        description = "Zero Fill selected cells",
        iconPath = "menu_icons/Fill_0.png",
        keyboardShortcut = 'Z'
    ) {
        zeroFillSelectedCells()
    }

val SimbrainJTable.fillAction
    get() = createAction(
        name = "Fill...",
        description = "Fill selected cells",
        iconPath = "menu_icons/fill.png"
    ) {
        val fillVal = JOptionPane.showInputDialog(this, "Value:", "0").toDouble()
        fillSelectedCells(fillVal)
    }

val SimbrainJTable.editRandomizerAction
    get() = createAction(
        name = "Edit randomizer...",
        description = "Edit table wide randomizer",
        iconPath = "menu_icons/Prefs.png"
    ) {
        AnnotatedPropertyEditor(objectWrapper("Table Randomizer", model.cellRandomizer)).displayInDialog()
    }

val SimbrainJTable.insertColumnAction
    get() = createAction(
        name = "Insert column",
        description = "Insert column to the right of selected column, or as the left-most column if none is selected.",
        iconPath = "menu_icons/AddTableColumn.png"
    ) {
        insertColumn()
    }

val SimbrainJTable.deleteColumnAction
    get() = createAction(
        name = "Delete columns",
        description = "Delete selected columns",
        iconPath = "menu_icons/DeleteColumnTable.png"
    ) {
        deleteSelectedColumns()
    }

val SimbrainJTable.insertRowAction
    get() = createAction(
        name = "Insert row",
        description = "Insert row to above the selected row, or as the bottom row if none is selected.",
        iconPath = "menu_icons/AddTableRow.png"
    ) {
        insertRow()
    }

val SimbrainJTable.deleteRowAction
    get() = createAction(
        name = "Delete rows",
        description = "Delete selected rows",
        iconPath = "menu_icons/DeleteRowTable.png"
    ) {
        deleteSelectedRows()
    }


val SimbrainJTable.showHistogramAction
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

val SimbrainJTable.showBoxPlotAction
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
val SimbrainJTable.showScatterPlotAction
    get() = createAction(
        name = "Scatter Plots",
        description = "Show all pairwise scatter plots across columns",
        iconPath = "menu_icons/ScatterIcon.png"
    ) {
        launch(context = Dispatchers.Default) {
            // TODO: User should be able to set which column is class
            // TODO: Set mark
            if (model is SmileDataFrame) {
                val canvas = PlotGrid.splom(model.df, '.', "V1")
                canvas.window()
            }
        }
    }

/**
 * @param useRowLabels If true, the row labels for this dataset are shown as labels in the corresponding points of the projection plot.
 */
fun SimbrainJTable.createOpenProjectionAction(useRowLabels: Boolean = false) = createAction(
        iconPath = "menu_icons/ProjectionIcon.png",
        description = "Open Projection"
    ) {
        withContext(Dispatchers.Default) {
            val projectionComponent = ProjectionComponent("$name Projection")
            projectionComponent.projector.useHotColor = false
            projectionComponent.projector.showLabels = useRowLabels
            SimbrainDesktop.workspace.addWorkspaceComponent(projectionComponent)
            val points = model.let { it.rowNames zip it.get2DDoubleArray() }.map { (name, data) -> DataPoint(data, label = name) }
            points.forEach { projectionComponent.addPoint(it) }
        }
    }

val SimbrainJTable.importArff
    get() = createAction(
        name = "Import arff file...",
        description = "Import WEKA arff file",
        iconPath = "menu_icons/import.png"
    ) {
        val chooser = SFileChooser(TABLE_DIRECTORY, "", "arff")
        val arffFile = chooser.showOpenDialog()
        if (arffFile != null) {
            model.let {
                if (it is SmileDataFrame) {
                    it.df = Read.arff(arffFile.absolutePath)
                    it.fireTableStructureChanged()
                } else if (it is BasicDataFrame) {
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

val SimbrainJTable.importCsv
    get() = importCSVAction()

/**
 * @dataTypes The data types to use for all cells in the table. Defaults to String. TODO: provide support for per-column data types.
 */
fun SimbrainJTable.importCSVAction(fixedColumns: Boolean = true, skipImportOptions: Boolean = false, defaultOptions: ImportExportOptions = ImportExportOptions(), dataType: KClass<*>? = null) = createAction(
    name = "Import csv...",
    description = "Import comma separated values file",
    iconPath = "menu_icons/import.png"
) {
    fun import(options: ImportExportOptions = defaultOptions) {
        val chooser = SFileChooser(TABLE_DIRECTORY, "", "csv")
        val csvFile = chooser.showOpenDialog()
        fun checkColumns(numColumns: Int): Boolean {
            if (numColumns != model.columnCount) {
                JOptionPane.showOptionDialog(
                    null,
                    "Trying to import a table with the wrong number of columns ",
                    "Warning",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, null, null
                )
                return false
            }
            return true
        }
        if (csvFile != null) {
            model.let {
                if (it is BasicDataFrame) {
                    val rawData = Utils.getStringMatrix(csvFile)
                    val importedData = createFrom2DArray(rawData, options, dataType)
                    if (!fixedColumns || checkColumns(importedData.columnCount)) {
                        it.data = importedData.data
                        it.columnNames = importedData.columnNames
                        it.rowNames = importedData.rowNames
                        it.fireTableStructureChanged()
                    }
                } else if (it is SmileDataFrame) {
                    val data = Read.csv(csvFile.absolutePath)
                    if (!fixedColumns || checkColumns(data.ncol())) {
                        it.df = data
                        it.fireTableStructureChanged()
                    }
                }
            }
        }
    }
    if (skipImportOptions) {
        import()
    } else {
        val options = ImportExportOptions()
        options.createEditorDialog {
            import(it)
        }.display()
    }
}

fun SimbrainJTable.exportCsv(fileName: String = "", skipExportOptions: Boolean = false, defaultOptions: ImportExportOptions = ImportExportOptions()) = createAction(
    name = "Export csv...",
    description = "Export comma separated values file",
    iconPath = "menu_icons/export.png"
) {
    fun export(options: ImportExportOptions = defaultOptions) {
        val chooser = SFileChooser(TABLE_DIRECTORY, "", "csv")
        val csvFile = chooser.showSaveDialog(fileName)
        if (csvFile != null) {
            val writer = csvFile.bufferedWriter()
            val printer = CSVPrinter(writer)

            model.toStringLists(options)
                .forEach { printer.writeln(it.toTypedArray()) }
        }
    }

    if (skipExportOptions) {
        export()
    } else {
        val options = ImportExportOptions()
        options.createEditorDialog {
            export(it)
        }.display()
    }
}

val SimbrainJTable.editColumnAction
    get() = createAction(
        name = "Edit column...",
        description = "Edit column properties",
        iconPath = "menu_icons/Prefs.png"
    ) {
        if (model is BasicDataFrame) {
            if (selectedColumn >= 0) {
                // TODO: Add access to histogram etc. from here?
                AnnotatedPropertyEditor(model.columns[selectedColumn]).displayInDialog()
            }
        }
    }

fun SimbrainJTable.createApplyAction(name: String = "Apply", applyInputs: suspend (selectedRow: Int) -> Unit) =
    createAction(
        name = name,
        description = "Apply current row as input to network",
        iconPath = "menu_icons/Step.png",
    ) {
        initRowSelection()
        applyInputs(selectedRow)
    }

fun SimbrainJTable.createAdvanceRowAction() = createAction(
    name = "Advance Row",
    description = "Increment the current row",
    iconPath = "menu_icons/plus.png",
) {
    incrementSelectedRow()
}

fun SimbrainJTable.createApplyAndAdvanceAction(applyInputs: suspend (selectedRow: Int) -> Unit) = createAction(
    name = "Apply and Advance",
    description = "Apply current row as input and increment selected row",
    iconPath = "menu_icons/Step.png",
) {
    initRowSelection()
    applyInputs(selectedRow)
    incrementSelectedRow()
}

fun SimbrainJTable.createShowMatrixPlotAction() = createAction(
    name = "Show Matrix Plot",
    description = "Show plots (like correlation plot) that display pairwise relation between row vectors in this table.",
    iconPath = "menu_icons/grid.png",
) {

    val binaryOperations =
        arrayOf("Correlation", "Covariance", "Cosine Similarity", "Euclidean Distance", "Dot Product")

    val (data, rowNames) = if (selectedRows.size > 0) {
        getSelectedRowDoubleValues().toDoubleArray() to getSelectedRowNames()
    } else {
        model.get2DDoubleArray() to model.getAllRowNames()
    }

    val panel = JPanel(BorderLayout())

    var matrixPlotPanel = MatrixPlot(rowNames, computeCorrelationMatrix(data))

    val toolbar = JToolBar()
    toolbar.isFloatable = false
    toolbar.add(JLabel("Comparison Function: "))
    toolbar.add(JComboBox(binaryOperations).apply {
        addActionListener { e ->
            val functionSelected = (e?.source as? JComboBox<*>)?.selectedItem as? String
            val newPanel = when (functionSelected) {
                "Correlation" -> MatrixPlot(rowNames, computeCorrelationMatrix(data))
                "Covariance" -> MatrixPlot(rowNames, computeCovarianceMatrix(data))
                "Cosine Similarity" -> MatrixPlot(rowNames, computeCosineSimilarityMatrix(data))
                "Euclidean Distance" -> MatrixPlot(rowNames, computeSimilarityMatrix(data))
                "Dot Product" -> MatrixPlot(rowNames, computeDotProductMatrix(data))
                else -> MatrixPlot(rowNames, computeCorrelationMatrix(data))
            }
            panel.remove(matrixPlotPanel)
            matrixPlotPanel = newPanel
            panel.add(matrixPlotPanel, BorderLayout.CENTER)
            panel.revalidate()
        }
        maximumSize = Dimension(150, preferredSize.height)
    })
    toolbar.add(createAction(
        name = "Show preferences...",
        iconPath = "menu_icons/Prefs.png"
    ) {
        matrixPlotPanel.properties.createEditorDialog {
            matrixPlotPanel.repaint()
        }.also {
            it.title = "Text World Preferences"
        }.display()
    })

    panel.add(toolbar, BorderLayout.NORTH)
    panel.add(JScrollPane(matrixPlotPanel).apply { border = null }, BorderLayout.CENTER)

    panel.displayInDialog()

}