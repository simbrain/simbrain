/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld.sensors

import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.sum
import org.simbrain.workspace.Producible
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
    @get:Producible(customDescriptionMethod = "getAttributeDescription")
    @Transient
    var smellVector = DoubleArray(0)

    /**
     * Update the smell vector by iterating over entities and adding up their distance-scaled smell vectors.
     */
    override fun update(parent: OdorWorldEntity) {
        smellVector = parent.world.entityList
            .filter { it != parent } // Don't smell yourself
            .map { Pair(it.smellSource, SimbrainMath.distance(it.location, computeAbsoluteLocation(parent))) }
            .map { (smellSource, distance) -> smellSource.getStimulus(distance) }
            .sum()
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

    override fun getLabel(): String {
        // TODO: Add labbel.ismpety check. Removed it because label is set by AddSensorDialog.
        //  So for now custom labels not possible on this sensor
        return directionString + smellSensorDescription
    }

    override fun copy(): SmellSensor {
        return SmellSensor(name, theta, radius).applyCommonCopy().apply {
            this@SmellSensor.smellVector = smellVector
        }
    }
}