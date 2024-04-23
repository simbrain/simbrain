package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.util.plus
import org.simbrain.workspace.Producible
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.geom.Point2D
import kotlin.math.cos
import kotlin.math.sin

/**
 * Angle of sensor in radians.
 */
const val DEFAULT_THETA = 0.0

/**
 * Initial length of mouse whisker.
 */
const val DEFAULT_RADIUS = 0.0

abstract class SensorWithRelativeLocation(
    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(
        label = "Sensor angle", description = "The angle theta (in polar coordinates, with radius) at " +
                "which the sensor will be added.", order = 3
    )
    var theta: Double = DEFAULT_THETA,

    /**
     * Relative location of the sensor in polar coordinates.
     */
    @UserParameter(
        label = "Sensor length",
        description = "The distance in pixels from the center of the entity to which the sensor is to be added.",
        order = 4
    )
    var radius: Double = DEFAULT_RADIUS
) : Sensor() {

    /**
     * Current value of the sensor.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription")
    var currentValue = 0.0
        protected set

    @UserParameter(
        description = "Maximum value of the sensor when agent is right on top of the associated object type",
        label = "Max Value",
        order = 10
    )
    var baseValue = 1.0
        protected set

    /**
     * Should the sensor node show a label on top.
     */
    @UserParameter(label = "Show Label", description = "Show label on top of the sensor node", order = 5)
    var isShowLabel = false
        set(value) {
            field = value
            events.propertyChanged.fire()
        }

    /**
     * Returns the sensor location in the local coordinate frame of the entity.
     * The entity containing the sensor should be passed in.
     */
    fun computeRelativeLocation(entity: OdorWorldEntity): Point2D {
        val sensorLocation = Point2D.Double(0.0, 0.0)
        sensorLocation.x = radius * cos(Math.toRadians(entity.heading + theta))
        sensorLocation.y = -radius * sin(Math.toRadians(entity.heading + theta))
        return sensorLocation
    }

    /**
     * Returns the sensor location in the world's coordinate frame.
     */
    fun computeAbsoluteLocation(entity: OdorWorldEntity): Point2D {
        return entity.location + computeRelativeLocation(entity)
    }

    fun <T: SensorWithRelativeLocation> T.applyCommonCopy(): T {
        return apply {
            id = this@SensorWithRelativeLocation.id
            baseValue = this@SensorWithRelativeLocation.baseValue
            isShowLabel = this@SensorWithRelativeLocation.isShowLabel
            theta = this@SensorWithRelativeLocation.theta
            radius = this@SensorWithRelativeLocation.radius
        }
    }

    /**
     * Return String direction (left / right) based on angle of the sensor
     */
    val directionString: String
        get() {
            return if (theta < 0 && theta > -45) {
                "Right "
            } else if (theta > 0 && theta < 45) {
                "Left "
            } else {
                ""
            }
            // TODO: Maybe add front, back, left-back and right-back
            // With length = 0 can also have center
        }


}