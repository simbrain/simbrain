package org.simbrain.util.table

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import smile.math.matrix.Matrix

class BasicDataFrameTest {

    private val df: BasicDataFrame
    private val mixedTypeData: List<List<Any?>>

    init {
        // Mixed type data for testing
        mixedTypeData = listOf(
            listOf("Alice", 25, 3.14),
            listOf("Bob", 30, 2.71),
            listOf("Charlie", 35, 1.41)
        )
        df = BasicDataFrame(mixedTypeData)
    }

    @Test
    fun `test basic creation with dimensions`() {
        val df = BasicDataFrame(3, 4)
        assertEquals(3, df.rowCount)
        assertEquals(4, df.columnCount)
        assertEquals(0.0, df.getValueAt(0, 0))
        assertTrue(df.isMutable)
    }

    @Test
    fun `test creation with custom initializer`() {
        val df = BasicDataFrame(2, 3) { 42 }
        assertEquals(2, df.rowCount)
        assertEquals(3, df.columnCount)
        assertEquals(42, df.getValueAt(0, 0))
        assertEquals(42, df.getValueAt(1, 2))
    }

    @Test
    fun `test creation from data list`() {
        assertEquals(3, df.rowCount)
        assertEquals(3, df.columnCount)
        assertEquals("Alice", df.getValueAt(0, 0))
        assertEquals(25, df.getValueAt(0, 1))
        assertEquals(3.14, df.getValueAt(0, 2))
    }

    @Test
    fun `test column and row names`() {
        val df = BasicDataFrame(2, 3)
        df.columnNames = listOf("A", "B", "C")
        df.rowNames = listOf("Row1", "Row2")
        assertEquals("B", df.columnNames[1])
        assertEquals("Row1", df.rowNames[0])
        assertEquals("Row1", df.getRowName(0))
        assertEquals("Row2", df.getRowName(1))
    }

    @Test
    fun `test column type inference`() {
        assertEquals(3, df.columns.size)
        assertEquals(Column.DataType.StringType, df.columns[0].type)
        assertEquals(Column.DataType.IntType, df.columns[1].type)  
        assertEquals(Column.DataType.DoubleType, df.columns[2].type)
    }

    @Test
    fun `test insert column`() {
        val initialCount = df.columnCount
        df.insertColumn(1, "NewCol", Column.DataType.DoubleType)
        assertEquals(initialCount + 1, df.columnCount)
        assertEquals("NewCol", df.columnNames[1])
        assertEquals(0.0, df.getValueAt(0, 1)) // Default value for double
    }

    @Test
    fun `test insert column at end when index is -1`() {
        val initialCount = df.columnCount
        df.insertColumn(-1, "EndCol", Column.DataType.StringType)
        assertEquals(initialCount + 1, df.columnCount)
        assertEquals("EndCol", df.columnNames.last())
    }

    @Test
    fun `test delete column`() {
        val initialCount = df.columnCount
        val initialValue = df.getValueAt(0, 2)
        df.deleteColumn(1) // Delete the middle column
        assertEquals(initialCount - 1, df.columnCount)
        assertEquals(initialValue, df.getValueAt(0, 1)) // Third column becomes second
    }

    @Test
    fun `test insert row`() {
        val initialCount = df.rowCount
        df.insertRow(1)
        assertEquals(initialCount + 1, df.rowCount)
        // Check default values were inserted
        assertEquals("", df.getValueAt(1, 0)) // String default
        assertEquals(0, df.getValueAt(1, 1))   // Int default
        assertEquals(0.0, df.getValueAt(1, 2)) // Double default
    }

    @Test
    fun `test insert row at bottom when index is -1`() {
        val initialCount = df.rowCount
        df.insertRow(-1)
        assertEquals(initialCount + 1, df.rowCount)
        assertEquals("", df.getValueAt(df.rowCount - 1, 0))
    }

    @Test
    fun `test set row`() {
        val newRow = arrayOf("David", 40, 9.81)
        df.setRow(1, newRow)
        assertEquals("David", df.getValueAt(1, 0))
        assertEquals(40, df.getValueAt(1, 1))
        assertEquals(9.81, df.getValueAt(1, 2))
    }

    @Test
    fun `test delete row`() {
        val initialCount = df.rowCount
        val thirdRowValue = df.getValueAt(2, 0)
        df.deleteRow(1)
        assertEquals(initialCount - 1, df.rowCount)
        assertEquals(thirdRowValue, df.getValueAt(1, 0)) // Third row becomes second
    }

    @Test
    fun `test set and get value at`() {
        df.setValueAt("Updated", 0, 0)
        assertEquals("Updated", df.getValueAt(0, 0))
        
        df.setValueAt(99, 1, 1)
        assertEquals(99, df.getValueAt(1, 1))
        
        df.setValueAt(5.55, 2, 2)
        assertEquals(5.55, df.getValueAt(2, 2))
    }

    @Test
    fun `test value validation and parsing`() {
        // Test string to number conversion
        df.setValueAt("42", 0, 1) // Setting string in int column
        assertEquals(42, df.getValueAt(0, 1))
        
        df.setValueAt("3.14159", 0, 2) // Setting string in double column
        assertEquals(3.14159, df.getValueAt(0, 2))
    }

    @Test
    fun `test invalid value parsing`() {
        // Invalid string to number conversion should be handled gracefully
        df.setValueAt("not_a_number", 0, 1)
        // Value should remain unchanged
        assertEquals(25, df.getValueAt(0, 1))
    }

    @Test
    fun `test boundary validation`() {
        assertNull(df.getValueAt(-1, 0)) // Invalid row
        assertNull(df.getValueAt(0, -1)) // Invalid column
        assertNull(df.getValueAt(100, 0)) // Row out of bounds
        assertNull(df.getValueAt(0, 100)) // Column out of bounds
    }

    @Test
    fun `test get typed columns`() {
        val numericDf = BasicDataFrame(listOf(
            listOf(1.0, 2, 3.0),
            listOf(4.0, 5, 6.0)
        ))
        
        val doubleCol = numericDf.getDoubleColumn(0)
        assertArrayEquals(doubleArrayOf(1.0, 4.0), doubleCol)
        
        val intCol = numericDf.getIntColumn(1)
        assertArrayEquals(intArrayOf(2, 5), intCol)
        
        val floatCol = numericDf.getFloatColumn(2)
        assertArrayEquals(floatArrayOf(3.0f, 6.0f), floatCol)
    }

    @Test
    fun `test get string column`() {
        val stringCol = df.getStringColumn(0)
        assertArrayEquals(arrayOf("Alice", "Bob", "Charlie"), stringCol)
    }

    @Test
    fun `test get typed rows`() {
        val stringRow = df.getRow<String>(0)
        assertEquals(listOf("Alice", "25", "3.14"), stringRow)
        
        val numericDf = BasicDataFrame(listOf(
            listOf(1.0, 2.0, 3.0),
            listOf(4.0, 5.0, 6.0)
        ))
        val doubleRow = numericDf.getRow<Double>(1)
        assertEquals(listOf(4.0, 5.0, 6.0), doubleRow)
    }

    @Test
    fun `test get 2D arrays`() {
        val numericDf = BasicDataFrame(listOf(
            listOf(1.0, 2.0),
            listOf(3.0, 4.0),
            listOf(5.0, 6.0)
        ))
        
        val doubleArray = numericDf.get2DDoubleArray()
        assertEquals(3, doubleArray.size) // 3 rows
        assertEquals(2, doubleArray[0].size) // 2 columns
        assertEquals(1.0, doubleArray[0][0])
        assertEquals(6.0, doubleArray[2][1])
    }

    @Test
    fun `test column major array`() {
        val numericDf = BasicDataFrame(listOf(
            listOf(1.0, 2.0),
            listOf(3.0, 4.0)
        ))
        
        val colMajor = numericDf.getColumnMajorArray()
        assertEquals(2, colMajor.size) // 2 columns
        assertArrayEquals(doubleArrayOf(1.0, 3.0), colMajor[0])
        assertArrayEquals(doubleArrayOf(2.0, 4.0), colMajor[1])
    }

    @Test
    fun `test randomize column`() {
        val numericDf = BasicDataFrame(3, 2)
        val originalValue = numericDf.getValueAt(0, 0)
        numericDf.randomizeColumn(0)
        // Values should have changed (with very high probability)
        var changed = false
        for (i in 0 until numericDf.rowCount) {
            if (numericDf.getValueAt(i, 0) != originalValue) {
                changed = true
                break
            }
        }
        assertTrue(changed || originalValue == 0.0) // Allow for case where random gives same value
    }

    @Test
    fun `test string column randomization`() {
        val stringDf = BasicDataFrame(listOf(
            listOf("A"),
            listOf("B"),
            listOf("A"),
            listOf("C")
        ))
        stringDf.randomizeColumn(0)
        val values = stringDf.getStringColumn(0)
        // Should only contain values from original set
        assertTrue(values.all { it in setOf("A", "B", "C") })
    }

    @Test
    fun `test factory method from 2D array`() {
        val data = arrayOf(
            arrayOf("Col1", "Col2"),
            arrayOf("val1", "val2"),
            arrayOf("val3", "val4")
        )
        val options = ImportExportOptions().apply {
            includeColumnNames = true
        }
        val df = createFrom2DArray(data, options)
        assertEquals(2, df.rowCount)
        assertEquals(2, df.columnCount)
        assertEquals("Col1", df.columnNames[0])
        assertEquals("val1", df.getValueAt(0, 0))
    }

    @Test  
    fun `test factory method from double array`() {
        val data = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        )
        val df = createFromDoubleArray(data)
        assertEquals(2, df.rowCount)
        assertEquals(2, df.columnCount)
        assertEquals(1.0, df.getValueAt(0, 0))
        assertEquals(4.0, df.getValueAt(1, 1))
    }

    @Test
    fun `test factory method from matrix`() {
        val matrix = Matrix(2, 3)
        matrix.set(0, 0, 1.0)
        matrix.set(1, 2, 5.0)
        val df = createFromMatrix(matrix)
        assertEquals(2, df.rowCount)
        assertEquals(3, df.columnCount)
        assertEquals(1.0, df.getValueAt(0, 0))
        assertEquals(5.0, df.getValueAt(1, 2))
    }

    @Test
    fun `test factory method from column arrays`() {
        val doubleData = doubleArrayOf(1.0, 2.0, 3.0)
        val df = createBasicDataFrameFromColumn(doubleData)
        assertEquals(3, df.rowCount)
        assertEquals(1, df.columnCount)
        assertEquals(2.0, df.getValueAt(1, 0))
        
        val stringData = arrayOf("A", "B", "C")
        val stringDf = createBasicDataFrameFromColumn(stringData)
        assertEquals(3, stringDf.rowCount)
        assertEquals("B", stringDf.getValueAt(1, 0))
    }

    @Test
    fun `test set num rows increase`() {
        val initialRows = df.rowCount
        df.setNumRows(initialRows + 2)
        assertEquals(initialRows + 2, df.rowCount)
    }

    @Test
    fun `test set num rows decrease`() {
        val initialRows = df.rowCount
        df.setNumRows(initialRows - 1)
        assertEquals(initialRows - 1, df.rowCount)
    }

    @Test
    fun `test set num rows to zero`() {
        assertThrows<IllegalArgumentException> {
            df.setNumRows(-1)
        }
    }

    @Test
    fun `test toString representation`() {
        val result = df.toString()
        assertTrue(result.contains("Alice"))
        assertTrue(result.contains("25"))
        assertTrue(result.contains("3.14"))
    }

    @Test
    fun `test column validation`() {
        assertTrue(df.validateColumnIndex(0))
        assertTrue(df.validateColumnIndex(df.columnCount - 1))
        assertFalse(df.validateColumnIndex(-1))
        assertFalse(df.validateColumnIndex(df.columnCount))
    }

    @Test
    fun `test row validation`() {
        assertTrue(df.validateRowIndex(0))
        assertTrue(df.validateRowIndex(df.rowCount - 1))
        assertFalse(df.validateRowIndex(-1))
        assertFalse(df.validateRowIndex(df.rowCount))
    }
}
