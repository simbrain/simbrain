package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.piccolo.getTileStackNear
import org.simbrain.util.piccolo.toPixelCoordinate
import org.simbrain.workspace.Producible
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * Sensor that reacts when an object of a given type is near it.
 * <br></br>
 * While the smell framework involves objects emitting smells, object type
 * sensors have a sensitivity, and are more "sensor" or "subject" based than
 * object based.
 * <br></br>
 * The sensor itself is currently fixed at the center of the agent. We may
 * make the location editable at some point, if use-cases emerge.
 */
class TileSensor @JvmOverloads constructor(
    @UserParameter(label = "Object Type", description = "What type of object this sensor responds to", order = 3)
    private var tileType: String = "water",
    radius: Double = DEFAULT_RADIUS,
    angle: Double = DEFAULT_THETA
) : Sensor(radius, angle), VisualizableEntityAttribute {
    /**
     * Current value of the sensor.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription")
    var currentValue = 0.0
        private set

    @UserParameter(
        description = "Maximum value of the sensor when agent is right on top of the associated object type",
        label = "Max Value",
        order = 10
    )
    var baseValue = 1.0
        private set

    /**
     * Decay function
     */
    @UserParameter(label = "Decay Function", isObjectType = true, showDetails = false, order = 15)
    var decayFunction: DecayFunction = LinearDecayFunction(70.0)

    @UserParameter(
        label = "Show dispersion",
        description = "Show dispersion of the sensor",
        useSetter = true,
        order = 4
    )
    @get:JvmName("isShowDispersion")
    var showDispersion = false

    /**
     * Should the sensor node show a label on top.
     */
    @UserParameter(label = "Show Label", description = "Show label on top of the sensor node", order = 5)
    var isShowLabel = false
        private set(value) {
            field = value
            events.firePropertyChanged()
        }

    override fun update(parent: OdorWorldEntity) {
        currentValue = 0.0
        val sensorLocation = computeAbsoluteLocation(parent)
        currentValue = with(parent.world.tileMap) {
            getTileStackNear(sensorLocation, decayFunction.dispersion)
                .filter { (_, tiles) -> tiles.any { it.type == tileType } }
                .map { (pos) -> pos.toPixelCoordinate().distance(sensorLocation) }
                .sumOf { decayFunction.getScalingFactor(it) * baseValue }
        }
    }

    override fun copy(): TileSensor {
        return TileSensor().apply {
            setId(this@TileSensor.id)
            baseValue = this@TileSensor.baseValue
            decayFunction = this@TileSensor.decayFunction.copy() as DecayFunction
            tileType = this@TileSensor.tileType
            isShowLabel = this@TileSensor.isShowLabel
        }
    }

    override val name: String
        get() = "Tile Sensor"

    override fun getLabel(): String {
        return if (super.getLabel().isEmpty()) {
            "$directionString$tileType Detector"
        } else {
            super.getLabel()
        }
    }
}