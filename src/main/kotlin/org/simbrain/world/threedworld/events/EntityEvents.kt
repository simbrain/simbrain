package org.simbrain.world.threedworld.events

import org.simbrain.util.Events
import org.simbrain.world.threedworld.entities.Effector
import org.simbrain.world.threedworld.entities.Entity
import org.simbrain.world.threedworld.entities.Sensor

/**
 * See [Events].
 */
class EntityEvents: Events() {
    val entityDeleted = RemovedEvent<Entity>()
    val entityAdded = AddedEvent<Entity>()
    val sensorDeleted = RemovedEvent<Sensor>()
    val sensorAdded = AddedEvent<Sensor>()
    val effectorDeleted = RemovedEvent<Effector>()
    val effectorAdded = AddedEvent<Effector>()
}
