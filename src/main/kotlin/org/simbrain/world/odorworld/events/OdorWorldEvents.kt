package org.simbrain.world.odorworld.events

import org.simbrain.util.FlowEvents
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * See [FlowEvents].
 */
class OdorWorldEvents: FlowEvents() {
    val updated = NoArgAwaitableEvent()
    val frameAdvanced = NoArgEvent()
    val worldStarted = NoArgEvent()
    val worldStopped = NoArgEvent()
    val animationStopped = NoArgEvent()
    val entityAdded = AwaitableEvent<OdorWorldEntity>()
    val entityRemoved = OneArgEvent<OdorWorldEntity>()
    val tileMapChanged = NoArgEvent()
    val cleanups = HashMap<OdorWorldEntity, () -> Unit>()
}