package org.simbrain.network.events

import org.simbrain.util.Events

/**
 * See [Events].
 */
class TrainerEvents: Events() {
    val beginTraining = NoArgEvent()
    val endTraining = NoArgEvent()
    val errorUpdated = AddedEvent<Double>()
    val progressUpdated = AddedEvent<Pair<String, Int>>()
}