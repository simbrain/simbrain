/**
 * The dialog shown after a csv file is chosen for import. It asks whether the file starts with a row of column names
 * or a column of row names, and shows the table those answers produce, redrawn as they change. The preview renders
 * the same [CsvImportPlan] the import applies.
 */
package org.simbrain.util.table

import net.miginfocom.swing.MigLayout
import org.simbrain.util.StandardDialog
import org.simbrain.util.Theme
import org.simbrain.util.blend
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * @param fileName shown at the top of the dialog
 * @param raw the parsed text of the file
 * @param requiredColumnCount the number of columns the import must produce, or null if any number will do
 * @param initialOptions how the checkboxes start out
 * @param makePlan how the file would be imported under a given set of options
 * @param onImport called with the plan on screen when the user confirms
 */
class CsvImportDialog(
    fileName: String,
    raw: Array<Array<String>>,
    private val requiredColumnCount: Int? = null,
    initialOptions: ImportExportOptions = guessImportOptions(raw),
    private val makePlan: (ImportExportOptions) -> CsvImportPlan,
    onImport: (CsvImportPlan) -> Unit = {}
) : StandardDialog() {

    private val columnNamesBox = JCheckBox("First row contains column names", initialOptions.includeColumnNames)

    private val rowNamesBox = JCheckBox("First column contains row names", initialOptions.includeRowNames)

    val options
        get() = ImportExportOptions(
            includeColumnNames = columnNamesBox.isSelected,
            includeRowNames = rowNamesBox.isSelected
        )

    var plan = makePlan(options)
        private set

    private val previewModel = object : AbstractTableModel() {
        override fun getRowCount() = minOf(plan.rowCount, PREVIEW_ROWS)
        override fun getColumnCount() = plan.columnCount
        override fun getColumnName(column: Int) = plan.columns[column].name
        override fun getValueAt(rowIndex: Int, columnIndex: Int) = plan.cells[rowIndex][columnIndex]
    }

    /**
     * Fills the scroll pane when its columns are narrower than it, and scrolls sideways when they are wider.
     */
    private val previewTable = object : JTable(previewModel) {
        override fun getScrollableTracksViewportWidth() = preferredSize.width < (parent?.width ?: 0)
    }.apply {
        setShowGrid(true)
        intercellSpacing = Dimension(1, 1)
        setGridColor(Theme.divider)
        tableHeader.reorderingAllowed = false
        rowSelectionAllowed = false
        isFocusable = false
        setDefaultRenderer(Any::class.java, PreviewCellRenderer())
    }

    private val previewPane = DataViewerScrollPane(previewTable).apply {
        preferredSize = Dimension(560, 240)
    }

    private val summaryLabel = JLabel().apply { foreground = Theme.mutedText.fixed() }

    private val messageLabel = JLabel()

    init {
        title = "Import CSV"
        val optionsPanel = JPanel(MigLayout("insets 0, wrap 1, gapy ${Theme.tightGap}")).apply {
            add(columnNamesBox)
            add(rowNamesBox)
        }
        contentPane = JPanel(
            MigLayout("insets 0, fill, wrap 2, hidemode 3, gapy ${Theme.componentGap}", "[][grow, right]")
        ).apply {
            add(JLabel(fileName).apply { font = Theme.bodyBold }, "span")
            add(optionsPanel, "span")
            add(Theme.sectionLabel("Preview"), "gaptop ${Theme.tightGap}")
            add(summaryLabel)
            add(previewPane, "span, grow, push")
            add(messageLabel, "span, growx")
        }
        previewTable.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        columnNamesBox.addActionListener { replan() }
        rowNamesBox.addActionListener { replan() }
        addCommitTask { onImport(plan) }
        showPlan()
    }

    private fun replan() {
        plan = makePlan(options)
        showPlan()
    }

    private fun showPlan() {
        previewModel.fireTableStructureChanged()
        fitColumnWidths()
        // After the structure change, which resets the names in the row header
        previewPane.rowNames = plan.rowNames?.take(PREVIEW_ROWS) ?: listOf()
        fitRowHeaderWidth()

        summaryLabel.text = buildString {
            append("%,d %s × %,d %s".format(
                plan.rowCount, if (plan.rowCount == 1) "row" else "rows",
                plan.columnCount, if (plan.columnCount == 1) "column" else "columns"
            ))
            if (plan.rowCount > PREVIEW_ROWS) {
                append(" · first $PREVIEW_ROWS shown")
            }
        }

        val problem = plan.blockingProblem(requiredColumnCount)
        val unreadable = plan.unreadableCount
        when {
            problem != null -> {
                messageLabel.text = problem
                messageLabel.foreground = Theme.errorText.fixed()
            }
            unreadable > 0 -> {
                messageLabel.text = if (unreadable == 1) {
                    "1 value is empty or not a number and will be imported as 0."
                } else {
                    "%,d values are empty or not numbers and will be imported as 0.".format(unreadable)
                }
                messageLabel.foreground = Theme.warningText
            }
        }
        messageLabel.isVisible = problem != null || unreadable > 0
        okButton.isEnabled = problem == null
    }

    /**
     * Size each column to its name and the values on show, within limits that keep one long value from taking over.
     */
    private fun fitColumnWidths() {
        val headerRenderer = previewTable.tableHeader.defaultRenderer
        for (col in 0 until previewTable.columnCount) {
            val column = previewTable.columnModel.getColumn(col)
            val headerWidth = headerRenderer
                .getTableCellRendererComponent(previewTable, column.headerValue, false, false, -1, col)
                .preferredSize.width
            val cellWidth = (0 until minOf(previewTable.rowCount, WIDTH_SAMPLE_ROWS)).maxOfOrNull { row ->
                previewTable.prepareRenderer(previewTable.getCellRenderer(row, col), row, col).preferredSize.width
            } ?: 0
            column.preferredWidth = (maxOf(headerWidth, cellWidth) + CELL_PADDING).coerceIn(MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH)
        }
    }

    private fun fitRowHeaderWidth() {
        val rowHeader = previewPane.rowTable
        val metrics = rowHeader.getFontMetrics(rowHeader.tableHeader?.font ?: rowHeader.font)
        val widest = previewPane.rowNames.maxOfOrNull { metrics.stringWidth(it) } ?: 0
        rowHeader.columnModel.getColumn(0).preferredWidth =
            (widest + CELL_PADDING).coerceIn(MIN_ROW_HEADER_WIDTH, MAX_COLUMN_WIDTH)
        rowHeader.preferredScrollableViewportSize = rowHeader.preferredSize
        previewPane.revalidate()
    }

    /**
     * Sets numbers to the right and text to the left, and marks the cells whose text does not fit their column's
     * type.
     */
    private inner class PreviewCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            super.getTableCellRendererComponent(table, value, false, false, row, column)
            val unreadable = plan.parse(row, column) == null
            val numeric = plan.columns[column].type != Column.DataType.StringType
            horizontalAlignment = if (numeric && !unreadable) RIGHT else LEFT
            foreground = if (unreadable) Theme.warningText else table.foreground
            background = if (unreadable) blend(Theme.warningText, table.background, UNREADABLE_TINT) else table.background
            return this
        }
    }
}

private const val PREVIEW_ROWS = 100

private const val UNREADABLE_TINT = 0.12

private const val WIDTH_SAMPLE_ROWS = 30

private const val CELL_PADDING = 16

private const val MIN_COLUMN_WIDTH = 60

private const val MAX_COLUMN_WIDTH = 200

private const val MIN_ROW_HEADER_WIDTH = 50

/**
 * A copy that survives a look and feel refresh, which replaces any color that came from the UI defaults.
 */
private fun Color.fixed() = Color(rgb, true)
