package org.simbrain.util.table

import org.simbrain.util.UserParameter
import org.simbrain.util.isIntegerValued
import org.simbrain.util.isRealValued
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.data.type.DataType
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

    /**
     * Index list of column classes. Previously overrode [getColumnClass] but this created problems.
     */
    val columnClasses: List<Class<*>>
        get() = columns.map { it.type.clazz() }

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
    var cellRandomizer = UniformRealDistribution()

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
        throw Error("getIntColumn called on a non-numeric column")
    }

    fun getStringColumn(col: Int): Array<String> {
        if (columns[col].type == Column.DataType.StringType ) {
            return (0 until rowCount)
                .map { (getValueAt(it, col) as String) }
                .toTypedArray()
        }
        throw Error("getStringColumn called on a column that is not a String")
    }

    /**
     * Returns all double columns as an array of double arrays.
     */
    fun getColumnMajorArray(): Array<DoubleArray> {
        return (0 until columnCount)
            .filter { columns[it].isNumeric() }
            .map { getDoubleColumn(it) }
            .toTypedArray()
    }

    /**
     * If all columns in [colIndices] are instances of one of the types in [classes], return true
     */
    fun columnsOfType(colIndices: List<Int>, vararg classes: Class<*>): Boolean {
        return colIndices.all {
            classes.contains(columnClasses[it])
        }
    }

    /**
     * Ensure all columns have the indicated type or types.
     */
    fun columnsOfType(vararg classes: Class<*>): Boolean {
        return  columnsOfType((0 until columnCount).toList(), *classes)
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

    /**
     * Returns a 2d double array using provided column indices.
     *
     * Note that numeric types are cast to doubles.
     */
    fun get2DDoubleArray(colIndices: List<Int>): Array<DoubleArray> {
        if (!columnsOfType(colIndices, Double::class.java, Int::class.java, Float::class.java)) {
            throw Error("getDoubleArray called on a non-double column")
        }
        return (0 until rowCount)
            .map { rowIndex ->
                colIndices.map { colIndex ->
                    (getValueAt(rowIndex, colIndex) as Number).toDouble()
                }.toDoubleArray()
            }.toTypedArray()
    }


    /**
     * Returns a 2d double array using columns in the provided range.
     */
    fun get2DDoubleArray(indices: IntRange): Array<DoubleArray> {
        return get2DDoubleArray(indices.toList())
    }

    /**
     * Returns an array of double array rows for the table (comparable to "row major" order).
     *
     * Numeric types are cast to doubles.
     */
    fun get2DDoubleArray(): Array<DoubleArray> {
        if (!columnsOfType(Double::class.java)) {
            throw Error("getDoubleArray called on a non-numeric column")
        }
        return (0 until rowCount)
            .map { getDoubleRowUnsafe(it) }
            .toTypedArray()
    }

    /**
     * Returns an array of float array rows for the table (comparable to "row major" order).
     *
     * Doubles and ints are cast to floats.
     */
    fun getFloat2DArray(): Array<FloatArray> {
        if (!columnsOfType(Double::class.java, Int::class.java)) {
            throw Error("getFloat2DArray called on a non-numeric column")
        }
        return (0 until rowCount)
            .map { getFloatRowUnsafe(it) }
            .toTypedArray()
    }

    /**
     * Returns an array of float array columns for the table.
     *
     * Doubles and ints are cast to floats.
     */
    fun getColumnMajorIntArray(): Array<IntArray> {
        return (0 until columnCount)
            .filter { columns[it].isNumeric() }
            .map { getIntColumn(it) }
            .toTypedArray()
    }

    open fun randomizeColumn(col: Int) {}

    /**
     * Override to provide this functionality.
     */
    open fun insertColumn(selectedColumn: Int) {}

    open fun deleteColumn(selectedColumn: Int, fireEvent: Boolean = true) {}

    open fun setColumnNames(columnNames: List<String?>) {
        columns = columns.mapIndexed { i, col ->
            Column(columnNames.getOrNull(i) ?: "Column ${i + 1}", col.type)
        }.toMutableList()
    }

    open fun insertRow(selectedRow: Int) {}

    open fun deleteRow(selectedRow: Int, fireEvent: Boolean = true) {}

    fun insertRowAtBottom() {
        insertRow(rowCount)
    }

    fun deleteLastRow() {
        deleteRow(rowCount - 1 , true)
    }

}

class Column(
    @UserParameter(label = "Name", order = 1)
    val columName: String,

    @UserParameter(label = "Type", order = 2)
    var type: DataType = DataType.DoubleType
) : EditableObject {

    /**
     * Construct a column using a Smile data type object.
     */
    constructor(name: String, smileDataType: smile.data.type.DataType) : this(
        name,
        smileToSimbrainDataType(smileDataType)
    )

    @UserParameter(label = "Enabled", order = 10)
    var enabled = true

    /**
     * Randomizer for this column.
     */
    @UserParameter(label = "Column Randomizer", order = 20)
    var columnRandomizer: ProbabilityDistribution = UniformRealDistribution()

    fun getRandom(): Number {
        if (type == DataType.DoubleType) {
            return columnRandomizer.sampleDouble()
        } else if (type == DataType.IntType) {
            return columnRandomizer.sampleInt()
        }
        return 0
    }

    override val name: String
        get() = columName

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

fun smileToSimbrainDataType(smileDataType: DataType): Column.DataType {
    return when (smileDataType.id()) {
        DataType.ID.Double -> Column.DataType.DoubleType
        DataType.ID.Integer -> Column.DataType.IntType
        else -> Column.DataType.StringType
    }
}
