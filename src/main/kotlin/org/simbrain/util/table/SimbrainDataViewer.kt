package org.simbrain.util.table

import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTable
import org.simbrain.util.StandardDialog
import org.simbrain.util.cartesianProduct
import org.simbrain.util.widgets.RowNumberTable
import smile.math.matrix.Matrix
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.text.JTextComponent


/**
 *
 * The main Simbrain table visualization. Can be used to represent mutable or immutable data, which can be numeric or
 * mixed. Provides ability to edit the table, randomize numeric values, produce plots and visualizations, etc.
 *
 * Visualization for [SimbrainDataModel], which in turns wraps several types of table. Depending on whether the
 * model is mutable or not, different GUI actions are enabled. These actions can be further customized  depending on
 * the context.
 */
class SimbrainDataViewer(val model: SimbrainDataModel, val useDefaultToolbarAndMenu: Boolean = true) : JPanel() {

    val table = DataViewerTable(model)
    val toolbar = JToolBar()

    init {
        layout = MigLayout()

        add(toolbar, "wrap")
        if (useDefaultToolbarAndMenu) {
            initDefaultToolbarAndMenu()
        }

        // Scroll panel
        val scrollPane = JScrollPane(table)
        val rowTable: JTable = RowNumberTable(table)
        // scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.setRowHeaderView(rowTable)
        scrollPane.setCorner(
            JScrollPane.UPPER_LEFT_CORNER,
            rowTable.tableHeader
        )
        add(scrollPane, "wrap")
    }

    fun initDefaultToolbarAndMenu() {
        if (model.isMutable) {
            addAction(table.fillAction)
            addAction(table.zeroFillAction)
            addAction(table.randomizeAction)
            addAction(table.editRadomizerAction)
            addAction(table.randomizeColumnAction)
        }
        if (model is DataFrameWrapper) {
            addAction(table.importArff)
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

class DataViewerTable(val model: SimbrainDataModel) : JXTable(model) {

    val popUpMenu = JPopupMenu()

    init {
        columnSelectionAllowed = true

        setGridColor(Color.gray)

        mouseListeners.forEach { l -> removeMouseListener(l) }
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popUpMenu.show(this@DataViewerTable, e.x, e.y)
                }
            }
        })
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return model.isMutable
    }

    fun getSelectedCells(): List<Pair<Int, Int>> {
        return selectedRows.toList().cartesianProduct(selectedColumns.toList())
    }

    fun randomizeSelectedCells() {
        getSelectedCells().forEach { (x, y) ->
            model.setValueAt(model.cellRandomizer.random, x, y)
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
    // val model = DataFrameWrapper(Read.arff("simulations/tables/iris.arff"))
    val model = DoubleDataWrapper(Matrix.randn(10, 4).toArray())

    StandardDialog().apply {
        val table = SimbrainDataViewer(model)
        contentPane = table
        isVisible = true
        pack()
        setLocationRelativeTo(null)
    }
}
