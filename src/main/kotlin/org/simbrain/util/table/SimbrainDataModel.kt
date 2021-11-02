package org.simbrain.util.table

import org.simbrain.util.UserParameter
import org.simbrain.util.math.ProbabilityDistribution
import org.simbrain.util.propertyeditor.EditableObject
import java.util.*
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

    // TODO: Remove when we use the overridden versino
    /**
     * Returns the data type of a column, as a Java class.
     */
    open fun getDataTypeAtColumn(col: Int) = columns[col].type.clazz()

    // override fun getColumnClass(col: Int): Class<*> {
    //     return columns.get(col).type.clazz()
    // }

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

    // TODO: Possibly remove or merge with overriding version
    open fun randomizeColumn(col: Int) {
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

class Column (
    @UserParameter(label = "Name", order =  1)
    val columName: String,

    @UserParameter(label = "Type", order = 2)
    val type: DataType
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
            // TODO: Something better for int, and make the randomizer type depend on type
            return columnRandomizer.random.toInt().toDouble()
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



