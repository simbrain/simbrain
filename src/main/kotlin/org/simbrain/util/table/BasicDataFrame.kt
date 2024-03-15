package org.simbrain.util.table

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix

/**
 * Mutable table whose columns have arbitrary types.
 */
class BasicDataFrame(
    data: MutableList<MutableList<Any?>>,
    override var columns: MutableList<Column> = inferColumns(data)
) : SimbrainDataFrame() {

    constructor(m: Int, n: Int, init: (Int) -> Any = { 0.0 }): this(MutableList(m) { MutableList(n, init) })

    override var isMutable = true

    var data: MutableList<MutableList<Any?>> = data
        set(value) {
            field = value
            columns = inferColumns(columns.map { it.columName }, value)
        }

    /**
     * Insert column to left, unless the index is -1 (no selection) in which case it is added as the right-most column.
     */
    fun insertColumn(
        colIndex: Int,
        name: String = "New Column",
        type: Column.DataType = Column.DataType.DoubleType
    ) {
        val newColIndex = if (colIndex == -1) columnCount else colIndex
        if (colIndex in -1 until columnCount) {
            val newColumn = Column(name, type)
            columns.add(newColIndex, newColumn)
            data.forEach { row -> row.add(newColIndex, newColumn.type.defaultValue) }
            fireTableStructureChanged()
        }
    }

    override fun insertColumn(selectedColumn: Int) {
        insertColumn(selectedColumn, "New Column")
    }

    override fun deleteColumn(colIndex: Int, fireEvent: Boolean) {
        if (validateColumnIndex(colIndex)) {
            data.forEach { row -> row.removeAt(colIndex) }
            if (fireEvent) {
                fireTableStructureChanged()
            }
        }
    }

    /**
     * Insert row above, unless the index is -1 (no selection) in which case it is added as the bottom.
     */
    override fun insertRow(selectedRow: Int) {
        val newRowIndex = if (selectedRow == -1) rowCount else selectedRow
        if (selectedRow in -1..rowCount) {
            data.add(newRowIndex, MutableList(columnCount) { columns[it].type.defaultValue })
            swingInvokeLater {
                fireTableStructureChanged()
            }
        }
    }

    override fun setRow(selectedRow: Int, row: Array<out Any?>) {
        if (validateRowIndex(selectedRow) && row.size == columnCount) {
            data[selectedRow].forEachIndexed { index, _ ->
                setValueAt(row[index], selectedRow, index)
            }
            fireTableDataChanged()
        }
    }

    override fun deleteRow(rowIndex: Int, fireEvent: Boolean) {
        // Allowing removal of all rows causes weird behavior, so we just aren't allowing it
        //  TODO: Empty tables should be possible.
        if (rowCount == 1) {
            return
        }
        if (validateRowIndex(rowIndex)) {
            data.removeAt(rowIndex)
            if (fireEvent) {
                fireTableStructureChanged()
            }
        }
    }

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return data[0].size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (validateRowIndex(rowIndex) && validateColumnIndex(columnIndex)) {
            return data[rowIndex][columnIndex]
        }
        return null
    }

    override fun setValueAt(value: Any?, rowIndex: Int, colIndex: Int) {
        if (canEditAt(rowIndex, colIndex) && validateRowIndex(rowIndex) && validateColumnIndex(colIndex)) {
            withValidatedValue(value, colIndex) {
                data[rowIndex][colIndex] = it
                fireTableDataChanged()
            }
        }
    }

    /**
     * Tries to parse a value into the type associated with a column, and executes a block if the parse is successful.
     */
    fun withValidatedValue(value: Any?, colIndex: Int, block: (Any) -> Unit) {
        try {
            when (columns[colIndex].type) {
                Column.DataType.DoubleType -> block(tryParsingDouble(value))
                Column.DataType.IntType -> block(tryParsingInt(value))
                Column.DataType.StringType -> if (value is String) block(value)
            }
        } catch (e: NumberFormatException) {
            println("There was a problem parsing ${value} in a column of type ${columns[colIndex].type}")
        }
    }

    override fun randomizeColumn(col: Int) {
        if (validateColumnIndex(col)) {
            // String case
            if (columns[col].type == Column.DataType.StringType) {
                randomizeStringColum(col)
            }
            // Numeric case
            (0 until rowCount).forEach {
                setValueAt(columns[col].getRandom(), it, col)
            }
            fireTableDataChanged()
        }
    }

    fun randomizeStringColum(col: Int) {
        if (validateColumnIndex(col) && columns[col].type == Column.DataType.StringType ) {
            val options = getStringColumn(col).toSet().toList()
            (0 until rowCount).forEach {
                setValueAt(options.sampleWithoutReplacement().first(), it, col)
            }
            fireTableDataChanged()
        }
    }
}

/**
 * Infer a column from a 2d array of data.
 */
private fun inferColumns(data: MutableList<MutableList<Any?>>) =
    (0..data.first().lastIndex).map { i ->
        createColumn("Column ${i + 1}", data.asSequence().map { it[i] }.firstNotNullOfOrNull { it })
    }.toMutableList()

private fun inferColumns(names: List<String?>, data: MutableList<MutableList<Any?>>) =
    (0..data.first().lastIndex).map { i ->
        createColumn(names.getOrNull(i) ?: "Column ${i + 1}", data.asSequence().map { it[i] }.firstNotNullOfOrNull { it })
    }.toMutableList()

fun createFrom2DArray(data: Array<out Array<out Any?>>, options: ImportExportOptions = ImportExportOptions()): BasicDataFrame {

    val rawData = data.map { it.toMutableList() }.toMutableList()

    val columnNames = if (options.includeColumnNames) {
        rawData[0]
            .run { if (options.includeRowNames) drop(1) else this }
            .map { it.toString() }
    } else {
        null
    }

    val rowNames = if (options.includeRowNames) {
        rawData.map { row -> row[0] }
            .run { if (options.includeColumnNames) drop(1) else this }
            .map { it.toString() }
    } else {
        null
    }

    fun List<List<Any?>>.dropColumnHeaders() = if (options.includeColumnNames) drop(1) else this

    fun List<List<Any?>>.dropRowHeaders() = if (options.includeRowNames) map { row -> row.drop(1) } else this

    val mainData = rawData.dropColumnHeaders().dropRowHeaders()

    return BasicDataFrame(mainData.map { it.toMutableList() }.toMutableList()).apply {
        columnNames?.let { this.columnNames = it.toMutableList() }
        rowNames?.let { this.rowNames = it.toMutableList() }
    }
}

fun createFromDoubleArray(data: Array<DoubleArray>): BasicDataFrame {
    return BasicDataFrame(data.map { it.toMutableList() as MutableList<Any?> }.toMutableList())
}

fun createFromMatrix(data: Matrix): BasicDataFrame {
    return BasicDataFrame(
        data.toArray().map { it.toMutableList() as MutableList<Any?> }.toMutableList()
    ).apply {
        columnNames = (1..data.ncol()).map { "$it" }.toMutableList()
    }
}

fun createFromFloatArray(data: Array<FloatArray>): BasicDataFrame {
    return BasicDataFrame(data.map { it.toMutableList() as MutableList<Any?> }.toMutableList())
}

fun createBasicDataFrameFromColumn(data: DoubleArray): BasicDataFrame {
    return BasicDataFrame(data.map { mutableListOf(it as Any?) }.toMutableList())
}

fun createBasicDataFrameFromColumn(data: FloatArray): BasicDataFrame {
    return BasicDataFrame(data.map { mutableListOf(it as Any?) }.toMutableList())
}

fun createBasicDataFrameFromColumn(data: IntArray): BasicDataFrame {
    return BasicDataFrame(data.map { mutableListOf(it as Any?) }.toMutableList())
}

fun createBasicDataFrameFromColumn(data: Array<String>): BasicDataFrame {
    return BasicDataFrame(data.map { mutableListOf(it as Any?) }.toMutableList())
}


class ImportExportOptions : EditableObject {
    @UserParameter(label = "Include column names", description = "Include column names in the exported file")
    var includeColumnNames = false

    @UserParameter(label = "Include row names", description = "Include row names in the exported file")
    var includeRowNames = false
}
