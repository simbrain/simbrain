package org.simbrain.util.table

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTableHeader
import org.simbrain.util.cartesianProduct
import org.simbrain.util.displayInDialog
import org.simbrain.util.widgets.RowNumberTable
import java.awt.AWTEvent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Toolkit
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.table.TableModel
import javax.swing.text.JTextComponent


/**
 * The main Simbrain table visualization. Can be used to represent mutable or immutable data, which can be numeric or
 * mixed. Provides ability to edit the table, randomize numeric values, produce plots and visualizations, etc.
 *
 * Visualization for [SimbrainDataFrame], which in turns wraps several types of table. Depending on whether the
 * model is mutable or not, different GUI actions are enabled. These actions can be further customized  depending on
 * the context.
 */
open class SimbrainTablePanel @JvmOverloads constructor(
    model: SimbrainDataFrame,
    useDefaultToolbarAndMenu: Boolean = true,
    useHeaders: Boolean = true,
    usePadding: Boolean = true,
) : JPanel() {

    val table = SimbrainJTable(model, useHeaders)
    val toolbar by lazy {
        JToolBar().also { add(it, BorderLayout.NORTH) }
    }

    val scrollPane = DataViewerScrollPane(table, useHeaders)

    var model:SimbrainDataFrame
        get() = table.model
        set(value) {
            // TODO: Allow for structure changes
            (0 until value.rowCount).forEach {i ->
                (0 until value.columnCount).forEach{j ->
                    table.model.setValueAt(value.getValueAt(i,j),i,j)
                }
            }
        }

    init {

        // Putting the toolbar in the top part of a border layout to avoid problems with horizontal scrollbars in the
        // main panel
        layout = BorderLayout()

        val constraints = buildList {
            add("fillx")
            if (!usePadding) add("insets 0")
        }.joinToString(",")

        val mainPanel = JPanel(MigLayout(constraints))
        add(mainPanel)

        if (useDefaultToolbarAndMenu) {
            initDefaultToolbarAndMenu()
        }

        mainPanel.add(scrollPane, "grow")

        model.addTableModelListener {
            table.tableHeader?.revalidate()
            scrollPane.updateResizeMode(it.source as TableModel)
        }

        model.events.currentRowChanged.on {
            table.selectedRow = model.currentRowIndex
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
        if (model is SmileDataFrame) {
            addAction(table.showScatterPlotAction)
        }
        addAction(table.openProjectionAction)
        addSeparator()
        addAction(table.showHistogramAction)
        addAction(table.showBoxPlotAction)
        addAction(table.createShowMatrixPlotAction())
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

class DataViewerScrollPane(val table: JTable, useHeaders: Boolean = true): JScrollPane(table) {

    /**
     * Custom table with row numbers shown
     */
    val rowTable by lazy {
        RowNumberTable(table).apply {
            // Main mouse listener. Handle row selection and popup menu
            addMouseListener(object : MouseAdapter() {

                override fun mouseReleased(e: MouseEvent) {
                    val row = rowAtPoint(e.getPoint())
                    if (e.isControlDown || e.button == 1 && table is SimbrainJTable) {
                        for (j in 0 until table.columnCount) {
                            table.changeSelection(row, j, true, true)
                        }
                    }
                }
            })
        }
    }

    init {
        if (useHeaders) {
            setRowHeaderView(rowTable)
        }
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

class SimbrainJTable(val model: SimbrainDataFrame, useHeaders: Boolean = true) : JTable(model), CoroutineScope {

    private var job = SupervisorJob()

    override var coroutineContext = Dispatchers.Swing + job

    val popUpMenu = JPopupMenu()

    /**
     * If false, null entries cannot be edited.
     */
    var allowNullEditing = false

    init {
        columnSelectionAllowed = true
        rowSelectionAllowed = true

        if (useHeaders) {
            tableHeader = JXTableHeader(columnModel)
        }

        setGridColor(Color.gray)

        // Manages beginning and endings edits in cells, which is surprisingly hard to get right.
        val unfocusedEvent = AWTEventListener { event ->
            if (event is MouseEvent
                && event.id == MouseEvent.MOUSE_PRESSED
                && this@SimbrainJTable.isEditing) {

                val editor = this@SimbrainJTable.editorComponent
                if (editor != null && event.source !== editor) {
                    cellEditor.stopCellEditing()
                }
            }
        }

        // Ensure that the AWT Event is unregistered, because the event holds references to instance variables,
        // and so these table objects won’t be garbage collected
        addPropertyChangeListener("tableCellEditor") {
            if (isEditing) {
                Toolkit.getDefaultToolkit().addAWTEventListener(unfocusedEvent, AWTEvent.MOUSE_EVENT_MASK)
            } else {
                Toolkit.getDefaultToolkit().removeAWTEventListener(unfocusedEvent)
            }
        }

        // mouseListeners.forEach { l -> removeMouseListener(l) }
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popUpMenu.show(this@SimbrainJTable, e.x, e.y)
                }
            }
        })

        selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                model.currentRowIndex = selectedRow
            }
        }
    }

    fun setSelectedRow(row: Int) {
        if (row < 0 || row >= rowCount) {
            throw IllegalArgumentException("Invalid row index $row")
        }
        selectAll()
        setRowSelectionInterval(row, row)
        model.currentRowIndex = row
    }

    fun initRowSelection() {
        if (selectedRow == -1) selectedRow = 0
    }

    fun incrementSelectedRow() {
        // if none selected, select first row (because selectedRow returns -1 in that case)
        val nextRow = (model.currentRowIndex + 1) % model.rowCount
        model.currentRowIndex = nextRow
        selectedRow = nextRow
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
        if (getValueAt(row, column) == null && !allowNullEditing) {
            return false
        }
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

    fun getSelectedRowDoubleValues() = selectedRows.map { model.getRow<Double>(it) }

    fun getSelectedRowNames() = selectedRows.map { model.getRowName(it) }

}

fun main() {

    // val model = MatrixDataWrapper(Matrix.randn(10, 4))
    // val model = DataFrameWrapper(read.csv("simulations/tables/toy-test.txt", delimiter='\t', header=false))
    // val model = DataFrameWrapper(Read.arff("simulations/tables/iris.arff"))
    // val model = createFromDoubleArray(Matrix.randn(10, 4).toArray())

    val numbersWithNulls: MutableList<MutableList<Any?>> = mutableListOf(
        mutableListOf(1, 2, 3, null, 5),
        mutableListOf(6, null, 8, 9, 10),
        mutableListOf(null, 12, 13, 14, 15),
        mutableListOf(16, 17, null, 19, 20),
        mutableListOf(21, 22, 23, 24, null)
    )
    val model = BasicDataFrame(numbersWithNulls)
    SimbrainTablePanel(model).displayInDialog()



}
