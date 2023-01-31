package org.simbrain.network.events

import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class TrainerEvents2: Events2() {
    val beginTraining = NoArgEvent()
    val endTraining = NoArgEvent()
    val errorUpdated = AddedEvent<Double>()
    val progressUpdated = AddedEvent<Pair<String, Int>>()
}