package org.simbrain.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import smile.math.matrix.Matrix

class SmileUtilsTest {

    @Test
    fun `test validate shape`() {
        val a = Matrix(1, 2)
        val b = Matrix(2, 1)
        assertDoesNotThrow{ a.validateShape(a) }
        assertThrows<IllegalArgumentException> { a.validateShape(b) }

        // Uncomment to check the exception formatting
        // a.validateShape(b)
    }
}
