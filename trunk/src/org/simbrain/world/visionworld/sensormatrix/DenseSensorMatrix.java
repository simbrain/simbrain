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

import cern.colt.matrix.ObjectMatrix2D;

import cern.colt.matrix.impl.DenseObjectMatrix2D;

import org.simbrain.world.visionworld.Sensor;

/**
 * Dense sensor matrix.
 */
public final class DenseSensorMatrix
    extends AbstractSensorMatrix {

    /** 2D object matrix of sensors. */
    private final ObjectMatrix2D sensors;


    /**
     * Create a new dense sensor matrix.
     *
     * @param rows number of rows
     * @param columns number of columns
     * @param receptiveFieldWidth receptive field width
     * @param receptiveFieldHeight receptive field height
     */
    public DenseSensorMatrix(final int rows, final int columns,
                             final double receptiveFieldWidth, final double receptiveFieldHeight) {
        super(receptiveFieldWidth, receptiveFieldHeight);
        sensors = new DenseObjectMatrix2D(rows, columns);
    }


    /** {@inheritDoc} */
    public int rows() {
        return sensors.rows();
    }

    /** {@inheritDoc} */
    public int columns() {
        return sensors.columns();
    }

    /** {@inheritDoc} */
    public Sensor get(final int row, final int column) {
        return (Sensor) sensors.get(row, column);
    }

    /** {@inheritDoc} */
    public void set(final int row, final int column, final Sensor sensor) {
        sensors.set(row, column, sensor);
    }
}
