package org.simbrain.network.trainers

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.events.TrainingStats
import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.network.llm.TinyLmModel

class TapeTrainerTest {

    private fun windows(corpus: IntArray, contextSize: Int) =
        (0 until corpus.size - contextSize).map { start ->
            corpus.copyOfRange(start, start + contextSize) to
                corpus.copyOfRange(start + 1, start + contextSize + 1)
        }

    private fun trainer(): TapeTrainer {
        val model = TinyLmModel(TinyLmConfig(
            contextSize = 6, embedDim = 12, numHeads = 3, hiddenDim = 16, vocabSize = 5, numLayers = 1
        ), seed = 7L)
        return TapeTrainer(model).apply {
            trainingWindows = windows(IntArray(30) { it % 5 }, 6)
            testingWindows = windows(IntArray(12) { (it + 2) % 5 }, 6)
        }
    }

    @Test
    fun `train once fires stats with loss accuracy and step size`() {
        val trainer = trainer()
        val stats = mutableListOf<TrainingStats>()
        trainer.events.errorUpdated.on { stats.add(it) }

        runBlocking {
            trainer.trainOnce()
            repeat(9) { trainer.trainOnce() }
        }
        assertEquals(10, trainer.iteration)
        assertEquals(10, stats.size)
        assertTrue(stats.last().trainingError < stats.first().trainingError, "loss must fall over an epoch loop")
        assertNotNull(stats.first().trainingAccuracy)
        assertNotNull(stats.first().effectiveStepSize)
        assertTrue(stats.first().effectiveStepSize!! > 0.0)
        val testStats = stats[9]
        assertNotNull(testStats.testingError, "iteration 10 hits the default test frequency")
        assertNotNull(testStats.testingAccuracy)
    }

    @Test
    fun `continuous training honors the stopping condition and fires lifecycle events`() {
        val trainer = trainer()
        trainer.stoppingCondition.maxIterations = 7
        var began = 0
        var ended = 0
        trainer.events.beginTraining.on { began++ }
        trainer.events.endTraining.on { ended++ }

        runBlocking {
            trainer.startTraining()
            withTimeout(30_000) {
                while (trainer.iteration < 7 || trainer.isRunning) delay(20)
            }
        }
        assertEquals(7, trainer.iteration)
        assertFalse(trainer.isRunning)
        assertEquals(1, began)
        assertEquals(1, ended)
    }

    @Test
    fun `error threshold stops a converging run before max iterations`() {
        val trainer = trainer()
        trainer.stoppingCondition.maxIterations = 500
        trainer.stoppingCondition.useErrorThreshold = true
        trainer.stoppingCondition.errorThreshold = 0.2

        runBlocking {
            trainer.startTraining()
            withTimeout(60_000) {
                while (trainer.isRunning || trainer.iteration == 0) delay(20)
            }
        }
        assertTrue(trainer.lastTrainingError < 0.2, "stopped at ${trainer.lastTrainingError}")
        assertTrue(trainer.iteration < 500, "threshold must fire before max iterations")
        assertTrue(trainer.lastTrainingAccuracy!! > 0.9, "cyclic corpus should be learned near-perfectly")
    }

    @Test
    fun `a train task arriving mid step walk stops cleanly instead of hanging`() {
        val trainer = trainer()
        val (tokens, targets) = trainer.trainingWindows.first()
        trainer.model.beginSteppedTrainStep(tokens, targets)

        runBlocking { withTimeout(10_000) { trainer.trainOnce() } }
        assertFalse(trainer.isRunning)
        assertEquals(0, trainer.iteration, "the walk-guarded train task must not run an epoch")

        while (trainer.model.midWalk) trainer.model.stepOp()
        runBlocking { withTimeout(10_000) { trainer.trainOnce() } }
        assertEquals(1, trainer.iteration, "the task loop must survive the refused train task")
    }

    @Test
    fun `starting training while a walk is armed refuses without firing beginTraining`() {
        val trainer = trainer()
        var began = 0
        trainer.events.beginTraining.on { began++ }
        val (tokens, targets) = trainer.trainingWindows.first()
        trainer.model.beginSteppedTrainStep(tokens, targets)

        runBlocking {
            withTimeout(10_000) { trainer.startTraining() }
            delay(100)
        }
        assertFalse(trainer.isRunning)
        assertEquals(0, began)
    }

    @Test
    fun `resetting the iteration counter fires the reset event and resets adam`() {
        val trainer = trainer()
        var resets = 0
        trainer.events.iterationReset.on { resets++ }
        runBlocking { trainer.trainOnce() }
        assertTrue(trainer.model.adam.timestep > 0)
        trainer.iteration = 0
        assertEquals(0, trainer.model.adam.timestep)
        runBlocking { withTimeout(5_000) { while (resets == 0) delay(10) } }
        assertEquals(1, resets)
    }
}
