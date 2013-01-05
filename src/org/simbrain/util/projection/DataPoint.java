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
package org.simbrain.util.projection;


/**
 * <b>Datapoint</b> represents a single datapoint.
 */
public class DataPoint {

    /** The main data vector. */
    private double[] data;

    /**
     * Initialize a datapoint.
     *
     * @param data the data to set this data point
     */
    public DataPoint(double[] data) {
        this.data = data;
    }

    /**
     * Returns the underlying data vector.
     *
     * @return the data vector
     */
    public double[] getVector() {
        return data;
    }

    /**
     * Get the value at a specified location in this vector.
     *
     * @param index the index for the data of interest
     * @return the double value at that location
     */
    public double get(int index) {
        return data[index];
    }

    /**
     * Set the data vector.
     *
     * @param data the new data
     */
    public void setData(double[] data) {
        this.data = data;
    }

    /**
     * Returns the number of components of the data vector.
     *
     * @return the dimensionalit of the space this point lives in.
     */
    public int getDimension() {
        return data.length;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            builder.append(data[i]);
            if (i < data.length - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

}