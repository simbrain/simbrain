package org.simbrain.world.odorworld.sensors

import org.simbrain.util.math.SimbrainMath
import org.simbrain.workspace.Producible
import org.simbrain.workspace.couplings.HIGH_PRIORITY
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * A sensor which is updated based on the presence of [SmellSource]s near it.
 */
class SmellSensor @JvmOverloads constructor(
    override val name: String = "Smell Sensor",
    theta: Double = 0.0,
    radius: Double = 0.0,
) : SensorWithRelativeLocation(theta, radius), VisualizableEntityAttribute {

    /**
     * The current vale of the smell sensors. A vector of smells obtained
     * by summing over scaled "distal" stimuli.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription", priority = HIGH_PRIORITY)
    var smellVector = DoubleArray(0)

    /**
     * Update the smell vector by iterating over entities and adding up their distance-scaled smell vectors.
     */
    override fun update(parent: OdorWorldEntity) {
        smellVector = parent.world.entityList
            .filter { it != parent } // Don't smell yourself
            .map { Pair(it.smellSource, SimbrainMath.distance(it.location, computeAbsoluteLocation(parent))) }
            .map { (smellSource, distance) -> smellSource.getStimulus(distance) }
            .fold(DoubleArray(smellVector.size) {0.0},  SimbrainMath::addVector)
    }

    /**
     * Returns a scalar value associated to the current smell vector.
     */
    @get:Producible(description = "Scalar smell")
    val currentScalarValue: Double
        get() = if (smellVector.size == 1) {
            smellVector[0]
        } else {
            // TODO: Provide other options for producing a scalar smell value from a vector, e.g. mean value or norm.
            smellVector.sum()
        }

    /**
     * Called by reflection to return a custom description for couplings.
     */
    val smellSensorDescription: String
        get() = "Smell sensor (" +
                SimbrainMath.roundDouble(theta, 2) + "," +
                SimbrainMath.roundDouble(radius, 2) + ")"

    override var label: String = directionString + smellSensorDescription

    override fun copy(): SmellSensor {
        return SmellSensor(name, theta, radius).applyCommonCopy().apply {
            this@SmellSensor.smellVector = smellVector
        }
    }
}