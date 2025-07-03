package org.simbrain.util.table

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import smile.math.matrix.Matrix

class MatrixDataFrameTest {

    private val matrix: Matrix
    private val df: MatrixDataFrame

    init {
        matrix = Matrix(3, 4)
        // Fill with test data
        for (i in 0 until 3) {
            for (j in 0 until 4) {
                matrix[i, j] = (i * 4 + j + 1).toDouble()
            }
        }
        df = MatrixDataFrame(matrix)
    }

    @Test
    fun `test creation from matrix`() {
        assertEquals(3, df.rowCount)
        assertEquals(4, df.columnCount)
        assertTrue(df.isMutable)
        assertEquals(4, df.columns.size)
    }

    @Test
    fun `test default column names`() {
        assertEquals("Column 1", df.columns[0].name)
        assertEquals("Column 2", df.columns[1].name)
        assertEquals("Column 3", df.columns[2].name)
        assertEquals("Column 4", df.columns[3].name)
    }

    @Test
    fun `test all columns are double type`() {
        assertTrue(df.columns.all { it.type == Column.DataType.DoubleType })
    }

    @Test
    fun `test get value at`() {
        assertEquals(1.0, df.getValueAt(0, 0))
        assertEquals(5.0, df.getValueAt(1, 0))
        assertEquals(12.0, df.getValueAt(2, 3))
        assertNull(df.getValueAt(-1, 0)) // Invalid row
        assertNull(df.getValueAt(0, -1)) // Invalid column
    }

    @Test
    fun `test set value at`() {
        df.setValueAt(99.5, 1, 2)
        assertEquals(99.5, df.getValueAt(1, 2))
        // Verify underlying matrix was updated
        assertEquals(99.5, matrix[1, 2])
    }

    @Test
    fun `test set value with string parsing`() {
        df.setValueAt("42.7", 0, 0)
        assertEquals(42.7, df.getValueAt(0, 0))
    }

    @Test
    fun `test insert row at beginning`() {
        val originalRows = df.rowCount
        df.insertRow(0)
        assertEquals(originalRows + 1, df.rowCount)
        assertEquals(4, df.columnCount)
        // New row should have default values (0.0)
        assertEquals(0.0, df.getValueAt(0, 0))
        assertEquals(0.0, df.getValueAt(0, 3))
        // Old first row should now be second row
        assertEquals(1.0, df.getValueAt(1, 0))
    }

    @Test
    fun `test insert row in middle`() {
        val originalValue = df.getValueAt(1, 0) // Save value at row 1
        df.insertRow(1)
        assertEquals(4, df.rowCount)
        // New row at index 1 should have zeros
        assertEquals(0.0, df.getValueAt(1, 0))
        // Old row 1 should now be at row 2
        assertEquals(originalValue, df.getValueAt(2, 0))
    }

    @Test
    fun `test insert row at end with -1 index`() {
        val originalRows = df.rowCount
        df.insertRow(-1)
        assertEquals(originalRows + 1, df.rowCount)
        // Last row should have zeros
        assertEquals(0.0, df.getValueAt(df.rowCount - 1, 0))
    }

    @Test
    fun `test set row`() {
        val newRow = arrayOf(10.0, 20.0, 30.0, 40.0)
        df.setRow(1, newRow)
        assertEquals(10.0, df.getValueAt(1, 0))
        assertEquals(20.0, df.getValueAt(1, 1))
        assertEquals(30.0, df.getValueAt(1, 2))
        assertEquals(40.0, df.getValueAt(1, 3))
    }

    @Test
    fun `test delete row`() {
        val originalRows = df.rowCount
        val row2Value = df.getValueAt(2, 0)
        df.deleteRow(1)
        assertEquals(originalRows - 1, df.rowCount)
        // Row 2 should have moved to position 1
        assertEquals(row2Value, df.getValueAt(1, 0))
    }

    @Test
    fun `test cannot delete last row when only one remains`() {
        val singleRowMatrix = Matrix(1, 3)
        val singleRowDf = MatrixDataFrame(singleRowMatrix)
        singleRowDf.deleteRow(0)
        assertEquals(1, singleRowDf.rowCount) // Should still have the row
    }

    @Test
    fun `test delete first row`() {
        val originalSecondRowValue = df.getValueAt(1, 0)
        df.deleteRow(0)
        assertEquals(2, df.rowCount)
        // Former second row should now be first
        assertEquals(originalSecondRowValue, df.getValueAt(0, 0))
    }

    @Test
    fun `test delete last row`() {
        val originalRows = df.rowCount
        df.deleteRow(originalRows - 1)
        assertEquals(originalRows - 1, df.rowCount)
    }

    @Test
    fun `test matrix backing stays synchronized`() {
        // Test that changes to dataframe reflect in underlying matrix
        df.setValueAt(100.0, 1, 1)
        assertEquals(100.0, matrix[1, 1])

        // Test row operations update matrix size
        val originalMatrixRows = matrix.nrow()
        df.insertRow(0)
        assertEquals(originalMatrixRows + 1, df.rowCount)
    }

    @Test
    fun `test data extraction methods`() {
        val column0 = df.getDoubleColumn(0)
        assertArrayEquals(doubleArrayOf(1.0, 5.0, 9.0), column0)

        val row0 = df.getRow<Double>(0)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), row0)
    }

    @Test
    fun `test 2D array extraction`() {
        val array2D = df.get2DDoubleArray()
        assertEquals(3, array2D.size)
        assertEquals(4, array2D[0].size)
        assertEquals(1.0, array2D[0][0])
        assertEquals(12.0, array2D[2][3])
    }

    @Test
    fun `test column major array`() {
        val colMajor = df.getColumnMajorArray()
        assertEquals(4, colMajor.size) // 4 columns
        assertArrayEquals(doubleArrayOf(1.0, 5.0, 9.0), colMajor[0])
        assertArrayEquals(doubleArrayOf(4.0, 8.0, 12.0), colMajor[3])
    }

    @Test
    fun `test custom column configuration`() {
        val customColumns = mutableListOf(
            Column("X", Column.DataType.DoubleType),
            Column("Y", Column.DataType.DoubleType),
            Column("Z", Column.DataType.DoubleType),
            Column("W", Column.DataType.DoubleType)
        )
        val customDf = MatrixDataFrame(matrix, customColumns)
        assertEquals("X", customDf.columns[0].name)
        assertEquals("Y", customDf.columns[1].name)
        assertEquals("Z", customDf.columns[2].name)
        assertEquals("W", customDf.columns[3].name)
    }

    @Test
    fun `test boundary validation`() {
        assertTrue(df.validateRowIndex(0))
        assertTrue(df.validateRowIndex(df.rowCount - 1))
        assertFalse(df.validateRowIndex(-1))
        assertFalse(df.validateRowIndex(df.rowCount))

        assertTrue(df.validateColumnIndex(0))
        assertTrue(df.validateColumnIndex(df.columnCount - 1))
        assertFalse(df.validateColumnIndex(-1))
        assertFalse(df.validateColumnIndex(df.columnCount))
    }

    @Test
    fun `test row operations preserve data integrity`() {
        // Store original data
        val originalData = mutableListOf<List<Double>>()
        for (i in 0 until df.rowCount) {
            originalData.add(df.getRow<Double>(i))
        }

        // Insert a row in the middle and then delete it
        df.insertRow(1)
        df.deleteRow(1)

        // Data should be back to original state
        assertEquals(originalData.size, df.rowCount)
        for (i in originalData.indices) {
            assertEquals(originalData[i], df.getRow<Double>(i))
        }
    }
}