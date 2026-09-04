/** Verifies the fixed sequence structure used by the introductory BPTT simulation. */
package org.simbrain.custom_sims

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.demos.buildVariableDelayRecallDataset
import kotlin.random.Random

class RememberSymbolSimTest {

    @Test
    fun `remember symbol data consists of complete seven step sequences with one recall cue each`() {
        val sequenceLength = 7
        val symbols = 4
        val dataset = buildVariableDelayRecallDataset(2, Random(1))
        assertEquals(2 * symbols * (sequenceLength - 1) * sequenceLength, dataset.size)
        dataset.inputs.chunked(sequenceLength).zip(dataset.targets.chunked(sequenceLength)).forEach { (inputs, targets) ->
            val symbol = inputs.first().indexOf(1.0)
            assertTrue(symbol in 0 until symbols)
            assertEquals(1.0, inputs.first().sum())
            val recallSteps = inputs.indices.filter { inputs[it].last() == 1.0 }
            assertEquals(1, recallSteps.size)
            val recallStep = recallSteps.single()
            assertTrue(recallStep in 1 until sequenceLength)
            inputs.forEachIndexed { step, row ->
                if (step != 0 && step != recallStep) assertTrue(row.all { it == 0.0 })
            }
            targets.forEachIndexed { step, row ->
                if (step == recallStep) assertEquals(1.0, row[symbol]) else assertTrue(row.all { it == 0.0 })
            }
        }
    }
}
