/**
 * SnapshotSet keeps insertion order without duplicates, lets an iteration in progress finish over the elements it
 * started with, and adds many elements in linear time.
 */
package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

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

    // Generous: adding 300k elements takes far less, where a copy-on-write set's quadratic time takes minutes
    @Test
    @Timeout(30)
    fun `adding many elements stays fast`() {
        val set = SnapshotSet<Int>()
        set.addAll((0 until 200_000).toList())
        (200_000 until 300_000).forEach { set.add(it) }
        assertEquals(300_000, set.size)
    }
}
