package org.simbrain.world.threedworld.events

import org.simbrain.util.Events2
import org.simbrain.world.threedworld.entities.Agent

/**
 * See [Events2].
 */
class ThreeDWorldEvents2: Events2() {
    val initialized = NoArgEvent()
    val closed = NoArgEvent()
    val updated = NoArgEvent()
    val agentAdded = AddedEvent<Agent>()
    val agentRemoved = RemovedEvent<Agent>()
}