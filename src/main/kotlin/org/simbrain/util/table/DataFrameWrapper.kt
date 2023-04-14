package org.simbrain.util.table

import smile.data.DataFrame
import smile.data.type.*

/**
 * Wrapper for Smile DataFrame. These are immutable tables similar to pandas dataframes.
 */
@Deprecated("No clear use cases; Smile's DataFrame object can be used directly in most instances")
class DataFrameWrapper(var df : DataFrame): SimbrainDataModel() {

    override val isMutable = false

    // TODO: Add setter
    override var columns: MutableList<Column> = df.schema().fields().map { Column(it.name, it.type)  }.toMutableList()

    override fun getRowCount(): Int {
        return df.nrow()
    }

    override fun getColumnCount(): Int {
        return df.ncol()
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        return df[rowIndex, columnIndex]
    }

    override fun getColumnName(column: Int): String {
        return df.column(column).name()
    }
}

/**
 * Map from Smile data types to Simbrain data types
 */
fun DataType.getColumnDataType(): Column.DataType {
    return when (this) {
        is DoubleType -> Column.DataType.DoubleType
        is FloatType -> Column.DataType.DoubleType
        is IntegerType -> Column.DataType.IntType
        is StringType -> Column.DataType.StringType
        is ByteType -> Column.DataType.IntType
        is ObjectType -> when(objectClass) {
            java.lang.Double::class.java -> Column.DataType.DoubleType
            java.lang.Float::class.java -> Column.DataType.DoubleType
            java.lang.Integer::class.java -> Column.DataType.IntType
            java.lang.Byte::class.java -> Column.DataType.IntType
            else-> Column.DataType.DoubleType
        }
        else -> Column.DataType.DoubleType
    }
}