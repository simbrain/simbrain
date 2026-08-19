package org.simbrain.network.trainers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.events.TrainingStats
import org.simbrain.network.llm.TinyLmModel

/**
 * Trainer for tape-backed op-plan models ([TinyLmModel]): the same outer loop the
 * classic [SupervisedTrainer] runs — serialized task queue, iteration counting, stopping
 * conditions, test cadence, [TrainerEvents] feeding the same plots and readouts — but the
 * per-iteration body is one epoch of tape train steps over token windows instead of Smile
 * backprop over flattened one-hot rows.
 */
class TapeTrainer(val model: TinyLmModel) : CoroutineScope {

    val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Default + job

    val events = TrainerEvents()

    /** Token windows: input token ids paired with next-token target ids (-1 = unsupervised). */
    var trainingWindows: List<Pair<IntArray, IntArray>> = emptyList()

    var testingWindows: List<Pair<IntArray, IntArray>> = emptyList()

    val stoppingCondition = SupervisedTrainer.StoppingCondition()

    var testFrequency = 10

    var computeAccuracy = true

    var learningRate: Float
        get() = model.adam.learningRate
        set(value) {
            model.adam.learningRate = value
        }

    var iteration = 0
        set(value) {
            field = value
            if (value == 0) {
                events.iterationReset.fire()
                model.adam.reset()
            }
        }

    var lastTrainingError = 0.0
        private set

    var lastTrainingAccuracy: Double? = null
        private set

    var lastTestingAccuracy: Double? = null
        private set

    @Volatile
    var isRunning = false
        private set

    private val processorChannel = Channel<Pair<Task, CompletableDeferred<Unit>>>(capacity = Channel.UNLIMITED)

    private enum class Task { Start, Train, Stop }

    init {
        launch {
            for ((task, signal) in processorChannel) {
                try {
                    when (task) {
                        Task.Start -> startHandler()
                        Task.Train -> trainOnceHandler()
                        Task.Stop -> stopHandler()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                    synchronized(model) { isRunning = false }
                    events.endTraining.fire()
                } finally {
                    signal.complete(Unit)
                }
            }
        }
    }

    private suspend fun submitTask(task: Task): CompletableDeferred<Unit> {
        val signal = CompletableDeferred<Unit>()
        processorChannel.send(task to signal)
        return signal
    }

    suspend fun startTraining() {
        submitTask(Task.Start).await()
    }

    suspend fun stopTraining() {
        submitTask(Task.Stop).await()
    }

    suspend fun trainOnce() {
        submitTask(Task.Train).await()
    }

    private suspend fun startHandler() {
        synchronized(model) {
            if (model.midWalk) return
            stoppingCondition.resetEarlyStopping()
            isRunning = true
        }
        events.beginTraining.fire()
        submitTask(Task.Train)
    }

    private fun stopHandler() {
        synchronized(model) { isRunning = false }
        events.endTraining.fire()
    }

    private suspend fun trainOnceHandler() {
        if (synchronized(model) { model.midWalk }) {
            stopHandler()
            return
        }
        if (trainingWindows.isEmpty()) {
            stopHandler()
            return
        }
        iteration++

        var totalLoss = 0.0
        var accuracySum = 0.0
        for ((tokens, targets) in trainingWindows) {
            val walkStarted = synchronized(model) {
                if (model.midWalk) true
                else {
                    totalLoss += model.trainStep(tokens, targets)
                    if (computeAccuracy) accuracySum += accuracyOf(targets)
                    false
                }
            }
            if (walkStarted) {
                stopHandler()
                return
            }
        }
        lastTrainingError = totalLoss / trainingWindows.size
        lastTrainingAccuracy = if (computeAccuracy) accuracySum / trainingWindows.size else null

        val shouldTest = testingWindows.isNotEmpty() && iteration % testFrequency == 0
        var testError: Double? = null
        var testAccuracy: Double? = null
        if (shouldTest) {
            var testLoss = 0.0
            var testAccuracySum = 0.0
            for ((tokens, targets) in testingWindows) {
                val walkStarted = synchronized(model) {
                    if (model.midWalk) true
                    else {
                        model.setSample(tokens, targets)
                        testLoss += model.forward()
                        if (computeAccuracy) testAccuracySum += accuracyOf(targets)
                        false
                    }
                }
                if (walkStarted) {
                    stopHandler()
                    return
                }
            }
            testError = testLoss / testingWindows.size
            if (computeAccuracy) {
                testAccuracy = testAccuracySum / testingWindows.size
                lastTestingAccuracy = testAccuracy
            }
        }

        events.errorUpdated.fire(TrainingStats(
            trainingError = lastTrainingError,
            testingError = testError,
            trainingAccuracy = lastTrainingAccuracy,
            testingAccuracy = testAccuracy,
            effectiveStepSize = model.adam.effectiveStepSize,
        ))

        if (isRunning) {
            if (stoppingCondition.validate(iteration, lastTrainingError, testError)) {
                submitTask(Task.Stop)
            } else {
                submitTask(Task.Train)
            }
        }
    }

    /** Fraction of supervised positions whose argmax prediction matches the target. */
    private fun accuracyOf(targets: IntArray): Double {
        val probs = model.probs.tensor
        var correct = 0
        var supervised = 0
        for (position in targets.indices) {
            val target = targets[position]
            if (target < 0) continue
            supervised++
            var best = 0
            for (c in 1 until probs.cols) {
                if (probs[position, c] > probs[position, best]) best = c
            }
            if (best == target) correct++
        }
        return if (supervised > 0) correct.toDouble() / supervised else 0.0
    }
}
