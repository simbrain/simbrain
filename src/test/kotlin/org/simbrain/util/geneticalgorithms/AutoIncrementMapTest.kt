package org.simbrain.util.geneticalgorithms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoIncrementMapTest {

    @Test
    fun `test forward mapping`() {
        val map = AutoIncrementMap<String>()
        map.add("one")
        assertEquals("one", map[0])
    }

    @Test
    fun `test inverse mapping`() {
        val map = AutoIncrementMap<String>()
        map.add("one")
        assertEquals(0, map["one"])
    }

    @Test
    fun `test auto increment`() {
        val map = AutoIncrementMap<String>()
        map.add("one")
        map.add("two")
        assertEquals("two", map[1])
        assertEquals(1, map["two"])
    }

    @Test
    fun `test copy`() {
        val map = AutoIncrementMap<String>().apply {
            add("one")
            add("two")
            add("three")
            add("four")
            add("five")
        }
        val newMap = map.copy { it }
        assertTrue((map.entries zip newMap.entries).all { (a, b) -> a.key == b.key && a.value == b.value })
    }
}