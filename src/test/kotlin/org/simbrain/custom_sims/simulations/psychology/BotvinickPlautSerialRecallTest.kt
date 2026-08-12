/** Tests the compact, simulation-specific Botvinick-Plaut serial-recall model. */
package org.simbrain.custom_sims.simulations.psychology

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BotvinickPlautSerialRecallTest {

    @Test
    fun `trial presents items then recalls them and ends`() {
        val model = BotvinickPlautModel(Random(1))
        val trial = model.newTrial(4)

        assertEquals(9, trial.inputs.size)
        assertEquals(9, trial.targets.size)
        assertEquals(trial.items[0], trial.inputs[0].indexOfFirst { it == 1.0 })
        assertEquals(model.inputSize - 1, trial.inputs[4].indexOfFirst { it == 1.0 })
        assertEquals(model.outputSize - 1, trial.targets.last().indexOfFirst { it == 1.0 })
        assertEquals(4, trial.items.toSet().size)
    }

    @Test
    fun `training improves frozen serial recall without changing weights during evaluation`() {
        val model = BotvinickPlautModel(Random(12))
        model.newTrial(4)
        repeat(12_000) { model.trainCycle() }

        val beforeEvaluation = model.weightChecksum()
        val evaluation = model.evaluate(30)

        assertEquals(beforeEvaluation, model.weightChecksum())
        assertTrue(evaluation.wholeListAccuracy.average() > 0.25) {
            "Expected trained model to exceed chance, whole=${evaluation.wholeListAccuracy.contentToString()} " +
                    "positions=${evaluation.serialPositionAccuracy}"
        }
        assertTrue(evaluation.serialPositionAccuracy.values.filterNotNull().average() > 0.5) {
            "Expected trained frozen recall to exceed chance by position: ${evaluation.serialPositionAccuracy}"
        }
    }
}
