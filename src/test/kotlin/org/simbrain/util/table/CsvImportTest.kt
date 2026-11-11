/**
 * Tests of how csv text is laid out as a table under different import options, of the guess at those options, and of
 * the import dialog that previews the result.
 */
package org.simbrain.util.table

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix
import java.awt.Component
import java.awt.Container
import javax.swing.JCheckBox
import javax.swing.SwingUtilities

class CsvImportTest {

    private fun csv(vararg rows: String) = rows.map { it.split(",").toTypedArray() }.toTypedArray()

    private val cars = csv(
        "Model,MPG,Cylinders",
        "Mazda RX4,21.0,6",
        "Datsun 710,22.8,4",
        "Valiant,18.1,6"
    )

    private val numbers = csv(
        "1,0,0.5",
        "0,1,0.25"
    )

    @Test
    fun `without names every row and column is data`() {
        val plan = planCsvImport(cars, ImportExportOptions())
        assertEquals(4, plan.rowCount)
        assertEquals(3, plan.columnCount)
        assertEquals(listOf("Column 1", "Column 2", "Column 3"), plan.columns.map { it.name })
        assertNull(plan.rowNames)
        assertEquals("Model", plan.cells[0][0])
    }

    @Test
    fun `first row becomes the column names`() {
        val plan = planCsvImport(cars, ImportExportOptions(includeColumnNames = true))
        assertEquals(3, plan.rowCount)
        assertEquals(listOf("Model", "MPG", "Cylinders"), plan.columns.map { it.name })
        assertEquals("Mazda RX4", plan.cells[0][0])
    }

    @Test
    fun `with both kinds of names the corner cell is dropped`() {
        val plan = planCsvImport(cars, ImportExportOptions(includeColumnNames = true, includeRowNames = true))
        assertEquals(3, plan.rowCount)
        assertEquals(listOf("MPG", "Cylinders"), plan.columns.map { it.name })
        assertEquals(listOf("Mazda RX4", "Datsun 710", "Valiant"), plan.rowNames)
        assertEquals(21.0, plan.valueAt(0, 0))
    }

    @Test
    fun `columns are numeric only when all of their values are numbers`() {
        val plan = planCsvImport(cars, ImportExportOptions(includeColumnNames = true))
        assertEquals(
            listOf(Column.DataType.StringType, Column.DataType.DoubleType, Column.DataType.DoubleType),
            plan.columns.map { it.type }
        )
        assertEquals(0, plan.unreadableCount)
    }

    @Test
    fun `a header left in the data stops its columns being numeric`() {
        val plan = planCsvImport(cars, ImportExportOptions(includeRowNames = true))
        assertTrue(plan.columns.all { it.type == Column.DataType.StringType })
    }

    @Test
    fun `a forced data type marks the values that do not fit it`() {
        val plan = planCsvImport(cars, ImportExportOptions(includeRowNames = true), dataType = Double::class)
        assertEquals(2, plan.unreadableCount)
        assertNull(plan.parse(0, 0))
        assertEquals(0.0, plan.valueAt(0, 0))
        assertEquals(21.0, plan.valueAt(1, 0))
    }

    @Test
    fun `short rows are padded to the widest row`() {
        val plan = planCsvImport(csv("1,2,3", "4", "5,6"), ImportExportOptions())
        assertEquals(3, plan.columnCount)
        assertEquals(listOf("4", "", ""), plan.cells[1])
    }

    @Test
    fun `a table with fixed columns keeps its columns when the file has no names`() {
        val table = BasicDataFrame(2, 3)
        table.columnNames = listOf("x", "y", "z")
        val plan = planCsvImport(numbers, ImportExportOptions(), table.columns, fixedColumns = true)
        plan.columns.forEachIndexed { i, column -> assertSame(table.columns[i], column) }
    }

    @Test
    fun `a table with fixed columns takes names from the file but keeps its types`() {
        val table = BasicDataFrame(2, 2)
        val plan = planCsvImport(
            cars,
            ImportExportOptions(includeColumnNames = true, includeRowNames = true),
            table.columns,
            fixedColumns = true
        )
        assertEquals(listOf("MPG", "Cylinders"), plan.columns.map { it.name })
        assertTrue(plan.columns.all { it.type == Column.DataType.DoubleType })
    }

    @Test
    fun `a wrong column count blocks the import until the options fix it`() {
        val withoutRowNames = planCsvImport(cars, ImportExportOptions(includeColumnNames = true))
        val withRowNames = planCsvImport(cars, ImportExportOptions(includeColumnNames = true, includeRowNames = true))
        assertNotNull(withoutRowNames.blockingProblem(requiredColumnCount = 2))
        assertNull(withRowNames.blockingProblem(requiredColumnCount = 2))
        assertNull(withoutRowNames.blockingProblem(requiredColumnCount = null))
    }

    @Test
    fun `an empty file has nothing to import`() {
        val plan = planCsvImport(arrayOf(), ImportExportOptions(includeColumnNames = true, includeRowNames = true))
        assertEquals(0, plan.rowCount)
        assertNotNull(plan.blockingProblem(requiredColumnCount = null))
    }

    @Test
    fun `text above and beside numbers is guessed to be names`() {
        val guess = guessImportOptions(cars)
        assertTrue(guess.includeColumnNames)
        assertTrue(guess.includeRowNames)
    }

    @Test
    fun `a table of numbers is guessed to have no names`() {
        val guess = guessImportOptions(numbers)
        assertFalse(guess.includeColumnNames)
        assertFalse(guess.includeRowNames)
    }

    @Test
    fun `a header over a numeric first column is guessed to be column names only`() {
        val guess = guessImportOptions(csv("age,job,balance", "58,management,2143", "44,technician,29"))
        assertTrue(guess.includeColumnNames)
        assertFalse(guess.includeRowNames)
    }

    @Test
    fun `a repeated label in the first column is not guessed to be row names`() {
        val guess = guessImportOptions(csv("setosa,5.1,3.5", "setosa,4.9,3.0", "virginica,6.3,3.3"))
        assertFalse(guess.includeRowNames)
    }

    @Test
    fun `a table of text is guessed to have no names`() {
        val guess = guessImportOptions(csv("word,kind", "dog,noun", "run,verb"))
        assertFalse(guess.includeColumnNames)
        assertFalse(guess.includeRowNames)
    }

    @Test
    fun `importing reshapes a basic data frame to the file`() {
        val table = BasicDataFrame(5, 2)
        val options = ImportExportOptions(includeColumnNames = true)
        table.applyCsvImport(planCsvImport(cars, options, table.columns, fixedColumns = false))
        assertEquals(3, table.rowCount)
        assertEquals(3, table.columnCount)
        assertEquals(listOf("Model", "MPG", "Cylinders"), table.columnNames)
        assertEquals("Valiant", table.getValueAt(2, 0))
        assertEquals(18.1, table.getValueAt(2, 1))
        assertEquals(Column.DataType.StringType, table.columns[0].type)
        assertEquals(Column.DataType.DoubleType, table.columns[1].type)
    }

    @Test
    fun `importing with row names names the rows`() {
        val table = BasicDataFrame(1, 2)
        val options = ImportExportOptions(includeColumnNames = true, includeRowNames = true)
        table.applyCsvImport(planCsvImport(cars, options, table.columns, fixedColumns = true))
        assertEquals(listOf("Mazda RX4", "Datsun 710", "Valiant"), table.getAllRowNames())
        assertEquals(6.0, table.getValueAt(0, 1))
    }

    @Test
    fun `importing without row names numbers the rows again`() {
        val table = BasicDataFrame(2, 3)
        table.rowNames = listOf("a", "b")
        table.applyCsvImport(planCsvImport(numbers, ImportExportOptions(), table.columns, fixedColumns = true))
        assertEquals(listOf("1", "2"), table.getAllRowNames())
    }

    @Test
    fun `a matrix data frame can be imported from a file with names`() {
        val table = MatrixDataFrame(Matrix(1, 2))
        val options = ImportExportOptions(includeColumnNames = true, includeRowNames = true)
        table.applyCsvImport(planCsvImport(cars, options, table.columns, fixedColumns = true, dataType = Double::class))
        assertEquals(3, table.rowCount)
        assertEquals(2, table.columnCount)
        assertEquals(listOf("MPG", "Cylinders"), table.columnNames)
        assertEquals(22.8, table.getValueAt(1, 0))
    }

    @Test
    fun `text imported into a matrix data frame becomes zero`() {
        val table = MatrixDataFrame(Matrix(1, 3))
        val plan = planCsvImport(cars, ImportExportOptions(), table.columns, fixedColumns = true, dataType = Double::class)
        assertEquals(6, plan.unreadableCount)
        table.applyCsvImport(plan)
        assertEquals(0.0, table.getValueAt(0, 1))
        assertEquals(21.0, table.getValueAt(1, 1))
    }

    private fun Component.checkBox(text: String): JCheckBox? = when {
        this is JCheckBox && this.text == text -> this
        this is Container -> components.firstNotNullOfOrNull { it.checkBox(text) }
        else -> null
    }

    @Test
    fun `the dialog starts from the guessed options and replans when they change`() {
        SwingUtilities.invokeAndWait {
            val dialog = CsvImportDialog("cars.csv", cars, makePlan = { planCsvImport(cars, it) })
            assertTrue(dialog.options.includeColumnNames)
            assertTrue(dialog.options.includeRowNames)
            assertEquals(2, dialog.plan.columnCount)

            dialog.contentPane.checkBox("First column contains row names")!!.doClick()
            assertFalse(dialog.options.includeRowNames)
            assertEquals(3, dialog.plan.columnCount)
            assertEquals(3, dialog.plan.rowCount)
            dialog.dispose()
        }
    }

    @Test
    fun `the dialog only allows an import that fits the table`() {
        SwingUtilities.invokeAndWait {
            val dialog = CsvImportDialog(
                "cars.csv",
                cars,
                requiredColumnCount = 3,
                makePlan = { planCsvImport(cars, it, dataType = Double::class) }
            )
            assertFalse(dialog.okButton.isEnabled)

            dialog.contentPane.checkBox("First column contains row names")!!.doClick()
            assertTrue(dialog.okButton.isEnabled)
            dialog.dispose()
        }
    }

    @Test
    fun `confirming the dialog imports the plan on screen`() {
        var imported: CsvImportPlan? = null
        SwingUtilities.invokeAndWait {
            val dialog = CsvImportDialog(
                "cars.csv",
                cars,
                makePlan = { planCsvImport(cars, it) },
                onImport = { imported = it }
            )
            dialog.contentPane.checkBox("First row contains column names")!!.doClick()
            dialog.okButton.doClick()
        }
        assertEquals(4, imported?.rowCount)
        assertEquals(listOf("Mazda RX4", "Datsun 710", "Valiant"), imported?.rowNames?.drop(1))
    }
}
