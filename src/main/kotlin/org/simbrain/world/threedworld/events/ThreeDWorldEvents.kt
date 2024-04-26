package org.simbrain.world.threedworld.events

import org.simbrain.util.Events
import org.simbrain.world.threedworld.entities.Agent

/**
 * See [Events].
 */
class ThreeDWorldEvents: Events() {
    val initialized = NoArgEvent()
    val closed = NoArgEvent()
    val updated = NoArgEvent()
    val agentAdded = OneArgEvent<Agent>()
    val agentRemoved = OneArgEvent<Agent>()
}