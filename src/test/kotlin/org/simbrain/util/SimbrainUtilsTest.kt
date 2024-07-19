package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimbrainUtilsTest {

    @Test
    fun `cached object init value is set`() {
        var callCounter = 0
        val cachedObject = CachedObject {
            callCounter += 1
            "thing"
        }

        assertEquals("thing", cachedObject.value)
        assertEquals(1, callCounter)
    }

    @Test
    fun `value should not update if not invalidated`() {
        var values = listOf(1, 2, 3)
        val cachedSum = CachedObject {
            values.sum()
        }
        assertEquals(6, cachedSum.value)
        values = listOf(4, 5, 6)
        assertEquals(6, cachedSum.value)
    }

    @Test
    fun `init function should not be called again if value is not invalidated`() {
        var callCounter = 0
        val cachedObject = CachedObject {
            callCounter += 1
            "thing"
        }
        assertEquals("thing", cachedObject.value)
        assertEquals("thing", cachedObject.value)
        assertEquals(1, callCounter)
    }

    @Test
    fun `value should update if invalidated`() {
        var values = listOf(1, 2, 3)
        val cachedSum = CachedObject {
            values.sum()
        }
        assertEquals(6, cachedSum.value)
        values = listOf(4, 5, 6)
        assertEquals(6, cachedSum.value)
        cachedSum.invalidate()
        assertEquals(15, cachedSum.value)
    }

    @Test
    fun `cached object should initialize lazily`() {
        var callCounter = 0
        val cachedObject = CachedObject {
            callCounter += 1
            "thing"
        }
        assertEquals(0, callCounter)
        cachedObject.value
        assertEquals(1, callCounter)
    }

    @Test
    fun `invalidate should be lazy`() {
        var callCounter = 0
        val cachedObject = CachedObject {
            callCounter += 1
            "thing"
        }
        assertEquals(0, callCounter)
        cachedObject.invalidate()
        assertEquals(0, callCounter)
        cachedObject.value
        assertEquals(1, callCounter)
    }

    @Test
    fun `cached object should be settable`() {
        val cachedObject = CachedObject {
            "thing"
        }
        assertEquals("thing", cachedObject.value)
        cachedObject.value = "other thing"
        assertEquals("other thing", cachedObject.value)
    }

    @Test
    fun `setting the value of the cached object should reset the dirty flag`() {
        var callCounter = 0
        var values = listOf(1, 2, 3)
        val cachedSum = CachedObject {
            callCounter += 1
            values.sum()
        }
        assertEquals(6, cachedSum.value)
        assertEquals(1, callCounter)
        values = listOf(4, 5, 6)
        cachedSum.value = values.sum()
        assertEquals(15, cachedSum.value)
        assertEquals(1, callCounter)
    }

}