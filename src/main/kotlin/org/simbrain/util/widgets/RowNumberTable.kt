package org.simbrain.util.widgets

import org.simbrain.util.Theme
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.SimbrainDataFrame
import org.simbrain.util.table.SimbrainJTable
import org.simbrain.util.table.paintRowGroupRules
import org.simbrain.util.table.rowBandColor
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
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
import javax.swing.table.TableCellRenderer
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

    var rowNames = main.model.let { if (it is SimbrainDataFrame) it.getAllRowNames() else listOf() }
        set(value) {
            field = value
            revalidate()
        }

    init {
        main.addPropertyChangeListener(this)
        main.model.addTableModelListener(this)
        setShowGrid(true)
        intercellSpacing = Dimension(1, 1)
        setGridColor(Theme.divider)
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

    /**
     * Taken from the main table so the row numbers band along with it. Without this the bands stop at the
     * edge of the data and the row header reads as a separate, ungrouped table.
     */
    private val groupSize: Int? get() = (main as? SimbrainJTable)?.rowGroupSize?.invoke()

    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
        val component = super.prepareRenderer(renderer, row, column)
        // Only the banded case needs setting here, unlike in the main table: [RowNumberRenderer] resets the
        // background to the header's on every cell, so nothing is left over to bleed.
        rowBandColor(row, groupSize)?.let { component.background = it }
        return component
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        paintRowGroupRules(this, g, groupSize)
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
            rowNames = main.model.let { if (it is BasicDataFrame) it.getAllRowNames() else listOf() }
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