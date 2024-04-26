package org.simbrain.world.threedworld.events

import org.simbrain.util.Events
import org.simbrain.world.threedworld.entities.Effector
import org.simbrain.world.threedworld.entities.Entity
import org.simbrain.world.threedworld.entities.Sensor

/**
 * See [Events].
 */
class EntityEvents: Events() {
    val entityDeleted = OneArgEvent<Entity>()
    val entityAdded = OneArgEvent<Entity>()
    val sensorDeleted = OneArgEvent<Sensor>()
    val sensorAdded = OneArgEvent<Sensor>()
    val effectorDeleted = OneArgEvent<Effector>()
    val effectorAdded = OneArgEvent<Effector>()
}
