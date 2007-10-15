/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld.node;

import org.simbrain.world.visionworld.Sensor;
import org.simbrain.world.visionworld.VisionWorld;

/**
 * Sensor node.
 */
public final class SensorNode
    extends AbstractSensorNode {

    /**
     * Create a new sensor node for the specified sensor.
     *
     * @param visionWorld vision world, must not be null
     * @param sensor sensor for this sensor node, must not be null
     */
    public SensorNode(final VisionWorld visionWorld, final Sensor sensor) {
        super(visionWorld, sensor);
    }
}
