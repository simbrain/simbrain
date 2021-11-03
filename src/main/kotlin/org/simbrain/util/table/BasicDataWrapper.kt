package org.simbrain.util.table

import org.pmw.tinylog.Logger

/**
 * Mutable table whose columns have arbitrary types.
 */
class BasicDataWrapper(var data: MutableList<MutableList<Any>>, columns: MutableList<Column>?): SimbrainDataModel
    () {

    // TODO
    override var columns: MutableList<Column>
        get() = _columns
        set(value) {
            _columns = value
        }

    private var _columns = data[0].mapIndexed { i, _ ->  Column("Column ${i + 1}", Column.DataType.IntType)}
        .toMutableList()

    override val isMutable = true

    // constructor(nrows: Int, ncols: Int) : this(
    //     Array(nrows) {
    //         DoubleArray(ncols)
    //     }
    // )

    /**
     * Create a single column table.
     */
    // constructor(columnVector: Array<Any>) : this(
    //     columnVector.map{arrayOf(it)}.toTypedArray()
    // )

    override fun insertColumn(colIndex: Int) {
        // TODO: Move this to a general check
        if (colIndex > -1 && colIndex < columnCount) {
            // TODO: Deal with columns and default values
            columns.add(colIndex, Column("Todo", Column.DataType.DoubleType))
            data.forEach { row -> row.add(colIndex, 0) }
            fireTableStructureChanged()
        }
    }

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return data[0].size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        return data[rowIndex][columnIndex]
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (value is String) {
            try {
                data[rowIndex][columnIndex] = value
            } catch (e: NumberFormatException) {
                Logger.warn("Warning: A string was entered in the table")
            }
        }
        if (value is Double) {
            data[rowIndex][columnIndex] = value
        }
        fireTableDataChanged()
    }

    override fun randomizeColumn(col: Int) {
        (0 until rowCount).forEach {
            setValueAt(columns[col].getRandom(), it, col)
        }
        fireTableDataChanged()

    }
}

fun createFromDoubleArray(data: Array<DoubleArray>) : BasicDataWrapper {
   return BasicDataWrapper(data.map {it.toMutableList() as MutableList<Any>}.toMutableList(), null) // TODO
}

fun createFromColumn(data: DoubleArray) : BasicDataWrapper {
    return BasicDataWrapper(data.map { mutableListOf(it as Any) }.toMutableList(), null)
}

fun createFromColumn(data: IntArray) : BasicDataWrapper {
    return BasicDataWrapper(data.map { mutableListOf(it as Any) }.toMutableList(), null)
}

