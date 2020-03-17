package org.simbrain.world.threedworld.events

import org.simbrain.util.Event
import org.simbrain.world.threedworld.ThreeDWorld
import org.simbrain.world.threedworld.entities.Agent
import org.simbrain.world.threedworld.entities.Entity
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class ThreeDWorldEvents(val world: ThreeDWorld):Event(PropertyChangeSupport(world)) {

    fun onInitialized(handler: Runnable) = "Initialized".event(handler)
    fun fireInitialized() = "Initialized"()

    fun onClosed(handler: Runnable) = "Closed".event(handler)
    fun fireClosed() = "Closed"()

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

    fun onAgentAdded(handler: Consumer<Agent>) = "AgentAdded".itemAddedEvent(handler)
    fun fireAgentAdded(entity: Agent) = "AgentAdded"(new = entity)

    fun onAgentRemoved(handler: Consumer<Agent>) = "AgentRemoved".itemRemovedEvent(handler)
    fun fireAgentRemoved(entity: Agent) = "AgentRemoved"(old = entity)

}