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
     * The number of rows will be at least one.
     *
     * @return the number of rows of this sensor matrix
     */
    int rows();

    /**
     * Return the number of columns of this sensor matrix.
     * The number of columns will be at least one.
     *
     * @return the number of columns of this sensor matrix
     */
    int columns();

    /**
     * Return the receptive field height for sensors in this sensor matrix.
     * The receptive field height will be at least zero.
     *
     * @return the receptive field height for sensors in this sensor matrix
     */
    int getReceptiveFieldHeight();

    /**
     * Return the receptive field width for sensors in this sensor matrix.
     * The receptive field width will be at least zero.
     *
     * @return the receptive field width for sensors in this sensor matrix
     */
    int getReceptiveFieldWidth();

    /**
     * Return the default filter for the sensors in this sensor matrix, if any.
     *
     * @return the default filter for the sensors in this sensor matrix or
     *    <code>null</code> if one has not been defined
     */
    Filter getDefaultFilter();

    /**
     * Return the sensor in this sensor matrix at the specified row and column, if any.
     *
     * @param row row, must be <code>&gt;= 0</code> and <code>&lt; rows()</code>
     * @param column column, must be <code>&gt;= 0</code> and <code>&lt; columns()</code>
     * @return the sensor in this sensor matrix at the specified row and column, if any
     * @throws IndexOutOfBoundsException if <code>row</code> is less than zero or
     *    greater than or equal to <code>rows()</code>, or if <code>column</code> is
     *    less than zero or greater than or equal to <code>columns()</code>
     */
    Sensor getSensor(int row, int column);
}
