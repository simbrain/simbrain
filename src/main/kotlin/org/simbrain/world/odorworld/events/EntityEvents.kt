package org.simbrain.world.odorworld.events

import org.simbrain.util.Event
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.sensors.Sensor
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * See [Event].
 */
class EntityEvents(val entity: OdorWorldEntity):Event(PropertyChangeSupport(entity)) {

    fun onDeleted(handler: Consumer<OdorWorldEntity>) = "Deleted".itemRemovedEvent(handler)
    fun fireDeleted() = "Deleted"(old = entity)

    fun onUpdated(handler: Runnable) = "Update".event(handler)
    fun fireUpdated() = "Update"()

    fun onMoved(handler: Runnable) = "Moved".event(handler)
    fun fireMoved() = "Moved"()

    fun onTypeChanged(handler: BiConsumer<EntityType, EntityType>) = "TypeChanged".itemChangedEvent(handler)
    fun fireTypeChanged(old: EntityType, new: EntityType) = "TypeChanged"(old = old, new = new)

    fun onSensorAdded(handler: Consumer<Sensor>) = "SensorAdded".itemAddedEvent(handler)
    fun fireSensorAdded(sensor: Sensor) = "SensorAdded"(new = sensor)

    fun onSensorRemoved(handler: Consumer<Sensor>) = "SensorRemoved".itemRemovedEvent(handler)
    fun fireSensorRemoved(sensor: Sensor) = "SensorRemoved"(old = sensor)

    fun onUpdateSensorVisiblity(handler: Runnable) = "UpdateSensorVisiblity".event(handler)
    fun fireUpdateSensorVisiblity() = "UpdateSensorVisiblity"()

    fun onEffectorAdded(handler: Consumer<Effector>) = "EffectorAdded".itemAddedEvent(handler)
    fun fireEffectorAdded(effector: Effector) = "EffectorAdded"(new =effector)

    fun onEffectorRemoved(handler: Consumer<Effector>) = "EffectorRemoved".itemRemovedEvent(handler)
    fun fireEffectorRemoved(effector: Effector) = "EffectorRemoved"(old = effector)

}

/**
 * [Sensor] and [Effector] events.
 */
class AttributeEvents(val attribute: PeripheralAttribute):Event(PropertyChangeSupport(attribute)) {

    fun onUpdate(handler: Runnable) = "Update".event(handler)
    fun fireUpdate() = "Update"()

}
