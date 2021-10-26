package org.simbrain.util.table

import org.pmw.tinylog.Logger

/**
 * Mutable 2d array of doubles.
 */
class DoubleDataWrapper(val data: Array<DoubleArray>): SimbrainDataModel() {

    constructor(nrows: Int, ncols: Int) : this(
        Array(nrows) {
            DoubleArray(ncols)
        }
    )

    /**
     * Create a single column table.
     */
    constructor(columnVector: DoubleArray) : this(
        columnVector.map{arrayOf(it).toDoubleArray()}.toTypedArray()
    )

    /**
     * Create a single column table.
     */
    constructor(columnVector: IntArray) : this(
        columnVector.map{arrayOf(it.toDouble()).toDoubleArray()}.toTypedArray()
    )


    override val isMutable = true

    override fun getDataTypeAtColumn(col: Int) = Double::class.java

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return data[0].size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return data[rowIndex][columnIndex]
    }

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (value is String) {
            try {
                data[rowIndex][columnIndex] = value.toDouble()
            } catch (e: NumberFormatException) {
                Logger.warn("Warning: A string was entered in the table")
            }
        }
        if (value is Double) {
            data[rowIndex][columnIndex] = value
        }
        fireTableDataChanged()
    }
}

