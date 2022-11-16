package org.simbrain.util.table

import org.simbrain.util.tryParsingDouble
import smile.math.matrix.Matrix

/**
 * Wraps a Smile Matrix.
 */
class MatrixDataWrapper(
    val data: Matrix,
    override var columns: MutableList<Column> = List(data.ncols()) {colNum ->
        Column("Column ${colNum+1}", Column.DataType.DoubleType)
    }.toMutableList()
) : SimbrainDataModel() {

    override val isMutable = true

    override fun getRowCount(): Int {
        return data.nrows()
    }

    override fun getColumnCount(): Int {
        return data.ncols()
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (validateRowIndex(rowIndex) && validateColumnIndex(columnIndex)) {
            return data.get(rowIndex, columnIndex)
        }
        return null
    }

    override fun setValueAt(value: Any?, rowIndex: Int, colIndex: Int) {
        if (validateRowIndex(rowIndex) && validateColumnIndex(colIndex)) {
                data.set(rowIndex, colIndex, tryParsingDouble(value))
                fireTableDataChanged()
            }
        }
}
