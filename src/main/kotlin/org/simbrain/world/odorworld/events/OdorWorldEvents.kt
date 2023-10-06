package org.simbrain.world.odorworld.events

import org.simbrain.util.Events
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * See [Events].
 */
class OdorWorldEvents: Events() {
    val updated = NoArgEvent()
    val frameAdvanced = NoArgEvent()
    val worldStarted = NoArgEvent()
    val worldStopped = NoArgEvent()
    val animationStopped = NoArgEvent()
    val entityAdded = AddedEvent<OdorWorldEntity>()
    val entityRemoved = RemovedEvent<OdorWorldEntity>()
    val tileMapChanged = NoArgEvent()
}