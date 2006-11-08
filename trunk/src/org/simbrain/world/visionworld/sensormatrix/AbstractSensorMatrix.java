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
package org.simbrain.world.visionworld.sensormatrix;

import org.simbrain.world.visionworld.Sensor;
import org.simbrain.world.visionworld.SensorMatrix;

/**
 * Abstract sensor matrix.
 */
abstract class AbstractSensorMatrix
    implements SensorMatrix {

    /** Receptive field height. */
    private final double receptiveFieldHeight;

    /** Receptive field width. */
    private final double receptiveFieldWidth;


    /**
     * Create a new abstract sensor matrix with the specified receptive field width and height.
     *
     * @param receptiveFieldWidth receptive field width
     * @param receptiveFieldHeight receptive field height
     */
    protected AbstractSensorMatrix(final double receptiveFieldWidth, final double receptiveFieldHeight) {
        this.receptiveFieldHeight = receptiveFieldHeight;
        this.receptiveFieldWidth = receptiveFieldWidth;
    }


    /** {@inheritDoc} */
    public final double getReceptiveFieldHeight() {
        return receptiveFieldHeight;
    }

    /** {@inheritDoc} */
    public final double getReceptiveFieldWidth() {
        return receptiveFieldWidth;
    }

    /** {@inheritDoc} */
    public void set(final int row, final int column, final Sensor sensor) {
        throw new UnsupportedOperationException("set operation not supported by this sensor matrix");
    }
}
