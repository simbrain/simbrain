package org.simbrain.util.table

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import smile.math.matrix.Matrix
import kotlin.reflect.KClass

/**
 * Mutable table whose columns have arbitrary types.
 */
class BasicDataFrame(
    data: List<List<Any?>>,
    override var columns: MutableList<Column> = inferColumns(data)
) : SimbrainDataFrame() {

    constructor(m: Int, n: Int, init: (Int) -> Any = { 0.0 }): this(MutableList(m) { MutableList(n, init) })

    override var isMutable = true

    var data: MutableList<MutableList<Any?>> = data.map { it.toMutableList() }.toMutableList()
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
            columns.removeAt(colIndex) // Also remove from columns list
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
        return columns.size
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
                Column.DataType.StringType -> if (value is String) block(value) else block(value?.toString() ?: "")
            }
        } catch (e: NumberFormatException) {
            // If we can't parse it, don't call the block - leave the original value unchanged
            // Could add logging here if needed: logger.debug("Failed to parse value $value for column type ${columns[colIndex].type}")
        }
    }

    override fun randomizeColumn(col: Int) {
        if (validateColumnIndex(col)) {
            // String case
            if (columns[col].type == Column.DataType.StringType) {
                randomizeStringColumn(col)
            } else {
                // Numeric case - only run this for non-string columns
                (0 until rowCount).forEach {
                    setValueAt(columns[col].getRandom(), it, col)
                }
            }
            fireTableDataChanged()
        }
    }

    fun randomizeStringColumn(col: Int) {
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
private fun inferColumns(data: List<List<*>>) =
    if (data.isEmpty()) {
        mutableListOf<Column>()
    } else {
        (0..data.first().lastIndex).map { i ->
            createColumn("Column ${i + 1}", data.asSequence().map { it[i] }.firstNotNullOfOrNull { it })
        }.toMutableList()
    }

private fun inferColumns(names: List<String?>, data: List<List<*>>) =
    if (data.isEmpty()) {
        mutableListOf<Column>()
    } else {
        (0..data.first().lastIndex).map { i ->
            createColumn(names.getOrNull(i) ?: "Column ${i + 1}", data.asSequence().map { it[i] }.firstNotNullOfOrNull { it })
        }.toMutableList()
    }

fun createFrom2DArray(
    data: Array<out Array<out Any?>>,
    options: ImportExportOptions = ImportExportOptions(),
    dataType: KClass<*>? = null
): BasicDataFrame {

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

    val valueParser = when (dataType) {
        Double::class -> { it: Any? -> (it as? String)?.toDouble() ?: it }
        Int::class -> { it: Any? -> (it as? String)?.toInt() ?: it }
        String::class -> { it: Any? -> it?.toString() ?: "" }
        else -> { it: Any? -> it }
    }

    return BasicDataFrame(mainData.map { it.map { cellValue -> valueParser(cellValue) }.toMutableList() }.toMutableList()).apply {
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


class ImportExportOptions(
    @UserParameter(label = "Include column names", description = "Include column names in the exported file")
    var includeColumnNames: Boolean = false,
    @UserParameter(label = "Include row names", description = "Include row names in the exported file")
    var includeRowNames: Boolean = false
) : EditableObject
