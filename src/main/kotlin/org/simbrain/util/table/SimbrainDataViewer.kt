package org.simbrain.util.table

import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTableHeader
import org.simbrain.util.cartesianProduct
import org.simbrain.util.displayInDialog
import org.simbrain.util.widgets.RowNumberTable
import smile.io.Read
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.table.TableModel
import javax.swing.text.JTextComponent

/**
 * The main Simbrain table visualization. Can be used to represent mutable or immutable data, which can be numeric or
 * mixed. Provides ability to edit the table, randomize numeric values, produce plots and visualizations, etc.
 *
 * Visualization for [SimbrainDataModel], which in turns wraps several types of table. Depending on whether the
 * model is mutable or not, different GUI actions are enabled. These actions can be further customized  depending on
 * the context.
 */
class SimbrainDataViewer(
    model: SimbrainDataModel,
    useDefaultToolbarAndMenu: Boolean = true
) : JPanel() {

    val table = DataViewerTable(model)
    val toolbar = JToolBar()

    val scrollPane = DataViewerScrollPane(table)

    var model:SimbrainDataModel = model
        set(value) {
            // Reset the table data
            field = value
            value.fireTableStructureChanged()
        }

    init {

        // Putting the toolbar in the top part of a border layout to avoid problems with horizontal scrollbars in the
        // main panel
        layout = BorderLayout()
        add(toolbar, BorderLayout.NORTH)

        // The main panel uses the mig layout
        val mainPanel = JPanel(MigLayout("fillx"))
        add(mainPanel)

        if (useDefaultToolbarAndMenu) {
            initDefaultToolbarAndMenu()
        }

        mainPanel.add(scrollPane, "grow")

        model.addTableModelListener {
            table.tableHeader.revalidate()
            scrollPane.updateResizeMode(it.source as TableModel)
        }
    }


    fun initDefaultToolbarAndMenu() {
        if (model.isMutable) {
            addAction(table.importCsv)
            addAction(table.importArff)
            addSeparator()
            addAction(table.insertColumnAction)
            addAction(table.deleteColumnAction)
            addAction(table.insertRowAction)
            addAction(table.deleteRowAction)
            addSeparator()
            addAction(table.fillAction)
            addAction(table.zeroFillAction)
            addAction(table.randomizeAction)
            addAction(table.editRandomizerAction)
            addSeparator()
            addAction(table.randomizeColumnAction)
            addAction(table.editColumnAction)
        }
        if (model is DataFrameWrapper) {
            addAction(table.showScatterPlotAction)
        }
        addSeparator()
        addAction(table.showHistogramAction)
        addAction(table.showBoxPlotAction)
    }

    fun addSeparator() {
        toolbar.addSeparator()
        table.popUpMenu.addSeparator()
    }

    /**
     * Add an action to both the toolbar and popupmenu.
     */
    fun addAction(a: AbstractAction) {
        toolbar.add(a)
        table.popUpMenu.add(a)
    }

}

class DataViewerScrollPane(val table: JTable): JScrollPane(table) {

    /**
     * Custom table with row numbers shown
     */
    val rowTable = RowNumberTable(table)

    init {
        setRowHeaderView(rowTable)
        updateResizeMode(table.model)
        setCorner(
            UPPER_LEFT_CORNER,
            rowTable.tableHeader
        )
    }

    /**
     * If less than 5 columns use auto-resize. Otherwise turn auto-resize off so that horizontal scroll bars work
     * property.
     */
    fun updateResizeMode(model: TableModel) {
        // TODO: It may be possible to achieve better results using model.getColumn.minWidth and
        //  AUTO_RESIZE_ALL_COLUMNS but we have not succeeded in this yet.
        if (model.columnCount < 5)  {
            table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        } else {
            table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        }
    }

}

class DataViewerTable(val model: SimbrainDataModel) : JTable(model) {

    val popUpMenu = JPopupMenu()

    init {
        columnSelectionAllowed = true

        tableHeader = JXTableHeader(columnModel)

        setGridColor(Color.gray)

        // mouseListeners.forEach { l -> removeMouseListener(l) }
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popUpMenu.show(this@DataViewerTable, e.x, e.y)
                }
                // We need a way to commit cell contents even clicking outside of the cell.
                // This won't work because it's local to the window. We don't even know if
                // stopCEllEditing() will force the commit.
                // getCellEditor().stopCellEditing()
            }
        })
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return model.isMutable
    }

    fun getSelectedCells(): List<Pair<Int, Int>> {
        return selectedRows.toList().cartesianProduct(selectedColumns.toList())
    }

    fun insertColumn() {
        model.insertColumn(selectedColumn)
    }
    fun insertRow() {
        model.insertRow(selectedRow)
    }

    fun deleteSelectedColumns() {
        for (i in 0 until selectedColumns.size) {
            if (columnCount <= 0) {
                break
            }
            model.deleteColumn(selectedColumn, false)
        }
        model.fireTableStructureChanged()
    }

    fun deleteSelectedRows() {
        for (i in 0 until selectedRows.size) {
            // Allowing removal of all rows causes weird behavior, so we just aren't allowing it
            //  TODO: Empty tables should be possible.
            if (rowCount <= 1) {
                break
            }
            model.deleteRow(selectedRow, false)
        }
        model.fireTableStructureChanged()
    }

    fun randomizeSelectedCells() {
        getSelectedCells().forEach { (x, y) ->
            model.setValueAt(model.cellRandomizer.sampleDouble(), x, y)
        }
    }

    fun fillSelectedCells(fillVal: Double) {
        getSelectedCells().forEach { (x, y) ->
            model.setValueAt(fillVal, x, y)
        }
    }

    fun zeroFillSelectedCells() {
        fillSelectedCells(0.0)
    }

    //
    // Improved cell editing courtesy of camick!
    //
    // http://www.camick.com/java/source/RXTable.java
    //
    private val isSelectAllForMouseEvent = true
    private val isSelectAllForActionEvent = true
    private val isSelectAllForKeyEvent = true

    /*
     * Override to provide Select All editing functionality
     */
    override fun editCellAt(row: Int, column: Int, e: EventObject?): Boolean {
        val result = super.editCellAt(row, column, e)
        if (isSelectAllForMouseEvent || isSelectAllForActionEvent || isSelectAllForKeyEvent) {
            selectAll(e)
        }
        return result
    }

    /*
     * Select the text when editing on a text related cell is started
     */
    private fun selectAll(e: EventObject?) {
        val editor = editorComponent
        if (editor == null || editor !is JTextComponent) return
        if (e == null) {
            editor.selectAll()
            return
        }

        // Typing in the cell was used to activate the editor
        if (e is KeyEvent && isSelectAllForKeyEvent) {
            editor.selectAll()
            return
        }

        // F2 was used to activate the editor
        if (e is ActionEvent && isSelectAllForActionEvent) {
            editor.selectAll()
            return
        }

        // A mouse click was used to activate the editor.
        // Generally this is a double click and the second mouse click is
        // passed to the editor which would remove the text selection unless
        // we use the invokeLater()
        if (e is MouseEvent && isSelectAllForMouseEvent) {
            SwingUtilities.invokeLater { editor.selectAll() }
        }

        // Camick end
    }
}

fun main() {

    // val model = DataFrameWrapper(read.csv("simulations/tables/toy-test.txt", delimiter='\t', header=false))
    val model = DataFrameWrapper(Read.arff("simulations/tables/iris.arff"))
    // val model = createFromDoubleArray(Matrix.randn(10, 4).toArray())
    SimbrainDataViewer(model).displayInDialog()

}
