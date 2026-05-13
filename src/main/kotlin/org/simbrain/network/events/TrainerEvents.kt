package org.simbrain.network.events

import org.simbrain.util.Events

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
 * See [Events].
 */
class TrainerEvents: Events() {
    val beginTraining = NoArgEvent()
    val endTraining = NoArgEvent()
    val errorUpdated = OneArgEvent<TrainingStats>()
    val progressUpdated = OneArgEvent<Pair<String, Int>>()
    val iterationReset = NoArgEvent()
}