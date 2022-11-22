package org.simbrain.util.table

import org.simbrain.util.tryParsingDouble
import smile.math.matrix.Matrix

/**
 * Wraps a Smile Matrix.
 */
class MatrixDataWrapper(
    var data: Matrix,
    override var columns: MutableList<Column> = List(data.ncols()) { colNum ->
        Column("Column ${colNum + 1}", Column.DataType.DoubleType)
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

    override fun insertRow(rowIndex: Int) {
        val newRowIndex = if (rowIndex == -1) rowCount else rowIndex
        if (newRowIndex in -1 .. rowCount) {
            val oldData = data
            data = Matrix(data.nrows() + 1, data.ncols())
            for (i in 0 until newRowIndex)
                for (j in 0 until data.ncols())
                    data[i,j] = oldData[i,j]
            for (i in newRowIndex + 1 until data.nrows())
                for (j in 0 until data.ncols())
                    data[i,j] = oldData[i-1,j]

            fireTableStructureChanged()
        }
    }

    override fun deleteRow(rowIndex: Int, fireEvent: Boolean) {
        if (rowCount == 1) {
            return
        }
        if (validateRowIndex(rowIndex)) {
            val oldData = data
            data = Matrix(data.nrows() - 1, data.ncols())
            for (i in 0 until rowIndex)
                for (j in 0 until data.ncols())
                    data[i,j] = oldData[i,j]
            for (i in rowIndex + 1 until data.nrows())
                for (j in 0 until data.ncols())
                    data[i-1,j] = oldData[i,j]
            if (fireEvent) {
                fireTableStructureChanged()
            }
        }
    }
}
