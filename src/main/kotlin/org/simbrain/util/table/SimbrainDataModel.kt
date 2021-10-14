package org.simbrain.util.table

import java.util.*
import javax.swing.table.AbstractTableModel

/**
 * Abstract data model that is viewed by [SimbrainDataViewer]. Subclasses wrap particular
 * table implementations.
 */
abstract class SimbrainDataModel() : AbstractTableModel() {

    /**
     * True if cells can be edited, and if the table structure can be edited.
     */
    abstract val isMutable: Boolean

    /**
     * Returns the data type of a column, as a Java class.
     */
    abstract fun getDataTypeAtColumn(col: Int): Class<*>

    /**
     * Returns a column (assumed to be numeric) as a double array.
     */
    fun getDoubleArray(col: Int): DoubleArray {
        if(getDataTypeAtColumn(col) == Float::class.java) {
            return (0 until rowCount)
                .map{(getValueAt(it, col) as Float).toDouble()}
                .toDoubleArray()
        }
        if(getDataTypeAtColumn(col) == Int::class.java) {
            return (0 until rowCount)
                .map{(getValueAt(it, col) as Int).toDouble()}
                .toDoubleArray()
        }
        if(getDataTypeAtColumn(col) == Byte::class.java) {
            return (0 until rowCount)
                .map{(getValueAt(it, col) as Byte).toDouble()}
                .toDoubleArray()
        }
        if(getDataTypeAtColumn(col) == Double::class.java) {
            return (0 until rowCount)
                .map{getValueAt(it, col) as Double}
                .toDoubleArray()
        }

        throw Error("getDoubleArray called on a non-numeric column")

    }

    /**
     * Returns all double columns as an array of double arrays.
     */
    fun getColumnMajorArray() : Array<DoubleArray> {
        return (0 until columnCount)
            .filter{getDataTypeAtColumn(it) == Double::class.java}
            .map{getDoubleArray(it)}
            .toTypedArray()
    }

    fun randomizeColumn(col: Int) {
        // TODO: Check datatype of column
        if (isMutable) {
            // TODO: Provide the random function and have it be associated with a column
            // See NumericTable
            val rand = Random()
            (0 until rowCount).forEach {
                setValueAt(rand.nextDouble(), it, col)
            }
            fireTableDataChanged()
        }
    }


}

