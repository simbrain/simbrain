package org.simbrain.network.events

import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.util.Events

/**
 * See [Events].
 */
class TrainerEvents: Events() {
    val beginTraining = NoArgEvent()
    val endTraining = NoArgEvent()
    val errorUpdated = OneArgEvent<SupervisedTrainer.AggregationFunction>()
    val progressUpdated = OneArgEvent<Pair<String, Int>>()
    val iterationReset = NoArgEvent()
}