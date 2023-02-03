package org.simbrain.world.odorworld.events

import org.simbrain.util.Events2
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.Sensor


interface EntityLocationEvent {
    val moved: Events2.NoArgEvent
}

/**
 * See [Events2].
 */
class EntityEvents2: Events2(), EntityLocationEvent {
    val updated = NoArgEvent()
    val typeChanged = ChangedEvent<EntityType>()
    val deleted = RemovedEvent<OdorWorldEntity>()
    val sensorAdded = AddedEvent<Sensor>()
    val sensorRemoved = RemovedEvent<Sensor>()
    val effectorAdded = AddedEvent<Effector>()
    val effectorRemoved = RemovedEvent<Effector>()
    val propertyChanged = NoArgEvent()
    val collided = AddedEvent<Bounded>()
    override val moved = NoArgEvent()

}
/**
 * See [Events2]
 */
class SensorEffectorEvents2: Events2() {
    val updated = NoArgEvent()
    val propertyChanged = NoArgEvent()
}
