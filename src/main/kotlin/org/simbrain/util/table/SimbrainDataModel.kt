package org.simbrain.util.table

import org.simbrain.util.UserParameter
import org.simbrain.util.math.ProbabilityDistribution
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
     * Table-wide cell randomizer for arbitrary groups of cells.
     */
    @UserParameter(label = "Table Randomizer", isObjectType = true)
    var cellRandomizer = ProbabilityDistribution.Randomizer()

    /**
     * Map from column indices to randomizers.
     */
    // val columnRandomizer = (0 until columnCount).associateWith { NormalDistribution() }.toMutableMap()

    /**
     * Returns a column (assumed to be numeric) as a double array.
     */
    fun getDoubleColumn(col: Int): DoubleArray {
        if (isColumnNumeric(col)) {
            return (0 until rowCount)
                .map { (getValueAt(it, col) as Number).toDouble() }
                .toDoubleArray()
        }

        throw Error("getDoubleArray called on a non-numeric column")

    }

    fun getIntColumn(col: Int): IntArray {
        if (isColumnNumeric(col)) {
            return (0 until rowCount)
                .map { (getValueAt(it, col) as Number).toInt() }
                .toIntArray()
        }
        throw Error("getIntArray called on a non-numeric column")
    }


    /**
     * Returns all double columns as an array of double arrays.
     */
    fun getColumnMajorArray(): Array<DoubleArray> {
        return (0 until columnCount)
            .filter { isColumnNumeric(it) }
            .map { getDoubleColumn(it) }
            .toTypedArray()
    }

    fun columnsSameType(clazz: Class<*>): Boolean {
        return (0 until columnCount).all {
            getDataTypeAtColumn(it) == clazz
        }
    }

    private fun getDoubleRowUnsafe(row: Int): DoubleArray {
        // No type check
        return (0 until columnCount)
            .map { (getValueAt(row, it) as Number).toDouble() }
            .toDoubleArray()
    }

    fun getRowMajorDoubleArray(): Array<DoubleArray> {
        if (!columnsSameType(Double::class.java)) {
            throw Error("getDoubleArray called on a non-numeric column")
        }
        return (0 until rowCount)
            .map { getDoubleRowUnsafe(it) }
            .toTypedArray()
    }

    fun getColumnMajorIntArray(): Array<IntArray> {
        return (0 until columnCount)
            .filter { isColumnNumeric(it) }
            .map { getIntColumn(it) }
            .toTypedArray()
    }

    fun isColumnNumeric(col: Int): Boolean {
        // TODO:  Is there a concise way to do this with Kotlin number?
        return when (getDataTypeAtColumn(col)) {
            Double::class.java -> true
            Float::class.java -> true
            Int::class.java -> true
            Byte::class.java -> true
            else -> false
        }

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

