package org.simbrain.util.table

/**
 * Mutable 2d array of doubles.
 */
class DoubleDataWrapper(val data: Array<DoubleArray>): SimbrainDataModel() {

    constructor(nrows: Int, ncols: Int) : this(
        Array(nrows) {
            DoubleArray(ncols)
        }
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
        if (value is Double) {
            data[rowIndex][columnIndex] = value
        }
    }
}

