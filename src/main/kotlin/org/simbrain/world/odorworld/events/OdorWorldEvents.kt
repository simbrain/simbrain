package org.simbrain.world.odorworld.events

import org.simbrain.util.Event
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class OdorWorldEvents(val world: OdorWorld):Event(PropertyChangeSupport(world)) {

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

    fun onFrameAdvance(handler: Runnable) = "FrameAdvance".event(handler)
    fun fireFrameAdvance() = "FrameAdvance"()

    fun onWorldStarted(handler: Runnable) = "WorldStarted".event(handler)
    fun fireWorldStarted() = "WorldStarted"()

    fun onWorldStopped(handler: Runnable) = "WorldStopped".event(handler)
    fun fireWorldStopped() = "WorldStopped"()

    fun onAnimationStopped(handler: Runnable) = "AnimationStopped".event(handler)
    fun fireAnimationStopped() = "AnimationStopped"()

    fun onTileMapChanged(handler: Runnable) = "TileMapChanged".event(handler)
    fun fireTileMapChanged() = "TileMapChanged"()

    fun onEntityAdded(handler: Consumer<OdorWorldEntity>) = "EntityAdded".itemAddedEvent(handler)
    fun fireEntityAdded(entity: OdorWorldEntity) = "EntityAdded"(new = entity)

    fun onEntityRemoved(handler: Consumer<OdorWorldEntity>) = "EntityRemoved".itemRemovedEvent(handler)
    fun fireEntityRemoved(entity: OdorWorldEntity) = "EntityRemoved"(old = entity)

}