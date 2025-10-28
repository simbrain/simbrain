package org.simbrain.network.events

import org.simbrain.util.Events

/**
 * Training statistics for a single training iteration.
 */
data class TrainingStats(
    val trainingError: Double,
    val testingError: Double? = null,
    val trainingAccuracy: Double? = null,
    val testingAccuracy: Double? = null
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