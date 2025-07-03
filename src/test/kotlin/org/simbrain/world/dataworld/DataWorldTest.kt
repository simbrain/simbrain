package org.simbrain.world.dataworld

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.world.dataworld.DataWorld.DataEntryMode

class DataWorldTest {

    private val dataWorld: DataWorld
    private val dataComponent: DataWorldComponent

    init {
        dataWorld = DataWorld(rows = 5, cols = 3)
        dataComponent = DataWorldComponent("Test Data World")
    }

    @Test
    fun `test initial data world creation`() {
        assertEquals(5, dataWorld.dataModel.rowCount)
        assertEquals(3, dataWorld.dataModel.columnCount)
        assertEquals(0, dataWorld.dataModel.currentRowIndex)
        assertEquals(DataEntryMode.LOOP, dataWorld.appendMode)
    }

    @Test
    fun `test current row access`() {
        // Set some test data
        dataWorld.dataModel.setRow(0, arrayOf("1.0", "2.0", "3.0"))
        dataWorld.dataModel.setRow(1, arrayOf("4.0", "5.0", "6.0"))
        
        // Test string row access
        val stringRow = dataWorld.getCurrentStringRow()
        assertArrayEquals(arrayOf("1.0", "2.0", "3.0"), stringRow)
        
        // Test numeric row access
        val numericRow = dataWorld.getCurrentNumericRow()
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), numericRow, 0.001)
    }

    @Test
    fun `test update advances current row`() {
        assertEquals(0, dataWorld.dataModel.currentRowIndex)
        
        dataWorld.update()
        assertEquals(1, dataWorld.dataModel.currentRowIndex)
        
        dataWorld.update()
        assertEquals(2, dataWorld.dataModel.currentRowIndex)
    }

    @Test
    fun `test loop mode wraps around`() {
        dataWorld.appendMode = DataEntryMode.LOOP
        
        // Advance to last row
        repeat(4) { dataWorld.update() }
        assertEquals(4, dataWorld.dataModel.currentRowIndex)
        
        // Should wrap back to 0
        dataWorld.update()
        assertEquals(0, dataWorld.dataModel.currentRowIndex)
    }

    @Test
    fun `test setting current string row`() {
        // Change columns to StringType to properly test string functionality
        dataWorld.dataModel.columns.forEach { it.type = org.simbrain.util.table.Column.DataType.StringType }
        
        val testRow = arrayOf("test1", "test2", "test3")
        dataWorld.setCurrentStringRow(testRow)
        
        val retrievedRow = dataWorld.dataModel.getRow<String>(0)
        assertEquals(testRow.toList(), retrievedRow)
    }

    @Test
    fun `test setting current numeric row`() {
        val testRow = doubleArrayOf(1.5, 2.5, 3.5)
        dataWorld.setCurrentNumericRow(testRow)
        
        val retrievedRow = dataWorld.dataModel.getRow<Double>(0)
        assertArrayEquals(testRow, retrievedRow.toDoubleArray(), 0.001)
    }

    @Test
    fun `test append mode in loop mode`() {
        dataWorld.appendMode = DataEntryMode.LOOP
        val initialRowCount = dataWorld.dataModel.rowCount
        
        // Change columns to StringType to properly test string functionality
        dataWorld.dataModel.columns.forEach { it.type = org.simbrain.util.table.Column.DataType.StringType }
        
        // Move to last row and set data
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentStringRow(arrayOf("last", "row", "data"))
        
        // Should not add new row in loop mode
        assertEquals(initialRowCount, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `test append mode creates new row`() {
        dataWorld.appendMode = DataEntryMode.APPEND
        val initialRowCount = dataWorld.dataModel.rowCount
        
        // Change columns to StringType to properly test string functionality
        dataWorld.dataModel.columns.forEach { it.type = org.simbrain.util.table.Column.DataType.StringType }
        
        // Move to last row and set data
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentStringRow(arrayOf("last", "row", "data"))
        
        // Should add new row in append mode
        assertEquals(initialRowCount + 1, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `test append mode with numeric data`() {
        dataWorld.appendMode = DataEntryMode.APPEND
        val initialRowCount = dataWorld.dataModel.rowCount
        
        // Move to last row and set numeric data
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentNumericRow(doubleArrayOf(7.0, 8.0, 9.0))
        
        // Should add new row
        assertEquals(initialRowCount + 1, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `test data world component creation`() {
        assertEquals("Test Data World", dataComponent.name)
        assertNotNull(dataComponent.dataWorld)
        assertEquals("Data World", dataComponent.dataWorld.id)
    }

    @Test
    fun `test data world with custom dimensions`() {
        val customWorld = DataWorld(rows = 10, cols = 4)
        assertEquals(10, customWorld.dataModel.rowCount)
        assertEquals(4, customWorld.dataModel.columnCount)
    }

    @Test
    fun `test data model operations`() {
        // Change columns to StringType to properly test string functionality
        dataWorld.dataModel.columns.forEach { it.type = org.simbrain.util.table.Column.DataType.StringType }
        
        // Test setting and getting individual cells
        dataWorld.dataModel.setValueAt("custom_value", 1, 1)
        assertEquals("custom_value", dataWorld.dataModel.getValueAt(1, 1))
        
        // Test getting row data
        dataWorld.dataModel.setRow(2, arrayOf("row2_col1", "row2_col2", "row2_col3"))
        val row = dataWorld.dataModel.getRow<String>(2)
        assertEquals(listOf("row2_col1", "row2_col2", "row2_col3"), row)
    }

    @Test
    fun `test serialization and deserialization`() {
        // Test BasicDataFrame serialization with string columns
        val dataFrame = BasicDataFrame(mutableListOf(
            mutableListOf("1", "2"),
            mutableListOf("3", "4"),
            mutableListOf("", "")  // Empty row for proper size
        ))
        
        // Test serialization
        val xstream = org.simbrain.util.getSimbrainXStream()
        val xmlString = xstream.toXML(dataFrame)
        
        // Test deserialization
        val deserializedDataFrame = xstream.fromXML(xmlString) as BasicDataFrame
        
        // Check dimensions are preserved
        assertEquals(3, deserializedDataFrame.rowCount)
        assertEquals(2, deserializedDataFrame.columnCount)
        
        // Since we created with string data, columns should be StringType
        assertEquals("1", deserializedDataFrame.getValueAt(0, 0))
        assertEquals("2", deserializedDataFrame.getValueAt(0, 1))
        assertEquals("3", deserializedDataFrame.getValueAt(1, 0))
        assertEquals("4", deserializedDataFrame.getValueAt(1, 1))
    }

    @Test
    fun `test empty data world behavior`() {
        val emptyWorld = DataWorld(rows = 0, cols = 0)
        assertEquals(0, emptyWorld.dataModel.rowCount)
        assertEquals(0, emptyWorld.dataModel.columnCount)
        
        // Should handle empty access gracefully
        assertNotNull(emptyWorld.getCurrentStringRow())
        assertNotNull(emptyWorld.getCurrentNumericRow())
    }



} 