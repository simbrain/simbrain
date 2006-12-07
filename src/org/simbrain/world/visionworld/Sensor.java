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

import java.awt.Image;

/**
 * Sensor.
 */
public final class Sensor {

    /** Filter for this sensor. */
    private final Filter filter;

    /** Receptive field for this sensor. */
    private final ReceptiveField receptiveField;


    /**
     * Create a new sensor with the specified filter and receptive field.
     *
     * @param filter filter for this sensor, must not be null
     * @param receptiveField receptive field for this sensor, must not be null
     */
    public Sensor(final Filter filter, final ReceptiveField receptiveField) {
        if (filter == null) {
            throw new IllegalArgumentException("filter must not be null");
        }
        if (receptiveField == null) {
            throw new IllegalArgumentException("receptiveField must not be null");
        }
        this.filter = filter;
        this.receptiveField = receptiveField;
    }


    /**
     * Sample the specified pixel matrix, reducing a view of the pixel matrix
     * through the receptive field of this sensor to a single numerical value
     * with the filter for this sensor.
     *
     * @see #getFilter
     * @see #getReceptiveField
     * @param pixelMatrix pixel matrix, must not be null
     * @return a view of the specified pixel matrix through the receptive field
     *    of this sensor reduced to a single numerical value
     */
    public double sample(final PixelMatrix pixelMatrix) {
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        Image image = pixelMatrix.view(receptiveField);
        return filter.filter(image);
    }

    /**
     * Return the filter for this sensor.
     * The filter will not be null.
     *
     * @return the filter for this sensor
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Return the receptive field for this sensor.
     * The receptive field will not be null.
     *
     * @return the receptive field for this sensor
     */
    public ReceptiveField getReceptiveField() {
        return receptiveField;
    }

    // todo:  couplings
}
