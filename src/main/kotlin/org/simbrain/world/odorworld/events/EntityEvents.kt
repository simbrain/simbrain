package org.simbrain.world.odorworld.events

import org.simbrain.util.Event
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.sensors.Sensor
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer


interface EntityLocationEvent {
    fun onMoved(handler: Runnable)
    fun fireMoved()
}

/**
 * See [Event].
 */
class EntityEvents : Event(PropertyChangeSupport(Any())), EntityLocationEvent {

    fun onDeleted(handler: Consumer<OdorWorldEntity>) = "Deleted".itemRemovedEvent(handler)
    fun fireDeleted(old: OdorWorldEntity) = "Deleted"(old)

    fun onUpdated(handler: Runnable) = "Update".event(handler)
    fun fireUpdated() = "Update"()

    override fun onMoved(handler: Runnable) = "Moved".event(handler)
    override fun fireMoved() = "Moved"()

    fun onTypeChanged(handler: BiConsumer<EntityType, EntityType>) = "TypeChanged".itemChangedEvent(handler)
    fun fireTypeChanged(old: EntityType, new: EntityType) = "TypeChanged"(old = old, new = new)

    fun onSensorAdded(handler: Consumer<Sensor>) = "SensorAdded".itemAddedEvent(handler)
    fun fireSensorAdded(sensor: Sensor) = "SensorAdded"(new = sensor)

    fun onSensorRemoved(handler: Consumer<Sensor>) = "SensorRemoved".itemRemovedEvent(handler)
    fun fireSensorRemoved(sensor: Sensor) = "SensorRemoved"(old = sensor)

    fun onPropertyChanged(handler: Runnable) = "PropertyChanged".event(handler)
    fun firePropertyChanged() = "PropertyChanged"()

    fun onEffectorAdded(handler: Consumer<Effector>) = "EffectorAdded".itemAddedEvent(handler)
    fun fireEffectorAdded(effector: Effector) = "EffectorAdded"(new = effector)

    fun onEffectorRemoved(handler: Consumer<Effector>) = "EffectorRemoved".itemRemovedEvent(handler)
    fun fireEffectorRemoved(effector: Effector) = "EffectorRemoved"(old = effector)

    /**
     * If using collisions isObjectBlocking must be true in OdorWorld.
     */
    fun onCollided(handler: Consumer<Bounded>) = "Collided".itemAddedEvent(handler)
    fun fireCollided(bound: Bounded) = "Collided"(new = bound)
}

/**
 * [Sensor] and [Effector] events.
 */
class SensorEffectorEvents(val attribute: PeripheralAttribute) : Event(PropertyChangeSupport(attribute)) {

    fun onUpdate(handler: Runnable) = "Update".event(handler)
    fun fireUpdated() = "Update"()

    fun onPropertyChange(handler: Runnable) = "PropertyChanged".event(handler)
    fun firePropertyChanged() = "PropertyChanged"()

}
