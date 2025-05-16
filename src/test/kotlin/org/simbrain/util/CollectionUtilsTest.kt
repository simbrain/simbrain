package org.simbrain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.geneticalgorithm.toProbabilityWeight

class CollectionUtilsTest {

    @Test
    fun `test set complement`() {
        val a = setOf(1, 2, 3)
        val b = setOf(3, 4)
        // a and b intersect in 3
        // left complement is 1, 2
        // right complement is 4
        val diff = a complement b
        assertEquals(setOf(1, 2), diff.leftComp)
        assertEquals(setOf(4), diff.rightComp)
        assertFalse(diff.isIdentical())
    }

    @Test
    fun `test identity using set complement`() {
        val a = setOf(1, 2, 3)
        assertTrue((a complement a).isIdentical())
    }

    @Test
    fun `test cartesian product`() {
        val list1 = listOf(1, 2)
        val list2 = listOf("a", "b")
        val product = list1 cartesianProduct list2
        assertEquals(
            listOf(1 to "a", 1 to "b", 2 to "a", 2 to "b"),
            product
        )
    }

    @Test
    fun `test flatten array`() {
        val array = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0))
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), flattenArray(array))
    }

    @Test
    fun `test reshape`() {
        val flat = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val reshaped = reshape(2, 2, flat)
        assertArrayEquals(doubleArrayOf(1.0, 2.0), reshaped[0])
        assertArrayEquals(doubleArrayOf(3.0, 4.0), reshaped[1])
    }

    @Test
    fun `test reshape 1x4`() {
        val input = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val result = reshape(1, 4, input)
        assertEquals(1.0, result[0][0])
        assertEquals(2.0, result[0][1])
        assertEquals(3.0, result[0][2])
        assertEquals(4.0, result[0][3])    }

    @Test
    fun `test reshape 4x1`() {
        val input = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val result = reshape(4, 1, input)
        assertEquals(1.0, result[0][0])
        assertEquals(2.0, result[1][0])
        assertEquals(3.0, result[2][0])
        assertEquals(4.0, result[3][0])
    }

    @Test
    fun `test min-max normalize`() {
        val values = listOf(10.0, 20.0, 30.0)
        val norm = values.minMaxNormalize()
        assertEquals(0.0, norm[0], 1e-6)
        assertEquals(0.5, norm[1], 1e-6)
        assertEquals(1.0, norm[2], 1e-6)
    }

    @Test
    fun `test min-max normalize for range of 0`() {
        val values = listOf(10.0, 10.0)
        val norm = values.minMaxNormalize()
        assertEquals(0.0, norm[0], 1e-6)
        assertEquals(0.0, norm[1], 1e-6)
    }

    @Test
    fun `test reshape on invalid size`() {
        val input = doubleArrayOf(1.0, 2.0, 3.0)
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            reshape(2, 2, input)
        }
    }

    @Test
    fun `test normalize`() {
        val values = listOf(2.0, 2.0)
        val norm = values.normalize()
        assertEquals(0.5, norm[0], 1e-6)
        assertEquals(0.5, norm[1], 1e-6)
        assertEquals(1.0, norm.sum(), 1e-6)
    }

    @Test
    fun `test deferred hash map`() = runBlocking {
        val map = CompletableDeferredHashMap<String, Int>()
        launch {
            delay(100)
            map["a"] = 42
        }
        // This one does not wait
        assertNull(map.getImmediately("a"))
        // This one waits
        assertEquals(42, map.get("a"))
    }


}

