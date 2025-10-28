package org.simbrain.world.dataworld

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.table.BasicDataFrame

class DataWorldTest {

    private val dataWorld: DataWorld = DataWorld(rows = 5, cols = 3)
    private val dataComponent: DataWorldComponent = DataWorldComponent("Test Data World")

    @Test
    fun `test initial data world creation`() {
        assertEquals(5, dataWorld.dataModel.rowCount)
        assertEquals(3, dataWorld.dataModel.columnCount)
        assertEquals(0, dataWorld.dataModel.currentRowIndex)
        // Defaults from DataWorld
        assertEquals(DataWorld.RowAdvancement.LOOP, dataWorld.rowAdvancement)
        assertEquals(DataWorld.WriteBehavior.OVERWRITE, dataWorld.writeBehavior)
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
    fun `test LOOP advancement moves forward and wraps`() = runBlocking {
        dataWorld.rowAdvancement = DataWorld.RowAdvancement.LOOP
        assertEquals(0, dataWorld.dataModel.currentRowIndex)

        // Advance a couple times
        dataWorld.update()
        assertEquals(1, dataWorld.dataModel.currentRowIndex)
        dataWorld.update()
        assertEquals(2, dataWorld.dataModel.currentRowIndex)

        // Jump to last and wrap
        repeat(5 - 1 - 2) { dataWorld.update() } // go to last row index = 4
        assertEquals(4, dataWorld.dataModel.currentRowIndex)

        dataWorld.update()
        assertEquals(0, dataWorld.dataModel.currentRowIndex) // wrapped
    }

    @Test
    fun `test STATIC advancement stays put`() = runBlocking {
        dataWorld.rowAdvancement = DataWorld.RowAdvancement.STATIC
        assertEquals(0, dataWorld.dataModel.currentRowIndex)

        repeat(5) { dataWorld.update() }
        assertEquals(0, dataWorld.dataModel.currentRowIndex)

        // Manually change row and verify it stays there
        dataWorld.dataModel.currentRowIndex = 3
        repeat(3) { dataWorld.update() }
        assertEquals(3, dataWorld.dataModel.currentRowIndex)
    }

    @Test
    fun `OVERWRITE does not add rows at end`() {
        dataWorld.writeBehavior = DataWorld.WriteBehavior.OVERWRITE
        val initialRowCount = dataWorld.dataModel.rowCount

        // Ensure string columns to avoid type surprises
        dataWorld.dataModel.columns.forEach {
            it.type = org.simbrain.util.table.Column.DataType.StringType
        }

        // Move to last row and write
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentStringRow(arrayOf("last", "row", "data"))

        // Should not grow
        assertEquals(initialRowCount, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `APPEND adds a new row when writing at last row (strings)`() {
        dataWorld.writeBehavior = DataWorld.WriteBehavior.APPEND
        val initialRowCount = dataWorld.dataModel.rowCount

        dataWorld.dataModel.columns.forEach {
            it.type = org.simbrain.util.table.Column.DataType.StringType
        }

        // Move to last row and write
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentStringRow(arrayOf("last", "row", "data"))

        // Should grow by 1
        assertEquals(initialRowCount + 1, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `APPEND adds a new row when writing at last row (numeric)`() {
        dataWorld.writeBehavior = DataWorld.WriteBehavior.APPEND
        val initialRowCount = dataWorld.dataModel.rowCount

        // Move to last row and write
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentNumericRow(doubleArrayOf(7.0, 8.0, 9.0))

        // Should grow by 1
        assertEquals(initialRowCount + 1, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `APPEND does not add row when writing in the middle`() {
        dataWorld.writeBehavior = DataWorld.WriteBehavior.APPEND
        val initialRowCount = dataWorld.dataModel.rowCount

        // Write in the middle (not at last row)
        dataWorld.dataModel.currentRowIndex = 1
        dataWorld.setCurrentNumericRow(doubleArrayOf(1.0, 2.0, 3.0))

        assertEquals(initialRowCount, dataWorld.dataModel.rowCount)
    }

    @Test
    fun `LOOP + OVERWRITE cycles without growing`() = runBlocking {
        dataWorld.rowAdvancement = DataWorld.RowAdvancement.LOOP
        dataWorld.writeBehavior = DataWorld.WriteBehavior.OVERWRITE
        val initialRowCount = dataWorld.dataModel.rowCount

        repeat(initialRowCount * 2) {
            dataWorld.setCurrentNumericRow(doubleArrayOf(it.toDouble(), 0.0, 0.0))
            dataWorld.update()
        }
        assertEquals(initialRowCount, dataWorld.dataModel.rowCount)
        // Index should have wrapped multiple times; exact position depends on parity, but rowCount unchanged
    }

    @Test
    fun `LOOP + APPEND grows when writing at boundary and keeps cycling`() = runBlocking {
        dataWorld.rowAdvancement = DataWorld.RowAdvancement.LOOP
        dataWorld.writeBehavior = DataWorld.WriteBehavior.APPEND

        val initialRowCount = dataWorld.dataModel.rowCount
        // Move to last row
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1

        // Write at the last row -> should append a new row
        dataWorld.setCurrentNumericRow(doubleArrayOf(1.0, 2.0, 3.0))
        assertEquals(initialRowCount + 1, dataWorld.dataModel.rowCount)

        // Next update should move to the newly created row (index initialRowCount)
        dataWorld.update()
        assertEquals(initialRowCount, dataWorld.dataModel.currentRowIndex)
    }

    @Test
    fun `STATIC + OVERWRITE keeps writing on same row without growing`() = runBlocking {
        dataWorld.rowAdvancement = DataWorld.RowAdvancement.STATIC
        dataWorld.writeBehavior = DataWorld.WriteBehavior.OVERWRITE
        val initialRowCount = dataWorld.dataModel.rowCount

        repeat(3) {
            dataWorld.setCurrentNumericRow(doubleArrayOf(it.toDouble(), it.toDouble(), it.toDouble()))
            dataWorld.update()
        }

        assertEquals(initialRowCount, dataWorld.dataModel.rowCount)
        assertEquals(0, dataWorld.dataModel.currentRowIndex)

        val retrieved = dataWorld.dataModel.getRow<Double>(0)
        assertArrayEquals(doubleArrayOf(2.0, 2.0, 2.0), retrieved.toDoubleArray(), 1e-6)
    }

    @Test
    fun `STATIC + APPEND appends only when at last row`() = runBlocking {
        dataWorld.rowAdvancement = DataWorld.RowAdvancement.STATIC
        dataWorld.writeBehavior = DataWorld.WriteBehavior.APPEND
        val initialRowCount = dataWorld.dataModel.rowCount

        // At row 0 (not last) -> no growth
        dataWorld.dataModel.currentRowIndex = 0
        dataWorld.setCurrentNumericRow(doubleArrayOf(1.0, 1.0, 1.0))
        assertEquals(initialRowCount, dataWorld.dataModel.rowCount)

        // Move to last row and write -> should append
        dataWorld.dataModel.currentRowIndex = initialRowCount - 1
        dataWorld.setCurrentNumericRow(doubleArrayOf(2.0, 2.0, 2.0))
        assertEquals(initialRowCount + 1, dataWorld.dataModel.rowCount)

        // STATIC means we did not auto-advance; still at last old row
        assertEquals(initialRowCount - 1, dataWorld.dataModel.currentRowIndex)
    }

    @Test
    fun `test setting current string row`() {
        dataWorld.dataModel.columns.forEach {
            it.type = org.simbrain.util.table.Column.DataType.StringType
        }

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
        dataWorld.dataModel.columns.forEach {
            it.type = org.simbrain.util.table.Column.DataType.StringType
        }

        dataWorld.dataModel.setValueAt("custom_value", 1, 1)
        assertEquals("custom_value", dataWorld.dataModel.getValueAt(1, 1))

        dataWorld.dataModel.setRow(2, arrayOf("row2_col1", "row2_col2", "row2_col3"))
        val row = dataWorld.dataModel.getRow<String>(2)
        assertEquals(listOf("row2_col1", "row2_col2", "row2_col3"), row)
    }

    @Test
    fun `test serialization and deserialization`() {
        val dataFrame = BasicDataFrame(mutableListOf(
            mutableListOf("1", "2"),
            mutableListOf("3", "4"),
            mutableListOf("", "")  // Empty row for proper size
        ))

        val xstream = org.simbrain.util.getSimbrainXStream()
        val xmlString = xstream.toXML(dataFrame)
        val deserializedDataFrame = xstream.fromXML(xmlString) as BasicDataFrame

        assertEquals(3, deserializedDataFrame.rowCount)
        assertEquals(2, deserializedDataFrame.columnCount)

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

        // Accessors should not throw
        assertNotNull(emptyWorld.getCurrentStringRow())
        assertNotNull(emptyWorld.getCurrentNumericRow())
    }
}
