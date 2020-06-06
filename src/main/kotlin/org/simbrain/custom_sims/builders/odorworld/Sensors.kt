package org.simbrain.custom_sims.builders.odorworld

import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ObjectSensor
import kotlin.properties.Delegates

abstract class SensorBuilder {
    var entity by Delegates.notNull<OdorWorldEntity>()
}

class ObjectSensorBuilder(template: ObjectSensor.() -> Unit) : SensorBuilder() {
    val sensor by lazy { ObjectSensor(entity).apply(template) }
}