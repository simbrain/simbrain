/**
 * How the text of a csv file lands in a table: which cells become column names, row names and data, and what type
 * each column takes. [CsvImportDialog] previews a [CsvImportPlan] and the import action applies that same plan, so
 * the preview shows exactly what the table receives.
 */
package org.simbrain.util.table

import smile.math.matrix.Matrix
import kotlin.reflect.KClass

/**
 * The outcome of reading a csv file with a given set of options, before anything is written to a table.
 *
 * @param columns name and type of each resulting column
 * @param rowNames names taken from the file, or null when rows are just numbered
 * @param cells the data cells as they appear in the file, one list per row, each [columns] wide
 */
class CsvImportPlan(
    val columns: List<Column>,
    val rowNames: List<String>?,
    val cells: List<List<String>>
) {

    val rowCount get() = cells.size

    val columnCount get() = columns.size

    /**
     * The value a cell takes in the table, or null if its text cannot be read as the column's type.
     */
    fun parse(row: Int, col: Int): Any? = columns[col].type.parseOrNull(cells[row][col])

    /**
     * The value written to the table. Text that does not fit the column's type becomes the type's default.
     */
    fun valueAt(row: Int, col: Int): Any = parse(row, col) ?: columns[col].type.defaultValue

    /**
     * How many cells hold text that does not fit their column's type. An empty cell in a numeric column is one of
     * them.
     */
    val unreadableCount: Int by lazy {
        val typedColumns = columns.indices.filter { columns[it].type != Column.DataType.StringType }
        cells.sumOf { row -> typedColumns.count { col -> columns[col].type.parseOrNull(row[col]) == null } }
    }

    /**
     * Why this plan cannot be imported, or null if it can.
     *
     * @param requiredColumnCount the number of columns the table insists on, or null if it takes the file's shape
     */
    fun blockingProblem(requiredColumnCount: Int?): String? = when {
        rowCount == 0 || columnCount == 0 -> "There is no data to import."
        requiredColumnCount != null && columnCount != requiredColumnCount ->
            "This table has $requiredColumnCount ${"column".pluralize(requiredColumnCount)}, " +
                    "but the file has $columnCount with these settings."
        else -> null
    }
}

private fun String.pluralize(count: Int) = if (count == 1) this else "${this}s"

private fun Column.DataType.parseOrNull(text: String): Any? = when (this) {
    Column.DataType.DoubleType -> text.trim().toDoubleOrNull()
    Column.DataType.IntType -> text.trim().toIntOrNull()
    Column.DataType.StringType -> text
}

private fun String.isNumber() = trim().toDoubleOrNull() != null

/**
 * Rows of [raw] padded with empty cells to the width of the widest row.
 */
private fun rectangular(raw: Array<Array<String>>): List<List<String>> {
    val width = raw.maxOfOrNull { it.size } ?: 0
    return raw.map { row -> List(width) { row.getOrNull(it) ?: "" } }
}

/**
 * Work out how the parsed csv text in [raw] would be imported.
 *
 * A table with [fixedColumns] keeps its own column types, and its own column names unless the file supplies names.
 * Otherwise the table takes the shape of the file: every column is [dataType] when one is given, and failing that
 * numeric if all of its cells are numbers and text if not.
 *
 * @param existingColumns the columns of the table being imported into
 */
fun planCsvImport(
    raw: Array<Array<String>>,
    options: ImportExportOptions,
    existingColumns: List<Column> = listOf(),
    fixedColumns: Boolean = false,
    dataType: KClass<*>? = null
): CsvImportPlan {
    val rows = rectangular(raw)
    val width = rows.firstOrNull()?.size ?: 0
    val firstDataColumn = if (options.includeRowNames && width > 0) 1 else 0
    val useHeader = options.includeColumnNames && rows.isNotEmpty()

    val body = if (useHeader) rows.drop(1) else rows
    val cells = body.map { it.subList(firstDataColumn, width) }
    val rowNames = if (firstDataColumn == 1) body.map { it[0] } else null
    val fileNames = if (useHeader) rows.first().subList(firstDataColumn, width) else null

    val forcedType = when (dataType) {
        Double::class -> Column.DataType.DoubleType
        Int::class -> Column.DataType.IntType
        String::class -> Column.DataType.StringType
        else -> null
    }

    fun inferType(col: Int): Column.DataType {
        val filled = cells.asSequence().map { it[col] }.filter { it.isNotBlank() }
        return if (filled.any() && filled.all { it.isNumber() }) Column.DataType.DoubleType else Column.DataType.StringType
    }

    val columns = List(width - firstDataColumn) { col ->
        val existing = existingColumns.getOrNull(col).takeIf { fixedColumns }
        val fileName = fileNames?.get(col)?.trim()?.takeIf { it.isNotEmpty() }
        when {
            existing != null && (fileName == null || fileName == existing.name) -> existing
            else -> Column(fileName ?: "Column ${col + 1}", existing?.type ?: forcedType ?: inferType(col))
        }
    }

    return CsvImportPlan(columns, rowNames, cells)
}

/**
 * A best guess at whether [raw] starts with a row of column names and a column of row names, judged from its first
 * rows. Meant as a starting point for the user to confirm, so it only says yes when the evidence is clear.
 */
fun guessImportOptions(raw: Array<Array<String>>): ImportExportOptions {
    val rows = rectangular(raw.take(GUESS_SAMPLE_ROWS).toTypedArray())
    val width = rows.firstOrNull()?.size ?: 0
    if (rows.size < 2 || width == 0) {
        return ImportExportOptions()
    }

    fun List<List<String>>.isNumericColumn(col: Int) =
        any { it[col].isNotBlank() } && all { it[col].isBlank() || it[col].isNumber() }

    // A row of names holds no numbers, and sits above at least one column that is otherwise all numbers.
    val first = rows.first()
    val rest = rows.drop(1)
    val hasColumnNames = first.any { it.isNotBlank() }
            && first.none { it.isNumber() }
            && (0 until width).any { rest.isNumericColumn(it) }

    // A column of names is distinct text in every row, next to at least one column of numbers.
    val body = if (hasColumnNames) rest else rows
    val firstColumn = body.map { it[0] }
    val hasRowNames = width > 1
            && firstColumn.none { it.isBlank() || it.isNumber() }
            && firstColumn.distinct().size == firstColumn.size
            && (1 until width).any { body.isNumericColumn(it) }

    return ImportExportOptions(includeColumnNames = hasColumnNames, includeRowNames = hasRowNames)
}

private const val GUESS_SAMPLE_ROWS = 200

/**
 * Replace the contents of this table with the data in [plan].
 */
fun SimbrainDataFrame.applyCsvImport(plan: CsvImportPlan) {
    when (this) {
        is BasicDataFrame -> {
            data = MutableList(plan.rowCount) { row ->
                MutableList<Any?>(plan.columnCount) { col -> plan.valueAt(row, col) }
            }
            // After the data, which re-infers column types when it is set
            columns = plan.columns.toMutableList()
        }
        is MatrixDataFrame -> {
            data = Matrix(plan.rowCount, plan.columnCount).also { matrix ->
                for (row in 0 until plan.rowCount) {
                    for (col in 0 until plan.columnCount) {
                        matrix[row, col] = (plan.parse(row, col) as? Number)?.toDouble() ?: 0.0
                    }
                }
            }
            columns = plan.columns.toMutableList()
        }
        else -> throw IllegalArgumentException("Cannot import csv data into a ${this::class.simpleName}")
    }
    rowNames = plan.rowNames ?: listOf()
    fireTableStructureChanged()
}
