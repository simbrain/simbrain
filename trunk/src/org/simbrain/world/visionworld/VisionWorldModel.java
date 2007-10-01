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

import java.util.List;

/**
 * A vision world model has exactly one pixel matrix and exactly
 * one sensor matrix.  Interested classes may receive notification
 * of changes in a VisionWorldModel via the VisionWorldModelListener interface.
 *
 * @see VisionWorldModelListener
 */
public interface VisionWorldModel {

    /**
     * Return the pixel matrix for this vision world model.
     * The pixel matrix will not be null.
     *
     * @return the pixel matrix for this vision world model
     */
    PixelMatrix getPixelMatrix();

    /**
     * Set the pixel matrix for this vision world model to <code>pixelMatrix</code>
     * (optional operation).
     *
     * @param pixelMatrix pixel matrix for this vision world model, must not be null
     * @throws UnsupportedOperationException if the <code>setPixelMatrix</code>
     *    operation is not supported by this vision world model
     */
    void setPixelMatrix(PixelMatrix pixelMatrix);

    /**
     * Return the sensor matrix for this vision world model.
     * The sensor matrix will not be null.
     *
     * @return the sensor matrix for this vision world model
     */
    SensorMatrix getSensorMatrix();

    /**
     * Set the sensor matrix for this vision world model to <code>sensorMatrix</code>
     * (optional operation).
     *
     * @param sensorMatrix sensor matrix for this vision world model, must not be null
     * @throws UnsupportedOperationException if the <code>setSensorMatrix</code>
     *    operation is not supported by this vision world model
     */
    void setSensorMatrix(SensorMatrix sensorMatrix);

    /**
     * Add the specified vision world model listener.
     *
     * @param listener vision world model listener to add
     */
    void addModelListener(VisionWorldModelListener listener);

    /**
     * Remove the specified vision world model listener.
     *
     * @param listener vision world model listener to remove
     */
    void removeModelListener(VisionWorldModelListener listener);
}
