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

import java.util.EventObject;

/**
 * Vision world model event.
 */
public final class VisionWorldModelEvent
    extends EventObject {

    /** Old pixel matrix. */
    private final PixelMatrix oldPixelMatrix;

    /** Pixel matrix. */
    private final PixelMatrix pixelMatrix;

    /** Sensor matrix added or removed. */
    private final SensorMatrix sensorMatrix;


    /**
     * Create a new vision world model event with the specified event source.
     *
     * @param source source of this event, must not be null
     * @param oldPixelMatrix old pixel matrix, must not be null
     * @param pixelMatrix pixel matrix, must not be null
     */
    public VisionWorldModelEvent(final VisionWorldModel source,
                                 final PixelMatrix oldPixelMatrix,
                                 final PixelMatrix pixelMatrix) {
        super(source);
        if (oldPixelMatrix == null) {
            throw new IllegalArgumentException("oldPixelMatrix must not be null");
        }
        if (pixelMatrix == null) {
            throw new IllegalArgumentException("pixelMatrix must not be null");
        }
        this.oldPixelMatrix = oldPixelMatrix;
        this.pixelMatrix = pixelMatrix;
        this.sensorMatrix = null;
    }

    /**
     * Create a new vision world model event with the specified event source.
     *
     * @param source source of this event, must not be null
     * @param sensorMatrix sensor matrix added or removed, must not be null
     */
    public VisionWorldModelEvent(final VisionWorldModel source,
                                 final SensorMatrix sensorMatrix) {
        super(source);
        if (sensorMatrix == null) {
            throw new IllegalArgumentException("sensorMatrix must not be null");
        }
        this.oldPixelMatrix = null;
        this.pixelMatrix = null;
        this.sensorMatrix = sensorMatrix;
    }


    /**
     * Return the source of this event as a vision world model.
     * The vision world model will not be null.
     *
     * @return the source of this event as a vision world model
     */
    public VisionWorldModel getVisionWorldModel() {
        return (VisionWorldModel) super.getSource();
    }

    /**
     * Return the old pixel matrix for this vision world model event, if any.
     *
     * @return the old pixel matrix for this vision world model event, if any
     */
    public PixelMatrix getOldPixelMatrix() {
        return oldPixelMatrix;
    }

    /**
     * Return the pixel matrix for this vision world model event, if any.
     *
     * @return the pixel matrix for this vision world model event, if any
     */
    public PixelMatrix getPixelMatrix() {
        return pixelMatrix;
    }

    /**
     * Return the added or removed sensor matrix for this vision world
     * model event, if any.
     *
     * @return the added or removed sensor matrix for this vision world
     *    model event, if any
     */
    public SensorMatrix getSensorMatrix() {
        return sensorMatrix;
    }
}
