package org.simbrain.network.events

import org.simbrain.util.FlowEvents

/**
 * Training statistics for a single training iteration.
 */
data class TrainingStats(
    val trainingError: Double,
    val testingError: Double? = null,
    val trainingAccuracy: Double? = null,
    val testingAccuracy: Double? = null,
    /**
     * RMS of the per-parameter update applied this iteration: sqrt(sum(update^2) / numParams).
     * Roughly the average magnitude of step the optimizer is taking per parameter — useful for
     * spotting flat-lines (≈ 0) and divergence (very large), and for comparing optimizers.
     */
    val effectiveStepSize: Double? = null
)

/**
 * See [FlowEvents].
 */
class TrainerEvents: FlowEvents() {
    val beginTraining = NoArgAwaitableEvent()
    val endTraining = NoArgEvent()
    val errorUpdated = AwaitableEvent<TrainingStats>()
    val progressUpdated = AwaitableEvent<Pair<String, Int>>()
    val iterationReset = NoArgEvent()
}