//package org.simbrain.util.table
//
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//import org.simbrain.util.stats.distributions.NormalDistribution
//import org.simbrain.util.stats.distributions.UniformIntegerDistribution
//import smile.data.type.DataType as SmileDataType
//
//class ColumnTest {
//
//    @Test
//    fun `test column creation`() {
//        val column = Column("TestCol", Column.DataType.DoubleType)
//        assertEquals("TestCol", column.name)
//        assertEquals("TestCol", column.columName)
//        assertEquals(Column.DataType.DoubleType, column.type)
//        assertTrue(column.enabled)
//        assertTrue(column.isNumeric())
//    }
//
//    @Test
//    fun `test column data types`() {
//        val doubleCol = Column("Double", Column.DataType.DoubleType)
//        val intCol = Column("Int", Column.DataType.IntType)
//        val stringCol = Column("String", Column.DataType.StringType)
//
//        assertEquals(Double::class.java, doubleCol.type.clazz())
//        assertEquals(Int::class.java, intCol.type.clazz())
//        assertEquals(String::class.java, stringCol.type.clazz())
//    }
//
//    @Test
//    fun `test default values`() {
//        assertEquals(0.0, Column.DataType.DoubleType.defaultValue)
//        assertEquals(0, Column.DataType.IntType.defaultValue)
//        assertEquals("", Column.DataType.StringType.defaultValue)
//    }
//
//    @Test
//    fun `test numeric type detection`() {
//        val doubleCol = Column("Double", Column.DataType.DoubleType)
//        val intCol = Column("Int", Column.DataType.IntType)
//        val stringCol = Column("String", Column.DataType.StringType)
//
//        assertTrue(doubleCol.isNumeric())
//        assertTrue(intCol.isNumeric())
//        assertFalse(stringCol.isNumeric())
//    }
//
//    @Test
//    fun `test random value generation for double column`() {
//        val column = Column("TestDouble", Column.DataType.DoubleType)
//        val randomValue = column.getRandom()
//        assertTrue(randomValue is Double)
//        assertTrue(randomValue.toDouble() >= 0.0 && randomValue.toDouble() <= 1.0) // Default uniform distribution
//    }
//
//    @Test
//    fun `test random value generation for int column`() {
//        val column = Column("TestInt", Column.DataType.IntType)
//        val randomValue = column.getRandom()
//        assertTrue(randomValue is Int)
//        assertTrue(randomValue.toInt() >= 0) // Should be non-negative int
//    }
//
//    @Test
//    fun `test custom randomizer for column`() {
//        val column = Column("TestCol", Column.DataType.DoubleType)
//        column.columnRandomizer = NormalDistribution(50.0, 10.0)
//
//        val values = (1..100).map { column.getRandom().toDouble() }
//        val mean = values.average()
//
//        // Should be roughly centered around 50 (allowing for some variance)
//        assertTrue(mean > 40.0 && mean < 60.0)
//    }
//
//    @Test
//    fun `test integer randomizer`() {
//        val column = Column("TestInt", Column.DataType.IntType)
//        column.columnRandomizer = UniformIntegerDistribution(10, 20)
//
//        val values = (1..50).map { column.getRandom().toInt() }
//
//        // All values should be within range
//        assertTrue(values.all { it in 10..20 })
//    }
//
//    @Test
//    fun `test column with smile data type constructor`() {
//        val doubleColumn = Column("SmileDouble", SmileDataType.DoubleType)
//        assertEquals(Column.DataType.DoubleType, doubleColumn.type)
//        assertEquals("SmileDouble", doubleColumn.name)
//
//        val intColumn = Column("SmileInt", SmileDataType.IntegerType)
//        assertEquals(Column.DataType.IntType, intColumn.type)
//    }
//
//    @Test
//    fun `test create column from value inference`() {
//        // Test integer inference
//        val intColumn = createColumn("IntCol", 42)
//        assertEquals(Column.DataType.IntType, intColumn.type)
//        assertEquals("IntCol", intColumn.name)
//
//        // Test double inference
//        val doubleColumn = createColumn("DoubleCol", 3.14)
//        assertEquals(Column.DataType.DoubleType, doubleColumn.type)
//
//        // Test string inference
//        val stringColumn = createColumn("StringCol", "hello")
//        assertEquals(Column.DataType.StringType, stringColumn.type)
//
//        // Test null value
//        val nullColumn = createColumn("NullCol", null)
//        assertEquals(Column.DataType.StringType, nullColumn.type)
//    }
//
//    @Test
//    fun `test create column from string number inference`() {
//        // Test string that looks like int
//        val intStringColumn = createColumn("IntString", "123")
//        assertEquals(Column.DataType.IntType, intStringColumn.type)
//
//        // Test string that looks like double
//        val doubleStringColumn = createColumn("DoubleString", "3.14")
//        assertEquals(Column.DataType.DoubleType, doubleStringColumn.type)
//
//        // Test string that's not a number
//        val stringColumn = createColumn("PureString", "hello world")
//        assertEquals(Column.DataType.StringType, stringColumn.type)
//    }
//
//    @Test
//    fun `test get data type from class`() {
//        assertEquals(Column.DataType.DoubleType, getDataType(Double::class.java))
//        assertEquals(Column.DataType.DoubleType, getDataType(Float::class.java))
//        assertEquals(Column.DataType.IntType, getDataType(Int::class.java))
//        assertEquals(Column.DataType.IntType, getDataType(Byte::class.java))
//        assertEquals(Column.DataType.StringType, getDataType(String::class.java))
//        assertEquals(Column.DataType.StringType, getDataType(Any::class.java))
//    }
//
//    @Test
//    fun `test smile to simbrain data type conversion`() {
//        assertEquals(Column.DataType.DoubleType, smileToSimbrainDataType(SmileDataType.DoubleType))
//        assertEquals(Column.DataType.IntType, smileToSimbrainDataType(SmileDataType.IntegerType))
//        assertEquals(Column.DataType.StringType, smileToSimbrainDataType(SmileDataType.StringType))
//        assertEquals(Column.DataType.StringType, smileToSimbrainDataType(SmileDataType.BooleanType))
//    }
//
//    @Test
//    fun `test smile data type get column data type`() {
//        assertEquals(Column.DataType.DoubleType, SmileDataType.DoubleType.getColumnDataType())
//        assertEquals(Column.DataType.DoubleType, SmileDataType.FloatType.getColumnDataType())
//        assertEquals(Column.DataType.IntType, SmileDataType.IntegerType.getColumnDataType())
//        assertEquals(Column.DataType.IntType, SmileDataType.ByteType.getColumnDataType())
//        assertEquals(Column.DataType.StringType, SmileDataType.StringType.getColumnDataType())
//    }
//
//    @Test
//    fun `test column enabled property`() {
//        val column = Column("TestCol", Column.DataType.DoubleType)
//        assertTrue(column.enabled) // Default should be enabled
//
//        column.enabled = false
//        assertFalse(column.enabled)
//
//        column.enabled = true
//        assertTrue(column.enabled)
//    }
//
//    @Test
//    fun `test column name property inheritance`() {
//        val column = Column("MyColumn", Column.DataType.StringType)
//        assertEquals("MyColumn", column.name) // Inherited from EditableObject
//        assertEquals("MyColumn", column.columName) // Column-specific property
//    }
//
//    @Test
//    fun `test data type enum completeness`() {
//        // Ensure all data types have proper class mappings
//        val dataTypes = Column.DataType.values()
//        assertTrue(dataTypes.isNotEmpty())
//
//        for (dataType in dataTypes) {
//            assertNotNull(dataType.clazz())
//            assertNotNull(dataType.defaultValue)
//        }
//    }
//}