/** Verifies the fixed sequence structure used by the introductory BPTT simulation. */
package org.simbrain.custom_sims

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.demos.buildRememberSymbolDataset
import kotlin.random.Random

class RememberSymbolSimTest {

    @Test
    fun `remember symbol data consists of complete four step sequences`() {
        val dataset = buildRememberSymbolDataset(3, Random(1))
        assertEquals(12, dataset.size)
        dataset.inputs.chunked(4).zip(dataset.targets.chunked(4)).forEach { (inputs, targets) ->
            assertEquals(1.0, inputs.first().sum())
            assertTrue(inputs.drop(1).take(2).all { it.all { value -> value == 0.0 } })
            assertEquals(1.0, inputs.last().last())
            assertTrue(targets.dropLast(1).all { it.all { value -> value == 0.0 } })
            assertEquals(1.0, targets.last().sum())
        }
    }
}
