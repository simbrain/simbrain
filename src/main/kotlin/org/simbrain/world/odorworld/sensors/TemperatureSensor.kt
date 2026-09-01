/**
 * Scalar temperature sensing for odor world entities. OdorWorld has no world-level scalar fields, so the
 * thermal environment is owned by the sensor as a [ThermalGradient] parameter object, following the
 * pattern of [TileSensor] holding its decay function. Simulations that render or reverse the gradient
 * should share the sensor's [ThermalGradient] instance so there is a single source of truth.
 */
package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.geom.Point2D

/**
 * A linear temperature gradient along the world's x axis. Temperature at the left/right edges is the
 * center temperature minus/plus half the span (swapped when [direction] is negative), plus a uniform
 * [offset] for warming or cooling the whole plate.
 */
class ThermalGradient(
    @UserParameter(
        label = "Center temperature",
        description = "Temperature at the horizontal center of the world.",
        increment = .5,
        order = 1
    )
    var centerTemperature: Double = 17.0,

    @UserParameter(
        label = "Span",
        description = "Temperature difference between the left and right edges of the world.",
        increment = .5,
        order = 2
    )
    var spanDegrees: Double = 6.0,

    @UserParameter(
        label = "Direction",
        description = "1 for warm on the right, -1 for warm on the left.",
        order = 3
    )
    var direction: Double = 1.0,

    @UserParameter(
        label = "Offset",
        description = "Uniform temperature added across the whole plate.",
        increment = .5,
        order = 4
    )
    var offset: Double = 0.0
) : EditableObject {

    fun temperatureAt(location: Point2D, world: OdorWorld): Double =
        centerTemperature + offset + direction * (spanDegrees / 2.0) * (2.0 * location.x / world.width - 1.0)

    fun copy(): ThermalGradient = ThermalGradient(centerTemperature, spanDegrees, direction, offset)

    override val name: String
        get() = "Thermal gradient"
}

class TemperatureSensor @JvmOverloads constructor(
    radius: Double = DEFAULT_RADIUS,
    angle: Double = DEFAULT_THETA
) : SensorWithRelativeLocation(angle, radius) {

    @UserParameter(label = "Thermal gradient", showDetails = false, order = 15)
    var gradient: ThermalGradient = ThermalGradient()

    override fun update(parent: OdorWorldEntity) {
        currentValue = gradient.temperatureAt(computeAbsoluteLocation(parent), parent.world)
    }

    override fun copy(): TemperatureSensor {
        return TemperatureSensor().applyCommonCopy().apply {
            gradient = this@TemperatureSensor.gradient.copy()
        }
    }

    override val name: String
        get() = "Temperature Sensor"

    override var label = if (super.label.isEmpty()) {
        "${directionString}Temperature Sensor"
    } else {
        super.label
    }
}
