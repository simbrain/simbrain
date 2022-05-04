package org.simbrain.world

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.util.environment.SmellSource

/**
 * Test decay functions
 */
class SmellSourceTest {

    @Test
    fun `test smell vector`() {
        val source = SmellSource(5)
        source.dispersion = 100.0
        assertEquals(5, source.stimulusDimension)
        assertTrue(source.getStimulus(0.0).sum() > 0.0)
        assertArrayEquals(DoubleArray(5) { 0.0 }, source.getStimulus(200.0))
    }
    
}