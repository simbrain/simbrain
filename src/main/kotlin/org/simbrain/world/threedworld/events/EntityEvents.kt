package org.simbrain.world.threedworld.events

import org.simbrain.util.Event
import org.simbrain.world.threedworld.entities.Effector
import org.simbrain.world.threedworld.entities.Entity
import org.simbrain.world.threedworld.entities.Sensor
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class EntityEvents(val entity: Entity):Event(PropertyChangeSupport(entity)) {

    fun onDeleted(handler: Consumer<Entity>) = "Deleted".itemRemovedEvent(handler)
    fun fireDeleted() = "Deleted"(old = entity)

    fun onSensorAdded(handler: Consumer<Sensor>) = "SensorAdded".itemAddedEvent(handler)
    fun fireSensorAdded(sensor: Sensor) = "SensorAdded"(new = sensor)

    fun onSensorRemoved(handler: Consumer<Sensor>) = "SensorRemoved".itemRemovedEvent(handler)
    fun fireSensorRemoved(sensor: Sensor) = "SensorRemoved"(old = sensor)

    fun onEffectorAdded(handler: Consumer<Effector>) = "EffectorAdded".itemAddedEvent(handler)
    fun fireEffectorAdded(effector: Effector) = "EffectorAdded"(new =effector)

    fun onEffectorRemoved(handler: Consumer<Effector>) = "EffectorRemoved".itemRemovedEvent(handler)
    fun fireEffectorRemoved(effector: Effector) = "EffectorRemoved"(old = effector)

}
