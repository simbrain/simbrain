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

import java.util.Collections;
import java.util.List;

/**
 * Immutable implementation of VisionWorldModel.
 */
public final class ImmutableVisionWorldModel
    extends AbstractVisionWorldModel {

    /** Pixel matrix. */
    private final PixelMatrix pixelMatrix;

    /** Sensor matrix. */
    private final SensorMatrix sensorMatrix;


    /**
     * Create a new immutable vision world model with the specified
     * pixel matrix and sensor matrix.
     *
     * @param pixelMatrix pixel matrix for this immutable vision world model,
     *    must not be null
     * @param sensorMatrix sensor matrix for this immutable vision world model,
     *    must not be null
     */
    public ImmutableVisionWorldModel(final PixelMatrix pixelMatrix,
                                     final SensorMatrix sensorMatrix) {
        super();
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        this.pixelMatrix = pixelMatrix;
        this.sensorMatrix = sensorMatrix;
    }


    /** {@inheritDoc} */
    public PixelMatrix getPixelMatrix() {
        return pixelMatrix;
    }

    /** {@inheritDoc} */
    public void setPixelMatrix(final PixelMatrix pixelMatrix) {
        throw new UnsupportedOperationException("setPixelMatrix operation not supported");
    }

    /** {@inheritDoc} */
    public int getSensorMatrixCount() {
        return 1;
    }

    /** {@inheritDoc} */
    public void addSensorMatrix(final SensorMatrix sensorMatrix) {
        throw new UnsupportedOperationException("addSensorMatrix operation not supported");
    }

    /** {@inheritDoc} */
    public void removeSensorMatrix(final SensorMatrix sensorMatrix) {
        throw new UnsupportedOperationException("removeSensorMatrix operation not supported");
    }

    /** {@inheritDoc} */
    public List<SensorMatrix> getSensorMatrices() {
        return Collections.unmodifiableList(Collections.singletonList(sensorMatrix));
    }
}
