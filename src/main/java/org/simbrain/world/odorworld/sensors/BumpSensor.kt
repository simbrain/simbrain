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

    override fun getLabel(): String {
        return if (super.getLabel().isEmpty()) {
            "$directionString Bump Sensor"
        } else {
            super.getLabel()
        }
    }
}