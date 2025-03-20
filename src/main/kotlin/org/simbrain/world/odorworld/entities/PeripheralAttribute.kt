package org.simbrain.world.odorworld.entities

import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.world.odorworld.events.SensorEffectorEvents

/**
 * Interface for effectors and sensors. "Peripheral" is supposed to suggest
 * the peripheral nervous system, which encompasses sensory and motor neurons.
 * It's the best I could come up with... :/
 *
 * @author Jeff Yoshimi
 */
interface PeripheralAttribute : AttributeContainer, CopyableObject {
    var label: String

    fun update(parent: OdorWorldEntity)

    val events: SensorEffectorEvents

    val attributeDescription: String get() = id + ":" + this.label
}