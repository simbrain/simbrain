package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.math.SimbrainMath
import org.simbrain.world.odorworld.entities.EntityType
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
class ObjectSensor @JvmOverloads constructor(
    @UserParameter(label = "Object Type", description = "What type of object this sensor responds to", order = 3)
    private var objectType: EntityType = EntityType.SWISS,
    radius: Double = DEFAULT_RADIUS,
    theta: Double = DEFAULT_THETA
) : SensorWithRelativeLocation(theta, radius), VisualizableEntityAttribute, WithDispersion {

    /**
     * Decay function
     */
    @UserParameter(label = "Decay Function", isObjectType = true, showDetails = false, order = 15)
    override var decayFunction: DecayFunction = LinearDecayFunction(70.0)

    override var showDispersion = false

    override fun update(parent: OdorWorldEntity) {
        currentValue = 0.0
        val sensorLocation = computeAbsoluteLocation(parent)
        for (otherEntity in parent.world.entityList) {
            if (otherEntity.entityType == objectType) {
                val scaleFactor = decayFunction.getScalingFactor(
                    SimbrainMath.distance(sensorLocation, otherEntity.location)
                )
                currentValue += baseValue * scaleFactor
            }
        }
    }

    override fun copy(): ObjectSensor {
        return ObjectSensor(objectType, radius, theta).applyCommonCopy().apply {
            objectType = this@ObjectSensor.objectType
            decayFunction = this@ObjectSensor.decayFunction.copy() as DecayFunction
        }
    }

    override val name: String
        get() = "Object Sensor"

    fun setObjectType(objectType: EntityType) {
        this.objectType = objectType
    }

    override fun getLabel(): String {
        return if (super.getLabel().isEmpty()) {
            directionString + objectType.description + " Detector"
        } else {
            super.getLabel()
        }
    }
}