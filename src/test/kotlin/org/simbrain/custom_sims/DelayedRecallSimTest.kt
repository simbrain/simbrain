/**
 * Tests the claim the delayed recall simulation is built to make: that truncation depth, not training
 * effort, is what decides whether the task can be learned.
 *
 * Uses the simulation's own sequence builder and scorer rather than copies, so the numbers quoted in its
 * documentation cannot drift away from what it actually does.
 */
package org.simbrain.custom_sims

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.simulations.demos.*
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.network.trainers.Xavier
import org.simbrain.util.setColConstant
import kotlin.random.Random

class DelayedRecallSimTest {

    private val SEEDS = listOf(1L, 2L, 3L)

    /**
     * Seeded through the simulation's own initialization strategy, so the run is repeatable without being
     * initialized differently from the way the simulation initializes.
     */
    private fun trainAndScore(truncationDepth: Int, weightSeed: Long, epochs: Int = 400): Double {
        val network = Network()
        val bptt = BPTTNetwork(DelayedRecall.ALPHABET_SIZE + 1, DelayedRecall.HIDDEN_UNITS, DelayedRecall.ALPHABET_SIZE)
        network.addNetworkModelsAsync(bptt)
        bptt.trainingSet = buildDelayedRecallSequence(DelayedRecall.TRAINING_TRIALS, Random(1))
        bptt.trainerConfig.truncationDepth = truncationDepth
        bptt.trainerConfig.learningRate = DelayedRecall.LEARNING_RATE
        bptt.trainerConfig.weightInitializationStrategy = Xavier(seed = weightSeed)
        val trainer = BPTTTrainer(network, bptt)
        runBlocking {
            trainer.randomize()
            // Biases are randomized from a shared, unseeded distribution, which would leave the run
            // dependent on what else had drawn from it. They start near zero and are trained anyway.
            listOf(bptt.hiddenLayer, bptt.outputLayer).forEach { it.biases.setColConstant(0, 0.0) }
            repeat(epochs) { trainer.trainOnce() }
        }
        return with(network) { bptt.measureRecallAccuracy(buildDelayedRecallSequence(60, Random(2))) }
    }

    @Test
    fun `only a window that spans a trial learns the task`() {
        // Averaged over a few seeds and stated as a comparison rather than as an absolute: a short window
        // occasionally starts from recurrent weights that happen to preserve the symbol well enough to beat
        // chance without ever learning to. What holds across seeds is the size of the gap.
        val spanning = SEEDS.map { trainAndScore(DelayedRecall.TRIAL_LENGTH, it) }.average()
        val tooShort = SEEDS.map { trainAndScore(2, it) }.average()

        assertTrue(spanning > 0.9) {
            "A depth of one trial reaches from each recall back to its cue, and should solve the task: " +
                    "spanning=$spanning tooShort=$tooShort"
        }
        assertTrue(spanning - tooShort > 0.4) {
            "A depth of 2 cannot span the ${DelayedRecall.DELAY + 1} step gap, so it should trail badly: " +
                    "spanning=$spanning tooShort=$tooShort"
        }
    }

    @Test
    fun `every trial ends on a recall step`() {
        val trials = 7
        val sequence = buildDelayedRecallSequence(trials, Random(3))
        assertEquals(trials * DelayedRecall.TRIAL_LENGTH, sequence.inputs.size)
        // The scorer picks out recall steps by position, so the layout it assumes has to be the real one.
        sequence.inputs.forEachIndexed { row, input ->
            val isRecallStep = row % DelayedRecall.TRIAL_LENGTH == DelayedRecall.TRIAL_LENGTH - 1
            assertEquals(if (isRecallStep) 1.0 else 0.0, input[DelayedRecall.ALPHABET_SIZE]) {
                "The recall cue unit should be on at recall steps and off everywhere else"
            }
            assertEquals(isRecallStep, sequence.targets[row].any { it > 0.0 }) {
                "Only recall steps should ask for an answer"
            }
        }
    }
}
