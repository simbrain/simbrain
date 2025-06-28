package org.simbrain.world.odorworld.sensors

import org.simbrain.world.odorworld.entities.Bound
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.intersect

/**
 * Very simple bump sensor. Holding off on more sophisticated "touch" sensors in
 * case an existing library can provide it.
 *
 *
 * TODO: Implement once collisions are implemented. At that point can rename to
 * collision sensor? Can also give the sensor a location and make it visible.
 */
class BumpSensor(theta: Double = DEFAULT_THETA, radius: Double = DEFAULT_RADIUS) : SensorWithRelativeLocation(theta, radius), VisualizableEntityAttribute {

    /**
     * The length of the sides of the square sensor shape
     */
    val sensorSize = 5

    override fun update(parent: OdorWorldEntity) {
        currentValue = 0.0
        val bound = Bound(
            parent.x - sensorSize / 2.0,
            parent.y - sensorSize / 2.0,
            parent.width + sensorSize,
            parent.height + sensorSize
        )
        val collided = parent.world.collidableObjects
            .stream()
            .filter { it: Bounded -> it !== parent }
            .anyMatch { it: Bounded? -> bound.intersect(it!!).intersect }
        if (collided) {
            currentValue = baseValue
        }
    }

    override fun copy(): BumpSensor {
        return BumpSensor(theta, radius).applyCommonCopy()
    }

    override val name: String
        get() = "Bump Sensor"



    override var label: String = if (super.label.isEmpty()) {
            "$directionString Bump Sensor"
        } else {
            super.label
        }
}