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
 * Vision world model.
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
     * Set the pixel matrix for this vision world model to <code>pixelMatrix</code>.
     *
     * @param pixelMatrix pixel matrix for this vision world model, must not be null
     */
    void setPixelMatrix(PixelMatrix pixelMatrix);

    /**
     * Return the number of sensor matrices in this vision world model.
     *
     * @return the number of sensor matrices in this vision world model
     */
    int getSensorMatrixCount();

    /**
     * Return an unmodifiable list of sensor matrices in this vision world model.
     * The list may be empty but will not be null.
     *
     * @return an unmodifiable list of sensor matrices in this vision world model
     */
    List<SensorMatrix> getSensorMatrices();

    // todo:  set, add, remove, indexOf
    // todo:  selected or active sensor matrix

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
