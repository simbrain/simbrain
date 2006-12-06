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
package org.simbrain.world.visionworld;

/**
 * Two-dimensional matrix of sensors with uniformly sized receptive fields.
 */
public interface SensorMatrix {

    /**
     * Return the number of rows of this sensor matrix.
     *
     * @return the number of rows of this sensor matrix
     */
    int rows();

    /**
     * Return the number of columns of this sensor matrix.
     *
     * @return the number of columns of this sensor matrix
     */
    int columns();

    /**
     * Return the receptive field height for sensors in this sensor matrix.
     *
     * @return the receptive field height for sensors in this sensor matrix
     */
    double getReceptiveFieldHeight();

    /**
     * Return the receptive field width for sensors in this sensor matrix.
     *
     * @return the receptive field width for sensors in this sensor matrix
     */
    double getReceptiveFieldWidth();

    /**
     * Return the sensor in this sensor matrix at the specified row and column, if any.
     *
     * @param row row
     * @param column column
     * @return the sensor in this sensor matrix at the specified row and column, if any
     */
    Sensor get(int row, int column);

    /**
     * Set the sensor in this sensor matrix at the specified row and column to
     * <code>sensor</code> (optional operation).
     *
     * @param row row
     * @param column column
     * @param sensor sensor, must not be null
     * @throws UnsupportedOperationException if the set operation is not supported by
     *    this sensor matrix
     */
    void set(int row, int column, Sensor sensor);
}
