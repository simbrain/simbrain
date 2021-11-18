package org.simbrain.util.table

import org.simbrain.util.UserParameter
import org.simbrain.util.isIntegerValued
import org.simbrain.util.isRealValued
import org.simbrain.util.math.ProbabilityDistribution
import org.simbrain.util.propertyeditor.EditableObject
import javax.swing.table.AbstractTableModel

/**
 * Abstract data model that is viewed by [SimbrainDataViewer]. Subclasses wrap particular
 * table implementations.
 */
abstract class SimbrainDataModel() : AbstractTableModel() {

    abstract var columns: MutableList<Column>

    // constructor with list of strings
    // constructor with list of datatypes
    // constructor with both
    // But replace construction logic with function

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return columns[columnIndex].type.clazz()
    }

    override fun getColumnName(col: Int): String {
        return columns[col].name
    }

    /**
     * True if cells can be edited, and if the table structure can be edited.
     */
    abstract val isMutable: Boolean

    /**
     * Table-wide cell randomizer for arbitrary groups of cells.
     */
    @UserParameter(label = "Table Randomizer")
    var cellRandomizer = ProbabilityDistribution.Randomizer()


    /**
     * Check that the provided column index is within range
     */
    fun validateColumnIndex(colIndex: Int): Boolean {
        return colIndex in 0 until columnCount
    }

    /**
     * Check that the provided row index is within range
     */
    fun validateRowIndex(rowIndex: Int): Boolean {
        return rowIndex in 0 until rowCount
    }

    /**
     * Returns a column (assumed to be numeric) as a double array.
     */
    fun getDoubleColumn(col: Int): DoubleArray {
        if (columns[col].isNumeric()) {
            return (0 until rowCount)
                .map { (getValueAt(it, col) as Number).toDouble() }
                .toDoubleArray()
        }
        throw Error("getDoubleColumn called on a non-numeric column")
    }

    fun getFloatColumn(col: Int): FloatArray {
        if (columns[col].isNumeric()) {
            return (0 until rowCount)
                .map { (getValueAt(it, col) as Number).toFloat() }
                .toFloatArray()
        }
        throw Error("getFloatColumn called on a non-numeric column")
    }

    fun getIntColumn(col: Int): IntArray {
        if (columns[col].isNumeric()) {
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
            .filter { columns[it].isNumeric()}
            .map { getDoubleColumn(it) }
            .toTypedArray()
    }

    fun columnsSameType(clazz: Class<*>): Boolean {
        return (0 until columnCount).all {
            getColumnClass(it) == clazz
        }
    }

    private fun getDoubleRowUnsafe(row: Int): DoubleArray {
        // No type check
        return (0 until columnCount)
            .map { (getValueAt(row, it) as Number).toDouble() }
            .toDoubleArray()
    }

    private fun getFloatRowUnsafe(row: Int): FloatArray {
        // No type check
        return (0 until columnCount)
            .map { (getValueAt(row, it) as Number).toFloat() }
            .toFloatArray()
    }

    fun getRowMajorDoubleArray(): Array<DoubleArray> {
        if (!columnsSameType(Double::class.java)) {
            throw Error("getDoubleArray called on a non-numeric column")
        }
        return (0 until rowCount)
            .map { getDoubleRowUnsafe(it) }
            .toTypedArray()
    }

    fun getRowMajorFloatArray(): Array<FloatArray> {
        if (!columnsSameType(Double::class.java)) {
            throw Error("getFloatArray called on a non-numeric column")
        }
        return (0 until rowCount)
            .map { getFloatRowUnsafe(it) }
            .toTypedArray()
    }

    fun getColumnMajorIntArray(): Array<IntArray> {
        return (0 until columnCount)
            .filter { columns[it].isNumeric()}
            .map { getIntColumn(it) }
            .toTypedArray()
    }

    open fun randomizeColumn(col: Int) {}

    /**
     * Override to provide this functionality.
     */
    open fun insertColumn(selectedColumn: Int) {}

    open fun deleteColumn(selectedColumn: Int, fireEvent: Boolean = true) {}

    open fun insertRow(selectedRow: Int) {}

    open fun deleteRow(selectedRow: Int, fireEvent: Boolean = true) {}

}

class Column(
    @UserParameter(label = "Name", order = 1)
    val columName: String,

    @UserParameter(label = "Type", order = 2)
    var type: DataType = DataType.DoubleType
) : EditableObject {

    @UserParameter(label = "Enabled", order = 10)
    var enabled = true

    /**
     * Randomizer for this column.
     */
    @UserParameter(label = "Column Randomizer", isEmbeddedObject = true, order = 20)
    var columnRandomizer = ProbabilityDistribution.Randomizer()

    fun getRandom(): Number {
        if (type == DataType.DoubleType) {
            return columnRandomizer.random
        } else if (type == DataType.IntType) {
            return columnRandomizer.randomInt
        }
        return 0
    }

    override fun getName(): String {
        return columName
    }

    enum class DataType {
        DoubleType {
            override fun clazz(): Class<*> {
                return Double::class.java
            }
        },
        IntType {
            override fun clazz(): Class<*> {
                return Int::class.java
            }
        },
        StringType {
            override fun clazz(): Class<*> {
                return String::class.java
            }
        };

        abstract fun clazz(): Class<*>

    }

    fun isNumeric(): Boolean {
        return type == DataType.DoubleType || type == DataType.IntType
    }

}

/**
 * Create a column from a value of unknown type. Try treating it as integer, then double, and if that fails treat it as
 * a String.
 */
fun createColumn(name: String, value: Any?): Column {
    if (value.isIntegerValued()) {
        return Column(name, Column.DataType.IntType)
    }
    if (value.isRealValued()) {
        return Column(name, Column.DataType.DoubleType)
    }
    if (value is String) {
        try {
            value.toInt()
            return Column(name, Column.DataType.IntType)
        } catch (e: NumberFormatException) {
            // Do nothing, move on to the next case
        }
        try {
            value.toDouble()
            return Column(name, Column.DataType.DoubleType)
        } catch (e: NumberFormatException) {
            // Do nothing, move on to the next case
        }
    }
    return Column(name, Column.DataType.StringType)

}

fun getDataType(clazz: Class<*>): Column.DataType {
    return when (clazz) {
        Double::class.java -> Column.DataType.DoubleType
        Float::class.java -> Column.DataType.DoubleType
        Int::class.java -> Column.DataType.IntType
        Byte::class.java -> Column.DataType.IntType
        String::class.java -> Column.DataType.StringType
        else -> Column.DataType.StringType
    }
}



