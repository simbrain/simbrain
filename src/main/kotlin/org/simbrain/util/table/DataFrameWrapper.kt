package org.simbrain.util.table

import smile.data.DataFrame
import smile.data.type.*

/**
 * Wrapper for Smile DataFrame. These are immutable tables similar to pandas dataframes.
 */
class DataFrameWrapper(var df : DataFrame): SimbrainDataModel() {

    override val isMutable = false

    override var columns: MutableList<Column>
        get() = TODO()
        set(value) = TODO()

    override fun getRowCount(): Int {
        return df.nrows()
    }

    override fun getColumnCount(): Int {
        return df.ncols()
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
            Double::class.java -> Column.DataType.DoubleType
            Float::class.java -> Column.DataType.DoubleType
            Integer::class.java -> Column.DataType.IntType
            Byte::class.java -> Column.DataType.IntType
            else-> Column.DataType.DoubleType
        }
        else -> Column.DataType.DoubleType
    }
}