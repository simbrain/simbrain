package org.simbrain.util.widgets

import org.simbrain.util.table.BasicDataFrame
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.UIManager
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn

/*
 *	Use a JTable as a renderer for row numbers of a given main table.
 *  This table must be added to the row header of the scrollpane that
 *  contains the main table.
 *
 * From https://tips4java.wordpress.com/2008/11/18/row-number-table/
 *
 * @author Rob Camick
 */
class RowNumberTable(private val main: JTable) : JTable(), ChangeListener, PropertyChangeListener, TableModelListener {

    var rowNames = main.model.let { if (it is BasicDataFrame) it.rowNames else listOf() }
        set(value) {
            field = value
            revalidate()
        }

    init {
        main.addPropertyChangeListener(this)
        main.model.addTableModelListener(this)
        setGridColor(Color.gray)
        isFocusable = false
        setAutoCreateColumnsFromModel(false)
        setSelectionModel(main.selectionModel)
        val column = TableColumn()
        column.headerValue = " "
        addColumn(column)
        column.cellRenderer = RowNumberRenderer()
        getColumnModel().getColumn(0).preferredWidth = 50
        preferredScrollableViewportSize = preferredSize
    }

    override fun addNotify() {
        super.addNotify()
        //  Keep scrolling of the row table in sync with the main table.
        (parent as? JViewport)?.addChangeListener(this)
    }

    /*
     *  Delegate method to main table
     */
    override fun getRowCount(): Int = main.rowCount

    override fun getRowHeight(row: Int): Int {
        val rowHeight = main.getRowHeight(row)
        if (rowHeight != super.getRowHeight(row)) {
            super.setRowHeight(row, rowHeight)
        }
        return rowHeight
    }

    /*
     *  No model is being used for this table so just use the row number
     *  as the value of the cell.
     */
    override fun getValueAt(row: Int, column: Int): Any {
        return rowNames.getOrNull(row) ?: (row + 1).toString()
    }

    /*
     *  Don't edit data in the main TableModel by mistake
     */
    override fun isCellEditable(row: Int, column: Int) = false

    /*
     *  Do nothing since the table ignores the model
     */
    override fun setValueAt(value: Any, row: Int, column: Int) {}

    //
    //  Implement the ChangeListener
    //
    override fun stateChanged(e: ChangeEvent) {
        //  Keep the scrolling of the row table in sync with main table
        val viewport = e.source as JViewport
        val scrollPane = viewport.parent as JScrollPane
        scrollPane.verticalScrollBar.value = viewport.viewPosition.y
    }

    //
    //  Implement the PropertyChangeListener
    //
    override fun propertyChange(e: PropertyChangeEvent) {
        //  Keep the row table in sync with the main table
        if ("selectionModel" == e.propertyName) {
            setSelectionModel(main.selectionModel)
        }
        if ("rowHeight" == e.propertyName) {
            repaint()
        }
        if ("model" == e.propertyName) {
            main.model.addTableModelListener(this)
            revalidate()
        }
    }

    //
    //  Implement the TableModelListener
    //
    override fun tableChanged(e: TableModelEvent) {
        if (main != null) { // do not simplify this. the super constructor calls this before main is set up.
            rowNames = main.model.let { if (it is BasicDataFrame) it.rowNames else listOf() }
        }
        revalidate()
    }

    /*
     *  Attempt to mimic the table header renderer
     */
    private class RowNumberRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = CENTER
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            if (table != null) {
                val header = table.tableHeader
                if (header != null) {
                    foreground = header.foreground
                    background = header.background
                    font = header.font
                }
            }
            if (isSelected) {
                font = font.deriveFont(Font.BOLD)
            }
            text = value?.toString() ?: ""
            border = UIManager.getBorder("TableHeader.cellBorder")
            return this
        }
    }
}