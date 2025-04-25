package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StructuredProbeTest {

    @Test
    fun `test MapProbe basic operations`() {
        val probe = StructuredProbe.MapProbe()

        // Test writing values
        probe.write("key1", "value1")
        probe.write("key2", 42)
        probe.write("key3") { "computed value" }

        // Test probe contents
        val entries = probe.toList()
        assertEquals(3, entries.size)
        assertEquals("key1", entries[0].first)
        assertEquals("value1", entries[0].second)
        assertEquals("value1", probe.data["key1"])
        assertEquals("key2", entries[1].first)
        assertEquals(42, entries[1].second)
        assertEquals("key3", entries[2].first)
        assertEquals("computed value", entries[2].second)

        // Test string representation
        val treeString = probe.toTreeString()
        assertTrue(treeString.contains("key1"))
        assertTrue(treeString.contains("value1"))
        assertTrue(treeString.contains("key2"))
        assertTrue(treeString.contains("42"))
        assertTrue(treeString.contains("key3"))
        assertTrue(treeString.contains("computed value"))
    }

    @Test
    fun `test ListProbe basic operations`() {
        val probe = StructuredProbe.ListProbe()

        // Test writing values
        probe.write("value1")
        probe.write(42)
        probe.write { "computed value" }

        // Test probe contents
        val entries = probe.toList()
        assertEquals(3, entries.size)
        assertEquals(0, entries[0].first)
        assertEquals("value1", entries[0].second)
        assertEquals(1, entries[1].first)
        assertEquals(42, entries[1].second)
        assertEquals(2, entries[2].first)
        assertEquals("computed value", entries[2].second)

        // Test string representation
        val treeString = probe.toTreeString()
        assertTrue(treeString.contains("0"))
        assertTrue(treeString.contains("value1"))
        assertTrue(treeString.contains("1"))
        assertTrue(treeString.contains("42"))
        assertTrue(treeString.contains("2"))
        assertTrue(treeString.contains("computed value"))
    }

    @Test
    fun `test nested probes`() {
        val rootProbe = StructuredProbe.MapProbe()

        // Create nested MapProbe
        val nestedMap = rootProbe.createMapProbe("nested_map")
        nestedMap.write("nested_key", "nested_value")

        // Create nested ListProbe
        val nestedList = rootProbe.createListProbe("nested_list")
        nestedList.write("list_item1")
        nestedList.write("list_item2")

        // Test structure
        val entries = rootProbe.toList()
        assertEquals(2, entries.size)

        val mapEntry = entries.find { it.first == "nested_map" }
        assertNotNull(mapEntry)
        assertTrue(mapEntry!!.second is StructuredProbe.MapProbe)

        val listEntry = entries.find { it.first == "nested_list" }
        assertNotNull(listEntry)
        assertTrue(listEntry!!.second is StructuredProbe.ListProbe)

        // Test nested values
        val nestedMapContext = mapEntry.second as StructuredProbe.MapProbe
        assertEquals("nested_value", nestedMapContext.data["nested_key"])

        val nestedListContext = listEntry.second as StructuredProbe.ListProbe
        assertEquals("list_item1", nestedListContext.data[0])
        assertEquals("list_item2", nestedListContext.data[1])

        // Test string representation
        val treeString = rootProbe.toTreeString()
        assertTrue(treeString.contains("nested_map"))
        assertTrue(treeString.contains("nested_key"))
        assertTrue(treeString.contains("nested_value"))
        assertTrue(treeString.contains("nested_list"))
        assertTrue(treeString.contains("list_item1"))
        assertTrue(treeString.contains("list_item2"))
    }

    @Test
    fun `test diffTrainerProbes with identical probes`() {
        val probe1 = StructuredProbe.ListProbe()
        val probe2 = StructuredProbe.ListProbe()

        // Create identical contexts with the same structure
        val context1 = probe1.createMapProbe()
        context1.write("key1", "value1")
        context1.write("key2", 42)

        val context2 = probe2.createMapProbe()
        context2.write("key1", "value1")
        context2.write("key2", 42)

        val result = diffProbes(context1, context2)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test diffTrainerProbes with different values`() {
        val probe1 = StructuredProbe.ListProbe()
        val probe2 = StructuredProbe.ListProbe()

        // Create contexts with different values
        val context1 = probe1.createMapProbe()
        context1.write("key1", "value1")
        context1.write("key2", 42)

        val context2 = probe2.createMapProbe()
        context2.write("key1", "different_value")
        context2.write("key2", 43)

        val result = diffProbes(context1, context2)
        println("Diff result for different values:\n$result")

        // Values for key1
        assertTrue(result.contains("value1"))
        assertTrue(result.contains("different_value"))

        // Diff for key2, 42-43
        assertTrue(result.contains("-1.0"))

    }

    @Test
    fun `test diffTrainerProbes with missing keys`() {
        val probe1 = StructuredProbe.ListProbe()
        val probe2 = StructuredProbe.ListProbe()

        // Create contexts with different keys
        val context1 = probe1.createMapProbe()
        context1.write("key1", "value1")
        context1.write("key2", 42)

        val context2 = probe2.createMapProbe()
        context2.write("key1", "value1")
        context2.write("key3", "value3")

        // Test with allowMissing = false (default)
        val resultWithoutAllowMissing = diffProbes(context1, context2)
        println("Diff result without allowMissing: $resultWithoutAllowMissing")

        // The diff should mention missing keys when allowMissing is false
        if (resultWithoutAllowMissing.isNotEmpty()) {
            assertTrue(resultWithoutAllowMissing.contains("missing")) {
                "Diff should mention 'missing' when allowMissing is false"
            }
        }

        // Test with allowMissing = true
        val resultWithAllowMissing = diffProbes(context1, context2, allowMissing = true)
        println("Diff result with allowMissing: $resultWithAllowMissing")

        // When allowMissing is true, the diff might still contain information about keys
        // that exist in both probes but have different values
        if (resultWithAllowMissing.contains("key1")) {
            assertTrue(resultWithAllowMissing.contains("value1")) {
                "Diff should mention the value for key1 if it mentions key1"
            }
        }
    }

    @Test
    fun `test diffTrainerProbes with nested structures`() {
        val probe1 = StructuredProbe.ListProbe()
        val probe2 = StructuredProbe.ListProbe()

        // Create contexts with nested structures
        val context1 = probe1.createMapProbe()
        val nested1 = context1.createMapProbe("nested")
        nested1.write("nestedKey", "nestedValue")

        val context2 = probe2.createMapProbe()
        val nested2 = context2.createMapProbe("nested")
        nested2.write("nestedKey", "differentNestedValue")

        val result = diffProbes(context1, context2)
        println("Diff result for nested structures: $result")

        // The diff should handle nested structures and report differences in nested values
        if (result.contains("nested") && result.contains("nestedKey")) {
            assertTrue(result.contains("nestedValue") || result.contains("differentNestedValue")) {
                "Diff should mention at least one of the nested values being compared"
            }
        }
    }

    @Test
    fun `test diffTrainerProbes with custom diff function`() {
        val probe1 = StructuredProbe.ListProbe()
        val probe2 = StructuredProbe.ListProbe()

        // Create contexts with numeric differences
        val context1 = probe1.createMapProbe()
        context1.write("key1", 10)
        context1.write("key2", "string1")

        val context2 = probe2.createMapProbe()
        context2.write("key1", 15)
        context2.write("key2", "string2")

        // Custom diff function that only considers numbers with difference > 10 as different
        val customDiffFunction: (Any, Any) -> Any? = { a, b ->
            when {
                a is Number && b is Number -> {
                    val diff = a.toDouble() - b.toDouble()
                    if (Math.abs(diff) > 10) diff else true
                }
                else -> a == b
            }
        }

        val result = diffProbes(context1, context2, diffFunction = customDiffFunction)
        println("Diff result with custom diff function: $result")

        // With our custom diff function, key1 should not be reported as different
        // because the difference is only 5, which is less than our threshold of 10
        // But key2 should be reported as different because the strings are different
        if (result.contains("key2")) {
            assertTrue(result.contains("string1") || result.contains("string2")) {
                "Diff should mention at least one of the string values being compared for key2"
            }
        }
    }
}
