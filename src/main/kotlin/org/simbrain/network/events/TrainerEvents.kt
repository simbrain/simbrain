package org.simbrain.network.events

import org.simbrain.network.trainers.IterableTrainer
import org.simbrain.util.Events

/**
 * See [Events].
 */
class TrainerEvents: Events() {
    val beginTraining = NoArgEvent()
    val endTraining = NoArgEvent()
    val errorUpdated = AddedEvent<IterableTrainer.LossFunction>()
    val progressUpdated = AddedEvent<Pair<String, Int>>()
    val iterationReset = NoArgEvent()
}