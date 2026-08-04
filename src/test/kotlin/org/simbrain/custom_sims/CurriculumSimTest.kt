/**
 * Tests the claim the curriculum simulation is built to make: at a truncation depth too shallow for the
 * long gaps, whether they can be learned depends on whether short gaps are present in the data.
 *
 * Uses the simulation's own sequence builder and scorer, so the numbers its documentation quotes cannot
 * drift away from what it does.
 */
package org.simbrain.custom_sims

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.demos.Curriculum
import org.simbrain.custom_sims.simulations.demos.buildVariableGapSequence
import org.simbrain.custom_sims.simulations.demos.measureAccuracyByGap
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.network.trainers.Xavier
import org.simbrain.util.setColConstant
import kotlin.random.Random

class CurriculumSimTest {

    private val trainingTrials = 56

    /** Mean accuracy on the long gaps only, which are the ones the window is too shallow to reach. */
    private fun longGapAccuracy(trainGaps: List<Int>, weightSeed: Long): Double {
        val network = Network()
        val bptt = BPTTNetwork(Curriculum.ALPHABET_SIZE + 1, Curriculum.HIDDEN_UNITS, Curriculum.ALPHABET_SIZE)
        network.addNetworkModelsAsync(bptt)
        bptt.trainingSet = buildVariableGapSequence(trainingTrials, trainGaps, Random(1))
        bptt.trainerConfig.truncationDepth = Curriculum.TRUNCATION_DEPTH
        bptt.trainerConfig.learningRate = Curriculum.LEARNING_RATE
        bptt.trainerConfig.weightInitializationStrategy = Xavier(seed = weightSeed)
        val trainer = BPTTTrainer(network, bptt)
        runBlocking {
            trainer.randomize()
            // Biases come from a shared unseeded randomizer, which would leave the run dependent on
            // whatever else had drawn from it.
            listOf(bptt.hiddenLayer, bptt.outputLayer).forEach { it.biases.setColConstant(0, 0.0) }
            repeat(400) { trainer.trainOnce() }
        }
        val byGap = with(network) {
            bptt.measureAccuracyByGap(buildVariableGapSequence(70, Curriculum.GAPS, Random(2)))
        }
        return Curriculum.LONG_GAPS.map { byGap.getValue(it) }.average()
    }

    @Test
    fun `short gaps in the data let a shallow window learn the long ones`() {
        val withLadder = longGapAccuracy(Curriculum.GAPS, weightSeed = 1L)
        val withoutLadder = longGapAccuracy(Curriculum.LONG_GAPS, weightSeed = 1L)

        assertTrue(withLadder > 0.9) {
            "A window of ${Curriculum.TRUNCATION_DEPTH} never spans these gaps, but a rule learned from " +
                    "the short ones composes to cover them: got $withLadder"
        }
        assertTrue(withoutLadder < 1.0 / Curriculum.ALPHABET_SIZE + 0.2) {
            "Without short gaps to build from there is nothing a window this shallow can learn, even " +
                    "though these are the very gaps it was trained on: got $withoutLadder"
        }
    }

    @Test
    fun `both conditions train on the same amount of data`() {
        // The comparison only means anything if the sets are the same size. The long-only set even holds
        // more examples of the gaps being tested, which is the stronger form of the claim.
        val all = buildVariableGapSequence(trainingTrials, Curriculum.GAPS, Random(1))
        val longOnly = buildVariableGapSequence(trainingTrials, Curriculum.LONG_GAPS, Random(1))
        assertEquals(all.inputs.size, longOnly.inputs.size)
        assertEquals(trainingTrials * Curriculum.TRIAL_LENGTH, all.inputs.size)
    }

    @Test
    fun `trials are declared as independent sequences`() {
        val data = buildVariableGapSequence(4, Curriculum.GAPS, Random(1))
        assertEquals(Curriculum.TRIAL_LENGTH, data.sequenceLength) {
            "Trials here really are independent, so the data should say so and have memory cleared between"
        }
        // Exactly one recall per trial, which is what the per-gap scorer assumes when it reads the gap off
        // a row's position.
        (0 until 4).forEach { trial ->
            val answered = (0 until Curriculum.TRIAL_LENGTH).count { row ->
                data.targets[trial * Curriculum.TRIAL_LENGTH + row].any { it > 0.0 }
            }
            assertEquals(1, answered)
        }
    }
}
