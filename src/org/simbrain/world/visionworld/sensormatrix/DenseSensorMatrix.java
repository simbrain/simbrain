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

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.ReceptiveField;
import org.simbrain.world.visionworld.Sensor;

/**
 * Dense sensor matrix.
 */
public final class DenseSensorMatrix
    extends AbstractSensorMatrix {

    /** 2D object matrix of sensors. */
    private final ObjectMatrix2D sensors;


    /**
     * Create a new dense sensor matrix with the specified filter.
     *
     * @param rows number of rows, must be <code>&gt;= 1</code>
     * @param columns number of columns, must be <code>&gt;= 1</code>
     * @param receptiveFieldWidth receptive field width, must be <code>&gt;= 0</code>
     * @param receptiveFieldHeight receptive field height, must be <code>&gt;= 0</code>
     * @param defaultFilter default filter
     */
    public DenseSensorMatrix(final int rows, final int columns,
                             final int receptiveFieldWidth, final int receptiveFieldHeight,
                             final Filter defaultFilter) {

        super(receptiveFieldWidth, receptiveFieldHeight, defaultFilter);
        if (rows < 1) {
            throw new IllegalArgumentException("rows must be >= 1");
        }
        if (columns < 1) {
            throw new IllegalArgumentException("columns must be >= 1");
        }
        sensors = new DenseObjectMatrix2D(rows, columns);
        createSensors();
    }


    /**
     * Create sensors.
     */
    private void createSensors() {
        Filter defaultFilter = getDefaultFilter();
        int receptiveFieldWidth = getReceptiveFieldWidth();
        int receptiveFieldHeight = getReceptiveFieldHeight();
        for (int row = 0, rows = rows(); row < rows; row++) {
            for (int column = 0, columns = columns(); column < columns; column++) {
                int x = column * receptiveFieldWidth;
                int y = row * receptiveFieldHeight;
                ReceptiveField receptiveField = new ReceptiveField(x, y, receptiveFieldWidth, receptiveFieldHeight);
                Sensor sensor;
                if (defaultFilter == null) {
                    sensor = new Sensor(row, column, receptiveField);
                } else {
                    sensor = new Sensor(row, column, defaultFilter, receptiveField);
                }
                sensors.set(row, column, sensor);
            }
        }
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
    public Sensor getSensor(final int row, final int column) {
        return (Sensor) sensors.get(row, column);
    }
}
