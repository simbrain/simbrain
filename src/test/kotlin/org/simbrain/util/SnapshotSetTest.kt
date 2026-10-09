package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SnapshotSetTest {

    @Test
    fun `iterates in insertion order without duplicates`() {
        val set = SnapshotSet<String>()
        set.add("b")
        set.add("a")
        assertFalse(set.add("b"))
        set.addAll(listOf("c", "a", "d"))
        assertEquals(listOf("b", "a", "c", "d"), set.toList())
        assertEquals(4, set.size)
        assertTrue("c" in set)
    }

    @Test
    fun `removes single elements, collections, and everything`() {
        val set = SnapshotSet<Int>()
        set.addAll((1..6).toList())
        set.remove(2)
        set.removeAll(listOf(4, 5, 99))
        assertEquals(listOf(1, 3, 6), set.toList())
        set.clear()
        assertTrue(set.isEmpty())
        assertEquals(emptyList<Int>(), set.toList())
    }

    @Test
    fun `an iteration in progress does not see later changes`() {
        val set = SnapshotSet<Int>()
        set.addAll(listOf(1, 2, 3))
        val seen = mutableListOf<Int>()
        for (element in set) {
            seen += element
            set.add(element + 10)
            set.remove(3)
        }
        assertEquals(listOf(1, 2, 3), seen)
        assertEquals(listOf(1, 2, 11, 12, 13), set.toList())
    }

    @Test
    fun `adding many elements stays fast`() {
        val set = SnapshotSet<Int>()
        val start = System.nanoTime()
        set.addAll((0 until 200_000).toList())
        (200_000 until 300_000).forEach { set.add(it) }
        assertEquals(300_000, set.size)
        assertTrue((System.nanoTime() - start) / 1e9 < 2.0, "adding 300k elements should take far less than the quadratic time a copy-on-write set needs")
    }
}
