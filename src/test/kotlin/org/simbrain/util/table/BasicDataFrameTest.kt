package org.simbrain.util.table

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BasicDataFrameTest {

    @Test
    fun `test creation`() {
        val df = BasicDataFrame(3, 4, )
        assertEquals(3, df.rowCount)
        assertEquals(4, df.columnCount)
        // println(df)
    }

    @Test
    fun `test column and rownames`() {
        val df = BasicDataFrame(2, 3, )
        df.columnNames = listOf("A", "B", "C")
        df.rowNames = listOf("Row1", "Row2")
        assertEquals("B", df.columnNames[1])
        assertEquals("Row1", df.rowNames[0])
        println(df)
    }

}
