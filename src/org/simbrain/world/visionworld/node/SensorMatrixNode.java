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
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.VisionWorld;

/**
 * Sensor matrix node.
 */
public final class SensorMatrixNode extends AbstractSensorMatrixNode {

    /**
     * Create a new sensor matrix node with the specified sensor matrix.
     *
     * @param visionWorld vision world for this sensor matrix node, must not be
     *            null
     * @param sensorMatrix sensor matrix for this sensor matrix node, must not
     *            be null
     */
    public SensorMatrixNode(final VisionWorld visionWorld,
            final SensorMatrix sensorMatrix) {
        super(sensorMatrix);

        // for each sensor in the sensor matrix, create and add a sensor node
        for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
            for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                Sensor sensor = sensorMatrix.getSensor(row, column);
                if (sensor != null) {
                    SensorNode node = new SensorNode(visionWorld, sensor);
                    node.offset(sensor.getReceptiveField().getX(), sensor
                            .getReceptiveField().getY());
                    addChild(node);
                }
            }
        }
    }
}
