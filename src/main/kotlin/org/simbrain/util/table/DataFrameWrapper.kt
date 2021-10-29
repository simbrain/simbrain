package org.simbrain.util.table

import smile.data.DataFrame
import smile.data.type.*

/**
 * Wrapper for Smile DataFrame. These are immutable tables similar to pandas dataframes.
 */
class DataFrameWrapper(var df : DataFrame): SimbrainDataModel() {

    override val isMutable = false

    override fun getDataTypeAtColumn(col: Int) = df.types()[col].getType()

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

fun DataType.getType(): Class<*> {
    return when (this) {
        is DoubleType -> Double::class.java
        is FloatType -> Float::class.java
        is IntegerType -> Int::class.java
        is StringType -> String::class.java
        is ByteType -> Byte::class.java
        else -> Double::class.java
    }
}