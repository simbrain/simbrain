package org.simbrain.world.imageworld.events

import org.simbrain.util.Event
import org.simbrain.world.imageworld.ImageWorld
import org.simbrain.world.imageworld.SensorMatrix
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class ImageWorldEvents(val world : ImageWorld) : Event(PropertyChangeSupport(world)) {

    fun onSensorMatrixAdded(handler: Consumer<SensorMatrix>) = "SensorMatrixAdded".itemAddedEvent(handler)
    fun fireSensorMatrixAdded(sensorMatrix: SensorMatrix) = "SensorMatrixAdded"(new = sensorMatrix)

    fun onSensorMatrixRemoved(handler: Consumer<SensorMatrix>) =
        "SensorMatrixRemoved".itemRemovedEvent(handler)
    fun fireSensorMatrixRemoved(sensorMatrix: SensorMatrix) = "SensorMatrixRemoved"(old = sensorMatrix)

}